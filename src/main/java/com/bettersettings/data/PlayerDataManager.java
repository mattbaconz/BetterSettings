package com.bettersettings.data;

import com.bettersettings.BetterSettings;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

/**
 * Manages player setting data persistence with thread-safe caching and async I/O operations.
 * Uses Folia-compatible schedulers for all operations.
 * Optimized for performance with batching, semaphores, and efficient file I/O.
 * Uses YAML for lightweight data storage (no Gson dependency).
 */
public class PlayerDataManager {
    
    private final BetterSettings plugin;
    private final File dataFolder;
    private final ConcurrentHashMap<UUID, ConcurrentHashMap<String, Boolean>> cache;
    private final ConcurrentHashMap<UUID, Long> lastAccess;
    private final Semaphore ioSemaphore;
    private final AtomicInteger pendingOperations;
    
    /**
     * Creates a new PlayerDataManager instance.
     * 
     * @param plugin The plugin instance
     */
    public PlayerDataManager(BetterSettings plugin) {
        this.plugin = plugin;
        this.dataFolder = new File(plugin.getDataFolder(), "playerdata");
        this.cache = new ConcurrentHashMap<>();
        this.lastAccess = new ConcurrentHashMap<>();
        
        // Limit concurrent I/O operations for performance
        int maxConcurrent = plugin.getConfigManager().getPerformanceConfig().getInt("io.max-concurrent", 4);
        this.ioSemaphore = new Semaphore(maxConcurrent);
        this.pendingOperations = new AtomicInteger(0);
        
        // Create playerdata directory if it doesn't exist
        if (!dataFolder.exists()) {
            if (!dataFolder.mkdirs()) {
                plugin.getLogger().warning("Failed to create playerdata directory!");
            }
        }
    }

    /**
     * Loads player data asynchronously from disk.
     * Uses async scheduler for file I/O and player scheduler for cache update.
     * 
     * @param uuid The player's UUID
     */
    public void loadData(UUID uuid) {
        if (!plugin.getConfigManager().getPerformanceConfig().getBoolean("auto-save.load-on-join", true)) {
            return;
        }
        
        File playerFile = new File(dataFolder, uuid.toString() + ".yml");
        boolean useAsync = plugin.getConfigManager().getPerformanceConfig().getBoolean("io.async", true);
        
        Runnable loadTask = () -> {
            try {
                ioSemaphore.acquire();
                pendingOperations.incrementAndGet();
                
                Map<String, Boolean> data = new HashMap<>();
                
                // Read file if it exists
                if (playerFile.exists()) {
                    try {
                        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(playerFile);
                        for (String key : yaml.getKeys(false)) {
                            data.put(key, yaml.getBoolean(key));
                        }
                    } catch (Exception e) {
                        plugin.getLogger().log(Level.WARNING, 
                            "Failed to load data for player " + uuid, e);
                    }
                }
                
                // Update cache
                ConcurrentHashMap<String, Boolean> playerCache = new ConcurrentHashMap<>(data);
                cache.put(uuid, playerCache);
                lastAccess.put(uuid, System.currentTimeMillis());
                
                if (plugin.getConfigManager().getPerformanceConfig().getBoolean("logging.data-operations", false)) {
                    plugin.getLogger().info("Loaded data for player " + uuid);
                }
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                plugin.getLogger().log(Level.WARNING, "Load operation interrupted for " + uuid, e);
            } finally {
                ioSemaphore.release();
                pendingOperations.decrementAndGet();
            }
        };
        
        if (useAsync) {
            Bukkit.getAsyncScheduler().runNow(plugin, task -> loadTask.run());
        } else {
            loadTask.run();
        }
    }

