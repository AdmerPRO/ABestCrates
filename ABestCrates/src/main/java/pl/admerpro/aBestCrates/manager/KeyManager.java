package pl.admerpro.aBestCrates.manager;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import pl.admerpro.aBestCrates.model.Crate;
import pl.admerpro.aBestCrates.model.KeyDefinition;
import pl.admerpro.aBestCrates.util.ColorUtil;

public class KeyManager {
    private final JavaPlugin plugin;
    private final CrateManager crateManager;
    private final File file;
    private final NamespacedKey crateKey;
    private final Map<UUID, Map<String, Integer>> virtualKeys = new HashMap<>();

    public KeyManager(JavaPlugin plugin, CrateManager crateManager) {
        this.plugin = plugin;
        this.crateManager = crateManager;
        this.file = new File(plugin.getDataFolder(), "virtual-keys.yml");
        this.crateKey = new NamespacedKey(plugin, "crate_id");
    }

    public void load() {
        virtualKeys.clear();
        if (!file.exists()) {
            return;
        }

        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection playersSection = configuration.getConfigurationSection("players");
        if (playersSection == null) {
            return;
        }

        for (String uuidValue : playersSection.getKeys(false)) {
            UUID uuid;
            try {
                uuid = UUID.fromString(uuidValue);
            } catch (IllegalArgumentException exception) {
                continue;
            }

            ConfigurationSection cratesSection = playersSection.getConfigurationSection(uuidValue + ".crates");
            if (cratesSection == null) {
                continue;
            }

            Map<String, Integer> playerKeys = new HashMap<>();
            for (String crateId : cratesSection.getKeys(false)) {
                playerKeys.put(key(crateId), Math.max(0, cratesSection.getInt(crateId)));
            }
            virtualKeys.put(uuid, playerKeys);
        }
    }

    public void save() {
        YamlConfiguration configuration = new YamlConfiguration();
        for (Map.Entry<UUID, Map<String, Integer>> playerEntry : virtualKeys.entrySet()) {
            for (Map.Entry<String, Integer> crateEntry : playerEntry.getValue().entrySet()) {
                configuration.set("players." + playerEntry.getKey() + ".crates." + crateEntry.getKey(), crateEntry.getValue());
            }
        }

        try {
            configuration.save(file);
        } catch (IOException exception) {
            plugin.getLogger().severe("Could not save virtual-keys.yml: " + exception.getMessage());
        }
    }

