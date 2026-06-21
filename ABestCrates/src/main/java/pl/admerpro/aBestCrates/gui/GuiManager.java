package pl.admerpro.aBestCrates.gui;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
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
import pl.admerpro.aBestCrates.model.KeyRequirement;
import pl.admerpro.aBestCrates.model.Reward;
import pl.admerpro.aBestCrates.service.MessageService;
import pl.admerpro.aBestCrates.service.AdvancedOpeningService;
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
    private final ChatInputManager chatInputManager;
    private final MessageService messageService;
    private final AdvancedOpeningService openingService;
    private final NamespacedKey crateTag;
    private final NamespacedKey rewardTag;
    private final NamespacedKey playerTag;
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
                      ChatInputManager chatInputManager, MessageService messageService, AdvancedOpeningService openingService) {
        this.plugin = plugin;
        this.crateManager = crateManager;
        this.keyManager = keyManager;
        this.crateLocationManager = crateLocationManager;
        this.chatInputManager = chatInputManager;
        this.messageService = messageService;
        this.openingService = openingService;
        this.crateTag = new NamespacedKey(plugin, "gui_crate");
        this.rewardTag = new NamespacedKey(plugin, "gui_reward");
        this.playerTag = new NamespacedKey(plugin, "gui_player");
    }

    public void openMain(Player player) {
        MenuHolder holder = new MenuHolder(MenuType.MAIN, null);
        Inventory inventory = Bukkit.createInventory(holder, 27, ColorUtil.component("&5ABestCrates"));
        holder.setInventory(inventory);

        inventory.setItem(10, new ItemBuilder(Material.EMERALD).name("&aCreate Crate").lore(List.of("&7Create a new crate draft.")).build());
        inventory.setItem(12, new ItemBuilder(Material.CHEST).name("&dManage Crates").lore(List.of("&7Open crate editor list.")).build());
        inventory.setItem(14, new ItemBuilder(Material.TRIPWIRE_HOOK).name("&eGive Keys To").lore(List.of(
            "&7Select key type, crate and player.",
            "&7Enter the amount directly in chat.")).build());
        inventory.setItem(16, new ItemBuilder(Material.WRITABLE_BOOK).name("&bLogs & Limits").lore(List.of(
            "&7Reward rolls: &fplugins/ABestCrates/reward-rolls.log",
            "&7Counts and limits: &fplayer-data.yml")).build());
        inventory.setItem(22, new ItemBuilder(Material.REDSTONE).name("&cReload").lore(List.of("&7Reload files from disk.")).build());
        fillEmpty(inventory);
        openMenu(player, inventory);
    }

    public void openManage(Player player) {
        openManage(player, 0);
    }

    private void openManage(Player player, int requestedPage) {
        List<Crate> crates = new ArrayList<>(crateManager.getCrates());
        int page = boundedPage(requestedPage, crates.size(), 45);
        MenuHolder holder = new MenuHolder(MenuType.MANAGE, null, null, page);
        Inventory inventory = Bukkit.createInventory(holder, 54, ColorUtil.component("&5Manage Crates"));
        holder.setInventory(inventory);

        int slot = 0;
        for (Crate crate : pageItems(crates, page, 45)) {
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
        addPageButtons(inventory, page, crates.size(), 45);
        fillEmpty(inventory);
        openMenu(player, inventory);
    }

    public void openEdit(Player player, Crate crate) {
        MenuHolder holder = new MenuHolder(MenuType.EDIT, crate.getId());
        Inventory inventory = Bukkit.createInventory(holder, 54, ColorUtil.component(crate.getDisplayName()));
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
        inventory.setItem(18, new ItemBuilder(crate.getCrateType() == pl.admerpro.aBestCrates.model.CrateType.CHOOSE
            ? Material.LIME_GLAZED_TERRACOTTA : Material.RED_GLAZED_TERRACOTTA).name("&dCrate Type").lore(List.of(
            "&7Current: &f" + crate.getCrateType().name(),
            "&7GAMBLE rolls a weighted reward.",
            "&7CHOOSE lets the player select one.")).build());
        inventory.setItem(19, new ItemBuilder(Material.BLAZE_POWDER).name("&6Particle Effect").lore(List.of(
            "&7Current: &f" + crate.getParticleEffect().name(), "&7Click to cycle effects.")).build());
        inventory.setItem(20, new ItemBuilder(Material.EXPERIENCE_BOTTLE).name("&eVirtual Key Display").lore(List.of(
            "&7Enabled: &f" + crate.isVirtualKeyDisplay(), "&7Shown per-player below the hologram.")).build());
        inventory.setItem(21, new ItemBuilder(Material.IRON_DOOR).name("&cPermission Requirement").lore(List.of(
            "&7Current: &f" + (crate.getPermission().isBlank() ? "none" : crate.getPermission()), "&7Click to edit.")).build());
        inventory.setItem(23, new ItemBuilder(Material.CLOCK).name("&bOpen Cooldown").lore(List.of(
            "&7Seconds: &f" + crate.getCooldownSeconds(), "&7Click to edit.")).build());
        inventory.setItem(24, new ItemBuilder(Material.GOLD_INGOT).name("&6Open Cost").lore(List.of(
            "&7Vault cost: &f" + crate.getOpenCost(), "&7Set 0 to disable.")).build());
        inventory.setItem(25, new ItemBuilder(Material.SLIME_BALL).name("&aPushback").lore(List.of(
            "&7Enabled: &f" + crate.isPushback(), "&7Pushes away players without access.")).build());
        inventory.setItem(26, new ItemBuilder(Material.TRIPWIRE_HOOK).name("&eKey Requirements").lore(keyRequirementLore(crate)).build());
        inventory.setItem(29, new ItemBuilder(Material.MAP).name("&bPreview GUI Title").lore(List.of(
            "&f" + crate.getPreviewTitle(), "&7Click to edit.")).build());
        inventory.setItem(31, new ItemBuilder(Material.PAINTING).name("&dOpening GUI Title").lore(List.of(
            "&f" + crate.getOpeningTitle(), "&7Click to edit.")).build());
        inventory.setItem(33, new ItemBuilder(Material.NETHER_STAR).name("&6Milestones").lore(milestoneLore(crate)).build());
        inventory.setItem(35, new ItemBuilder(Material.CHEST_MINECART).name("&bRewards Per Open").lore(List.of(
            "&7Current: &f" + crate.getRewardRolls(), "&7Range: 1-9", "&7Click to edit.")).build());
        inventory.setItem(46, new ItemBuilder(crate.getBlockMaterial()).name("&aGet Crate Item").lore(List.of(
            "&7Gives a placeable linked crate item.")).build());
        inventory.setItem(49, new ItemBuilder(Material.LIME_DYE).name("&aSave").lore(List.of("&7Save crate to disk.")).build());
        fillEmpty(inventory);
        openMenu(player, inventory);
    }

    public void openRewards(Player player, Crate crate) {
        openRewards(player, crate, 0);
    }

    private void openRewards(Player player, Crate crate, int requestedPage) {
        List<Reward> rewards = new ArrayList<>(crate.getRewards());
        int page = boundedPage(requestedPage, rewards.size(), 45);
        MenuHolder holder = new MenuHolder(MenuType.REWARDS, crate.getId(), null, page);
        Inventory inventory = Bukkit.createInventory(holder, 54, ColorUtil.component("&5Rewards: &f" + crate.getId()));
        holder.setInventory(inventory);

        int slot = 0;
        for (Reward reward : pageItems(rewards, page, 45)) {
            inventory.setItem(slot++, tag(rewardDisplay(reward, true), rewardTag, reward.getId()));
        }

        inventory.setItem(45, new ItemBuilder(Material.LIME_DYE).name("&aAdd Held Item").lore(List.of("&7Adds item from your main hand.")).build());
        inventory.setItem(53, new ItemBuilder(Material.ARROW).name("&eBack").build());
        if (page > 0) inventory.setItem(46, new ItemBuilder(Material.ARROW).name("&ePrevious Page").build());
        if ((page + 1) * 45 < rewards.size()) inventory.setItem(52, new ItemBuilder(Material.ARROW).name("&eNext Page").build());
        fillEmpty(inventory);
        openMenu(player, inventory);
    }

    public void openRewardEdit(Player player, Crate crate, Reward reward) {
        MenuHolder holder = new MenuHolder(MenuType.REWARD_EDIT, crate.getId(), reward.getId());
        Inventory inventory = Bukkit.createInventory(holder, 54, ColorUtil.component("&5Reward: &f" + reward.getId()));
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
        inventory.setItem(18, new ItemBuilder(Material.AMETHYST_SHARD).name("&dRarity").lore(List.of(
            "&7Current: &f" + reward.getRarity(), "&7Affects weight multiplier.")).build());
        inventory.setItem(20, new ItemBuilder(Material.HEAVY_CORE).name("&bWeight").lore(List.of(
            "&7Current: &f" + reward.getWeight(), "&7Auto-scaled against other rewards.")).build());
        inventory.setItem(24, new ItemBuilder(Material.COMMAND_BLOCK).name("&eCommands").lore(List.of(
            "&7Configured: &f" + reward.getCommands().size(), "&7Separate commands with |.")).build());
        inventory.setItem(26, new ItemBuilder(Material.IRON_DOOR).name("&aRequired Permissions").lore(List.of(
            "&7Configured: &f" + reward.getRequiredPermissions().size(), "&7Comma-separated; all are required.")).build());
        inventory.setItem(28, new ItemBuilder(Material.BARRIER).name("&cBlocked Permissions").lore(List.of(
            "&7Configured: &f" + reward.getBlockedPermissions().size(), "&7Comma-separated; any blocks reward.")).build());
        inventory.setItem(30, new ItemBuilder(Material.BELL).name("&6Broadcast").lore(List.of(
            "&7Enabled: &f" + reward.isBroadcast())).build());
        inventory.setItem(32, new ItemBuilder(Material.FIREWORK_ROCKET).name("&dFireworks").lore(List.of(
            "&7Enabled: &f" + reward.isFireworks())).build());
        inventory.setItem(34, new ItemBuilder(Material.REPEATER).name("&cReward Limits").lore(List.of(
            "&7Global: &f" + reward.getGlobalLimit(), "&7Per player: &f" + reward.getPlayerLimit(),
            "&7Enter: global,player")).build());
        inventory.setItem(36, new ItemBuilder(Material.BUNDLE).name("&aReward Items").lore(List.of(
            "&7Items: &f" + reward.getItemRewards().size() + "/27", "&7Click to edit bundle.")).build());
        inventory.setItem(49, new ItemBuilder(Material.LIME_DYE).name("&aSave").lore(List.of("&7Save reward to disk.")).build());
        inventory.setItem(53, new ItemBuilder(Material.ARROW).name("&eBack").build());
        fillEmpty(inventory);
        openMenu(player, inventory);
    }

    public void openPreview(Player player, Crate crate) {
        openPreview(player, crate, false);
    }

    public void openPreview(Player player, Crate crate, boolean returnToSettings) {
        openPreview(player, crate, returnToSettings, 0);
    }

    private void openPreview(Player player, Crate crate, boolean returnToSettings, int requestedPage) {
        List<Reward> rewards = new ArrayList<>(crate.getRewards());
        int page = boundedPage(requestedPage, rewards.size(), 28);
        MenuHolder holder = new MenuHolder(MenuType.PREVIEW, crate.getId(), returnToSettings ? "settings" : null, page);
        String title = crate.getPreviewTitle().replace("%crate%", crate.getId())
            .replace("%crate_displayname%", ColorUtil.removeColor(crate.getDisplayName()));
        Inventory inventory = Bukkit.createInventory(holder, 54, ColorUtil.component(title));
        holder.setInventory(inventory);

        int[] previewSlots = {10,11,12,13,14,15,16,19,20,21,22,23,24,25,
            28,29,30,31,32,33,34,37,38,39,40,41,42,43};
        List<Reward> visible = pageItems(rewards, page, previewSlots.length);
        for (int index = 0; index < visible.size(); index++) {
            inventory.setItem(previewSlots[index], rewardDisplay(visible.get(index), false));
        }
        inventory.setItem(48, new ItemBuilder(Material.LIME_DYE).name("&aOpen this crate").lore(List.of(
            "&7Uses the configured key requirements.")).build());
        inventory.setItem(50, new ItemBuilder(Material.LIGHT_BLUE_DYE).name("&bOpen with all keys").lore(List.of(
            "&7Opens as many times as possible.")).build());
        if (returnToSettings) inventory.setItem(45, new ItemBuilder(Material.ARROW).name("&eBack to Settings").build());
        if (page > 0) inventory.setItem(46, new ItemBuilder(Material.ARROW).name("&ePrevious Page").build());
        if ((page + 1) * previewSlots.length < rewards.size()) inventory.setItem(52, new ItemBuilder(Material.ARROW).name("&eNext Page").build());
        fillEmpty(inventory);
        openMenu(player, inventory);
    }

    public void openGiveKeyType(Player player) {
        MenuHolder holder = new MenuHolder(MenuType.GIVE_KEY_TYPE, null);
        Inventory inventory = Bukkit.createInventory(holder, 27, ColorUtil.component("&5Give Keys To"));
        holder.setInventory(inventory);
        inventory.setItem(11, new ItemBuilder(Material.TRIPWIRE_HOOK).name("&ePhysical Keys").lore(List.of(
            "&7Give tradable key items.")).build());
        inventory.setItem(15, new ItemBuilder(Material.ENDER_EYE).name("&dVirtual Keys").lore(List.of(
            "&7Give account-bound virtual keys.")).build());
        inventory.setItem(22, new ItemBuilder(Material.ARROW).name("&eBack").build());
        fillEmpty(inventory);
        openMenu(player, inventory);
    }

    private void openGiveKeyCrates(Player player, String mode) {
        openGiveKeyCrates(player, mode, 0);
    }

    private void openGiveKeyCrates(Player player, String mode, int requestedPage) {
        List<Crate> crates = new ArrayList<>(crateManager.getCrates());
        int page = boundedPage(requestedPage, crates.size(), 45);
        MenuHolder holder = new MenuHolder(MenuType.GIVE_KEY_CRATE, mode, null, page);
        Inventory inventory = Bukkit.createInventory(holder, 54, ColorUtil.component("&5Select Crate"));
        holder.setInventory(inventory);
        int slot = 0;
        for (Crate crate : pageItems(crates, page, 45)) {
            inventory.setItem(slot++, tag(new ItemBuilder(crate.getBlockMaterial()).name(crate.getDisplayName())
                .lore(List.of("&7Technical: &f" + crate.getId(), "&eClick to select.")).build(), crateTag, crate.getId()));
        }
        inventory.setItem(49, new ItemBuilder(Material.ARROW).name("&eBack").build());
        addPageButtons(inventory, page, crates.size(), 45);
        fillEmpty(inventory);
        openMenu(player, inventory);
    }

    private void openGiveKeyPlayers(Player player, Crate crate, String mode) {
        MenuHolder holder = new MenuHolder(MenuType.GIVE_KEY_PLAYER, crate.getId(), mode);
        Inventory inventory = Bukkit.createInventory(holder, 54, ColorUtil.component("&5Select Player"));
        holder.setInventory(inventory);
        int slot = 0;
        for (Player target : Bukkit.getOnlinePlayers()) {
            if (slot >= 45) {
                break;
            }
            inventory.setItem(slot++, tag(new ItemBuilder(Material.PLAYER_HEAD).name("&f" + target.getName())
                .lore(List.of("&eClick to select.")).build(), playerTag, target.getName()));
        }
        inventory.setItem(49, new ItemBuilder(Material.ARROW).name("&eBack").build());
        fillEmpty(inventory);
        openMenu(player, inventory);
    }

    private void openRewardItems(Player player, Crate crate, Reward reward) {
        MenuHolder holder = new MenuHolder(MenuType.REWARD_ITEMS, crate.getId(), reward.getId());
        Inventory inventory = Bukkit.createInventory(holder, 36, ColorUtil.component("&5Reward Items (max 27)"));
        holder.setInventory(inventory);
        List<ItemStack> items = reward.getItemRewards();
        for (int index = 0; index < Math.min(27, items.size()); index++) {
            inventory.setItem(index, items.get(index));
        }
        inventory.setItem(31, new ItemBuilder(Material.LIME_DYE).name("&aSave").build());
        inventory.setItem(35, new ItemBuilder(Material.ARROW).name("&eBack").build());
        for (int slot = 27; slot < inventory.getSize(); slot++) {
            if (inventory.getItem(slot) == null) inventory.setItem(slot, filler());
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
        if (holder.getType() == MenuType.REWARD_ITEMS) {
            handleRewardItemsClick(player, holder, event);
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
            case GIVE_KEY_TYPE -> handleGiveKeyType(player, event.getRawSlot());
            case GIVE_KEY_CRATE -> handleGiveKeyCrate(player, holder, event);
            case GIVE_KEY_PLAYER -> handleGiveKeyPlayer(player, holder, event);
            case PREVIEW -> handlePreview(player, holder, event.getRawSlot());
            case OPENING, CHOOSE_OPEN, REWARD_ITEMS -> { }
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
        if (holder.getType() == MenuType.REWARD_ITEMS) {
            boolean onlyEditableSlots = event.getRawSlots().stream()
                .filter(slot -> slot < event.getInventory().getSize())
                .allMatch(slot -> slot < 27);
            event.setCancelled(!onlyEditableSlots);
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

        if (holder.getType() == MenuType.REWARD_ITEMS) {
            saveRewardItems(holder, event.getInventory());
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
            case 14 -> openGiveKeyType(player);
            case 22 -> {
                plugin.reloadConfig();
                messageService.reload();
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
        MenuHolder holder = (MenuHolder) event.getInventory().getHolder();
        if (event.getRawSlot() == 45 && holder.getPage() > 0) {
            openManage(player, holder.getPage() - 1);
            return;
        }
        if (event.getRawSlot() == 53) {
            openManage(player, holder.getPage() + 1);
            return;
        }
        if (event.getRawSlot() == 49) {
            openMain(player);
            return;
        }

        readTag(event.getCurrentItem(), crateTag)
            .flatMap(crateManager::getCrate)
            .ifPresent(crate -> openEdit(player, crate));
    }

    private void handleGiveKeyType(Player player, int slot) {
        switch (slot) {
            case 11 -> openGiveKeyCrates(player, "physical");
            case 15 -> openGiveKeyCrates(player, "virtual");
            case 22 -> openMain(player);
            default -> { }
        }
    }

    private void handleGiveKeyCrate(Player player, MenuHolder holder, InventoryClickEvent event) {
        if (event.getRawSlot() == 45 && holder.getPage() > 0) {
            openGiveKeyCrates(player, holder.getCrateId(), holder.getPage() - 1);
            return;
        }
        if (event.getRawSlot() == 53) {
            openGiveKeyCrates(player, holder.getCrateId(), holder.getPage() + 1);
            return;
        }
        if (event.getRawSlot() == 49) {
            openGiveKeyType(player);
            return;
        }
        readTag(event.getCurrentItem(), crateTag).flatMap(crateManager::getCrate)
            .ifPresent(crate -> openGiveKeyPlayers(player, crate, holder.getCrateId()));
    }

    private void handleGiveKeyPlayer(Player player, MenuHolder holder, InventoryClickEvent event) {
        if (event.getRawSlot() == 49) {
            crateManager.getCrate(holder.getCrateId())
                .ifPresent(crate -> openGiveKeyCrates(player, holder.getRewardId()));
            return;
        }
        String targetName = readTag(event.getCurrentItem(), playerTag).orElse(null);
        Player target = targetName == null ? null : Bukkit.getPlayerExact(targetName);
        Crate crate = crateManager.getCrate(holder.getCrateId()).orElse(null);
        if (target == null || crate == null) {
            return;
        }
        requestChat(player, "&eEnter the number of keys to give:", input -> {
            int amount = Math.max(1, parsePositiveInt(input, 1));
            if ("virtual".equalsIgnoreCase(holder.getRewardId())) {
                keyManager.addVirtualKeys(target, crate.getId(), amount);
                messageService.send(player, "virtual-keys-added", Map.of(
                    "%player%", target.getName(), "%crate%", crate.getId(), "%amount%", String.valueOf(amount)));
            } else {
                givePhysicalKeys(target, crate, amount);
                messageService.send(player, "key-given", Map.of(
                    "%player%", target.getName(), "%crate%", crate.getId(), "%amount%", String.valueOf(amount)));
            }
            openMain(player);
        });
    }

    private void handleRewardItemsClick(Player player, MenuHolder holder, InventoryClickEvent event) {
        int rawSlot = event.getRawSlot();
        int topSize = event.getView().getTopInventory().getSize();
        if (rawSlot < 27) {
            return;
        }
        if (rawSlot >= topSize) {
            if (event.isShiftClick()) {
                event.setCancelled(true);
                ItemStack source = event.getCurrentItem();
                if (!isUsableItem(source)) {
                    return;
                }
                Inventory top = event.getView().getTopInventory();
                for (int slot = 0; slot < 27; slot++) {
                    if (!isUsableItem(top.getItem(slot))) {
                        top.setItem(slot, source.clone());
                        event.setCurrentItem(null);
                        return;
                    }
                }
            }
            return;
        }
        event.setCancelled(true);
        if (rawSlot != 31 && rawSlot != 35) {
            return;
        }
        saveRewardItems(holder, event.getView().getTopInventory());
        crateManager.getCrate(holder.getCrateId()).ifPresent(crate ->
            crate.getReward(holder.getRewardId()).ifPresent(reward -> openRewardEdit(player, crate, reward)));
    }

    private void saveRewardItems(MenuHolder holder, Inventory inventory) {
        Crate crate = crateManager.getCrate(holder.getCrateId()).orElse(null);
        Reward reward = crate == null ? null : crate.getReward(holder.getRewardId()).orElse(null);
        if (reward == null) {
            return;
        }
        List<ItemStack> items = new ArrayList<>();
        for (int slot = 0; slot < 27; slot++) {
            ItemStack item = inventory.getItem(slot);
            if (isUsableItem(item)) {
                items.add(item.clone());
            }
        }
        reward.setItemRewards(items);
        crateManager.save();
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
            case 34 -> openPreview(player, crate, true);
            case 18 -> {
                crate.setCrateType(crate.getCrateType().next());
                crateManager.save();
                openEdit(player, crate);
            }
            case 19 -> {
                crate.setParticleEffect(crate.getParticleEffect().next());
                crateManager.save();
                openEdit(player, crate);
            }
            case 20 -> {
                crate.setVirtualKeyDisplay(!crate.isVirtualKeyDisplay());
                saveAndRefreshHolograms();
                openEdit(player, crate);
            }
            case 21 -> requestChat(player, "&eEnter permission or 'none':", input -> {
                crate.setPermission(input.equalsIgnoreCase("none") ? "" : input);
                crateManager.save();
                openEdit(player, crate);
            });
            case 23 -> requestChat(player, "&eEnter cooldown in seconds (0 disables):", input -> {
                crate.setCooldownSeconds(parsePositiveLong(input, 0L));
                crateManager.save();
                openEdit(player, crate);
            });
            case 24 -> requestChat(player, "&eEnter Vault open cost (0 disables):", input -> {
                crate.setOpenCost(parseDouble(input, crate.getOpenCost()));
                crateManager.save();
                openEdit(player, crate);
            });
            case 25 -> {
                crate.setPushback(!crate.isPushback());
                crateManager.save();
                openEdit(player, crate);
            }
            case 26 -> requestChat(player, "&eEnter requirements: crate:amount,crate2:amount", input -> {
                crate.setKeyRequirements(parseKeyRequirements(input));
                crateManager.save();
                openEdit(player, crate);
            });
            case 29 -> requestChat(player, "&eEnter preview title:", input -> {
                crate.setPreviewTitle(input);
                crateManager.save();
                openEdit(player, crate);
            });
            case 31 -> requestChat(player, "&eEnter opening GUI title:", input -> {
                crate.setOpeningTitle(input);
                crateManager.save();
                openEdit(player, crate);
            });
            case 33 -> requestChat(player, "&eEnter milestones: amount:rewardId,amount:rewardId", input -> {
                crate.setMilestones(parseMilestones(input));
                crateManager.save();
                openEdit(player, crate);
            });
            case 35 -> requestChat(player, "&eHow many different rewards per open? (1-9):", input -> {
                crate.setRewardRolls(Math.max(1, Math.min(9, parsePositiveInt(input, crate.getRewardRolls()))));
                crateManager.save();
                openEdit(player, crate);
            });
            case 46 -> {
                player.getInventory().addItem(keyManager.createCrateItem(crate, 1)).values()
                    .forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
                messageService.send(player, "crate-item-given", Map.of("%crate%", crate.getId()));
            }
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

        if (event.getRawSlot() == 46 && holder.getPage() > 0) {
            openRewards(player, crate, holder.getPage() - 1);
            return;
        }
        if (event.getRawSlot() == 52) {
            openRewards(player, crate, holder.getPage() + 1);
            return;
        }

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
            openRewards(player, crate, holder.getPage());
            return;
        }

        crate.getReward(rewardId.get()).ifPresent(reward -> openRewardEdit(player, crate, reward));
    }

    private void handlePreview(Player player, MenuHolder holder, int slot) {
        Crate crate = crateManager.getCrate(holder.getCrateId()).orElse(null);
        if (crate == null) {
            return;
        }
        boolean settings = "settings".equals(holder.getRewardId());
        switch (slot) {
            case 45 -> {
                if (settings) openEdit(player, crate);
            }
            case 46 -> openPreview(player, crate, settings, holder.getPage() - 1);
            case 48 -> {
                if (crate.getAnimationType() != pl.admerpro.aBestCrates.model.AnimationType.INSTANT
                    || crate.getCrateType() == pl.admerpro.aBestCrates.model.CrateType.CHOOSE) {
                    suppressNextClose(player);
                }
                openingService.open(player, crate);
            }
            case 50 -> openingService.openAllKeys(player, crate);
            case 52 -> openPreview(player, crate, settings, holder.getPage() + 1);
            default -> { }
        }
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
            case 18 -> requestChat(player, "&eEnter rarity name:", input -> {
                reward.setRarity(input);
                crateManager.save();
                openRewardEdit(player, crate, reward);
            });
            case 20 -> requestChat(player, "&eEnter reward weight:", input -> {
                reward.setWeight(parseDouble(input, reward.getWeight()));
                crateManager.save();
                openRewardEdit(player, crate, reward);
            });
            case 24 -> requestChat(player, "&eEnter commands separated with | (or none):", input -> {
                reward.setCommands(parseList(input, "\\|"));
                crateManager.save();
                openRewardEdit(player, crate, reward);
            });
            case 26 -> requestChat(player, "&eEnter required permissions separated with commas (or none):", input -> {
                reward.setRequiredPermissions(parseList(input, ","));
                crateManager.save();
                openRewardEdit(player, crate, reward);
            });
            case 28 -> requestChat(player, "&eEnter blocked permissions separated with commas (or none):", input -> {
                reward.setBlockedPermissions(parseList(input, ","));
                crateManager.save();
                openRewardEdit(player, crate, reward);
            });
            case 30 -> {
                reward.setBroadcast(!reward.isBroadcast());
                crateManager.save();
                openRewardEdit(player, crate, reward);
            }
            case 32 -> {
                reward.setFireworks(!reward.isFireworks());
                crateManager.save();
                openRewardEdit(player, crate, reward);
            }
            case 34 -> requestChat(player, "&eEnter limits as global,player (0 disables):", input -> {
                String[] values = input.split(",", 2);
                reward.setGlobalLimit(parsePositiveInt(values[0], reward.getGlobalLimit()));
                if (values.length > 1) {
                    reward.setPlayerLimit(parsePositiveInt(values[1], reward.getPlayerLimit()));
                }
                crateManager.save();
                openRewardEdit(player, crate, reward);
            });
            case 36 -> openRewardItems(player, crate, reward);
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
            meta.displayName(ColorUtil.component(name));
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
        if (displayItem == null || !displayItem.hasItemMeta()) {
            return "&7Not set";
        }
        ItemMeta meta = displayItem.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) {
            return "&7Not set";
        }
        return ColorUtil.legacy(meta.displayName());
    }

    private List<String> hologramLore(Crate crate) {
        List<String> lore = new ArrayList<>(crate.getHologram());
        lore.add("");
        lore.add("&7Click to edit in chat.");
        return lore;
    }

    private List<String> keyRequirementLore(Crate crate) {
        List<String> lore = new ArrayList<>();
        for (KeyRequirement requirement : crate.getKeyRequirements()) {
            lore.add("&7" + requirement.crateId() + ": &f" + requirement.amount());
        }
        lore.add("");
        lore.add("&7Click to edit: crate:amount,...");
        return lore;
    }

    private List<String> milestoneLore(Crate crate) {
        List<String> lore = new ArrayList<>();
        crate.getMilestones().forEach((amount, rewardId) -> lore.add("&7" + amount + " opens: &f" + rewardId));
        if (crate.getMilestones().isEmpty()) {
            lore.add("&7No milestones configured.");
        }
        lore.add("");
        lore.add("&7Click to edit.");
        return lore;
    }

    private List<KeyRequirement> parseKeyRequirements(String input) {
        List<KeyRequirement> requirements = new ArrayList<>();
        if (input.equalsIgnoreCase("none")) {
            return requirements;
        }
        for (String entry : input.split(",")) {
            String[] values = entry.trim().split(":", 2);
            if (!values[0].isBlank()) {
                requirements.add(new KeyRequirement(values[0], values.length > 1 ? parsePositiveInt(values[1], 1) : 1));
            }
        }
        return requirements;
    }

    private Map<Integer, String> parseMilestones(String input) {
        Map<Integer, String> milestones = new LinkedHashMap<>();
        if (input.equalsIgnoreCase("none")) {
            return milestones;
        }
        for (String entry : input.split(",")) {
            String[] values = entry.trim().split(":", 2);
            if (values.length == 2) {
                int amount = parsePositiveInt(values[0], 0);
                if (amount > 0 && !values[1].isBlank()) {
                    milestones.put(amount, values[1].trim());
                }
            }
        }
        return milestones;
    }

    private List<String> parseList(String input, String separatorRegex) {
        if (input.equalsIgnoreCase("none") || input.isBlank()) {
            return List.of();
        }
        return java.util.Arrays.stream(input.split(separatorRegex)).map(String::trim)
            .filter(value -> !value.isBlank()).toList();
    }

    private void givePhysicalKeys(Player target, Crate crate, int requestedAmount) {
        int remaining = Math.max(1, requestedAmount);
        int maxStack = Math.max(1, crate.getKeyDefinition().getMaterial().getMaxStackSize());
        while (remaining > 0) {
            int amount = Math.min(maxStack, remaining);
            target.getInventory().addItem(keyManager.createPhysicalKey(crate, amount)).values()
                .forEach(item -> target.getWorld().dropItemNaturally(target.getLocation(), item));
            remaining -= amount;
        }
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
        MenuHolder previousMenu = player.getOpenInventory().getTopInventory().getHolder() instanceof MenuHolder holder ? holder : null;
        suppressNextClose(player);
        chatInputManager.request(player, prompt, callback, () -> reopenMenu(player, previousMenu));
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
            case REWARD_ITEMS -> crateManager.getCrate(holder.getCrateId()).ifPresent(crate ->
                crate.getReward(holder.getRewardId()).ifPresent(reward -> openRewardEdit(player, crate, reward)));
            case PREVIEW -> {
                if ("settings".equals(holder.getRewardId())) {
                    crateManager.getCrate(holder.getCrateId()).ifPresent(crate -> openEdit(player, crate));
                }
            }
            case GIVE_KEY_TYPE -> openMain(player);
            case GIVE_KEY_CRATE -> openGiveKeyType(player);
            case GIVE_KEY_PLAYER -> openGiveKeyCrates(player, holder.getRewardId());
            default -> {
            }
        }
    }

    private void reopenMenu(Player player, MenuHolder holder) {
        if (holder == null) {
            return;
        }

        switch (holder.getType()) {
            case MAIN -> openMain(player);
            case MANAGE -> openManage(player, holder.getPage());
            case EDIT -> crateManager.getCrate(holder.getCrateId()).ifPresent(crate -> openEdit(player, crate));
            case REWARDS -> crateManager.getCrate(holder.getCrateId()).ifPresent(crate -> openRewards(player, crate, holder.getPage()));
            case REWARD_EDIT -> crateManager.getCrate(holder.getCrateId()).ifPresent(crate ->
                crate.getReward(holder.getRewardId()).ifPresent(reward -> openRewardEdit(player, crate, reward)));
            case REWARD_ITEMS -> crateManager.getCrate(holder.getCrateId()).ifPresent(crate ->
                crate.getReward(holder.getRewardId()).ifPresent(reward -> openRewardItems(player, crate, reward)));
            case PREVIEW -> crateManager.getCrate(holder.getCrateId()).ifPresent(crate ->
                openPreview(player, crate, "settings".equals(holder.getRewardId()), holder.getPage()));
            case GIVE_KEY_TYPE -> openGiveKeyType(player);
            case GIVE_KEY_CRATE -> openGiveKeyCrates(player, holder.getCrateId(), holder.getPage());
            case GIVE_KEY_PLAYER -> crateManager.getCrate(holder.getCrateId()).ifPresent(crate ->
                openGiveKeyPlayers(player, crate, holder.getRewardId()));
            case OPENING, CHOOSE_OPEN -> { }
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

    private ItemStack filler() {
        return new ItemBuilder(Material.LIGHT_GRAY_STAINED_GLASS_PANE).name(" ").build();
    }

    private void fillEmpty(Inventory inventory) {
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            if (inventory.getItem(slot) == null) {
                inventory.setItem(slot, filler());
            }
        }
    }

    private void addPageButtons(Inventory inventory, int page, int itemCount, int pageSize) {
        if (page > 0) {
            inventory.setItem(45, new ItemBuilder(Material.ARROW).name("&ePrevious Page").build());
        }
        if ((page + 1) * pageSize < itemCount) {
            inventory.setItem(53, new ItemBuilder(Material.ARROW).name("&eNext Page").build());
        }
    }

    private int boundedPage(int requestedPage, int itemCount, int pageSize) {
        int maximum = Math.max(0, Math.max(0, itemCount - 1) / pageSize);
        return Math.max(0, Math.min(requestedPage, maximum));
    }

    private <T> List<T> pageItems(List<T> items, int page, int pageSize) {
        int start = Math.min(items.size(), page * pageSize);
        int end = Math.min(items.size(), start + pageSize);
        return items.subList(start, end);
    }

    private double parseDouble(String value, double fallback) {
        try {
            return Math.max(0.0D, Double.parseDouble(value.replace(",", ".").trim()));
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private int parsePositiveInt(String value, int fallback) {
        try {
            return Math.max(0, Integer.parseInt(value.trim()));
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private long parsePositiveLong(String value, long fallback) {
        try {
            return Math.max(0L, Long.parseLong(value.trim()));
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private record CrateColor(String name, String code, Material icon) {
    }
}
