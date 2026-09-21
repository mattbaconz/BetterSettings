package com.bettersettings.settings;

import com.bettersettings.api.SettingOption;
import com.bettersettings.api.SettingType;
import org.bukkit.Material;

import java.util.List;

/** Immutable definition loaded from packaged defaults, a preset, or owner YAML. */
public record SettingDefinition(
    String id,
    String description,
    Material material,
    SettingType type,
    String defaultValue,
    List<SettingOption> options,
    String permission,
    String categoryId,
    int priority,
    String behaviorId,
    List<SettingAction> actions,
    String source
) {
    public SettingDefinition {
        options = List.copyOf(options);
        actions = List.copyOf(actions);
    }
}
