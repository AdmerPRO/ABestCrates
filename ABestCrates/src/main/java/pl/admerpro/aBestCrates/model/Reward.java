package pl.admerpro.aBestCrates.model;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.inventory.ItemStack;

public class Reward {
    private final String id;
    private ItemStack displayItem;
    private ItemStack itemReward;
    private List<String> commands = new ArrayList<>();
    private double realChance;
    private double displayChance;
    private String rarity = "Common";
    private boolean broadcast;
    private boolean fireworks;

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
        return itemReward == null ? null : itemReward.clone();
    }

    public void setItemReward(ItemStack itemReward) {
        this.itemReward = itemReward == null ? null : itemReward.clone();
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
}
