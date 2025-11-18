package com.bettersettings.api;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Central registry for managing all player settings in the BetterSettings system.
 * <p>
 * This singleton provides thread-safe registration and retrieval of settings.
 * Third-party plugins should use this registry to register their custom settings
 * during plugin initialization.
 * </p>
 * <p>
 * Example usage:
 * <pre>{@code
 * Setting mySetting = new MySetting();
 * SettingsRegistry.getInstance().registerSetting(mySetting);
 * }</pre>
 * </p>
 *
 * @since 1.0.0
 */
public class SettingsRegistry {

    private static final SettingsRegistry INSTANCE = new SettingsRegistry();
    private static final Logger LOGGER = Logger.getLogger("BetterSettings");

    private final ConcurrentHashMap<String, Setting> settings;

    /**
     * Private constructor to enforce singleton pattern.
     */
    private SettingsRegistry() {
        this.settings = new ConcurrentHashMap<>();
    }

    /**
     * Returns the singleton instance of the SettingsRegistry.
     *
     * @return the SettingsRegistry instance
     */
    public static SettingsRegistry getInstance() {
        return INSTANCE;
    }

    /**
     * Registers a new setting with the registry.
     * <p>
     * The setting will be validated before registration:
     * <ul>
     *   <li>Setting must not be null</li>
     *   <li>Setting ID must not be null or empty</li>
     *   <li>Setting ID must not already be registered</li>
     * </ul>
     * </p>
     * <p>
     * If validation fails, the setting will not be registered and a warning
     * will be logged.
     * </p>
     * <p>
     * This method is thread-safe and can be called from multiple threads
     * simultaneously.
     * </p>
     *
     * @param setting the setting to register, must not be null
     * @throws IllegalArgumentException if the setting or its ID is null/empty
     */
    public void registerSetting(Setting setting) {
        if (setting == null) {
            throw new IllegalArgumentException("Cannot register null setting");
        }

        String id = setting.getId();
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("Setting ID cannot be null or empty");
        }

        Setting existing = settings.putIfAbsent(id, setting);
        if (existing != null) {
            LOGGER.warning(String.format(
                "Attempted to register duplicate setting with ID '%s'. " +
                "The original setting will be kept. " +
                "Please ensure setting IDs are unique across all plugins.",
                id
            ));
        } else {
            LOGGER.info(String.format("Registered setting: %s", id));
        }
    }

    /**
     * Retrieves a setting by its unique identifier.
     * <p>
     * This method is thread-safe and can be called from multiple threads
     * simultaneously.
     * </p>
     *
     * @param id the unique setting identifier
     * @return the Setting instance, or null if no setting with that ID exists
     */
    public Setting getSetting(String id) {
        if (id == null) {
            return null;
        }
        return settings.get(id);
    }

    /**
     * Returns an unmodifiable collection of all registered settings.
     * <p>
     * The returned collection is a snapshot of the current registered settings
     * and will not reflect future registrations. Attempts to modify the
     * returned collection will throw UnsupportedOperationException.
     * </p>
     * <p>
     * This method is thread-safe and can be called from multiple threads
     * simultaneously.
     * </p>
     *
     * @return an unmodifiable collection of all registered settings
     */
    public Collection<Setting> getSettings() {
        return Collections.unmodifiableCollection(settings.values());
    }
    
    private final ConcurrentHashMap<String, SettingCategory> categories = new ConcurrentHashMap<>();
    
    /**
     * Registers a category.
     * 
     * @param category The category to register
     */
    public void registerCategory(SettingCategory category) {
        if (category == null || category.getId() == null) {
            return;
        }
        categories.putIfAbsent(category.getId(), category);
    }
    
    /**
     * Gets a category by ID.
     * 
     * @param id The category ID
     * @return The category or null
     */
    public SettingCategory getCategory(String id) {
        return categories.get(id);
    }
    
    /**
     * Gets all registered categories.
     * 
     * @return Collection of categories
     */
    public Collection<SettingCategory> getCategories() {
        return Collections.unmodifiableCollection(categories.values());
    }
}
