package pl.admerpro.aBestCrates.manager;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
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
import pl.admerpro.aBestCrates.integration.CustomItemIntegrationService;
import pl.admerpro.aBestCrates.model.KeyDefinition;
import pl.admerpro.aBestCrates.model.KeyRequirement;
import pl.admerpro.aBestCrates.util.ColorUtil;

public class KeyManager {
    private static final NamespacedKey UNBREAKING_KEY = NamespacedKey.minecraft("unbreaking");

    private final JavaPlugin plugin;
    private final CrateManager crateManager;
    private final File file;
    private final NamespacedKey crateKey;
    private final NamespacedKey crateItemKey;
    private final CustomItemIntegrationService customItems;
    private final Map<UUID, Map<String, Integer>> virtualKeys = new HashMap<>();
    private Runnable changeListener = () -> { };
    private int batchDepth;
    private boolean batchDirty;

    public KeyManager(JavaPlugin plugin, CrateManager crateManager, CustomItemIntegrationService customItems) {
        this.plugin = plugin;
        this.crateManager = crateManager;
        this.customItems = customItems;
        this.file = new File(plugin.getDataFolder(), "virtual-keys.yml");
        this.crateKey = new NamespacedKey(plugin, "crate_id");
        this.crateItemKey = new NamespacedKey(plugin, "crate_item_id");
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
            plugin.getLogger().log(Level.SEVERE, "Could not save virtual-keys.yml.", exception);
        }
    }

    public ItemStack createPhysicalKey(Crate crate, int amount) {
        KeyDefinition definition = crate.getKeyDefinition();
        ItemStack templateItem = definition.getTemplateItem();
        ItemStack itemStack = templateItem == null
            ? new ItemStack(definition.getMaterial(), Math.max(1, amount))
            : customItems.refresh(templateItem).asQuantity(Math.max(1, amount));
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            meta.displayName(ColorUtil.component(applyCratePlaceholders(definition.getDisplayName(), crate)));
            if (templateItem == null) {
                meta.lore(ColorUtil.components(definition.getLore().stream()
                    .map(line -> applyCratePlaceholders(line, crate))
                    .toList()));
            }
            if (templateItem == null && definition.getCustomModelData() != null) {
                meta.setCustomModelData(definition.getCustomModelData());
            }
            meta.getPersistentDataContainer().set(crateKey, PersistentDataType.STRING, crate.getId());
            if (definition.isGlow()) {
                Enchantment unbreaking = getUnbreakingEnchantment();
                if (unbreaking != null) {
                    meta.addEnchant(unbreaking, 1, true);
                }
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }
            itemStack.setItemMeta(meta);
        }
        return itemStack;
    }

    public ItemStack createCrateItem(Crate crate, int amount) {
        ItemStack itemStack = new ItemStack(crate.getBlockMaterial(), Math.max(1, amount));
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            meta.displayName(ColorUtil.component(crate.getDisplayName()));
            meta.lore(ColorUtil.components(List.of(
                "&7Place this block to create",
                "&7a linked crate zone.",
                "&8Crate: &f" + crate.getId()
            )));
            meta.getPersistentDataContainer().set(crateItemKey, PersistentDataType.STRING, crate.getId());
            itemStack.setItemMeta(meta);
        }
        return itemStack;
    }

    public Optional<String> getCrateIdFromCrateItem(ItemStack itemStack) {
        if (itemStack == null || itemStack.getType().isAir() || !itemStack.hasItemMeta()) {
            return Optional.empty();
        }
        return Optional.ofNullable(itemStack.getItemMeta().getPersistentDataContainer()
            .get(crateItemKey, PersistentDataType.STRING));
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

    public boolean hasRequiredKeys(Player player, Crate crate) {
        Map<String, Integer> totals = requirementTotals(crate);
        for (Map.Entry<String, Integer> entry : totals.entrySet()) {
            Crate requiredCrate = crateManager.getCrate(entry.getKey()).orElse(null);
            if (requiredCrate == null) {
                return false;
            }
            int available = countPhysicalKeys(player, requiredCrate)
                + getVirtualKeys(player.getUniqueId(), requiredCrate.getId());
            if (available < entry.getValue()) {
                return false;
            }
        }
        return true;
    }

    public boolean consumeRequiredKeys(Player player, Crate crate) {
        if (!hasRequiredKeys(player, crate)) {
            return false;
        }
        for (Map.Entry<String, Integer> entry : requirementTotals(crate).entrySet()) {
            Crate requiredCrate = crateManager.getCrate(entry.getKey()).orElseThrow();
            int physical = Math.min(entry.getValue(), countPhysicalKeys(player, requiredCrate));
            consumePhysicalKeys(player, requiredCrate, physical);
            int virtual = entry.getValue() - physical;
            if (virtual > 0) {
                removeVirtualKeysInternal(player.getUniqueId(), requiredCrate.getId(), virtual);
            }
        }
        markChanged();
        return true;
    }

    public int countOpenable(Player player, Crate crate) {
        int openable = Integer.MAX_VALUE;
        for (Map.Entry<String, Integer> entry : requirementTotals(crate).entrySet()) {
            Crate requiredCrate = crateManager.getCrate(entry.getKey()).orElse(null);
            if (requiredCrate == null) {
                return 0;
            }
            int keys = countPhysicalKeys(player, requiredCrate) + getVirtualKeys(player.getUniqueId(), requiredCrate.getId());
            openable = Math.min(openable, keys / entry.getValue());
        }
        return openable == Integer.MAX_VALUE ? 0 : openable;
    }

    public int countPhysicalKeys(Player player, Crate crate) {
        int amount = 0;
        for (ItemStack itemStack : player.getInventory().getStorageContents()) {
            if (itemStack != null && isPhysicalKeyForCrate(itemStack, crate)) {
                amount += itemStack.getAmount();
            }
        }
        ItemStack offHand = player.getInventory().getItemInOffHand();
        if (isPhysicalKeyForCrate(offHand, crate)) {
            amount += offHand.getAmount();
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
            if (itemStack == null || !isPhysicalKeyForCrate(itemStack, crate)) {
                continue;
            }

            int removed = Math.min(remaining, itemStack.getAmount());
            remaining -= removed;
            int newAmount = itemStack.getAmount() - removed;
            contents[index] = newAmount <= 0 ? null : itemStack.asQuantity(newAmount);
        }
        player.getInventory().setStorageContents(contents);

        if (remaining > 0) {
            ItemStack offHand = player.getInventory().getItemInOffHand();
            if (isPhysicalKeyForCrate(offHand, crate)) {
                int removed = Math.min(remaining, offHand.getAmount());
                int newAmount = offHand.getAmount() - removed;
                player.getInventory().setItemInOffHand(newAmount <= 0 ? null : offHand.asQuantity(newAmount));
            }
        }
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
        markChanged();
    }

    public void removeVirtualKeys(UUID uuid, String crateId, int amount) {
        if (amount <= 0) {
            return;
        }
        removeVirtualKeysInternal(uuid, crateId, amount);
        markChanged();
    }

    public void beginBatch() {
        batchDepth++;
    }

    public void endBatch() {
        if (batchDepth <= 0) {
            return;
        }
        batchDepth--;
        if (batchDepth == 0 && batchDirty) {
            batchDirty = false;
            save();
            changeListener.run();
        }
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
        boolean changed = false;
        for (Map<String, Integer> playerKeys : virtualKeys.values()) {
            Integer amount = playerKeys.remove(oldKey);
            if (amount != null) {
                playerKeys.merge(newKey, amount, Integer::sum);
                changed = true;
            }
        }
        if (changed) {
            save();
            changeListener.run();
        }
    }

    public void removeVirtualCrate(String crateId) {
        String normalizedCrate = key(crateId);
        for (Map<String, Integer> playerKeys : virtualKeys.values()) {
            playerKeys.remove(normalizedCrate);
        }
        save();
        changeListener.run();
    }

    public void setChangeListener(Runnable changeListener) {
        this.changeListener = changeListener == null ? () -> { } : changeListener;
    }

    public void renamePhysicalKeysForOnlinePlayers(String oldId, String newId) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            ItemStack[] contents = player.getInventory().getStorageContents();
            for (ItemStack itemStack : contents) {
                updateTaggedItemId(itemStack, oldId, newId);
            }
            player.getInventory().setStorageContents(contents);
            updateTaggedItemId(player.getInventory().getItemInOffHand(), oldId, newId);
        }
    }

    private boolean consumePhysicalKey(Player player, Crate crate, EquipmentSlot slot) {
        ItemStack itemStack = slot == EquipmentSlot.HAND
            ? player.getInventory().getItemInMainHand()
            : player.getInventory().getItemInOffHand();
        if (itemStack == null) {
            return false;
        }

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

    private void updateTaggedItemId(ItemStack itemStack, String oldId, String newId) {
        if (itemStack == null || !itemStack.hasItemMeta()) {
            return;
        }

        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) {
            return;
        }

        for (NamespacedKey key : List.of(crateKey, crateItemKey)) {
            String crateId = meta.getPersistentDataContainer().get(key, PersistentDataType.STRING);
            if (crateId != null && crateId.equalsIgnoreCase(oldId)) {
                meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, newId);
            }
        }
        itemStack.setItemMeta(meta);
    }

    private String applyCratePlaceholders(String text, Crate crate) {
        return text.replace("%crate%", crate.getId())
            .replace("%crate_displayname%", crate.getDisplayName());
    }

    @SuppressWarnings("deprecation")
    private Enchantment getUnbreakingEnchantment() {
        return Enchantment.getByKey(UNBREAKING_KEY);
    }

    private String key(String crateId) {
        return crateId == null ? "" : crateId.toLowerCase(Locale.ROOT);
    }

    private void removeVirtualKeysInternal(UUID uuid, String crateId, int amount) {
        Map<String, Integer> playerKeys = virtualKeys.computeIfAbsent(uuid, ignored -> new HashMap<>());
        String normalizedCrate = key(crateId);
        int remaining = Math.max(0, playerKeys.getOrDefault(normalizedCrate, 0) - amount);
        if (remaining <= 0) {
            playerKeys.remove(normalizedCrate);
            if (playerKeys.isEmpty()) {
                virtualKeys.remove(uuid);
            }
            return;
        }
        playerKeys.put(normalizedCrate, remaining);
    }

    private void markChanged() {
        if (batchDepth > 0) {
            batchDirty = true;
            return;
        }
        save();
        changeListener.run();
    }

    private Map<String, Integer> requirementTotals(Crate crate) {
        Map<String, Integer> totals = new java.util.LinkedHashMap<>();
        for (KeyRequirement requirement : crate.getKeyRequirements()) {
            totals.merge(key(requirement.crateId()), requirement.amount(), Integer::sum);
        }
        return totals;
    }
}
