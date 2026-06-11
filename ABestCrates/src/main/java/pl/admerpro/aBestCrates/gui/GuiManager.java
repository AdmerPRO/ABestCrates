package pl.admerpro.aBestCrates.gui;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import pl.admerpro.aBestCrates.manager.CrateLocationManager;
import pl.admerpro.aBestCrates.manager.CrateManager;
import pl.admerpro.aBestCrates.manager.KeyManager;
import pl.admerpro.aBestCrates.model.Crate;
import pl.admerpro.aBestCrates.model.Reward;
import pl.admerpro.aBestCrates.service.MessageService;
import pl.admerpro.aBestCrates.service.OpeningService;
import pl.admerpro.aBestCrates.util.ColorUtil;
import pl.admerpro.aBestCrates.util.ItemBuilder;

public class GuiManager implements Listener {
    private static final int SLOT_CRATE_REAL_NAME = 4;
    private static final int SLOT_CRATE_COLOR = 22;
    private static final int SLOT_CRATE_BLOCK = 12;
    private static final int SLOT_KEY_ITEM = 40;
    private static final int SLOT_REWARD_NAME = 10;
    private static final int SLOT_REWARD_DISPLAY_ITEM = 12;
    private static final int SLOT_REWARD_DROP_ITEM = 14;
    private static final int SLOT_REWARD_REAL_CHANCE = 16;
    private static final int SLOT_REWARD_DISPLAY_CHANCE = 22;

    private final JavaPlugin plugin;
    private final CrateManager crateManager;
    private final KeyManager keyManager;
    private final CrateLocationManager crateLocationManager;
    private final OpeningService openingService;
    private final ChatInputManager chatInputManager;
    private final MessageService messageService;
    private final NamespacedKey crateTag;
    private final NamespacedKey rewardTag;
    private final Set<UUID> suppressedCloses = new HashSet<>();
    private final List<CrateColor> crateColors = List.of(
        new CrateColor("Dark Purple", "&5", Material.PURPLE_DYE),
        new CrateColor("Gold", "&6", Material.ORANGE_DYE),
        new CrateColor("Aqua", "&b", Material.LIGHT_BLUE_DYE),
        new CrateColor("Green", "&a", Material.LIME_DYE),
        new CrateColor("Red", "&c", Material.RED_DYE),
        new CrateColor("White", "&f", Material.WHITE_DYE)
    );
    private final List<Material> blockCycle = List.of(
        Material.CHEST,
        Material.ENDER_CHEST,
        Material.BARREL,
        Material.SHULKER_BOX,
        Material.GOLD_BLOCK,
        Material.DIAMOND_BLOCK
    );

    public GuiManager(JavaPlugin plugin, CrateManager crateManager, KeyManager keyManager, CrateLocationManager crateLocationManager,
                      OpeningService openingService, ChatInputManager chatInputManager, MessageService messageService) {
        this.plugin = plugin;
        this.crateManager = crateManager;
        this.keyManager = keyManager;
        this.crateLocationManager = crateLocationManager;
        this.openingService = openingService;
        this.chatInputManager = chatInputManager;
        this.messageService = messageService;
        this.crateTag = new NamespacedKey(plugin, "gui_crate");
        this.rewardTag = new NamespacedKey(plugin, "gui_reward");
    }

    public void openMain(Player player) {
        MenuHolder holder = new MenuHolder(MenuType.MAIN, null);
        Inventory inventory = Bukkit.createInventory(holder, 27, ColorUtil.color("&5ABestCrates"));
        holder.setInventory(inventory);

        inventory.setItem(10, new ItemBuilder(Material.EMERALD).name("&aCreate Crate").lore(List.of("&7Create a new crate draft.")).build());
        inventory.setItem(12, new ItemBuilder(Material.CHEST).name("&dManage Crates").lore(List.of("&7Open crate editor list.")).build());
        inventory.setItem(14, new ItemBuilder(Material.TRIPWIRE_HOOK).name("&eKeys").lore(List.of("&7Use commands to manage keys.", "&f/acrates givekey")).build());
        inventory.setItem(16, new ItemBuilder(Material.PAPER).name("&bStatistics").lore(List.of("&7Coming in a later module.")).build());
        inventory.setItem(22, new ItemBuilder(Material.REDSTONE).name("&cReload").lore(List.of("&7Reload files from disk.")).build());

        openMenu(player, inventory);
    }

