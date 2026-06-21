package pl.admerpro.aBestCrates.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Random;
import java.util.LinkedHashMap;
import java.util.Map;
import org.bukkit.Material;

public class Crate {
    private static final java.util.regex.Pattern VALID_ID =
        java.util.regex.Pattern.compile("[A-Za-z0-9][A-Za-z0-9_-]{0,31}");
    private String id;
    private String displayName;
    private String color = "&5";
    private Material blockMaterial = Material.CHEST;
    private List<String> hologram = new ArrayList<>();
    private String noKeyMessage = "&cYou do not have a key for %crate_displayname%";
    private AnimationType animationType = AnimationType.INSTANT;
    private CrateType crateType = CrateType.GAMBLE;
    private ParticleEffectType particleEffect = ParticleEffectType.NONE;
    private boolean virtualKeyDisplay = true;
    private String permission = "";
    private long cooldownSeconds;
    private double openCost;
    private boolean pushback;
    private String previewTitle = "&5Preview: &f%crate_displayname%";
    private String openingTitle = "&5Opening: &f%crate_displayname%";
    private int rewardRolls = 1;
    private final List<KeyRequirement> keyRequirements = new ArrayList<>();
    private final Map<Integer, String> milestones = new LinkedHashMap<>();
    private KeyDefinition keyDefinition = new KeyDefinition();
    private final List<Reward> rewards = new ArrayList<>();

    public Crate(String id) {
        this.id = normalizeId(id);
        this.displayName = "&5" + id.toUpperCase(Locale.ROOT) + " CRATE";
        this.hologram.add("&5" + id.toUpperCase(Locale.ROOT));
        this.hologram.add("&7Click to open");
    }

    public static String normalizeId(String id) {
        return id == null ? "" : id.trim();
    }

    public static boolean isValidId(String id) {
        return VALID_ID.matcher(normalizeId(id)).matches();
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

    public CrateType getCrateType() {
        return crateType;
    }

    public void setCrateType(CrateType crateType) {
        this.crateType = crateType == null ? CrateType.GAMBLE : crateType;
    }

    public ParticleEffectType getParticleEffect() {
        return particleEffect;
    }

    public void setParticleEffect(ParticleEffectType particleEffect) {
        this.particleEffect = particleEffect == null ? ParticleEffectType.NONE : particleEffect;
    }

    public boolean isVirtualKeyDisplay() {
        return virtualKeyDisplay;
    }

    public void setVirtualKeyDisplay(boolean virtualKeyDisplay) {
        this.virtualKeyDisplay = virtualKeyDisplay;
    }

    public String getPermission() {
        return permission;
    }

    public void setPermission(String permission) {
        this.permission = permission == null ? "" : permission.trim();
    }

    public long getCooldownSeconds() {
        return cooldownSeconds;
    }

    public void setCooldownSeconds(long cooldownSeconds) {
        this.cooldownSeconds = Math.max(0L, cooldownSeconds);
    }

    public double getOpenCost() {
        return openCost;
    }

    public void setOpenCost(double openCost) {
        this.openCost = Math.max(0.0D, openCost);
    }

    public boolean isPushback() {
        return pushback;
    }

    public void setPushback(boolean pushback) {
        this.pushback = pushback;
    }

    public String getPreviewTitle() {
        return previewTitle;
    }

    public void setPreviewTitle(String previewTitle) {
        this.previewTitle = previewTitle == null || previewTitle.isBlank() ? "&5Preview: &f%crate_displayname%" : previewTitle;
    }

    public String getOpeningTitle() {
        return openingTitle;
    }

    public void setOpeningTitle(String openingTitle) {
        this.openingTitle = openingTitle == null || openingTitle.isBlank() ? "&5Opening: &f%crate_displayname%" : openingTitle;
    }

    public int getRewardRolls() {
        return rewardRolls;
    }

    public void setRewardRolls(int rewardRolls) {
        this.rewardRolls = Math.max(1, Math.min(9, rewardRolls));
    }

    public List<KeyRequirement> getKeyRequirements() {
        if (keyRequirements.isEmpty()) {
            return List.of(new KeyRequirement(id, 1));
        }
        return Collections.unmodifiableList(keyRequirements);
    }

    public void setKeyRequirements(List<KeyRequirement> requirements) {
        keyRequirements.clear();
        if (requirements != null) {
            requirements.stream().filter(requirement -> requirement != null && !requirement.crateId().isBlank()).forEach(keyRequirements::add);
        }
    }

    public Map<Integer, String> getMilestones() {
        return Collections.unmodifiableMap(milestones);
    }

    public void setMilestones(Map<Integer, String> configuredMilestones) {
        milestones.clear();
        if (configuredMilestones != null) {
            configuredMilestones.forEach((amount, rewardId) -> {
                if (amount != null && amount > 0 && rewardId != null && !rewardId.isBlank()) {
                    milestones.put(amount, rewardId);
                }
            });
        }
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
