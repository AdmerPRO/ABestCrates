package pl.admerpro.aBestCrates.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import pl.admerpro.aBestCrates.gui.MenuHolder;
import pl.admerpro.aBestCrates.gui.MenuType;
import pl.admerpro.aBestCrates.manager.KeyManager;
import pl.admerpro.aBestCrates.model.AnimationType;
import pl.admerpro.aBestCrates.model.Crate;
import pl.admerpro.aBestCrates.model.Reward;
import pl.admerpro.aBestCrates.util.ColorUtil;

public class OpeningService {
    private final JavaPlugin plugin;
    private final KeyManager keyManager;
    private final MessageService messageService;
    private final Random random = new Random();

    public OpeningService(JavaPlugin plugin, KeyManager keyManager, MessageService messageService) {
        this.plugin = plugin;
        this.keyManager = keyManager;
        this.messageService = messageService;
    }

    public void open(Player player, Crate crate) {
        open(player, crate, false);
    }

    public void forceOpen(Player player, Crate crate) {
        open(player, crate, true);
    }

    public void openAllKeys(Player player, Crate crate) {
        if (!player.hasPermission("abestcrates.use")) {
            messageService.send(player, "no-permission");
            return;
        }

        int physicalKeys = keyManager.countPhysicalKeys(player, crate);
        int virtualKeys = keyManager.getVirtualKeys(player.getUniqueId(), crate.getId());
        int keyAmount = physicalKeys + virtualKeys;
        if (keyAmount <= 0) {
            player.sendMessage(ColorUtil.component(applyPlaceholders(crate.getNoKeyMessage(), player, crate, null)));
            return;
        }

        List<Reward> rewards = new ArrayList<>();
        for (int index = 0; index < keyAmount; index++) {
            Reward reward = crate.rollReward(random).orElse(null);
            if (reward == null) {
                messageService.send(player, "crate-empty", Map.of("%crate%", crate.getId()));
                return;
            }
            rewards.add(reward);
        }

        if (!hasSpaceForRewardsAfterKeys(player, crate, physicalKeys, rewards)) {
            messageService.send(player, "not-enough-inventory-space", Map.of("%amount%", String.valueOf(keyAmount)));
            return;
        }

        keyManager.consumePhysicalKeys(player, crate, physicalKeys);
        if (virtualKeys > 0) {
            keyManager.removeVirtualKeys(player.getUniqueId(), crate.getId(), virtualKeys);
        }
        for (Reward reward : rewards) {
            giveItemReward(player, reward);
            executeCommands(player, crate, reward);
            if (reward.isFireworks()) {
                spawnFirework(player);
            }
            if (reward.isBroadcast()) {
                broadcastReward(player, crate, reward);
            }
        }

        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 1.25F);
        messageService.send(player, "bulk-opened", Map.of(
            "%amount%", String.valueOf(keyAmount),
            "%crate%", crate.getId(),
            "%crate_displayname%", crate.getDisplayName()
        ));
    }

    private void open(Player player, Crate crate, boolean forced) {
        if (!forced && !player.hasPermission("abestcrates.use")) {
            messageService.send(player, "no-permission");
            return;
        }

        if (!forced && !keyManager.consumeAnyKey(player, crate)) {
            player.sendMessage(ColorUtil.component(applyPlaceholders(crate.getNoKeyMessage(), player, crate, null)));
            return;
        }

        Reward reward = crate.rollReward(random).orElse(null);
        if (reward == null) {
            messageService.send(player, "crate-empty", Map.of("%crate%", crate.getId()));
            return;
        }

        if (!forced && crate.getAnimationType() != AnimationType.INSTANT) {
            playAnimation(player, crate, reward);
            return;
        }

        completeOpen(player, crate, reward);
    }

    private void completeOpen(Player player, Crate crate, Reward reward) {
        giveItemReward(player, reward);
        executeCommands(player, crate, reward);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 1.25F);

        if (reward.isFireworks()) {
            spawnFirework(player);
        }

        messageService.send(player, "reward-received", Map.of(
            "%crate%", crate.getId(),
            "%crate_displayname%", crate.getDisplayName(),
            "%reward%", rewardName(reward)
        ));

        if (reward.isBroadcast()) {
            broadcastReward(player, crate, reward);
        }
    }

    private void playAnimation(Player player, Crate crate, Reward reward) {
        MenuHolder holder = new MenuHolder(MenuType.PREVIEW, crate.getId());
        Inventory inventory = Bukkit.createInventory(holder, 27, ColorUtil.component("&5Opening: &f" + ColorUtil.removeColor(crate.getDisplayName())));
        holder.setInventory(inventory);
        ItemStack centerMarker = centerMarker();
        inventory.setItem(4, centerMarker);
        inventory.setItem(22, centerMarker);
        player.openInventory(inventory);

        List<ItemStack> displayItems = crate.getRewards().stream()
            .map(Reward::getDisplayItem)
            .filter(itemStack -> itemStack != null && !itemStack.getType().isAir())
            .toList();
        if (displayItems.isEmpty()) {
            displayItems = List.of(new ItemStack(Material.PAPER));
        }

        int iterations = switch (crate.getAnimationType()) {
            case FAST -> 10;
            case CLASSIC -> 16;
            case INSTANT -> 0;
        };
        long period = crate.getAnimationType() == AnimationType.FAST ? 1L : 2L;
        List<ItemStack> animationItems = displayItems;

        new BukkitRunnable() {
            private int tick;

            @Override
            public void run() {
                if (tick >= iterations) {
                    inventory.clear();
                    inventory.setItem(4, centerMarker);
                    inventory.setItem(13, rewardDisplayItem(reward));
                    inventory.setItem(22, centerMarker);
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 1.35F);
                    completeOpen(player, crate, reward);
                    cancel();
                    return;
                }

                for (int slot = 9; slot <= 17; slot++) {
                    inventory.setItem(slot, animationItems.get(random.nextInt(animationItems.size())).clone());
                }
                inventory.setItem(4, centerMarker);
                inventory.setItem(13, animationItems.get(random.nextInt(animationItems.size())).clone());
                inventory.setItem(22, centerMarker);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7F, 1.4F);
                tick++;
            }
        }.runTaskTimer(plugin, 0L, period);
    }

    private void giveItemReward(Player player, Reward reward) {
        ItemStack itemReward = reward.getItemReward();
        if (itemReward == null) {
            return;
        }

        Map<Integer, ItemStack> leftovers = player.getInventory().addItem(itemReward);
        leftovers.values().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
    }

    private void executeCommands(Player player, Crate crate, Reward reward) {
        for (String command : reward.getCommands()) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), applyPlaceholders(command, player, crate, reward));
        }
    }

    private boolean hasSpaceForRewardsAfterKeys(Player player, Crate crate, int keyAmount, List<Reward> rewards) {
        ItemStack[] contents = Arrays.stream(player.getInventory().getStorageContents())
            .map(itemStack -> itemStack == null ? null : itemStack.clone())
            .toArray(ItemStack[]::new);

        consumeKeysInCopy(contents, crate, keyAmount);
        for (Reward reward : rewards) {
            ItemStack itemReward = reward.getItemReward();
            if (itemReward != null && !canFit(contents, itemReward.clone())) {
                return false;
            }
        }
        return true;
    }

    private void consumeKeysInCopy(ItemStack[] contents, Crate crate, int keyAmount) {
        int remaining = keyAmount;
        for (int index = 0; index < contents.length && remaining > 0; index++) {
            ItemStack itemStack = contents[index];
            if (!keyManager.isPhysicalKeyForCrate(itemStack, crate)) {
                continue;
            }

            int removed = Math.min(remaining, itemStack.getAmount());
            remaining -= removed;
            itemStack.setAmount(itemStack.getAmount() - removed);
            if (itemStack.getAmount() <= 0) {
                contents[index] = null;
            }
        }
    }

    private boolean canFit(ItemStack[] contents, ItemStack itemStack) {
        int remaining = itemStack.getAmount();
        for (int index = 0; index < contents.length; index++) {
            ItemStack content = contents[index];
            if (content == null || content.getType().isAir()) {
                int moved = Math.min(remaining, itemStack.getMaxStackSize());
                ItemStack copy = itemStack.clone();
                copy.setAmount(moved);
                contents[index] = copy;
                remaining -= moved;
            } else if (content.isSimilar(itemStack) && content.getAmount() < content.getMaxStackSize()) {
                int moved = Math.min(remaining, content.getMaxStackSize() - content.getAmount());
                content.setAmount(content.getAmount() + moved);
                remaining -= moved;
            }

            if (remaining <= 0) {
                return true;
            }
        }
        return false;
    }

    private void broadcastReward(Player player, Crate crate, Reward reward) {
        Bukkit.broadcast(ColorUtil.component(plugin.getConfig().getString("messages.reward-broadcast", "")
            .replace("%player%", player.getName())
            .replace("%crate%", crate.getId())
            .replace("%crate_displayname%", crate.getDisplayName())
            .replace("%reward%", rewardName(reward))));
    }

    private void spawnFirework(Player player) {
        Firework firework = player.getWorld().spawn(player.getLocation(), Firework.class);
        FireworkMeta meta = firework.getFireworkMeta();
        meta.addEffect(FireworkEffect.builder()
            .withColor(Color.PURPLE, Color.AQUA)
            .with(FireworkEffect.Type.BALL_LARGE)
            .trail(true)
            .build());
        meta.setPower(1);
        firework.setFireworkMeta(meta);
    }

    private String applyPlaceholders(String text, Player player, Crate crate, Reward reward) {
        String result = (text == null ? "" : text)
            .replace("%player%", player.getName())
            .replace("%crate%", crate.getId())
            .replace("%crate_displayname%", crate.getDisplayName());
        if (reward != null) {
            result = result.replace("%reward%", rewardName(reward));
        }
        return result;
    }

    private String rewardName(Reward reward) {
        ItemStack displayItem = reward.getDisplayItem();
        if (displayItem != null && displayItem.hasItemMeta()) {
            ItemMeta meta = displayItem.getItemMeta();
            if (meta != null && meta.hasDisplayName()) {
                return ColorUtil.legacy(meta.displayName());
            }
        }
        if (displayItem != null) {
            return displayItem.getType().name();
        }
        return reward.getId();
    }

    private ItemStack rewardDisplayItem(Reward reward) {
        ItemStack displayItem = reward.getDisplayItem();
        return displayItem == null ? new ItemStack(Material.PAPER) : displayItem.clone();
    }

    private ItemStack centerMarker() {
        ItemStack itemStack = new ItemStack(Material.LIME_BANNER);
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            meta.displayName(ColorUtil.component("&a&lWIN"));
            itemStack.setItemMeta(meta);
        }
        return itemStack;
    }
}
