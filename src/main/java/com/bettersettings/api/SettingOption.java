package com.bettersettings.api;

import java.util.Objects;

/** A persisted choice value and the label shown to players. */
public record SettingOption(String value, String displayName) {
    public SettingOption {
        value = Objects.requireNonNull(value, "value").trim();
        displayName = Objects.requireNonNull(displayName, "displayName");
        if (value.isEmpty()) {
            throw new IllegalArgumentException("Setting option value cannot be empty");
        }
    }
}
