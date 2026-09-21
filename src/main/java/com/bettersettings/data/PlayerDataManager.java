package com.bettersettings.data;

import com.bettersettings.BetterSettings;
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
public final class PlayerDataManager {

    private final BetterSettings plugin;
    private final File dataFolder;
    private final ConcurrentHashMap<UUID, ConcurrentHashMap<String, String>> cache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Long> lastAccess = new ConcurrentHashMap<>();
    private final Semaphore ioSemaphore;
    private final AtomicInteger pendingOperations = new AtomicInteger();

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
        dispatchIo(() -> {
            File file = playerFile(uuid);
            Map<String, String> data = Map.of();
            if (file.exists()) {
                try {
                    data = PlayerValueCodec.read(YamlConfiguration.loadConfiguration(file));
                } catch (RuntimeException exception) {
                    plugin.getLogger().log(Level.WARNING, "Failed to load data for player " + uuid, exception);
                }
            }
            Map<String, String> loaded = data;
            cache.compute(uuid, (ignored, current) -> {
                ConcurrentHashMap<String, String> merged = new ConcurrentHashMap<>(loaded);
                if (current != null) merged.putAll(current);
                return merged;
            });
            lastAccess.put(uuid, System.currentTimeMillis());
        });
    }

    public void saveData(UUID uuid) {
        if (!plugin.getConfigManager().getPerformanceConfig().getBoolean("auto-save.save-on-quit", true)) return;
        Map<String, String> snapshot = snapshot(uuid);
        if (!snapshot.isEmpty()) dispatchIo(() -> write(uuid, snapshot));
    }

    public void saveAllData() {
        Map<UUID, Map<String, String>> snapshot = snapshotAll();
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

        Map<UUID, Map<String, String>> snapshot = snapshotAll();
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

    private boolean saveSnapshot(Map<UUID, Map<String, String>> snapshot) {
        int saved = 0;
        for (Map.Entry<UUID, Map<String, String>> entry : snapshot.entrySet()) {
            if (write(entry.getKey(), entry.getValue())) saved++;
        }
        if (plugin.getConfigManager().getPerformanceConfig().getBoolean("logging.debug", false)) {
            plugin.getLogger().info("Saved " + saved + " player data files");
        }
        return saved == snapshot.size();
    }

    private boolean write(UUID uuid, Map<String, String> values) {
        File target = playerFile(uuid);
        File temporary = new File(dataFolder, uuid + ".tmp");
        try {
            PlayerValueCodec.write(values).save(temporary);
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

    private Map<String, String> snapshot(UUID uuid) {
        Map<String, String> values = cache.get(uuid);
        return values == null ? Map.of() : Map.copyOf(values);
    }

    private Map<UUID, Map<String, String>> snapshotAll() {
        Map<UUID, Map<String, String>> snapshot = new HashMap<>();
        cache.forEach((uuid, values) -> {
            if (!values.isEmpty()) snapshot.put(uuid, Map.copyOf(values));
        });
        return snapshot;
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
            lastAccess.remove(uuid);
        });
    }

    /** Existing boolean accessor retained unchanged. */
    public boolean getSetting(UUID uuid, String settingId, boolean defaultValue) {
        return Boolean.parseBoolean(getSettingValue(uuid, settingId, Boolean.toString(defaultValue)));
    }

    /** Existing boolean mutator retained unchanged. */
    public void setSetting(UUID uuid, String settingId, boolean value) {
        setSettingValue(uuid, settingId, Boolean.toString(value));
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

    public void setSettingValue(UUID uuid, String settingId, String value) {
        if (uuid == null || settingId == null || value == null) return;
        cache.computeIfAbsent(uuid, ignored -> new ConcurrentHashMap<>()).put(settingId, value);
        lastAccess.put(uuid, System.currentTimeMillis());
    }

    public int getPendingOperations() { return pendingOperations.get(); }
    public int getCacheSize() { return cache.size(); }
}
