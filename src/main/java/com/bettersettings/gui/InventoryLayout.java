package com.bettersettings.gui;

/** Slot rules shared by the fixed six-row settings and category layouts. */
final class InventoryLayout {
    static final int SIZE = 54;

    private InventoryLayout() {}

    static int resolveSize(int configuredSize) {
        return SIZE;
    }
}