    public void openManage(Player player) {
        MenuHolder holder = new MenuHolder(MenuType.MANAGE, null);
        Inventory inventory = Bukkit.createInventory(holder, 54, ColorUtil.color("&5Manage Crates"));
        holder.setInventory(inventory);

        int slot = 0;
        for (Crate crate : crateManager.getCrates()) {
            if (slot >= 45) {
                break;
            }
            inventory.setItem(slot++, tag(new ItemBuilder(crate.getBlockMaterial())
                .name(crate.getDisplayName())
                .lore(List.of(
                    "&7Technical: &f" + crate.getId(),
                    "&7Rewards: &f" + crate.getRewards().size(),
                    "&7Animation: &f" + crate.getAnimationType().name(),
                    "",
                    "&eClick to edit."
                ))
                .build(), crateTag, crate.getId()));
        }

        inventory.setItem(49, new ItemBuilder(Material.ARROW).name("&eBack").build());
        openMenu(player, inventory);
    }

    public void openEdit(Player player, Crate crate) {
        MenuHolder holder = new MenuHolder(MenuType.EDIT, crate.getId());
        Inventory inventory = Bukkit.createInventory(holder, 54, ColorUtil.color(crate.getDisplayName()));
        holder.setInventory(inventory);

        inventory.setItem(10, new ItemBuilder(Material.NAME_TAG).name("&dDisplay Name").lore(List.of("&f" + crate.getDisplayName(), "&7Click to edit in chat.")).build());
        inventory.setItem(SLOT_CRATE_REAL_NAME, new ItemBuilder(Material.PAPER).name("&bReal Name").lore(List.of(
            "&f" + crate.getId(),
            "&7This is the technical crate id.",
            "&7Click to rename it."
        )).build());
        inventory.setItem(SLOT_CRATE_COLOR, colorItem(crate));
        inventory.setItem(SLOT_CRATE_BLOCK, new ItemBuilder(crate.getBlockMaterial()).name("&6Block Type").lore(List.of(
            "&f" + crate.getBlockMaterial().name(),
            "&7Left click: set from hand/cursor.",
            "&7Drag a block here to set it.",
            "&7Right click: cycle default blocks."
        )).build());
        inventory.setItem(14, new ItemBuilder(Material.OAK_SIGN).name("&bHologram").lore(hologramLore(crate)).build());
        inventory.setItem(16, new ItemBuilder(Material.DIAMOND).name("&aRewards").lore(List.of("&7Rewards: &f" + crate.getRewards().size(), "&7Click to manage.")).build());
        inventory.setItem(SLOT_KEY_ITEM, keyItem(crate));
        inventory.setItem(28, new ItemBuilder(Material.TRIPWIRE_HOOK).name("&eGive Test Key").lore(List.of("&7Gives one physical key to you.")).build());
        inventory.setItem(30, new ItemBuilder(Material.WRITABLE_BOOK).name("&cNo Key Message").lore(List.of("&f" + crate.getNoKeyMessage(), "&7Click to edit in chat.")).build());
        inventory.setItem(32, new ItemBuilder(Material.CLOCK).name("&dAnimation").lore(List.of("&f" + crate.getAnimationType().name(), "&7Click to cycle.")).build());
        inventory.setItem(34, new ItemBuilder(Material.ENDER_EYE).name("&5Preview").lore(List.of("&7Open rewards preview.")).build());
        inventory.setItem(49, new ItemBuilder(Material.LIME_DYE).name("&aSave").lore(List.of("&7Save crate to disk.")).build());

        openMenu(player, inventory);
    }

    public void openRewards(Player player, Crate crate) {
        MenuHolder holder = new MenuHolder(MenuType.REWARDS, crate.getId());
        Inventory inventory = Bukkit.createInventory(holder, 54, ColorUtil.color("&5Rewards: &f" + crate.getId()));
        holder.setInventory(inventory);

        int slot = 0;
        for (Reward reward : crate.getRewards()) {
            if (slot >= 45) {
                break;
            }
            inventory.setItem(slot++, tag(rewardDisplay(reward, true), rewardTag, reward.getId()));
        }

        inventory.setItem(45, new ItemBuilder(Material.LIME_DYE).name("&aAdd Held Item").lore(List.of("&7Adds item from your main hand.")).build());
        inventory.setItem(53, new ItemBuilder(Material.ARROW).name("&eBack").build());
        openMenu(player, inventory);
    }

