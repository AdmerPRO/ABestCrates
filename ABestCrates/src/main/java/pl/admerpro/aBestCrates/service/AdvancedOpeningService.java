package pl.admerpro.aBestCrates.service;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;
import pl.admerpro.aBestCrates.gui.MenuHolder;
import pl.admerpro.aBestCrates.gui.MenuType;
import pl.admerpro.aBestCrates.manager.KeyManager;
import pl.admerpro.aBestCrates.model.AnimationType;
import pl.admerpro.aBestCrates.model.Crate;
import pl.admerpro.aBestCrates.model.CrateType;
import pl.admerpro.aBestCrates.model.Reward;
import pl.admerpro.aBestCrates.util.ColorUtil;

public class AdvancedOpeningService extends OpeningService implements Listener {
    private final JavaPlugin plugin;
    private final KeyManager keyManager;
    private final MessageService messageService;
    private final PlayerDataService playerData;
    private final EconomyService economyService;
    private final PlaceholderService placeholderService;
    private final NamespacedKey choiceRewardKey;
    private final Random random = new Random();
    private final Map<UUID, PendingChoice> pendingChoices = new HashMap<>();

    public AdvancedOpeningService(JavaPlugin plugin, KeyManager keyManager, MessageService messageService,
                                  PlayerDataService playerData, EconomyService economyService,
                                  PlaceholderService placeholderService) {
        super(plugin, keyManager, messageService);
        this.plugin = plugin;
        this.keyManager = keyManager;
        this.messageService = messageService;
        this.playerData = playerData;
        this.economyService = economyService;
        this.placeholderService = placeholderService;
        this.choiceRewardKey = new NamespacedKey(plugin, "choice_reward");
    }

    @Override
    public void open(Player player, Crate crate) {
        open(player, crate, null, false);
    }

    public void open(Player player, Crate crate, Location source) {
        open(player, crate, source, false);
    }

    @Override
    public void forceOpen(Player player, Crate crate) {
        open(player, crate, null, true);
    }

    @Override
    public void openAllKeys(Player player, Crate crate) {
        if (!canUse(player, crate, null)) {
            return;
        }
        if (crate.getCrateType() == CrateType.CHOOSE) {
            messageService.send(player, "mass-open-choose");
            return;
        }
        int maximum = Math.min(keyManager.countOpenable(player, crate),
            Math.max(1, plugin.getConfig().getInt("mass-open.max-at-once", 1000)));
        if (maximum <= 0) {
            sendNoKey(player, crate);
            return;
        }
        if (!canPay(player, crate)) {
            return;
        }

        int opened = 0;
        for (int index = 0; index < maximum; index++) {
            List<Reward> rewards = rollRewards(player, crate);
            if (rewards.isEmpty() || !keyManager.hasRequiredKeys(player, crate)) {
                break;
            }
            if (crate.getOpenCost() > 0.0D && (!economyService.has(player, crate.getOpenCost())
                || !economyService.withdraw(player, crate.getOpenCost()))) {
                break;
            }
            if (!keyManager.consumeRequiredKeys(player, crate)) {
                break;
            }
            completeOpen(player, crate, rewards);
            opened++;
        }
        if (opened <= 0) {
            messageService.send(player, "crate-empty", Map.of("%crate%", crate.getId()));
            return;
        }
        playerData.startCooldown(player.getUniqueId(), crate);
        messageService.send(player, "bulk-opened", Map.of(
            "%amount%", String.valueOf(opened), "%crate%", crate.getId(),
            "%crate_displayname%", crate.getDisplayName()));
    }

