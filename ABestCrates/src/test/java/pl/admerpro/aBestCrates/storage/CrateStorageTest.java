package pl.admerpro.aBestCrates.storage;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class CrateStorageTest {
    @Test
    public void convertsModernItemStackFieldsToBukkitMaterialFormat() {
        String serialized = """
            crates:
              example:
                rewards:
                  diamond:
                    display-item:
                      ==: org.bukkit.inventory.ItemStack
                      DataVersion: 4189
                      id: minecraft:diamond
                      count: 3
                      schema_version: 1
                      meta:
                        ==: ItemMeta
                        id: minecraft:custom_name
                    commands: []
            """;

        String normalized = CrateStorage.normalizeSerializedItemStacks(serialized);

        assertTrue(normalized.contains("type: DIAMOND"));
        assertTrue(normalized.contains("amount: 3"));
        assertFalse(normalized.contains("DataVersion:"));
        assertFalse(normalized.contains("schema_version:"));
        assertTrue("Nested metadata must not be rewritten as an item material",
            normalized.contains("id: minecraft:custom_name"));
    }

    @Test
    public void convertsNamespacedTypeToBukkitMaterialFormat() {
        String serialized = """
            item:
              ==: org.bukkit.inventory.ItemStack
              type: minecraft:tripwire_hook
              count: 2
            """;

        String normalized = CrateStorage.normalizeSerializedItemStacks(serialized);

        assertTrue(normalized.contains("type: TRIPWIRE_HOOK"));
        assertTrue(normalized.contains("amount: 2"));
        assertFalse(normalized.contains("type: minecraft:tripwire_hook"));
        assertFalse(normalized.contains("count: 2"));
    }

    @Test
    public void leavesLegacyItemStackFieldsUntouched() {
        String serialized = """
            item:
              ==: org.bukkit.inventory.ItemStack
              v: 3465
              type: DIAMOND
              amount: 2
            """;

        String normalized = CrateStorage.normalizeSerializedItemStacks(serialized);

        assertTrue(normalized.contains("v: 3465"));
        assertTrue(normalized.contains("type: DIAMOND"));
        assertTrue(normalized.contains("amount: 2"));
    }
}
