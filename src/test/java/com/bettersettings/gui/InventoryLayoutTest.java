package com.bettersettings.gui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InventoryLayoutTest {

    @Test
    void fixedSixRowLayoutAlwaysUses54Slots() {
        assertEquals(54, InventoryLayout.resolveSize(0));
        assertEquals(54, InventoryLayout.resolveSize(27));
        assertEquals(54, InventoryLayout.resolveSize(54));
        assertEquals(54, InventoryLayout.resolveSize(63));
    }
}
