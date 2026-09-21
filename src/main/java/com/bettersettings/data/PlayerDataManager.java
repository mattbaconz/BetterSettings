package com.bettersettings.data;

import com.bettersettings.BetterSettings;
import com.bettersettings.api.SettingType;
import com.bettersettings.api.SettingsRegistry;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

/** Thread-safe typed player-value cache with atomic YAML persistence. */
public class PlayerDataManager {

    private final BetterSettings plugin;
    private final File dataFolder;
    private final ConcurrentHashMap<UUID, ConcurrentHashMap<String, String>> cache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Set<String>> persistedBooleanIds = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Long> lastAccess = new ConcurrentHashMap<>();
    private final Semaphore ioSemaphore;
    private final AtomicInteger pendingOperations = new AtomicInteger();
    private final PlayerSaveCoordinator saveCoordinator = new PlayerSaveCoordinator();

    public PlayerDataManager(BetterSettings plugin) {
        this.plugin = plugin;
        dataFolder = new File(plugin.getDataFolder(), "playerdata");
        int maxConcurrent = Math.max(1,
            plugin.getConfigManager().getPerformanceConfig().getInt("io.max-concurrent", 4));
        ioSemaphore = new Semaphore(maxConcurrent);
        if (!dataFolder.exists() && !dataFolder.mkdirs()) {
            plugin.getLogger().warning("Failed to create playerdata directory");
        }
    }

    public void loadData(UUID uuid) {
        if (!plugin.getConfigManager().getPerformanceConfig().getBoolean("auto-save.load-on-join", true)) return;
        dispatchIo(() -> loadIntoCache(uuid));
    }

    /** Loads during Paper's asynchronous pre-login phase so join handlers see persisted values. */
    public void loadDataNow(UUID uuid) {
        if (!plugin.getConfigManager().getPerformanceConfig().getBoolean("auto-save.load-on-join", true)) return;
        runIo(() -> loadIntoCache(uuid));
    }

    public void saveData(UUID uuid) {
        if (!plugin.getConfigManager().getPerformanceConfig().getBoolean("auto-save.save-on-quit", true)) return;
        SaveRequest request = snapshot(uuid);
        if (request != null) dispatchIo(() -> write(request));
    }

    public void saveAllData() {
        Map<UUID, SaveRequest> snapshot = snapshotAll();
        if (!snapshot.isEmpty()) dispatchIo(() -> saveSnapshot(snapshot));
    }

