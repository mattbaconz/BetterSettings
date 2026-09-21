package com.bettersettings.settings;

import com.bettersettings.api.SettingType;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DefinitionCatalogTest {

    @Test
    void duplicateIdsInvalidateCandidateInsteadOfReplacing() {
        SettingDefinition first = definition("duplicate", "first.yml");
        SettingDefinition second = definition("duplicate", "second.yml");

        DefinitionCatalog catalog = DefinitionCatalog.build(List.of(first, second));

        assertFalse(catalog.valid());
        assertEquals(1, catalog.duplicateCount());
        assertTrue(catalog.settings().isEmpty());
    }

    private static SettingDefinition definition(String id, String source) {
        return new SettingDefinition(
            id, "Example", Material.PAPER, SettingType.TOGGLE, "true", List.of(),
            null, "uncategorized", 100, null, List.of(), source
        );
    }
}