    @EventHandler
    public void onChoiceClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)
            || !(event.getInventory().getHolder() instanceof MenuHolder holder)
            || holder.getType() != MenuType.CHOOSE_OPEN) {
            return;
        }
        event.setCancelled(true);
        ItemStack clicked = event.getCurrentItem();
        PendingChoice pending = pendingChoices.get(player.getUniqueId());
        if (clicked == null || !clicked.hasItemMeta() || pending == null) {
            return;
        }
        String rewardId = clicked.getItemMeta().getPersistentDataContainer()
            .get(choiceRewardKey, PersistentDataType.STRING);
        Reward reward = rewardId == null ? null : pending.rewards.stream()
            .filter(candidate -> candidate.getId().equalsIgnoreCase(rewardId)).findFirst().orElse(null);
        if (reward == null || pending.selected.contains(reward) || !isEligible(player, pending.crate, reward)) {
            return;
        }
        pending.selected.add(reward);
        event.getInventory().setItem(event.getRawSlot(), filler());
        playConfiguredSound(player, "sounds.opening.cycle", Sound.UI_BUTTON_CLICK);
        if (pending.selected.size() >= pending.target) {
            pendingChoices.remove(player.getUniqueId());
            player.closeInventory();
            completeOpen(player, pending.crate, pending.selected);
        }
    }

    @EventHandler
    public void onChoiceClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)
            || !(event.getInventory().getHolder() instanceof MenuHolder holder)
            || holder.getType() != MenuType.CHOOSE_OPEN
            || !pendingChoices.containsKey(player.getUniqueId())) {
            return;
        }
        Bukkit.getScheduler().runTask(plugin, () -> {
            PendingChoice pending = pendingChoices.get(player.getUniqueId());
            if (pending != null && player.isOnline()) {
                openChoiceMenu(player, pending);
            }
        });
    }

    private void open(Player player, Crate crate, Location source, boolean forced) {
        if (!forced && !canUse(player, crate, source)) {
            return;
        }
        List<Reward> eligible = eligibleRewards(player, crate);
        if (eligible.isEmpty()) {
            messageService.send(player, "crate-empty", Map.of("%crate%", crate.getId()));
            return;
        }
        if (!forced) {
            if (!keyManager.hasRequiredKeys(player, crate)) {
                deny(player, crate, source);
                sendNoKey(player, crate);
                return;
            }
            if (!canPay(player, crate)) {
                deny(player, crate, source);
                return;
            }
            if (crate.getOpenCost() > 0.0D && !economyService.withdraw(player, crate.getOpenCost())) {
                messageService.send(player, "open-cost-failed");
                return;
            }
            if (!keyManager.consumeRequiredKeys(player, crate)) {
                sendNoKey(player, crate);
                return;
            }
            playerData.startCooldown(player.getUniqueId(), crate);
        }
        if (crate.getCrateType() == CrateType.CHOOSE) {
            PendingChoice pending = new PendingChoice(crate, eligible,
                Math.min(crate.getRewardRolls(), eligible.size()));
            pendingChoices.put(player.getUniqueId(), pending);
            openChoiceMenu(player, pending);
            return;
        }
        List<Reward> rewards = rollDistinct(eligible, crate.getRewardRolls());
        if (!forced && crate.getAnimationType() != AnimationType.INSTANT) {
            playAnimation(player, crate, rewards, eligible);
        } else {
            completeOpen(player, crate, rewards);
        }
    }

    private boolean canUse(Player player, Crate crate, Location source) {
        if (!player.hasPermission("abestcrates.use")) {
            messageService.send(player, "no-permission");
            deny(player, crate, source);
            return false;
        }
        if (!crate.getPermission().isBlank() && !player.hasPermission(crate.getPermission())) {
            messageService.send(player, "crate-permission", Map.of("%permission%", crate.getPermission()));
            deny(player, crate, source);
            return false;
        }
        long cooldown = playerData.remainingCooldown(player.getUniqueId(), crate);
        if (cooldown > 0L) {
            messageService.send(player, "open-cooldown", Map.of("%seconds%", String.valueOf(cooldown)));
            deny(player, crate, source);
            return false;
        }
        return true;
    }

    private boolean canPay(Player player, Crate crate) {
        if (crate.getOpenCost() <= 0.0D) {
            return true;
        }
        if (!economyService.isAvailable()) {
            messageService.send(player, "economy-unavailable");
            return false;
        }
        if (!economyService.has(player, crate.getOpenCost())) {
            messageService.send(player, "not-enough-money",
                Map.of("%cost%", String.format(java.util.Locale.US, "%.2f", crate.getOpenCost())));
            return false;
        }
        return true;
    }

    private List<Reward> eligibleRewards(Player player, Crate crate) {
        return crate.getRewards().stream().filter(reward -> isEligible(player, crate, reward)).toList();
    }

    private boolean isEligible(Player player, Crate crate, Reward reward) {
        return reward.getRequiredPermissions().stream().allMatch(player::hasPermission)
            && reward.getBlockedPermissions().stream().noneMatch(player::hasPermission)
            && playerData.canReceive(player, crate, reward)
            && weightedChance(reward) > 0.0D;
    }

    private Reward roll(List<Reward> rewards) {
        double total = rewards.stream().mapToDouble(this::weightedChance).sum();
        if (total <= 0.0D) {
            return null;
        }
        double value = random.nextDouble() * total;
        double cursor = 0.0D;
        for (Reward reward : rewards) {
            cursor += weightedChance(reward);
            if (value <= cursor) {
                return reward;
            }
        }
        return rewards.get(rewards.size() - 1);
    }

    private List<Reward> rollRewards(Player player, Crate crate) {
        return rollDistinct(eligibleRewards(player, crate), crate.getRewardRolls());
    }

    private List<Reward> rollDistinct(List<Reward> rewards, int requested) {
        List<Reward> pool = new ArrayList<>(rewards);
        List<Reward> result = new ArrayList<>();
        while (!pool.isEmpty() && result.size() < Math.max(1, requested)) {
            Reward reward = roll(pool);
            if (reward == null) {
                break;
            }
            result.add(reward);
            pool.remove(reward);
        }
        return result;
    }

    private double weightedChance(Reward reward) {
        double rarityMultiplier = plugin.getConfig().getDouble(
            "rarities." + reward.getRarity().toLowerCase() + ".multiplier", 1.0D);
        return Math.max(0.0D, reward.getWeight() * Math.max(0.0D, rarityMultiplier));
    }

    private void completeOpen(Player player, Crate crate, List<Reward> rewards) {
        if (rewards == null || rewards.isEmpty()) {
            messageService.send(player, "crate-empty", Map.of("%crate%", crate.getId()));
            return;
        }
        for (Reward reward : rewards) {
            giveReward(player, crate, reward, rewards.size() == 1);
        }
        if (rewards.size() > 1) {
            messageService.send(player, "rewards-received", Map.of(
                "%amount%", String.valueOf(rewards.size()), "%crate%", crate.getId(),
                "%crate_displayname%", crate.getDisplayName()));
        }
        playConfiguredSound(player, "sounds.opening.finish", Sound.ENTITY_PLAYER_LEVELUP);
        int streak = playerData.recordOpen(player.getUniqueId(), crate);
        String milestoneRewardId = crate.getMilestones().get(streak);
        if (milestoneRewardId != null) {
            crate.getReward(milestoneRewardId).filter(candidate -> isEligible(player, crate, candidate)).ifPresent(milestone -> {
                giveReward(player, crate, milestone, false);
                messageService.send(player, "milestone-received",
                    Map.of("%amount%", String.valueOf(streak), "%reward%", rewardName(milestone)));
            });
        }
    }

    private void giveReward(Player player, Crate crate, Reward reward, boolean announce) {
        for (ItemStack item : reward.getItemRewards()) {
            player.getInventory().addItem(item).values()
                .forEach(leftover -> player.getWorld().dropItemNaturally(player.getLocation(), leftover));
        }
        for (String command : reward.getCommands()) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), applyPlaceholders(command, player, crate, reward));
        }
        playerData.recordReward(player, crate, reward);
        if (reward.isFireworks()) {
            spawnFirework(player);
        }
        if (announce) {
            messageService.send(player, "reward-received", Map.of(
                "%crate%", crate.getId(), "%crate_displayname%", crate.getDisplayName(), "%reward%", rewardName(reward)));
        }
        if (reward.isBroadcast()) {
            String message = messageService.get("reward-broadcast")
                .replace("%player%", player.getName()).replace("%crate%", crate.getId())
                .replace("%crate_displayname%", crate.getDisplayName()).replace("%reward%", rewardName(reward));
            Bukkit.broadcast(ColorUtil.component(placeholderService.apply(player, message)));
        }
    }

    private void openChoiceMenu(Player player, PendingChoice pending) {
        Crate crate = pending.crate;
        List<Reward> rewards = pending.rewards;
        int size = Math.max(9, Math.min(54, ((rewards.size() - 1) / 9 + 1) * 9));
        MenuHolder holder = new MenuHolder(MenuType.CHOOSE_OPEN, crate.getId());
        Inventory inventory = Bukkit.createInventory(holder, size,
            ColorUtil.component(applyPlaceholders(crate.getOpeningTitle(), player, crate, null)));
        holder.setInventory(inventory);
        fillInventory(inventory, filler());
        for (int index = 0; index < Math.min(size, rewards.size()); index++) {
            Reward reward = rewards.get(index);
            if (pending.selected.contains(reward)) {
                inventory.setItem(index, filler());
                continue;
            }
            ItemStack item = rewardDisplayItem(reward);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.getPersistentDataContainer().set(choiceRewardKey, PersistentDataType.STRING, reward.getId());
                item.setItemMeta(meta);
            }
            inventory.setItem(index, item);
        }
        player.openInventory(inventory);
    }

    private void playAnimation(Player player, Crate crate, List<Reward> rewards, List<Reward> eligible) {
        MenuHolder holder = new MenuHolder(MenuType.OPENING, crate.getId());
        Inventory inventory = Bukkit.createInventory(holder, 27,
            ColorUtil.component(applyPlaceholders(crate.getOpeningTitle(), player, crate,
                rewards.isEmpty() ? null : rewards.get(0))));
        holder.setInventory(inventory);
        fillInventory(inventory, filler());
        player.openInventory(inventory);
        List<ItemStack> displayItems = eligible.stream().map(this::rewardDisplayItem).toList();
        AnimationProfile profile = profile(crate.getAnimationType());
        runAnimationStep(player, crate, inventory, rewards, displayItems, profile, 0);
    }

    private void runAnimationStep(Player player, Crate crate, Inventory inventory, List<Reward> rewards,
                                  List<ItemStack> displayItems, AnimationProfile profile, int step) {
        if (!player.isOnline()) {
            return;
        }
        if (step >= profile.steps) {
            fillInventory(inventory, filler());
            int[] resultSlots = centeredSlots(rewards.size());
            for (int index = 0; index < rewards.size(); index++) {
                inventory.setItem(resultSlots[index], rewardDisplayItem(rewards.get(index)));
            }
            inventory.setItem(4, centerMarker());
            inventory.setItem(22, centerMarker());
            completeOpen(player, crate, rewards);
            return;
        }
        for (int slot : profile.slots) {
            inventory.setItem(slot, displayItems.get(random.nextInt(displayItems.size())).clone());
        }
        String soundPath = step > profile.steps * 0.7D ? "sounds.opening.slow" : "sounds.opening.cycle";
        playConfiguredSound(player, soundPath, step > profile.steps * 0.7D
            ? Sound.BLOCK_NOTE_BLOCK_PLING : Sound.UI_BUTTON_CLICK);
        long delay = profile.baseDelay + Math.round(profile.slowdown * Math.pow(step / (double) profile.steps, 2));
        Bukkit.getScheduler().runTaskLater(plugin,
            () -> runAnimationStep(player, crate, inventory, rewards, displayItems, profile, step + 1),
            Math.max(1L, delay));
    }

    private AnimationProfile profile(AnimationType type) {
        return switch (type) {
            case FAST -> new AnimationProfile(new int[]{9,10,11,12,13,14,15,16,17}, 10, 1, 1);
            case CLASSIC -> new AnimationProfile(new int[]{9,10,11,12,13,14,15,16,17}, 16, 2, 2);
            case CASINO -> new AnimationProfile(new int[]{3,4,5,12,13,14,21,22,23}, 22, 1, 8);
            case COSMIC -> new AnimationProfile(new int[]{1,2,3,5,6,7,9,17,18,26}, 24, 1, 9);
            case ROULETTE -> new AnimationProfile(new int[]{0,1,2,3,4,5,6,7,8,17,26,25,24,23,22,21,20,19,18,9}, 26, 1, 10);
            case WHEEL -> new AnimationProfile(new int[]{2,4,6,10,16,20,22,24}, 24, 1, 10);
            case WONDER -> new AnimationProfile(new int[]{0,2,4,6,8,10,12,14,16,18,20,22,24,26}, 20, 1, 8);
            case ROLL, CSGO -> new AnimationProfile(new int[]{9,10,11,12,13,14,15,16,17}, 28, 1, 12);
            case INSTANT -> new AnimationProfile(new int[]{13}, 0, 1, 0);
        };
    }

    private int[] centeredSlots(int amount) {
        int count = Math.max(1, Math.min(9, amount));
        int start = 9 + (9 - count) / 2;
        int[] slots = new int[count];
        for (int index = 0; index < count; index++) {
            slots[index] = start + index;
        }
        return slots;
    }

    private void fillInventory(Inventory inventory, ItemStack item) {
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            inventory.setItem(slot, item.clone());
        }
    }

    private ItemStack filler() {
        ItemStack item = new ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(ColorUtil.component(" "));
            item.setItemMeta(meta);
        }
        return item;
    }

    private void playConfiguredSound(Player player, String path, Sound fallback) {
        if (!plugin.getConfig().getBoolean(path + ".enabled", true)) {
            return;
        }
        String configured = plugin.getConfig().getString(path + ".name", "");
        String soundKey = configured.contains(":")
            ? configured.toLowerCase(Locale.ROOT)
            : "minecraft:" + configured.toLowerCase(Locale.ROOT).replace('_', '.');
        NamespacedKey namespacedKey = NamespacedKey.fromString(soundKey);
        Sound registered = namespacedKey == null ? null : Registry.SOUNDS.get(namespacedKey);
        Sound sound = registered == null ? fallback : registered;
        player.playSound(player.getLocation(), sound,
            (float) plugin.getConfig().getDouble(path + ".volume", 1.0D),
            (float) plugin.getConfig().getDouble(path + ".pitch", 1.0D));
    }

    private void deny(Player player, Crate crate, Location source) {
        if (!crate.isPushback() || source == null || source.getWorld() != player.getWorld()) {
            return;
        }
        Vector direction = player.getLocation().toVector()
            .subtract(source.clone().add(0.5D, 0.5D, 0.5D).toVector());
        if (direction.lengthSquared() < 0.01D) {
            direction = player.getLocation().getDirection().multiply(-1.0D);
        }
        player.setVelocity(direction.normalize().multiply(1.15D).setY(0.35D));
    }

    private void sendNoKey(Player player, Crate crate) {
        player.sendMessage(ColorUtil.component(applyPlaceholders(crate.getNoKeyMessage(), player, crate, null)));
    }

    private String applyPlaceholders(String text, Player player, Crate crate, Reward reward) {
        String result = (text == null ? "" : text).replace("%player%", player.getName())
            .replace("%crate%", crate.getId()).replace("%crate_displayname%", crate.getDisplayName());
        if (reward != null) {
            result = result.replace("%reward%", rewardName(reward));
        }
        return placeholderService.apply(player, result);
    }

    private String rewardName(Reward reward) {
        ItemStack display = reward.getDisplayItem();
        if (display != null && display.hasItemMeta() && display.getItemMeta().hasDisplayName()) {
            return ColorUtil.legacy(display.getItemMeta().displayName());
        }
        return display == null ? reward.getId() : display.getType().name();
    }

    private ItemStack rewardDisplayItem(Reward reward) {
        ItemStack display = reward.getDisplayItem();
        return display == null || display.getType().isAir() ? new ItemStack(Material.PAPER) : display.clone();
    }

    private ItemStack centerMarker() {
        ItemStack item = new ItemStack(Material.LIME_BANNER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(ColorUtil.component("&aWinner"));
            item.setItemMeta(meta);
        }
        return item;
    }

    private void spawnFirework(Player player) {
        Firework firework = player.getWorld().spawn(player.getLocation(), Firework.class);
        FireworkMeta meta = firework.getFireworkMeta();
        meta.addEffect(FireworkEffect.builder().withColor(Color.PURPLE, Color.AQUA)
            .with(FireworkEffect.Type.BALL_LARGE).trail(true).build());
        meta.setPower(1);
        firework.setFireworkMeta(meta);
    }

    private static final class PendingChoice {
        private final Crate crate;
        private final List<Reward> rewards;
        private final List<Reward> selected = new ArrayList<>();
        private final int target;

        private PendingChoice(Crate crate, List<Reward> rewards, int target) {
            this.crate = crate;
            this.rewards = List.copyOf(rewards);
            this.target = Math.max(1, target);
        }
    }

    private record AnimationProfile(int[] slots, int steps, long baseDelay, long slowdown) {
    }
}