    public void openRewardEdit(Player player, Crate crate, Reward reward) {
        MenuHolder holder = new MenuHolder(MenuType.REWARD_EDIT, crate.getId(), reward.getId());
        Inventory inventory = Bukkit.createInventory(holder, 54, ColorUtil.color("&5Reward: &f" + reward.getId()));
        holder.setInventory(inventory);

        inventory.setItem(SLOT_REWARD_NAME, new ItemBuilder(Material.NAME_TAG).name("&dCrate Item Name").lore(List.of(
            "&f" + rewardDisplayName(reward),
            "&7Click to edit in chat."
        )).build());
        inventory.setItem(SLOT_REWARD_DISPLAY_ITEM, rewardEditorItem(reward.getDisplayItem(), Material.ITEM_FRAME, "&eDisplay Item", List.of(
            "&7This item is shown in preview.",
            "&7Left click: set from hand/cursor.",
            "&7Drag an item here to set it."
        )));
        inventory.setItem(SLOT_REWARD_DROP_ITEM, rewardEditorItem(reward.getItemReward(), Material.CHEST_MINECART, "&aDropped Item", List.of(
            "&7This item is given to player.",
            "&7Left click: set from hand/cursor.",
            "&7Drag an item here to set it."
        )));
        inventory.setItem(SLOT_REWARD_REAL_CHANCE, new ItemBuilder(Material.REDSTONE).name("&cReal Chance").lore(List.of(
            "&f" + reward.getRealChance() + "%",
            "&7Click to edit in chat."
        )).build());
        inventory.setItem(SLOT_REWARD_DISPLAY_CHANCE, new ItemBuilder(Material.GLOWSTONE_DUST).name("&6Display Chance").lore(List.of(
            "&f" + reward.getDisplayChance() + "%",
            "&7Click to edit in chat."
        )).build());
        inventory.setItem(49, new ItemBuilder(Material.LIME_DYE).name("&aSave").lore(List.of("&7Save reward to disk.")).build());
        inventory.setItem(53, new ItemBuilder(Material.ARROW).name("&eBack").build());

        openMenu(player, inventory);
    }

