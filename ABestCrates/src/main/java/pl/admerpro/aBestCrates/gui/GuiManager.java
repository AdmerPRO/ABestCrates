package pl.admerpro.aBestCrates.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
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
    private final JavaPlugin plugin;
    private final CrateManager crateManager;
    private final KeyManager keyManager;
    private final CrateLocationManager crateLocationManager;
    private final OpeningService openingService;
    private final ChatInputManager chatInputManager;
    private final MessageService messageService;
    private final NamespacedKey crateTag;
    private final NamespacedKey rewardTag;
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

        player.openInventory(inventory);
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
        player.openInventory(inventory);
    }

    public void openEdit(Player player, Crate crate) {
        MenuHolder holder = new MenuHolder(MenuType.EDIT, crate.getId());
        Inventory inventory = Bukkit.createInventory(holder, 54, ColorUtil.color(crate.getDisplayName()));
        holder.setInventory(inventory);

        inventory.setItem(10, new ItemBuilder(Material.NAME_TAG).name("&dDisplay Name").lore(List.of("&f" + crate.getDisplayName(), "&7Click to edit in chat.")).build());
        inventory.setItem(12, new ItemBuilder(crate.getBlockMaterial()).name("&6Block Type").lore(List.of("&f" + crate.getBlockMaterial().name(), "&7Click to cycle.")).build());
        inventory.setItem(14, new ItemBuilder(Material.OAK_SIGN).name("&bHologram").lore(hologramLore(crate)).build());
        inventory.setItem(16, new ItemBuilder(Material.DIAMOND).name("&aRewards").lore(List.of("&7Rewards: &f" + crate.getRewards().size(), "&7Click to manage.")).build());
        inventory.setItem(28, new ItemBuilder(Material.TRIPWIRE_HOOK).name("&eGive Test Key").lore(List.of("&7Gives one physical key to you.")).build());
        inventory.setItem(30, new ItemBuilder(Material.WRITABLE_BOOK).name("&cNo Key Message").lore(List.of("&f" + crate.getNoKeyMessage(), "&7Click to edit in chat.")).build());
        inventory.setItem(32, new ItemBuilder(Material.CLOCK).name("&dAnimation").lore(List.of("&f" + crate.getAnimationType().name(), "&7Click to cycle.")).build());
        inventory.setItem(34, new ItemBuilder(Material.ENDER_EYE).name("&5Preview").lore(List.of("&7Open rewards preview.")).build());
        inventory.setItem(49, new ItemBuilder(Material.LIME_DYE).name("&aSave").lore(List.of("&7Save crate to disk.")).build());

        player.openInventory(inventory);
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

        inventory.setItem(45, new ItemBuilder(Material.LIME_DYE).name("&aAdd Held Item").lore(List.of("&7Adds item from your main hand.", "&7Left click reward: set chances.", "&7Right click reward: delete.")).build());
        inventory.setItem(53, new ItemBuilder(Material.ARROW).name("&eBack").build());
        player.openInventory(inventory);
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

        player.openInventory(inventory);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player) || !(event.getInventory().getHolder() instanceof MenuHolder holder)) {
            return;
        }
        event.setCancelled(true);
        if (event.getRawSlot() < 0 || event.getRawSlot() >= event.getInventory().getSize()) {
            return;
        }

        switch (holder.getType()) {
            case MAIN -> handleMain(player, event.getRawSlot());
            case MANAGE -> handleManage(player, event);
            case EDIT -> handleEdit(player, holder, event.getRawSlot());
            case REWARDS -> handleRewards(player, holder, event);
            case PREVIEW -> {
            }
        }
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

    private void handleEdit(Player player, MenuHolder holder, int slot) {
        Optional<Crate> optionalCrate = crateManager.getCrate(holder.getCrateId());
        if (optionalCrate.isEmpty()) {
            player.closeInventory();
            return;
        }
        Crate crate = optionalCrate.get();

        switch (slot) {
            case 10 -> chatInputManager.request(player, "&eWpisz nowy display name:", input -> {
                crate.setDisplayName(input);
                crateManager.save();
                openEdit(player, crate);
            });
            case 12 -> {
                crate.setBlockMaterial(nextBlock(crate.getBlockMaterial()));
                crateManager.save();
                openEdit(player, crate);
            }
            case 14 -> chatInputManager.request(player, "&eWpisz linie hologramu oddzielone znakiem |:", input -> {
                crate.setHologram(List.of(input.split("\\|")));
                crateManager.save();
                openEdit(player, crate);
            });
            case 16 -> openRewards(player, crate);
            case 28 -> {
                player.getInventory().addItem(keyManager.createPhysicalKey(crate, 1)).values()
                    .forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
                messageService.send(player, "key-given", Map.of("%player%", player.getName(), "%crate%", crate.getId(), "%amount%", "1"));
            }
            case 30 -> chatInputManager.request(player, "&eWpisz wiadomosc braku klucza:", input -> {
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

        crate.getReward(rewardId.get()).ifPresent(reward -> chatInputManager.request(player, "&eWpisz szanse jako real;display, np. 5;10:", input -> {
            String[] parts = input.split(";");
            if (parts.length >= 1) {
                reward.setRealChance(parseDouble(parts[0], reward.getRealChance()));
            }
            if (parts.length >= 2) {
                reward.setDisplayChance(parseDouble(parts[1], reward.getDisplayChance()));
            }
            crateManager.save();
            openRewards(player, crate);
        }));
    }

    private void addHeldItemReward(Player player, Crate crate) {
        ItemStack heldItem = player.getInventory().getItemInMainHand();
        if (heldItem.getType() == Material.AIR) {
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
        openRewards(player, crate);
    }

    private ItemStack rewardDisplay(Reward reward, boolean editor) {
        ItemStack base = reward.getDisplayItem() == null ? new ItemStack(Material.PAPER) : reward.getDisplayItem();
        List<String> lore = new ArrayList<>();
        lore.add("&7Display Chance: &f" + reward.getDisplayChance() + "%");
        if (editor) {
            lore.add("&7Real Chance: &f" + reward.getRealChance() + "%");
            lore.add("&7Rarity: &f" + reward.getRarity());
            lore.add("");
            lore.add("&eLeft click to set chances.");
            lore.add("&cRight click to delete.");
        } else {
            lore.add("&7Rarity: &f" + reward.getRarity());
        }
        return new ItemBuilder(base).addLore("").lore(lore).hideFlags().build();
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

    private double parseDouble(String value, double fallback) {
        try {
            return Math.max(0.0D, Double.parseDouble(value.replace(",", ".").trim()));
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }
}