    public ItemStack createPhysicalKey(Crate crate, int amount) {
        KeyDefinition definition = crate.getKeyDefinition();
        ItemStack itemStack = new ItemStack(definition.getMaterial(), Math.max(1, amount));
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorUtil.color(applyCratePlaceholders(definition.getDisplayName(), crate)));
            meta.setLore(ColorUtil.color(definition.getLore().stream()
                .map(line -> applyCratePlaceholders(line, crate))
                .toList()));
            if (definition.getCustomModelData() != null) {
                meta.setCustomModelData(definition.getCustomModelData());
            }
            meta.getPersistentDataContainer().set(crateKey, PersistentDataType.STRING, crate.getId());
            if (definition.isGlow()) {
                meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }
            itemStack.setItemMeta(meta);
        }
        return itemStack;
    }

    public Optional<String> getCrateIdFromKey(ItemStack itemStack) {
        if (itemStack == null || itemStack.getType() == Material.AIR || !itemStack.hasItemMeta()) {
            return Optional.empty();
        }
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) {
            return Optional.empty();
        }
        String crateId = meta.getPersistentDataContainer().get(crateKey, PersistentDataType.STRING);
        return Optional.ofNullable(crateId);
    }

    public boolean consumeAnyKey(Player player, Crate crate) {
        if (consumePhysicalKey(player, crate, EquipmentSlot.HAND) || consumePhysicalKey(player, crate, EquipmentSlot.OFF_HAND)) {
            return true;
        }

        int keys = getVirtualKeys(player.getUniqueId(), crate.getId());
        if (keys <= 0) {
            return false;
        }
        removeVirtualKeys(player.getUniqueId(), crate.getId(), 1);
        return true;
    }

    public int countPhysicalKeys(Player player, Crate crate) {
        int amount = 0;
        for (ItemStack itemStack : player.getInventory().getStorageContents()) {
            if (isPhysicalKeyForCrate(itemStack, crate)) {
                amount += itemStack.getAmount();
            }
        }
        return amount;
    }

    public void consumePhysicalKeys(Player player, Crate crate, int amount) {
        if (amount <= 0) {
            return;
        }

        ItemStack[] contents = player.getInventory().getStorageContents();
        int remaining = amount;
        for (int index = 0; index < contents.length && remaining > 0; index++) {
            ItemStack itemStack = contents[index];
            if (!isPhysicalKeyForCrate(itemStack, crate)) {
                continue;
            }

            int removed = Math.min(remaining, itemStack.getAmount());
            remaining -= removed;
            int newAmount = itemStack.getAmount() - removed;
            contents[index] = newAmount <= 0 ? null : itemStack.asQuantity(newAmount);
        }
        player.getInventory().setStorageContents(contents);
    }

    public int getVirtualKeys(UUID uuid, String crateId) {
        return virtualKeys.getOrDefault(uuid, Map.of()).getOrDefault(key(crateId), 0);
    }

    public void addVirtualKeys(OfflinePlayer player, String crateId, int amount) {
        if (amount <= 0) {
            return;
        }
        virtualKeys.computeIfAbsent(player.getUniqueId(), ignored -> new HashMap<>())
            .merge(key(crateId), amount, Integer::sum);
        save();
    }

    public void removeVirtualKeys(UUID uuid, String crateId, int amount) {
        if (amount <= 0) {
            return;
        }
        Map<String, Integer> playerKeys = virtualKeys.computeIfAbsent(uuid, ignored -> new HashMap<>());
        String normalizedCrate = key(crateId);
        int remaining = Math.max(0, playerKeys.getOrDefault(normalizedCrate, 0) - amount);
        playerKeys.put(normalizedCrate, remaining);
        save();
    }

    public boolean isKnownKey(ItemStack itemStack) {
        return getCrateIdFromKey(itemStack)
            .flatMap(crateManager::getCrate)
            .isPresent();
    }

    public boolean isPhysicalKeyForCrate(ItemStack itemStack, Crate crate) {
        return getCrateIdFromKey(itemStack)
            .map(crateId -> crateId.equalsIgnoreCase(crate.getId()))
            .orElse(false);
    }

    public void renameVirtualCrate(String oldId, String newId) {
        String oldKey = key(oldId);
        String newKey = key(newId);
        for (Map<String, Integer> playerKeys : virtualKeys.values()) {
            Integer amount = playerKeys.remove(oldKey);
            if (amount != null) {
                playerKeys.merge(newKey, amount, Integer::sum);
            }
        }
        save();
    }

    public void renamePhysicalKeysForOnlinePlayers(String oldId, String newId) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            ItemStack[] contents = player.getInventory().getStorageContents();
            for (ItemStack itemStack : contents) {
                updatePhysicalKeyId(itemStack, oldId, newId);
            }
            player.getInventory().setStorageContents(contents);
            updatePhysicalKeyId(player.getInventory().getItemInOffHand(), oldId, newId);
        }
    }

    private boolean consumePhysicalKey(Player player, Crate crate, EquipmentSlot slot) {
        ItemStack itemStack = slot == EquipmentSlot.HAND
            ? player.getInventory().getItemInMainHand()
            : player.getInventory().getItemInOffHand();

        Optional<String> keyCrate = getCrateIdFromKey(itemStack);
        if (keyCrate.isEmpty() || !keyCrate.get().equalsIgnoreCase(crate.getId())) {
            return false;
        }

        int amount = itemStack.getAmount();
        if (amount <= 1) {
            if (slot == EquipmentSlot.HAND) {
                player.getInventory().setItemInMainHand(null);
            } else {
                player.getInventory().setItemInOffHand(null);
            }
        } else {
            itemStack.setAmount(amount - 1);
        }
        return true;
    }

    private void updatePhysicalKeyId(ItemStack itemStack, String oldId, String newId) {
        if (itemStack == null || !itemStack.hasItemMeta()) {
            return;
        }

        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) {
            return;
        }

        String crateId = meta.getPersistentDataContainer().get(crateKey, PersistentDataType.STRING);
        if (crateId == null || !crateId.equalsIgnoreCase(oldId)) {
            return;
        }

        meta.getPersistentDataContainer().set(crateKey, PersistentDataType.STRING, newId);
        itemStack.setItemMeta(meta);
    }

    private String applyCratePlaceholders(String text, Crate crate) {
        return text.replace("%crate%", crate.getId())
            .replace("%crate_displayname%", crate.getDisplayName());
    }

    private String key(String crateId) {
        return crateId == null ? "" : crateId.toLowerCase(Locale.ROOT);
    }
}
