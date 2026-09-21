package com.bettersettings.api;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SettingValuesTest {

    private static final List<SettingOption> OPTIONS = List.of(
        new SettingOption("everyone", "Everyone"),
        new SettingOption("friends", "Friends"),
        new SettingOption("nobody", "Nobody")
    );

    @Test
    void cyclesChoicesInBothDirectionsAndRepairsInvalidValues() {
        assertEquals("friends", SettingValues.next("everyone", OPTIONS));
        assertEquals("everyone", SettingValues.next("nobody", OPTIONS));
        assertEquals("nobody", SettingValues.previous("everyone", OPTIONS));
        assertEquals("everyone", SettingValues.next("invalid", OPTIONS));
    }
}
