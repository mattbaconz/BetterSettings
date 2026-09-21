package com.bettersettings.api;

import com.bettersettings.settings.ValidationIssue;
import com.bettersettings.settings.ValidationReport;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SettingsRegistryTest {

    @Test
    void managedReloadIsAtomicAndPreservesProgrammaticSettings() {
        SettingsRegistry registry = SettingsRegistry.isolatedForTests();
        assertTrue(registry.replaceManaged(
            List.of(setting("managed-old")), List.of(), ValidationReport.success("classic", 1, 0, List.of())
        ));
        registry.registerSetting(setting("programmatic"));

        assertTrue(registry.replaceManaged(
            List.of(setting("managed-new")), List.of(), ValidationReport.success("classic", 1, 0, List.of())
        ));

        assertNull(registry.getSetting("managed-old"));
        assertNotNull(registry.getSetting("managed-new"));
        assertNotNull(registry.getSetting("programmatic"));

        ValidationReport invalid = new ValidationReport(
            "classic", 0, 0, 0, 1, 0, false,
            List.of(new ValidationIssue(ValidationIssue.Severity.ERROR, "invalid", "test", "x", "broken"))
        );
        assertFalse(registry.replaceManaged(List.of(setting("must-not-appear")), List.of(), invalid));
        assertNotNull(registry.getSetting("managed-new"));
        assertNull(registry.getSetting("must-not-appear"));
    }

    @Test
    void capabilitySettingBecomesAvailableWhenBehaviorRegisters() {
        SettingsRegistry registry = SettingsRegistry.isolatedForTests();
        ValueSetting setting = new TestValueSetting("auction-alerts", "auction");
        assertTrue(registry.replaceManaged(
            List.of(setting), List.of(), ValidationReport.success("donutsmp", 1, 0, List.of())
        ));

        assertFalse(registry.isAvailable(setting));
        registry.registerBehavior(new SettingBehavior() {
            @Override public String getId() { return "auction"; }
            @Override public void apply(Player player, ValueSetting valueSetting, String oldValue, String newValue) {}
        });
        assertTrue(registry.isAvailable(setting));
    }

    private static Setting setting(String id) {
        return new Setting() {
            @Override public String getId() { return id; }
            @Override public String getDescription() { return id; }
            @Override public ItemStack getIcon(Player player, boolean state) { return null; }
            @Override public boolean getDefaultState() { return true; }
            @Override public String getPermission() { return null; }
            @Override public boolean onToggle(Player player, boolean newState) { return true; }
        };
    }

    private static final class TestValueSetting implements ValueSetting {
        private final String id;
        private final String behavior;
        private TestValueSetting(String id, String behavior) { this.id = id; this.behavior = behavior; }
        @Override public String getId() { return id; }
        @Override public String getDescription() { return id; }
        @Override public ItemStack getIcon(Player player, String value) { return null; }
        @Override public SettingType getType() { return SettingType.TOGGLE; }
        @Override public String getDefaultValue() { return "true"; }
        @Override public List<SettingOption> getOptions() { return List.of(); }
        @Override public String getPermission() { return null; }
        @Override public String getBehaviorId() { return behavior; }
    }
}
