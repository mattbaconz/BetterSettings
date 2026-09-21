package com.bettersettings.settings;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SettingActionTest {

    @Test
    void anyConditionIsCaseInsensitive() {
        SettingAction action = new SettingAction(SettingAction.Type.MESSAGE, "hello", "ANY");

        assertTrue(action.matches("true"));
        assertTrue(action.matches("false"));
    }

    @Test
    void specificConditionMatchesTheNewValueOnly() {
        SettingAction action = new SettingAction(SettingAction.Type.MESSAGE, "hello", "TRUE");

        assertTrue(action.matches("true"));
        assertFalse(action.matches("false"));
    }
}
