package pl.admerpro.aBestCrates.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Random;
import org.bukkit.Material;

public class Crate {
    private String id;
    private String displayName;
    private String color = "&5";
    private Material blockMaterial = Material.CHEST;
    private List<String> hologram = new ArrayList<>();
    private String noKeyMessage = "&cYou do not have a key for %crate_displayname%";
    private AnimationType animationType = AnimationType.INSTANT;
    private KeyDefinition keyDefinition = new KeyDefinition();
    private final List<Reward> rewards = new ArrayList<>();

    public Crate(String id) {
        this.id = normalizeId(id);
        this.displayName = "&5" + id.toUpperCase(Locale.ROOT) + " CRATE";
        this.hologram.add("&5" + id.toUpperCase(Locale.ROOT));
        this.hologram.add("&7Click to open");
    }

    public static String normalizeId(String id) {
        return id == null ? "" : id.trim().replace(" ", "_");
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = normalizeId(id);
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName == null || displayName.isBlank() ? id : displayName;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color == null || color.isBlank() ? "&5" : color;
    }

    public Material getBlockMaterial() {
        return blockMaterial;
    }

    public void setBlockMaterial(Material blockMaterial) {
        this.blockMaterial = blockMaterial != null && blockMaterial.isBlock() ? blockMaterial : Material.CHEST;
    }

    public List<String> getHologram() {
        return hologram;
    }

    public void setHologram(List<String> hologram) {
        this.hologram = hologram == null ? new ArrayList<>() : new ArrayList<>(hologram);
    }

    public String getNoKeyMessage() {
        return noKeyMessage;
    }

    public void setNoKeyMessage(String noKeyMessage) {
        this.noKeyMessage = noKeyMessage == null || noKeyMessage.isBlank() ? "&cYou do not have a key for %crate_displayname%" : noKeyMessage;
    }

    public AnimationType getAnimationType() {
        return animationType;
    }

    public void setAnimationType(AnimationType animationType) {
        this.animationType = animationType == null ? AnimationType.INSTANT : animationType;
    }

    public KeyDefinition getKeyDefinition() {
        return keyDefinition;
    }

    public void setKeyDefinition(KeyDefinition keyDefinition) {
        this.keyDefinition = keyDefinition == null ? new KeyDefinition() : keyDefinition;
    }

    public List<Reward> getRewards() {
        return Collections.unmodifiableList(rewards);
    }

    public void addReward(Reward reward) {
        if (reward != null) {
            rewards.add(reward);
        }
    }

    public boolean removeReward(String rewardId) {
        return rewards.removeIf(reward -> reward.getId().equalsIgnoreCase(rewardId));
    }

    public Optional<Reward> getReward(String rewardId) {
        return rewards.stream()
            .filter(reward -> reward.getId().equalsIgnoreCase(rewardId))
            .findFirst();
    }

    public Optional<Reward> rollReward(Random random) {
        double totalChance = rewards.stream().mapToDouble(Reward::getRealChance).sum();
        if (totalChance <= 0.0D) {
            return Optional.empty();
        }

        double roll = random.nextDouble() * totalChance;
        double cursor = 0.0D;
        for (Reward reward : rewards) {
            cursor += reward.getRealChance();
            if (roll <= cursor) {
                return Optional.of(reward);
            }
        }

        return rewards.isEmpty() ? Optional.empty() : Optional.of(rewards.get(rewards.size() - 1));
    }
}
