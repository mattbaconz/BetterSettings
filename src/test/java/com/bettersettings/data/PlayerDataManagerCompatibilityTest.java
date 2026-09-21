package com.bettersettings.data;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.assertFalse;

class PlayerDataManagerCompatibilityTest {

    @Test
    void dataManagerRemainsSubclassable() {
        assertFalse(Modifier.isFinal(PlayerDataManager.class.getModifiers()));
    }
}
