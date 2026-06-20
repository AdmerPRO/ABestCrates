package pl.admerpro.aBestCrates.model;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.inventory.ItemStack;

public class Reward {
    private final String id;
    private ItemStack displayItem;
    private ItemStack itemReward;
    private List<ItemStack> itemRewards = new ArrayList<>();
    private List<String> commands = new ArrayList<>();
    private double realChance;
    private double displayChance;
    private String rarity = "Common";
    private boolean broadcast;
    private boolean fireworks;
    private double weight;
    private List<String> requiredPermissions = new ArrayList<>();
    private List<String> blockedPermissions = new ArrayList<>();
    private int globalLimit;
    private int playerLimit;

    public Reward(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public ItemStack getDisplayItem() {
        return displayItem == null ? null : displayItem.clone();
    }

    public void setDisplayItem(ItemStack displayItem) {
        this.displayItem = displayItem == null ? null : displayItem.clone();
    }

    public ItemStack getItemReward() {
        if (!itemRewards.isEmpty()) {
            return itemRewards.get(0).clone();
        }
        return itemReward == null ? null : itemReward.clone();
    }

    public void setItemReward(ItemStack itemReward) {
        this.itemReward = itemReward == null ? null : itemReward.clone();
        this.itemRewards.clear();
        if (itemReward != null && !itemReward.getType().isAir()) {
            this.itemRewards.add(itemReward.clone());
        }
    }

    public List<ItemStack> getItemRewards() {
        return itemRewards.stream().map(ItemStack::clone).toList();
    }

    public void setItemRewards(List<ItemStack> itemRewards) {
        this.itemRewards = itemRewards == null ? new ArrayList<>() : itemRewards.stream()
            .filter(item -> item != null && !item.getType().isAir())
            .limit(27)
            .map(ItemStack::clone)
            .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        this.itemReward = this.itemRewards.isEmpty() ? null : this.itemRewards.get(0).clone();
    }

    public List<String> getCommands() {
        return commands;
    }

    public void setCommands(List<String> commands) {
        this.commands = commands == null ? new ArrayList<>() : new ArrayList<>(commands);
    }

    public double getRealChance() {
        return realChance;
    }

    public void setRealChance(double realChance) {
        this.realChance = Math.max(0.0D, realChance);
    }

    public double getDisplayChance() {
        return displayChance;
    }

    public void setDisplayChance(double displayChance) {
        this.displayChance = Math.max(0.0D, displayChance);
    }

    public String getRarity() {
        return rarity;
    }

    public void setRarity(String rarity) {
        this.rarity = rarity == null || rarity.isBlank() ? "Common" : rarity;
    }

    public boolean isBroadcast() {
        return broadcast;
    }

    public void setBroadcast(boolean broadcast) {
        this.broadcast = broadcast;
    }

    public boolean isFireworks() {
        return fireworks;
    }

    public void setFireworks(boolean fireworks) {
        this.fireworks = fireworks;
    }

    public double getWeight() {
        return weight > 0.0D ? weight : realChance;
    }

    public void setWeight(double weight) {
        this.weight = Math.max(0.0D, weight);
    }

    public List<String> getRequiredPermissions() {
        return List.copyOf(requiredPermissions);
    }

    public void setRequiredPermissions(List<String> requiredPermissions) {
        this.requiredPermissions = sanitize(requiredPermissions);
    }

    public List<String> getBlockedPermissions() {
        return List.copyOf(blockedPermissions);
    }

    public void setBlockedPermissions(List<String> blockedPermissions) {
        this.blockedPermissions = sanitize(blockedPermissions);
    }

    public int getGlobalLimit() {
        return globalLimit;
    }

    public void setGlobalLimit(int globalLimit) {
        this.globalLimit = Math.max(0, globalLimit);
    }

    public int getPlayerLimit() {
        return playerLimit;
    }

    public void setPlayerLimit(int playerLimit) {
        this.playerLimit = Math.max(0, playerLimit);
    }

    private List<String> sanitize(List<String> values) {
        if (values == null) {
            return new ArrayList<>();
        }
        return values.stream().filter(value -> value != null && !value.isBlank()).map(String::trim)
            .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
    }
}
