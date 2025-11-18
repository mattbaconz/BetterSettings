package com.bettersettings.api;

import org.bukkit.Material;
import java.util.List;
import java.util.ArrayList;

/**
 * Represents a category for grouping related settings.
 * Categories help organize settings in the GUI and provide better navigation.
 */
public class SettingCategory {
    
    private final String id;
    private final String name;
    private final Material icon;
    private final int priority;
    private final String permission;
    private final List<String> description;
    
    /**
     * Creates a new setting category.
     * 
     * @param id Unique identifier for the category
     * @param name Display name shown in GUI
     * @param icon Material icon for the category
     * @param priority Sort priority (lower = first)
     * @param permission Permission required to view (null = no permission)
     */
    public SettingCategory(String id, String name, Material icon, int priority, String permission) {
        this(id, name, icon, priority, permission, new ArrayList<>());
    }
    
    /**
     * Creates a new setting category with description.
     * 
     * @param id Unique identifier for the category
     * @param name Display name shown in GUI
     * @param icon Material icon for the category
     * @param priority Sort priority (lower = first)
     * @param permission Permission required to view (null = no permission)
     * @param description Description lines for the category
     */
    public SettingCategory(String id, String name, Material icon, int priority, String permission, List<String> description) {
        this.id = id;
        this.name = name;
        this.icon = icon;
        this.priority = priority;
        this.permission = permission;
        this.description = description != null ? new ArrayList<>(description) : new ArrayList<>();
    }
    
    /**
     * Gets the category ID.
     * 
     * @return category ID
     */
    public String getId() {
        return id;
    }
    
    /**
     * Gets the display name.
     * 
     * @return display name
     */
    public String getName() {
        return name;
    }
    
    /**
     * Gets the icon material.
     * 
     * @return icon material
     */
    public Material getIcon() {
        return icon;
    }
    
    /**
     * Gets the priority.
     * 
     * @return priority (lower = first)
     */
    public int getPriority() {
        return priority;
    }
    
    /**
     * Gets the permission required.
     * 
     * @return permission or null
     */
    public String getPermission() {
        return permission;
    }
    
    /**
     * Gets the description lines.
     * 
     * @return description lines
     */
    public List<String> getDescription() {
        return new ArrayList<>(description);
    }
}
