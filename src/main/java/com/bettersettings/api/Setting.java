/*
 * MIT License
 *
 * Copyright (c) 2024 BetterSettings
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.bettersettings.api;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Represents a toggleable player setting that can be registered with BetterSettings.
 * <p>
 * Settings are displayed in the settings GUI and can be toggled on/off by players.
 * Third-party plugins can implement this interface to create custom settings that
 * integrate seamlessly with the BetterSettings system.
 * </p>
 * <p>
 * Implementations must be thread-safe as methods may be called from different threads
 * in Folia's regionized environment.
 * </p>
 *
 * @since 1.0.0
 */
public interface Setting {

    /**
     * Returns the unique identifier for this setting.
     * <p>
     * The ID should be namespaced to prevent conflicts with other plugins.
     * For example: "myplugin_mysetting" or "bettersettings_chat".
     * </p>
     * <p>
     * This ID is used for:
     * <ul>
     *   <li>Storing the setting state in player data files</li>
     *   <li>Looking up the setting from the registry</li>
     *   <li>Identifying the setting in click events</li>
     * </ul>
     * </p>
     *
     * @return the unique setting identifier, must not be null or empty
     */
    String getId();

    /**
     * Returns the human-readable description of this setting.
     * <p>
     * This description is displayed as the item name in the settings GUI.
     * It should clearly explain what the setting does.
     * </p>
     * <p>
     * Color codes using the § symbol or MiniMessage format are supported.
     * </p>
     *
     * @return the setting description, must not be null
     */
    String getDescription();

    /**
     * Returns the icon to display for this setting in the GUI.
     * <p>
     * The icon can be customized based on the player viewing it and the current
     * state of the setting. The BetterSettings plugin will automatically add
     * enchantment glows and state lore to the returned ItemStack.
     * </p>
     * <p>
     * This method may be called from different threads, so implementations should
     * be thread-safe and avoid modifying shared state.
     * </p>
     *
     * @param player the player viewing the setting
     * @param state the current state of the setting (true = enabled, false = disabled)
     * @return the ItemStack to display, must not be null
     */
    ItemStack getIcon(Player player, boolean state);

    /**
     * Returns the default state for this setting.
     * <p>
     * This value is used when a player has no saved preference for this setting,
     * such as when they first join the server or when the setting is newly registered.
     * </p>
     *
     * @return true if the setting should be enabled by default, false otherwise
     */
    boolean getDefaultState();

    /**
     * Returns the permission node required to view and toggle this setting.
     * <p>
     * If this method returns null, the setting will be available to all players.
     * If a permission is specified, only players with that permission will see
     * the setting in the GUI and be able to toggle it.
     * </p>
     * <p>
     * Permission checks are performed both when displaying the GUI and when
     * processing toggle actions.
     * </p>
     *
     * @return the permission node, or null if no permission is required
     */
    String getPermission();
    
    /**
     * Returns the category this setting belongs to.
     * <p>
     * Categories help organize settings in the GUI. If null, the setting
     * will be placed in the "Uncategorized" category.
     * </p>
     *
     * @return the category, or null for uncategorized
     */
    default SettingCategory getCategory() {
        return null;
    }
    
    /**
     * Returns the sort priority within the category.
     * <p>
     * Lower values appear first. Default is 100.
     * </p>
     *
     * @return the priority (lower = first)
     */
    default int getPriority() {
        return 100;
    }

    /**
     * Called when a player toggles this setting.
     * <p>
     * This method is executed on the player's region scheduler in Folia, making it
     * safe to perform player-specific operations like modifying their scoreboard,
     * hiding/showing other players, or sending messages.
     * </p>
     * <p>
     * The setting state has already been updated in the cache before this method
     * is called, so you can focus on applying the effects of the toggle.
     * </p>
     * <p>
     * If this method returns false, the toggle will be cancelled and the setting
     * state will be reverted to its previous value.
     * </p>
     *
     * @param player the player who toggled the setting
     * @param newState the new state of the setting (true = enabled, false = disabled)
     * @return true to allow the toggle, false to cancel it
     */
    boolean onToggle(Player player, boolean newState);
}