    /**
     * Saves player data asynchronously to disk.
     * Uses async scheduler for file I/O with atomic write operation.
     * 
     * @param uuid The player's UUID
     */
    public void saveData(UUID uuid) {
        if (!plugin.getConfigManager().getPerformanceConfig().getBoolean("auto-save.save-on-quit", true)) {
            return;
        }
        
        // Get cached data
        ConcurrentHashMap<String, Boolean> playerData = cache.get(uuid);
        if (playerData == null || playerData.isEmpty()) {
            return; // No data to save
        }
        
        // Create a snapshot of the data for async operation
        final Map<String, Boolean> dataSnapshot = new HashMap<>(playerData);
        boolean useAsync = plugin.getConfigManager().getPerformanceConfig().getBoolean("io.async", true);
        
        Runnable saveTask = () -> {
            try {
                ioSemaphore.acquire();
                pendingOperations.incrementAndGet();
                
                File playerFile = new File(dataFolder, uuid.toString() + ".yml");
                File tempFile = new File(dataFolder, uuid.toString() + ".tmp");
                
                try {
                    // Write to temporary file first
                    YamlConfiguration yaml = new YamlConfiguration();
                    for (Map.Entry<String, Boolean> entry : dataSnapshot.entrySet()) {
                        yaml.set(entry.getKey(), entry.getValue());
                    }
                    yaml.save(tempFile);
                    
                    // Atomic move operation
                    Files.move(tempFile.toPath(), playerFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    
                    if (plugin.getConfigManager().getPerformanceConfig().getBoolean("logging.data-operations", false)) {
                        plugin.getLogger().info("Saved data for player " + uuid);
                    }
                    
                } catch (IOException e) {
                    plugin.getLogger().log(Level.SEVERE, 
                        "Failed to save data for player " + uuid, e);
                    // Clean up temp file if it exists
                    if (tempFile.exists()) {
                        tempFile.delete();
                    }
                }
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                plugin.getLogger().log(Level.WARNING, "Save operation interrupted for " + uuid, e);
            } finally {
                ioSemaphore.release();
                pendingOperations.decrementAndGet();
            }
        };
        
        if (useAsync) {
            Bukkit.getAsyncScheduler().runNow(plugin, task -> saveTask.run());
        } else {
            saveTask.run();
        }
    }
    
    /**
     * Saves all cached player data to disk.
     * Used during plugin shutdown and periodic auto-saves.
     */
    public void saveAllData() {
        if (!plugin.getConfigManager().getPerformanceConfig().getBoolean("io.batch-saves", true)) {
            // Save individually
            for (UUID uuid : cache.keySet()) {
                saveData(uuid);
            }
            return;
        }
        
        // Batch save operation
        List<UUID> uuidsToSave = new ArrayList<>(cache.keySet());
        if (uuidsToSave.isEmpty()) {
            return;
        }
        
        boolean useAsync = plugin.getConfigManager().getPerformanceConfig().getBoolean("io.async", true);
        
        Runnable batchSaveTask = () -> {
            int saved = 0;
            for (UUID uuid : uuidsToSave) {
                ConcurrentHashMap<String, Boolean> playerData = cache.get(uuid);
                if (playerData == null || playerData.isEmpty()) {
                    continue;
                }
                
                try {
                    ioSemaphore.acquire();
                    pendingOperations.incrementAndGet();
                    
                    Map<String, Boolean> dataSnapshot = new HashMap<>(playerData);
                    File playerFile = new File(dataFolder, uuid.toString() + ".yml");
                    File tempFile = new File(dataFolder, uuid.toString() + ".tmp");
                    
                    YamlConfiguration yaml = new YamlConfiguration();
                    for (Map.Entry<String, Boolean> entry : dataSnapshot.entrySet()) {
                        yaml.set(entry.getKey(), entry.getValue());
                    }
                    yaml.save(tempFile);
                    
                    Files.move(tempFile.toPath(), playerFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    saved++;
                    
                } catch (IOException e) {
                    plugin.getLogger().log(Level.WARNING, "Failed to save data for " + uuid, e);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } finally {
                    ioSemaphore.release();
                    pendingOperations.decrementAndGet();
                }
            }
            
            if (plugin.getConfigManager().getPerformanceConfig().getBoolean("logging.debug", false)) {
                plugin.getLogger().info("Batch saved " + saved + " player data files");
            }
        };
        
        if (useAsync) {
            Bukkit.getAsyncScheduler().runNow(plugin, task -> batchSaveTask.run());
        } else {
            batchSaveTask.run();
        }
    }
    
    /**
     * Cleans up cached data for offline players to save memory.
     */
    public void cleanupCache() {
        long currentTime = System.currentTimeMillis();
        long maxAge = plugin.getConfigManager().getPerformanceConfig().getLong("cache.max-age", 300000);
        
        List<UUID> toRemove = new ArrayList<>();
        for (Map.Entry<UUID, Long> entry : lastAccess.entrySet()) {
            UUID uuid = entry.getKey();
            long lastAccessTime = entry.getValue();
            
            // Check if player is offline and cache is old
            org.bukkit.entity.Player player = Bukkit.getPlayer(uuid);
            if (player == null || !player.isOnline()) {
                if (currentTime - lastAccessTime > maxAge) {
                    toRemove.add(uuid);
                }
            }
        }
        
        for (UUID uuid : toRemove) {
            cache.remove(uuid);
            lastAccess.remove(uuid);
        }
        
        if (!toRemove.isEmpty() && plugin.getConfigManager().getPerformanceConfig().getBoolean("logging.debug", false)) {
            plugin.getLogger().info("Cleaned up " + toRemove.size() + " cached player data entries");
        }
    }

    /**
     * Retrieves a setting state from cache with default fallback.
     * Thread-safe access to nested ConcurrentHashMaps.
     * 
     * @param uuid The player's UUID
     * @param settingId The setting ID
     * @param defaultValue The default value if setting is not found
     * @return The setting state
     */
    public boolean getSetting(UUID uuid, String settingId, boolean defaultValue) {
        if (uuid == null || settingId == null) {
            return defaultValue;
        }
        
        ConcurrentHashMap<String, Boolean> playerData = cache.get(uuid);
        if (playerData == null) {
            return defaultValue;
        }
        lastAccess.put(uuid, System.currentTimeMillis());
        return playerData.getOrDefault(settingId, defaultValue);
    }
    
    /**
     * Updates a setting state in cache.
     * Thread-safe access to nested ConcurrentHashMaps.
     * 
     * @param uuid The player's UUID
     * @param settingId The setting ID
     * @param value The new setting state
     */
    public void setSetting(UUID uuid, String settingId, boolean value) {
        if (uuid == null || settingId == null) {
            return;
        }
        
        cache.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>())
             .put(settingId, value);
        lastAccess.put(uuid, System.currentTimeMillis());
    }
    
    /**
     * Returns the number of pending I/O operations.
     * 
     * @return pending operation count
     */
    public int getPendingOperations() {
        return pendingOperations.get();
    }
    
    /**
     * Returns the number of cached player data entries.
     * 
     * @return cache size
     */
    public int getCacheSize() {
        return cache.size();
    }
}
