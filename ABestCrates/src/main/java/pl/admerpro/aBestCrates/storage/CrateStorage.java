package pl.admerpro.aBestCrates.storage;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import pl.admerpro.aBestCrates.model.AnimationType;
import pl.admerpro.aBestCrates.model.Crate;
import pl.admerpro.aBestCrates.model.CrateType;
import pl.admerpro.aBestCrates.model.KeyDefinition;
import pl.admerpro.aBestCrates.model.KeyRequirement;
import pl.admerpro.aBestCrates.model.ParticleEffectType;
import pl.admerpro.aBestCrates.model.Reward;

public class CrateStorage {
    public static final int CURRENT_CONFIG_VERSION = 2;

    private final JavaPlugin plugin;
    private final File file;
    private final File keysFile;

    public CrateStorage(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "crates.yml");
        this.keysFile = new File(plugin.getDataFolder(), "keys.yml");
    }

    public List<Crate> load() {
        if (!file.exists()) {
            return new ArrayList<>();
        }

        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(file);
        YamlConfiguration keysConfiguration = YamlConfiguration.loadConfiguration(keysFile);
        ConfigurationSection cratesSection = configuration.getConfigurationSection("crates");
        if (cratesSection == null) {
            return new ArrayList<>();
        }

        List<Crate> crates = new ArrayList<>();
        Set<String> loadedIds = new HashSet<>();
        for (String storedId : cratesSection.getKeys(false)) {
            String crateId = storedId;
            if (!Crate.isValidId(crateId) || loadedIds.contains(crateId.toLowerCase(java.util.Locale.ROOT))) {
                crateId = migratedId(storedId, loadedIds);
                plugin.getLogger().warning("Migrating invalid or duplicate crate id '" + storedId + "' to '" + crateId + "'.");
            }
            loadedIds.add(crateId.toLowerCase(java.util.Locale.ROOT));
            ConfigurationSection section = cratesSection.getConfigurationSection(storedId);
            if (section == null) {
                continue;
            }

            Crate crate = new Crate(crateId);
            crate.setDisplayName(section.getString("display-name", crate.getDisplayName()));
            crate.setColor(section.getString("color", crate.getColor()));
            crate.setBlockMaterial(readMaterial(section.getString("block"), Material.CHEST));
            crate.setHologram(section.getStringList("hologram"));
            crate.setNoKeyMessage(section.getString("no-key-message", crate.getNoKeyMessage()));
            crate.setAnimationType(readEnum(AnimationType.class, section.getString("animation"), AnimationType.INSTANT));
            crate.setCrateType(readEnum(CrateType.class, section.getString("type"), CrateType.GAMBLE));
            crate.setParticleEffect(readEnum(ParticleEffectType.class, section.getString("particle-effect"), ParticleEffectType.NONE));
            crate.setVirtualKeyDisplay(section.getBoolean("virtual-key-display", true));
            crate.setPermission(section.getString("permission", ""));
            crate.setCooldownSeconds(section.getLong("open-cooldown-seconds", 0L));
            crate.setOpenCost(section.getDouble("open-cost", 0.0D));
            crate.setPushback(section.getBoolean("pushback", false));
            crate.setPreviewTitle(section.getString("preview.title", crate.getPreviewTitle()));
            crate.setOpeningTitle(section.getString("opening.title", crate.getOpeningTitle()));
            crate.setRewardRolls(section.getInt("reward-rolls", 1));
            if (section.contains("key-requirements")) {
                crate.setKeyRequirements(readKeyRequirements(section.getStringList("key-requirements")));
            }
            crate.setMilestones(readMilestones(section.getConfigurationSection("milestones")));
            ConfigurationSection keySection = keysConfiguration.getConfigurationSection("keys." + storedId);
            crate.setKeyDefinition(readKey(keySection == null ? section.getConfigurationSection("key") : keySection));
            readRewards(section.getConfigurationSection("rewards"), crate);
            crates.add(crate);
        }
        return crates;
    }

    public void save(Collection<Crate> crates) {
        YamlConfiguration configuration = new YamlConfiguration();
        YamlConfiguration keysConfiguration = new YamlConfiguration();
        configuration.set("config-version", CURRENT_CONFIG_VERSION);
        keysConfiguration.set("config-version", CURRENT_CONFIG_VERSION);
        for (Crate crate : crates) {
            writeCrate(configuration, keysConfiguration, crate);
        }

        try {
            configuration.save(file);
            keysConfiguration.save(keysFile);
        } catch (IOException exception) {
            plugin.getLogger().log(Level.SEVERE, "Could not save crates.yml.", exception);
        }
    }

    public boolean exportTemplate(Crate crate, File templateFile) {
        YamlConfiguration configuration = new YamlConfiguration();
        configuration.set("config-version", CURRENT_CONFIG_VERSION);
        configuration.set("template-crate", crate.getId());
        writeCrate(configuration, configuration, crate);

        File parent = templateFile.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            plugin.getLogger().warning("Could not create template directory: " + parent);
            return false;
        }

        try {
            configuration.save(templateFile);
            return true;
        } catch (IOException exception) {
            plugin.getLogger().log(Level.SEVERE, "Could not export crate template.", exception);
            return false;
        }
    }

    public Optional<Crate> importTemplate(File templateFile, String newId) {
        if (!templateFile.exists()) {
            return Optional.empty();
        }

        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(templateFile);
        ConfigurationSection cratesSection = configuration.getConfigurationSection("crates");
        if (cratesSection == null || cratesSection.getKeys(false).isEmpty()) {
            return Optional.empty();
        }

        String storedId = configuration.getString("template-crate", "");
        if (storedId.isBlank() || cratesSection.getConfigurationSection(storedId) == null) {
            storedId = cratesSection.getKeys(false).iterator().next();
        }
        String crateId = newId == null || newId.isBlank() ? storedId : newId;
        if (!Crate.isValidId(crateId)) {
            return Optional.empty();
        }

        ConfigurationSection section = cratesSection.getConfigurationSection(storedId);
        if (section == null) {
            return Optional.empty();
        }
        Crate crate = readCrate(crateId, section,
            configuration.getConfigurationSection("keys." + storedId));
        if (!storedId.equalsIgnoreCase(crateId)) {
            String sourceId = storedId;
            crate.setKeyRequirements(crate.getKeyRequirements().stream()
                .map(requirement -> requirement.crateId().equalsIgnoreCase(sourceId)
                    ? new KeyRequirement(crateId, requirement.amount()) : requirement)
                .toList());
        }
        return Optional.of(crate);
    }

    private KeyDefinition readKey(ConfigurationSection section) {
        KeyDefinition key = new KeyDefinition();
        if (section == null) {
            return key;
        }

        key.setMaterial(readMaterial(section.getString("material"), Material.TRIPWIRE_HOOK));
        key.setDisplayName(section.getString("display-name", key.getDisplayName()));
        key.setLore(section.getStringList("lore"));
        key.setGlow(section.getBoolean("glow", true));
        if (section.isInt("custom-model-data")) {
            key.setCustomModelData(section.getInt("custom-model-data"));
        }
        key.setTemplateItem(section.getItemStack("template-item"));
        return key;
    }

    private Crate readCrate(String crateId, ConfigurationSection section, ConfigurationSection keySection) {
        Crate crate = new Crate(crateId);
        crate.setDisplayName(section.getString("display-name", crate.getDisplayName()));
        crate.setColor(section.getString("color", crate.getColor()));
        crate.setBlockMaterial(readMaterial(section.getString("block"), Material.CHEST));
        crate.setHologram(section.getStringList("hologram"));
        crate.setNoKeyMessage(section.getString("no-key-message", crate.getNoKeyMessage()));
        crate.setAnimationType(readEnum(AnimationType.class, section.getString("animation"), AnimationType.INSTANT));
        crate.setCrateType(readEnum(CrateType.class, section.getString("type"), CrateType.GAMBLE));
        crate.setParticleEffect(readEnum(ParticleEffectType.class, section.getString("particle-effect"), ParticleEffectType.NONE));
        crate.setVirtualKeyDisplay(section.getBoolean("virtual-key-display", true));
        crate.setPermission(section.getString("permission", ""));
        crate.setCooldownSeconds(section.getLong("open-cooldown-seconds", 0L));
        crate.setOpenCost(section.getDouble("open-cost", 0.0D));
        crate.setPushback(section.getBoolean("pushback", false));
        crate.setPreviewTitle(section.getString("preview.title", crate.getPreviewTitle()));
        crate.setOpeningTitle(section.getString("opening.title", crate.getOpeningTitle()));
        crate.setRewardRolls(section.getInt("reward-rolls", 1));
        if (section.contains("key-requirements")) {
            crate.setKeyRequirements(readKeyRequirements(section.getStringList("key-requirements")));
        }
        crate.setMilestones(readMilestones(section.getConfigurationSection("milestones")));
        crate.setKeyDefinition(readKey(keySection == null ? section.getConfigurationSection("key") : keySection));
        readRewards(section.getConfigurationSection("rewards"), crate);
        return crate;
    }

    private void writeCrate(YamlConfiguration configuration, YamlConfiguration keysConfiguration, Crate crate) {
        String path = "crates." + crate.getId() + ".";
        configuration.set(path + "display-name", crate.getDisplayName());
        configuration.set(path + "color", crate.getColor());
        configuration.set(path + "block", crate.getBlockMaterial().name());
        configuration.set(path + "hologram", crate.getHologram());
        configuration.set(path + "no-key-message", crate.getNoKeyMessage());
        configuration.set(path + "animation", crate.getAnimationType().name());
        configuration.set(path + "type", crate.getCrateType().name());
        configuration.set(path + "particle-effect", crate.getParticleEffect().name());
        configuration.set(path + "virtual-key-display", crate.isVirtualKeyDisplay());
        configuration.set(path + "permission", crate.getPermission());
        configuration.set(path + "open-cooldown-seconds", crate.getCooldownSeconds());
        configuration.set(path + "open-cost", crate.getOpenCost());
        configuration.set(path + "pushback", crate.isPushback());
        configuration.set(path + "preview.title", crate.getPreviewTitle());
        configuration.set(path + "opening.title", crate.getOpeningTitle());
        configuration.set(path + "reward-rolls", crate.getRewardRolls());
        configuration.set(path + "key-requirements", crate.getKeyRequirements().stream()
            .map(requirement -> requirement.crateId() + ":" + requirement.amount()).toList());
        crate.getMilestones().forEach((amount, rewardId) -> configuration.set(path + "milestones." + amount, rewardId));

        KeyDefinition key = crate.getKeyDefinition();
        String keyPath = "keys." + crate.getId() + ".";
        keysConfiguration.set(keyPath + "material", key.getMaterial().name());
        keysConfiguration.set(keyPath + "display-name", key.getDisplayName());
        keysConfiguration.set(keyPath + "lore", key.getLore());
        keysConfiguration.set(keyPath + "glow", key.isGlow());
        keysConfiguration.set(keyPath + "custom-model-data", key.getCustomModelData());
        keysConfiguration.set(keyPath + "template-item", key.getTemplateItem());

        for (Reward reward : crate.getRewards()) {
            String rewardPath = path + "rewards." + reward.getId() + ".";
            configuration.set(rewardPath + "display-item", reward.getDisplayItem());
            configuration.set(rewardPath + "item-reward", reward.getItemReward());
            configuration.set(rewardPath + "item-rewards", reward.getItemRewards());
            configuration.set(rewardPath + "commands", reward.getCommands());
            configuration.set(rewardPath + "real-chance", reward.getRealChance());
            configuration.set(rewardPath + "display-chance", reward.getDisplayChance());
            configuration.set(rewardPath + "rarity", reward.getRarity());
            configuration.set(rewardPath + "broadcast", reward.isBroadcast());
            configuration.set(rewardPath + "fireworks", reward.isFireworks());
            configuration.set(rewardPath + "weight", reward.getWeight());
            configuration.set(rewardPath + "required-permissions", reward.getRequiredPermissions());
            configuration.set(rewardPath + "blocked-permissions", reward.getBlockedPermissions());
            configuration.set(rewardPath + "global-limit", reward.getGlobalLimit());
            configuration.set(rewardPath + "player-limit", reward.getPlayerLimit());
        }
    }

    private void readRewards(ConfigurationSection section, Crate crate) {
        if (section == null) {
            return;
        }

        for (String rewardId : section.getKeys(false)) {
            ConfigurationSection rewardSection = section.getConfigurationSection(rewardId);
            if (rewardSection == null) {
                continue;
            }

            Reward reward = new Reward(rewardId);
            reward.setDisplayItem(rewardSection.getItemStack("display-item"));
            reward.setItemReward(rewardSection.getItemStack("item-reward"));
            List<?> serializedItems = rewardSection.getList("item-rewards", List.of());
            List<org.bukkit.inventory.ItemStack> itemRewards = serializedItems.stream()
                .filter(org.bukkit.inventory.ItemStack.class::isInstance)
                .map(org.bukkit.inventory.ItemStack.class::cast)
                .toList();
            if (!itemRewards.isEmpty()) {
                reward.setItemRewards(itemRewards);
            }
            reward.setCommands(rewardSection.getStringList("commands"));
            reward.setRealChance(rewardSection.getDouble("real-chance", 0.0D));
            reward.setDisplayChance(rewardSection.getDouble("display-chance", reward.getRealChance()));
            reward.setRarity(rewardSection.getString("rarity", "Common"));
            reward.setBroadcast(rewardSection.getBoolean("broadcast", false));
            reward.setFireworks(rewardSection.getBoolean("fireworks", false));
            reward.setWeight(rewardSection.getDouble("weight", reward.getRealChance()));
            reward.setRequiredPermissions(rewardSection.getStringList("required-permissions"));
            reward.setBlockedPermissions(rewardSection.getStringList("blocked-permissions"));
            reward.setGlobalLimit(rewardSection.getInt("global-limit", 0));
            reward.setPlayerLimit(rewardSection.getInt("player-limit", 0));
            crate.addReward(reward);
        }
    }

    private List<KeyRequirement> readKeyRequirements(List<String> values) {
        List<KeyRequirement> requirements = new ArrayList<>();
        for (String value : values) {
            String[] parts = value.split(":", 2);
            if (parts[0].isBlank()) {
                continue;
            }
            int amount = 1;
            if (parts.length == 2) {
                try {
                    amount = Math.max(1, Integer.parseInt(parts[1]));
                } catch (NumberFormatException ignored) {
                    // Keep the safe default amount.
                }
            }
            requirements.add(new KeyRequirement(parts[0], amount));
        }
        return requirements;
    }

    private Map<Integer, String> readMilestones(ConfigurationSection section) {
        Map<Integer, String> milestones = new LinkedHashMap<>();
        if (section == null) {
            return milestones;
        }
        for (String key : section.getKeys(false)) {
            try {
                int amount = Integer.parseInt(key);
                String rewardId = section.getString(key);
                if (amount > 0 && rewardId != null && !rewardId.isBlank()) {
                    milestones.put(amount, rewardId);
                }
            } catch (NumberFormatException ignored) {
                // Ignore invalid milestone keys instead of rejecting the crate.
            }
        }
        return milestones;
    }

    private Material readMaterial(String value, Material fallback) {
        Material material = value == null ? null : Material.matchMaterial(value);
        return material == null ? fallback : material;
    }

    private String migratedId(String source, Set<String> usedIds) {
        String candidate = source == null ? "crate" : source.trim().replaceAll("[^A-Za-z0-9_-]", "_");
        if (candidate.isBlank()) {
            candidate = "crate";
        }
        if (!Character.isLetterOrDigit(candidate.charAt(0))) {
            candidate = "crate_" + candidate;
        }
        candidate = candidate.substring(0, Math.min(32, candidate.length()));
        String base = candidate;
        int suffix = 2;
        while (usedIds.contains(candidate.toLowerCase(java.util.Locale.ROOT))) {
            String ending = "_" + suffix++;
            candidate = base.substring(0, Math.min(base.length(), 32 - ending.length())) + ending;
        }
        return candidate;
    }

    private <T extends Enum<T>> T readEnum(Class<T> enumClass, String value, T fallback) {
        if (value == null) {
            return fallback;
        }
        try {
            String normalized = value.equalsIgnoreCase("WHELL") ? "WHEEL" : value.toUpperCase();
            return Enum.valueOf(enumClass, normalized);
        } catch (IllegalArgumentException exception) {
            return fallback;
        }
    }
}