    public void openPreview(Player player, Crate crate) {
        MenuHolder holder = new MenuHolder(MenuType.PREVIEW, crate.getId());
        Inventory inventory = Bukkit.createInventory(holder, 54, ColorUtil.color("&5Preview: &f" + ColorUtil.removeColor(crate.getDisplayName())));
        holder.setInventory(inventory);

        int slot = 0;
        for (Reward reward : crate.getRewards()) {
            if (slot >= 54) {
                break;
            }
            inventory.setItem(slot++, rewardDisplay(reward, false));
        }

        openMenu(player, inventory);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player) || !(event.getInventory().getHolder() instanceof MenuHolder holder)) {
            return;
        }
        if (event.getRawSlot() < 0) {
            return;
        }
        if (event.getRawSlot() >= event.getInventory().getSize()) {
            if (event.isShiftClick()) {
                event.setCancelled(true);
            }
            return;
        }
        event.setCancelled(true);

        switch (holder.getType()) {
            case MAIN -> handleMain(player, event.getRawSlot());
            case MANAGE -> handleManage(player, event);
            case EDIT -> handleEdit(player, holder, event);
            case REWARDS -> handleRewards(player, holder, event);
            case REWARD_EDIT -> handleRewardEdit(player, holder, event);
            case PREVIEW -> {
            }
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player) || !(event.getInventory().getHolder() instanceof MenuHolder holder)) {
            return;
        }

        boolean touchesTopInventory = event.getRawSlots().stream().anyMatch(slot -> slot < event.getInventory().getSize());
        if (!touchesTopInventory) {
            return;
        }
        event.setCancelled(true);

        if (holder.getType() == MenuType.EDIT && event.getRawSlots().contains(SLOT_CRATE_BLOCK)) {
            crateManager.getCrate(holder.getCrateId()).ifPresent(crate -> setCrateBlockFromItem(player, crate, event.getOldCursor()));
            return;
        }

        if (holder.getType() == MenuType.EDIT && event.getRawSlots().contains(SLOT_KEY_ITEM)) {
            crateManager.getCrate(holder.getCrateId()).ifPresent(crate -> setKeyTemplateFromItem(player, crate, event.getOldCursor()));
            return;
        }

        if (holder.getType() == MenuType.REWARD_EDIT) {
            Optional<Crate> optionalCrate = crateManager.getCrate(holder.getCrateId());
            if (optionalCrate.isEmpty()) {
                return;
            }
            Optional<Reward> optionalReward = optionalCrate.get().getReward(holder.getRewardId());
            if (optionalReward.isEmpty()) {
                return;
            }

            Reward reward = optionalReward.get();
            if (event.getRawSlots().contains(SLOT_REWARD_DISPLAY_ITEM)) {
                setRewardDisplayItem(player, optionalCrate.get(), reward, event.getOldCursor());
            } else if (event.getRawSlots().contains(SLOT_REWARD_DROP_ITEM)) {
                setRewardDropItem(player, optionalCrate.get(), reward, event.getOldCursor());
            }
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player) || !(event.getInventory().getHolder() instanceof MenuHolder holder)) {
            return;
        }

        if (suppressedCloses.remove(player.getUniqueId())) {
            return;
        }

        Bukkit.getScheduler().runTask(plugin, () -> goBack(player, holder));
    }

    private void handleMain(Player player, int slot) {
        switch (slot) {
            case 10 -> {
                Crate crate = crateManager.createCrate(crateManager.nextGeneratedName());
                messageService.send(player, "crate-created", Map.of("%crate%", crate.getId()));
                openEdit(player, crate);
            }
            case 12 -> openManage(player);
            case 22 -> {
                plugin.reloadConfig();
                crateManager.load();
                keyManager.load();
                crateLocationManager.load();
                crateLocationManager.refreshHolograms();
                messageService.send(player, "reloaded");
                openMain(player);
            }
            default -> {
            }
        }
    }

    private void handleManage(Player player, InventoryClickEvent event) {
        if (event.getRawSlot() == 49) {
            openMain(player);
            return;
        }

        readTag(event.getCurrentItem(), crateTag)
            .flatMap(crateManager::getCrate)
            .ifPresent(crate -> openEdit(player, crate));
    }

    private void handleEdit(Player player, MenuHolder holder, InventoryClickEvent event) {
        Optional<Crate> optionalCrate = crateManager.getCrate(holder.getCrateId());
        if (optionalCrate.isEmpty()) {
            suppressNextClose(player);
            player.closeInventory();
            return;
        }
        Crate crate = optionalCrate.get();
        int slot = event.getRawSlot();

        switch (slot) {
            case 10 -> requestChat(player, "&eEnter the new display name:", input -> {
                crate.setDisplayName(input);
                saveAndRefreshHolograms();
                openEdit(player, crate);
            });
            case SLOT_CRATE_REAL_NAME -> requestChat(player, "&eEnter the new real crate name:", input -> renameCrate(player, crate, input));
            case SLOT_CRATE_COLOR -> {
                cycleCrateColor(crate);
                saveAndRefreshHolograms();
                messageService.send(player, "crate-color-updated", Map.of("%color%", crateColorName(crate.getColor())));
                openEdit(player, crate);
            }
            case SLOT_CRATE_BLOCK -> {
                if (event.isRightClick()) {
                    crate.setBlockMaterial(nextBlock(crate.getBlockMaterial()));
                    crateManager.save();
                    crateLocationManager.updatePlacedBlocks(crate);
                    openEdit(player, crate);
                    return;
                }
                ItemStack sourceItem = selectedItem(player, event, true);
                setCrateBlockFromItem(player, crate, sourceItem);
            }
            case 14 -> requestChat(player, "&eEnter hologram lines separated with |:", input -> {
                crate.setHologram(List.of(input.split("\\|")));
                saveAndRefreshHolograms();
                openEdit(player, crate);
            });
            case 16 -> openRewards(player, crate);
            case SLOT_KEY_ITEM -> setKeyTemplateFromItem(player, crate, selectedItem(player, event, true));
            case 28 -> {
                player.getInventory().addItem(keyManager.createPhysicalKey(crate, 1)).values()
                    .forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
                messageService.send(player, "key-given", Map.of("%player%", player.getName(), "%crate%", crate.getId(), "%amount%", "1"));
            }
            case 30 -> requestChat(player, "&eEnter the no-key message:", input -> {
                crate.setNoKeyMessage(input);
                crateManager.save();
                openEdit(player, crate);
            });
            case 32 -> {
                crate.setAnimationType(crate.getAnimationType().next());
                crateManager.save();
                openEdit(player, crate);
            }
            case 34 -> openPreview(player, crate);
            case 49 -> {
                crateManager.save();
                messageService.send(player, "saved");
                openEdit(player, crate);
            }
            default -> {
            }
        }
    }

    private void handleRewards(Player player, MenuHolder holder, InventoryClickEvent event) {
        Optional<Crate> optionalCrate = crateManager.getCrate(holder.getCrateId());
        if (optionalCrate.isEmpty()) {
            suppressNextClose(player);
            player.closeInventory();
            return;
        }
        Crate crate = optionalCrate.get();

        if (event.getRawSlot() == 53) {
            openEdit(player, crate);
            return;
        }
        if (event.getRawSlot() == 45) {
            addHeldItemReward(player, crate);
            return;
        }

        Optional<String> rewardId = readTag(event.getCurrentItem(), rewardTag);
        if (rewardId.isEmpty()) {
            return;
        }

        if (event.isRightClick()) {
            crate.removeReward(rewardId.get());
            crateManager.save();
            openRewards(player, crate);
            return;
        }

        crate.getReward(rewardId.get()).ifPresent(reward -> openRewardEdit(player, crate, reward));
    }

    private void handleRewardEdit(Player player, MenuHolder holder, InventoryClickEvent event) {
        Optional<Crate> optionalCrate = crateManager.getCrate(holder.getCrateId());
        if (optionalCrate.isEmpty()) {
            suppressNextClose(player);
            player.closeInventory();
            return;
        }
        Crate crate = optionalCrate.get();
        Optional<Reward> optionalReward = crate.getReward(holder.getRewardId());
        if (optionalReward.isEmpty()) {
            openRewards(player, crate);
            return;
        }
        Reward reward = optionalReward.get();

        switch (event.getRawSlot()) {
            case SLOT_REWARD_NAME -> requestChat(player, "&eEnter the item name shown in the crate:", input -> {
                setRewardDisplayName(reward, input);
                crateManager.save();
                openRewardEdit(player, crate, reward);
            });
            case SLOT_REWARD_DISPLAY_ITEM -> setRewardDisplayItem(player, crate, reward, selectedItem(player, event, true));
            case SLOT_REWARD_DROP_ITEM -> setRewardDropItem(player, crate, reward, selectedItem(player, event, false));
            case SLOT_REWARD_REAL_CHANCE -> requestChat(player, "&eEnter real chance, e.g. 5:", input -> {
                reward.setRealChance(parseDouble(input, reward.getRealChance()));
                crateManager.save();
                openRewardEdit(player, crate, reward);
            });
            case SLOT_REWARD_DISPLAY_CHANCE -> requestChat(player, "&eEnter display chance, e.g. 10:", input -> {
                reward.setDisplayChance(parseDouble(input, reward.getDisplayChance()));
                crateManager.save();
                openRewardEdit(player, crate, reward);
            });
            case 49 -> {
                crateManager.save();
                messageService.send(player, "saved");
                openRewardEdit(player, crate, reward);
            }
            case 53 -> openRewards(player, crate);
            default -> {
            }
        }
    }

    private void addHeldItemReward(Player player, Crate crate) {
        ItemStack heldItem = player.getInventory().getItemInMainHand();
        if (!isUsableItem(heldItem)) {
            messageService.send(player, "hold-item");
            return;
        }

        Reward reward = new Reward("reward_" + System.currentTimeMillis());
        ItemStack rewardItem = heldItem.clone();
        ItemStack displayItem = heldItem.clone();
        displayItem.setAmount(1);
        reward.setItemReward(rewardItem);
        reward.setDisplayItem(displayItem);
        reward.setRealChance(crate.getRewards().isEmpty() ? 100.0D : 10.0D);
        reward.setDisplayChance(reward.getRealChance());
        crate.addReward(reward);
        crateManager.save();
        openRewardEdit(player, crate, reward);
    }

    private void setCrateBlockFromItem(Player player, Crate crate, ItemStack itemStack) {
        if (!isUsableItem(itemStack) || !itemStack.getType().isBlock()) {
            messageService.send(player, "block-item-required");
            return;
        }

        crate.setBlockMaterial(itemStack.getType());
        crateManager.save();
        crateLocationManager.updatePlacedBlocks(crate);
        messageService.send(player, "crate-block-updated", Map.of("%block%", itemStack.getType().name()));
        openEdit(player, crate);
    }

    private void setKeyTemplateFromItem(Player player, Crate crate, ItemStack itemStack) {
        if (!isUsableItem(itemStack)) {
            messageService.send(player, "hold-item");
            return;
        }

        crate.getKeyDefinition().setTemplateItem(itemStack);
        crateManager.save();
        messageService.send(player, "key-item-updated");
        openEdit(player, crate);
    }

    private void renameCrate(Player player, Crate crate, String input) {
        String oldId = crate.getId();
        String newId = Crate.normalizeId(input);
        if (newId.isBlank()) {
            messageService.send(player, "crate-rename-invalid");
            openEdit(player, crate);
            return;
        }
        if (oldId.equalsIgnoreCase(newId)) {
            openEdit(player, crate);
            return;
        }
        if (crateManager.exists(newId)) {
            messageService.send(player, "crate-exists", Map.of("%crate%", newId));
            openEdit(player, crate);
            return;
        }

        crateManager.renameCrate(oldId, newId).ifPresentOrElse(renamedCrate -> {
            crateLocationManager.renameCrate(oldId, renamedCrate.getId());
            keyManager.renameVirtualCrate(oldId, renamedCrate.getId());
            keyManager.renamePhysicalKeysForOnlinePlayers(oldId, renamedCrate.getId());
            messageService.send(player, "crate-renamed", Map.of("%old%", oldId, "%new%", renamedCrate.getId()));
            openEdit(player, renamedCrate);
        }, () -> {
            messageService.send(player, "crate-rename-invalid");
            openEdit(player, crate);
        });
    }

    private void setRewardDisplayItem(Player player, Crate crate, Reward reward, ItemStack itemStack) {
        if (!isUsableItem(itemStack)) {
            messageService.send(player, "hold-item");
            return;
        }

        ItemStack displayItem = itemStack.clone();
        displayItem.setAmount(1);
        reward.setDisplayItem(displayItem);
        crateManager.save();
        messageService.send(player, "reward-display-item-updated");
        openRewardEdit(player, crate, reward);
    }

    private void setRewardDropItem(Player player, Crate crate, Reward reward, ItemStack itemStack) {
        if (!isUsableItem(itemStack)) {
            messageService.send(player, "hold-item");
            return;
        }

        reward.setItemReward(itemStack.clone());
        crateManager.save();
        messageService.send(player, "reward-drop-item-updated");
        openRewardEdit(player, crate, reward);
    }

    private void setRewardDisplayName(Reward reward, String name) {
        ItemStack displayItem = reward.getDisplayItem();
        if (displayItem == null || displayItem.getType() == Material.AIR) {
            displayItem = reward.getItemReward();
        }
        if (displayItem == null || displayItem.getType() == Material.AIR) {
            displayItem = new ItemStack(Material.PAPER);
        }

        ItemMeta meta = displayItem.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorUtil.color(name));
            displayItem.setItemMeta(meta);
        }
        reward.setDisplayItem(displayItem);
    }

    private ItemStack selectedItem(Player player, InventoryClickEvent event, boolean singleItem) {
        ItemStack source = isUsableItem(event.getCursor()) ? event.getCursor() : player.getInventory().getItemInMainHand();
        if (!isUsableItem(source)) {
            return null;
        }

        ItemStack copy = source.clone();
        if (singleItem) {
            copy.setAmount(1);
        }
        return copy;
    }

    private ItemStack rewardEditorItem(ItemStack source, Material fallback, String name, List<String> lore) {
        ItemStack base = isUsableItem(source) ? source.clone() : new ItemStack(fallback);
        base.setAmount(1);
        return new ItemBuilder(base).name(name).lore(lore).hideFlags().build();
    }

    private ItemStack keyItem(Crate crate) {
        ItemStack templateItem = crate.getKeyDefinition().getTemplateItem();
        ItemStack base = isUsableItem(templateItem) ? templateItem : new ItemStack(crate.getKeyDefinition().getMaterial());
        return new ItemBuilder(base).name("&eKey Item").lore(List.of(
            "&7Left click: set from hand/cursor.",
            "&7Drag any item here to use it.",
            "&7All item attributes are copied.",
            "&7Only the key name is replaced."
        )).hideFlags().build();
    }

    private ItemStack colorItem(Crate crate) {
        CrateColor color = crateColors.stream()
            .filter(crateColor -> crateColor.code().equals(crate.getColor()))
            .findFirst()
            .orElse(crateColors.get(0));
        return new ItemBuilder(color.icon()).name(color.code() + "Color").lore(List.of(
            "&7Current: " + color.code() + color.name(),
            "&7Click to cycle."
        )).build();
    }

    private ItemStack rewardDisplay(Reward reward, boolean editor) {
        ItemStack base = reward.getDisplayItem() == null ? new ItemStack(Material.PAPER) : reward.getDisplayItem();
        List<String> lore = new ArrayList<>();
        lore.add("&7Display Chance: &f" + reward.getDisplayChance() + "%");
        if (editor) {
            lore.add("&7Real Chance: &f" + reward.getRealChance() + "%");
            lore.add("&7Rarity: &f" + reward.getRarity());
            lore.add("");
            lore.add("&eLeft click to edit.");
            lore.add("&cRight click to delete.");
        } else {
            lore.add("&7Rarity: &f" + reward.getRarity());
        }
        return new ItemBuilder(base).lore(lore).hideFlags().build();
    }

    private String rewardDisplayName(Reward reward) {
        ItemStack displayItem = reward.getDisplayItem();
        if (displayItem == null || !displayItem.hasItemMeta() || !displayItem.getItemMeta().hasDisplayName()) {
            return "&7Not set";
        }
        return displayItem.getItemMeta().getDisplayName();
    }

    private List<String> hologramLore(Crate crate) {
        List<String> lore = new ArrayList<>(crate.getHologram());
        lore.add("");
        lore.add("&7Click to edit in chat.");
        return lore;
    }

    private Material nextBlock(Material current) {
        int index = blockCycle.indexOf(current);
        if (index < 0) {
            return blockCycle.get(0);
        }
        return blockCycle.get((index + 1) % blockCycle.size());
    }

    private void cycleCrateColor(Crate crate) {
        int index = 0;
        for (int i = 0; i < crateColors.size(); i++) {
            if (crateColors.get(i).code().equals(crate.getColor())) {
                index = i;
                break;
            }
        }
        CrateColor nextColor = crateColors.get((index + 1) % crateColors.size());
        crate.setColor(nextColor.code());
        crate.setDisplayName(nextColor.code() + ColorUtil.removeColor(crate.getDisplayName()));
    }

    private String crateColorName(String colorCode) {
        return crateColors.stream()
            .filter(crateColor -> crateColor.code().equals(colorCode))
            .map(CrateColor::name)
            .findFirst()
            .orElse("Custom");
    }

    private void saveAndRefreshHolograms() {
        crateManager.save();
        crateLocationManager.refreshHolograms();
    }

    private void requestChat(Player player, String prompt, java.util.function.Consumer<String> callback) {
        suppressNextClose(player);
        chatInputManager.request(player, prompt, callback);
    }

    private void openMenu(Player player, Inventory inventory) {
        if (player.getOpenInventory().getTopInventory().getHolder() instanceof MenuHolder) {
            suppressNextClose(player);
        }
        player.openInventory(inventory);
    }

    private void suppressNextClose(Player player) {
        suppressedCloses.add(player.getUniqueId());
    }

    private void goBack(Player player, MenuHolder holder) {
        switch (holder.getType()) {
            case MANAGE -> openMain(player);
            case EDIT -> openManage(player);
            case REWARDS -> crateManager.getCrate(holder.getCrateId()).ifPresent(crate -> openEdit(player, crate));
            case REWARD_EDIT -> crateManager.getCrate(holder.getCrateId()).ifPresent(crate -> openRewards(player, crate));
            default -> {
            }
        }
    }

    private ItemStack tag(ItemStack itemStack, NamespacedKey key, String value) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, value);
            itemStack.setItemMeta(meta);
        }
        return itemStack;
    }

    private Optional<String> readTag(ItemStack itemStack, NamespacedKey key) {
        if (itemStack == null || !itemStack.hasItemMeta()) {
            return Optional.empty();
        }
        return Optional.ofNullable(itemStack.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING));
    }

    private boolean isUsableItem(ItemStack itemStack) {
        return itemStack != null && itemStack.getType() != Material.AIR;
    }

    private double parseDouble(String value, double fallback) {
        try {
            return Math.max(0.0D, Double.parseDouble(value.replace(",", ".").trim()));
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private record CrateColor(String name, String code, Material icon) {
    }
}