    /**
     * Performs one final snapshot flush and waits no longer than the requested bound.
     * This does not claim that command-side effects or another plugin's data are transactional.
     */
    public boolean saveAllDataAndWait(long timeoutMillis) {
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(Math.max(1L, timeoutMillis));
        try {
            while (pendingOperations.get() > 0) {
                long remaining = deadline - System.nanoTime();
                if (remaining <= 0L) return false;
                Thread.sleep(Math.min(10L, Math.max(1L, TimeUnit.NANOSECONDS.toMillis(remaining))));
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return false;
        }

        Map<UUID, SaveRequest> snapshot = snapshotAll();
        if (snapshot.isEmpty()) return true;
        AtomicBoolean saved = new AtomicBoolean(false);
        Thread worker = Thread.ofVirtual().name("BetterSettings-final-save")
            .start(() -> runIo(() -> saved.set(saveSnapshot(snapshot))));
        try {
            long remaining = deadline - System.nanoTime();
            if (remaining <= 0L) return false;
            worker.join(Math.max(1L, TimeUnit.NANOSECONDS.toMillis(remaining)));
            return !worker.isAlive() && saved.get();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private boolean saveSnapshot(Map<UUID, SaveRequest> snapshot) {
        int saved = 0;
        for (SaveRequest request : snapshot.values()) {
            if (write(request)) saved++;
        }
        if (plugin.getConfigManager().getPerformanceConfig().getBoolean("logging.debug", false)) {
            plugin.getLogger().info("Saved " + saved + " player data files");
        }
        return saved == snapshot.size();
    }

    private boolean write(SaveRequest request) {
        return saveCoordinator.execute(request.ticket(), () -> writeFile(request));
    }

    private boolean writeFile(SaveRequest request) {
        UUID uuid = request.ticket().uuid();
        File target = playerFile(uuid);
        File temporary = new File(dataFolder, uuid + "." + request.ticket().generation() + ".tmp");
        try {
            PlayerValueCodec.write(request.values(), request.booleanIds()).save(temporary);
            try {
                Files.move(temporary.toPath(), target.toPath(),
                    StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporary.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            return true;
        } catch (IOException exception) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save data for player " + uuid, exception);
            if (temporary.exists() && !temporary.delete()) {
                plugin.getLogger().warning("Failed to remove temporary player data file " + temporary.getName());
            }
            return false;
        }
    }

    private void dispatchIo(Runnable operation) {
        boolean async = plugin.getConfigManager().getPerformanceConfig().getBoolean("io.async", true);
        if (async) {
            pendingOperations.incrementAndGet();
            Bukkit.getAsyncScheduler().runNow(plugin, task -> {
                try {
                    runIo(operation);
                } finally {
                    pendingOperations.decrementAndGet();
                }
            });
        } else {
            runIo(operation);
        }
    }

    private void runIo(Runnable operation) {
        boolean acquired = false;
        try {
            ioSemaphore.acquire();
            acquired = true;
            operation.run();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            plugin.getLogger().log(Level.WARNING, "Player data I/O interrupted", exception);
        } finally {
            if (acquired) ioSemaphore.release();
        }
    }

    private void loadIntoCache(UUID uuid) {
        File file = playerFile(uuid);
        Map<String, String> data = Map.of();
        Set<String> booleanIds = Set.of();
        if (file.exists()) {
            try {
                YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
                data = PlayerValueCodec.read(yaml);
                booleanIds = PlayerValueCodec.readBooleanIds(yaml);
            } catch (RuntimeException exception) {
                plugin.getLogger().log(Level.WARNING, "Failed to load data for player " + uuid, exception);
            }
        }
        Map<String, String> loaded = data;
        Set<String> loadedBooleanIds = booleanIds;
        SettingTypes types = currentSettingTypes();
        cache.compute(uuid, (ignored, current) -> {
            ConcurrentHashMap<String, String> merged = new ConcurrentHashMap<>(loaded);
            if (current != null) merged.putAll(current);
            Set<String> knownBooleanIds = persistedBooleanIds.computeIfAbsent(
                uuid, key -> ConcurrentHashMap.newKeySet());
            for (String id : loadedBooleanIds) {
                if (current == null || !current.containsKey(id)) knownBooleanIds.add(id);
            }
            knownBooleanIds.removeAll(types.choiceIds());
            return merged;
        });
        lastAccess.put(uuid, System.currentTimeMillis());
    }

    private SaveRequest snapshot(UUID uuid) {
        Map<String, String> values = cache.get(uuid);
        if (values == null || values.isEmpty()) return null;
        SettingTypes types = currentSettingTypes();
        return new SaveRequest(saveCoordinator.reserve(uuid), Map.copyOf(values),
            booleanIdsForSave(uuid, types));
    }

    private Map<UUID, SaveRequest> snapshotAll() {
        Map<UUID, SaveRequest> snapshot = new HashMap<>();
        SettingTypes types = currentSettingTypes();
        cache.forEach((uuid, values) -> {
            if (!values.isEmpty()) {
                snapshot.put(uuid, new SaveRequest(
                    saveCoordinator.reserve(uuid), Map.copyOf(values), booleanIdsForSave(uuid, types)));
            }
        });
        return snapshot;
    }

    private SettingTypes currentSettingTypes() {
        Set<String> toggleIds = new java.util.HashSet<>();
        Set<String> choiceIds = new java.util.HashSet<>();
        SettingsRegistry.getInstance().getValueSettings().forEach(setting -> {
            if (setting.getType() == SettingType.TOGGLE) toggleIds.add(setting.getId());
            else choiceIds.add(setting.getId());
        });
        return new SettingTypes(Set.copyOf(toggleIds), Set.copyOf(choiceIds));
    }

    private Set<String> booleanIdsForSave(UUID uuid, SettingTypes types) {
        return PlayerValueCodec.booleanIdsForSave(
            persistedBooleanIds.getOrDefault(uuid, Set.of()), types.toggleIds(), types.choiceIds());
    }

    private File playerFile(UUID uuid) {
        return new File(dataFolder, uuid + ".yml");
    }

    public void cleanupCache() {
        long now = System.currentTimeMillis();
        long maxAge = plugin.getConfigManager().getPerformanceConfig().getLong("cache.max-age", 300_000L);
        List<UUID> remove = new ArrayList<>();
        lastAccess.forEach((uuid, accessed) -> {
            org.bukkit.entity.Player player = Bukkit.getPlayer(uuid);
            if ((player == null || !player.isOnline()) && now - accessed > maxAge) remove.add(uuid);
        });
        remove.forEach(uuid -> {
            cache.remove(uuid);
            persistedBooleanIds.remove(uuid);
            lastAccess.remove(uuid);
        });
    }

    /** Existing boolean accessor retained unchanged. */
    public boolean getSetting(UUID uuid, String settingId, boolean defaultValue) {
        return Boolean.parseBoolean(getSettingValue(uuid, settingId, Boolean.toString(defaultValue)));
    }

    /** Existing boolean mutator retained unchanged. */
    public void setSetting(UUID uuid, String settingId, boolean value) {
        setSettingValue(uuid, settingId, Boolean.toString(value), SettingType.TOGGLE);
    }

    public String getSettingValue(UUID uuid, String settingId, String defaultValue) {
        if (uuid == null || settingId == null) return defaultValue;
        Map<String, String> playerData = cache.get(uuid);
        if (playerData == null) return defaultValue;
        lastAccess.put(uuid, System.currentTimeMillis());
        return playerData.getOrDefault(settingId, defaultValue);
    }

    public String getChoice(UUID uuid, String settingId, String defaultValue, Set<String> allowedValues) {
        String stored = getSettingValue(uuid, settingId, defaultValue);
        return PlayerValueCodec.resolveChoice(stored, defaultValue, allowedValues);
    }

    public boolean hasSettingValue(UUID uuid, String settingId) {
        Map<String, String> playerData = cache.get(uuid);
        return playerData != null && playerData.containsKey(settingId);
    }

    public void setSettingValue(UUID uuid, String settingId, String value) {
        com.bettersettings.api.ValueSetting setting = SettingsRegistry.getInstance().getValueSetting(settingId);
        setSettingValue(uuid, settingId, value, setting == null ? null : setting.getType());
    }

    private void setSettingValue(UUID uuid, String settingId, String value, SettingType type) {
        if (uuid == null || settingId == null || value == null) return;
        cache.compute(uuid, (ignored, current) -> {
            ConcurrentHashMap<String, String> values = current == null
                ? new ConcurrentHashMap<>() : current;
            values.put(settingId, value);
            Set<String> booleanIds = persistedBooleanIds.computeIfAbsent(
                uuid, key -> ConcurrentHashMap.newKeySet());
            if (type == SettingType.TOGGLE) booleanIds.add(settingId);
            else if (type == SettingType.CHOICE) booleanIds.remove(settingId);
            return values;
        });
        lastAccess.put(uuid, System.currentTimeMillis());
    }

    public int getPendingOperations() { return pendingOperations.get(); }
    public int getCacheSize() { return cache.size(); }

    private record SaveRequest(PlayerSaveCoordinator.Ticket ticket, Map<String, String> values,
                               Set<String> booleanIds) {}
    private record SettingTypes(Set<String> toggleIds, Set<String> choiceIds) {}
}
