package pl.admerpro.aBestCrates.service;

import java.util.Map;
import java.util.Random;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Sound;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.plugin.java.JavaPlugin;
import pl.admerpro.aBestCrates.manager.CrateManager;
import pl.admerpro.aBestCrates.manager.KeyManager;
import pl.admerpro.aBestCrates.model.Crate;
import pl.admerpro.aBestCrates.model.Reward;
import pl.admerpro.aBestCrates.util.ColorUtil;

public class OpeningService {
    private final JavaPlugin plugin;
    private final CrateManager crateManager;
    private final KeyManager keyManager;
    private final MessageService messageService;
    private final Random random = new Random();

    public OpeningService(JavaPlugin plugin, CrateManager crateManager, KeyManager keyManager, MessageService messageService) {
        this.plugin = plugin;
        this.crateManager = crateManager;
        this.keyManager = keyManager;
        this.messageService = messageService;
    }

    public void open(Player player, Crate crate) {
        open(player, crate, false);
    }

    public void forceOpen(Player player, Crate crate) {
        open(player, crate, true);
    }

    private void open(Player player, Crate crate, boolean forced) {
        if (!forced && !player.hasPermission("abestcrates.use")) {
            messageService.send(player, "no-permission");
            return;
        }

        if (!forced && !keyManager.consumeAnyKey(player, crate)) {
            player.sendMessage(ColorUtil.color(applyPlaceholders(crate.getNoKeyMessage(), player, crate, null)));
            return;
        }

        Reward reward = crate.rollReward(random).orElse(null);
        if (reward == null) {
            messageService.send(player, "crate-empty", Map.of("%crate%", crate.getId()));
            return;
        }

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
            Bukkit.broadcastMessage(ColorUtil.color(plugin.getConfig().getString("messages.reward-broadcast", "")
                .replace("%player%", player.getName())
                .replace("%crate%", crate.getId())
                .replace("%crate_displayname%", crate.getDisplayName())
                .replace("%reward%", rewardName(reward))));
        }
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
        String result = text
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
        if (displayItem != null && displayItem.hasItemMeta() && displayItem.getItemMeta().hasDisplayName()) {
            return displayItem.getItemMeta().getDisplayName();
        }
        if (displayItem != null) {
            return displayItem.getType().name();
        }
        return reward.getId();
    }
}
