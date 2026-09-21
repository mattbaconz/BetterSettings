package com.bettersettings.api;

import java.util.List;

/** Deterministic value navigation shared by the GUI and API consumers. */
public final class SettingValues {
    private SettingValues() {}

    public static String next(String current, List<SettingOption> options) { return move(current, options, 1); }
    public static String previous(String current, List<SettingOption> options) { return move(current, options, -1); }

    private static String move(String current, List<SettingOption> options, int direction) {
        if (options == null || options.isEmpty()) return current;
        int index = -1;
        for (int i = 0; i < options.size(); i++) {
            if (options.get(i).value().equals(current)) { index = i; break; }
        }
        if (index < 0) return options.getFirst().value();
        return options.get(Math.floorMod(index + direction, options.size())).value();
    }
}
