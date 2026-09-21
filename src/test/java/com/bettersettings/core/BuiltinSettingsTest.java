package com.bettersettings.core;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BuiltinSettingsTest {

    @Test
    void everyStatefulNativeSettingIsIncludedInJoinRestoration() {
        assertEquals(Set.of(
            "bettersettings_scoreboard",
            "bettersettings_visibility",
            "bettersettings_weather",
            "bettersettings_time",
            "bettersettings_nightvision",
            "bettersettings_flight",
            "bettersettings_godmode",
            "bettersettings_speed",
            "bettersettings_jump",
            "bettersettings_waterbreathing",
            "bettersettings_fireresistance",
            "bettersettings_collision",
            "bettersettings_vanish",
            "bettersettings_tablist"
        ), BuiltinSettings.restorableSettingIds());
    }

    @Test
    void existingViewerHidesANewArrivalOnlyWhenStoredVisibilityIsDisabledAndPermitted() {
        assertTrue(BuiltinSettings.shouldHideNewArrival(false, true));
        assertFalse(BuiltinSettings.shouldHideNewArrival(true, true));
        assertFalse(BuiltinSettings.shouldHideNewArrival(false, false));
    }
}
