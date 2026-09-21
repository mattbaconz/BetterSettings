package com.bettersettings.settings;

/** A validated, owner-configured action executed after a value is committed. */
public record SettingAction(Type type, String value, String when) {
    public SettingAction(Type type, String value) {
        this(type, value, "any");
    }
    public enum Type {
        MESSAGE,
        PLAYER_COMMAND,
        CONSOLE_COMMAND
    }
}
