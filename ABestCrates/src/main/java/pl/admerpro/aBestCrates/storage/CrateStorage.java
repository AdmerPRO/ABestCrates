package pl.admerpro.aBestCrates.storage;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import pl.admerpro.aBestCrates.model.AnimationType;
import pl.admerpro.aBestCrates.model.Crate;
import pl.admerpro.aBestCrates.model.KeyDefinition;
import pl.admerpro.aBestCrates.model.Reward;

public class CrateStorage {
    private static final Pattern ITEM_STACK_START_PATTERN = Pattern.compile("^(\\s*)==:\\s*org\\.bukkit\\.inventory\\.ItemStack\\s*$");
    private static final Pattern NAMESPACED_ITEM_ID_PATTERN = Pattern.compile("^(\\s*)id:\\s*minecraft:([a-z0-9_]+)\\s*$");
    private static final Pattern ITEM_COUNT_PATTERN = Pattern.compile("^(\\s*)count:\\s*(\\d+)\\s*$");
    private static final Pattern ITEM_VERSION_PATTERN = Pattern.compile("^\\s*(DataVersion|schema_version):\\s*.*$");

    private final JavaPlugin plugin;
    private final File file;

    public CrateStorage(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "crates.yml");
    }

    public List<Crate> load() {
        if (!file.exists()) {
            return new ArrayList<>();
        }

        YamlConfiguration configuration = loadConfiguration();
        ConfigurationSection cratesSection = configuration.getConfigurationSection("crates");
        if (cratesSection == null) {
            return new ArrayList<>();
        }

        List<Crate> crates = new ArrayList<>();
        for (String crateId : cratesSection.getKeys(false)) {
            ConfigurationSection section = cratesSection.getConfigurationSection(crateId);
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
            crate.setKeyDefinition(readKey(section.getConfigurationSection("key")));
            readRewards(section.getConfigurationSection("rewards"), crate);
            crates.add(crate);
        }
        return crates;
    }

    public void save(Collection<Crate> crates) {
        YamlConfiguration configuration = new YamlConfiguration();
        for (Crate crate : crates) {
            String path = "crates." + crate.getId() + ".";
            configuration.set(path + "display-name", crate.getDisplayName());
            configuration.set(path + "color", crate.getColor());
            configuration.set(path + "block", crate.getBlockMaterial().name());
            configuration.set(path + "hologram", crate.getHologram());
            configuration.set(path + "no-key-message", crate.getNoKeyMessage());
            configuration.set(path + "animation", crate.getAnimationType().name());

            KeyDefinition key = crate.getKeyDefinition();
            configuration.set(path + "key.material", key.getMaterial().name());
            configuration.set(path + "key.display-name", key.getDisplayName());
            configuration.set(path + "key.lore", key.getLore());
            configuration.set(path + "key.glow", key.isGlow());
            configuration.set(path + "key.custom-model-data", key.getCustomModelData());
            configuration.set(path + "key.template-item", key.getTemplateItem());

            for (Reward reward : crate.getRewards()) {
                String rewardPath = path + "rewards." + reward.getId() + ".";
                configuration.set(rewardPath + "display-item", reward.getDisplayItem());
                configuration.set(rewardPath + "item-reward", reward.getItemReward());
                configuration.set(rewardPath + "commands", reward.getCommands());
                configuration.set(rewardPath + "real-chance", reward.getRealChance());
                configuration.set(rewardPath + "display-chance", reward.getDisplayChance());
                configuration.set(rewardPath + "rarity", reward.getRarity());
                configuration.set(rewardPath + "broadcast", reward.isBroadcast());
                configuration.set(rewardPath + "fireworks", reward.isFireworks());
            }
        }

        try {
            configuration.save(file);
        } catch (IOException exception) {
            plugin.getLogger().log(Level.SEVERE, "Could not save crates.yml.", exception);
        }
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
            reward.setCommands(rewardSection.getStringList("commands"));
            reward.setRealChance(rewardSection.getDouble("real-chance", 0.0D));
            reward.setDisplayChance(rewardSection.getDouble("display-chance", reward.getRealChance()));
            reward.setRarity(rewardSection.getString("rarity", "Common"));
            reward.setBroadcast(rewardSection.getBoolean("broadcast", false));
            reward.setFireworks(rewardSection.getBoolean("fireworks", false));
            crate.addReward(reward);
        }
    }

    private Material readMaterial(String value, Material fallback) {
        Material material = value == null ? null : Material.matchMaterial(value, false);
        return material == null ? fallback : material;
    }

    private YamlConfiguration loadConfiguration() {
        YamlConfiguration configuration = new YamlConfiguration();
        try {
            String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
            configuration.loadFromString(normalizeSerializedItemStacks(content));
        } catch (IOException | InvalidConfigurationException exception) {
            plugin.getLogger().log(Level.SEVERE, "Could not load crates.yml.", exception);
        }
        return configuration;
    }

    private String normalizeSerializedItemStacks(String content) {
        StringBuilder normalized = new StringBuilder(content.length());
        String[] lines = content.split("\\R", -1);
        int itemStackIndent = -1;
        boolean appendedLine = false;
        for (int index = 0; index < lines.length; index++) {
            String line = lines[index];
            if (itemStackIndent >= 0 && !line.isBlank() && indentation(line) <= itemStackIndent) {
                itemStackIndent = -1;
            }

            Matcher itemStackMatcher = ITEM_STACK_START_PATTERN.matcher(line);
            if (itemStackMatcher.matches()) {
                itemStackIndent = itemStackMatcher.group(1).length();
            }

            if (itemStackIndent >= 0) {
                line = normalizeSerializedItemStackLine(line);
            }

            if (line == null) {
                continue;
            }
            if (appendedLine) {
                normalized.append(System.lineSeparator());
            }
            normalized.append(line);
            appendedLine = true;
        }
        return normalized.toString();
    }

    private String normalizeSerializedItemStackLine(String line) {
        if (ITEM_VERSION_PATTERN.matcher(line).matches()) {
            return null;
        }

        Matcher idMatcher = NAMESPACED_ITEM_ID_PATTERN.matcher(line);
        if (idMatcher.matches()) {
            return idMatcher.group(1) + "type: " + readSerializedMaterialName(idMatcher.group(2));
        }

        Matcher countMatcher = ITEM_COUNT_PATTERN.matcher(line);
        if (countMatcher.matches()) {
            return countMatcher.group(1) + "amount: " + countMatcher.group(2);
        }

        return line;
    }

    private String readSerializedMaterialName(String materialId) {
        String materialName = materialId.toUpperCase(Locale.ROOT);
        Material material = Material.matchMaterial(materialName, false);
        return material == null ? Material.STONE.name() : material.name();
    }

    private int indentation(String line) {
        int indentation = 0;
        while (indentation < line.length() && Character.isWhitespace(line.charAt(indentation))) {
            indentation++;
        }
        return indentation;
    }

    private <T extends Enum<T>> T readEnum(Class<T> enumClass, String value, T fallback) {
        if (value == null) {
            return fallback;
        }
        try {
            return Enum.valueOf(enumClass, value.toUpperCase());
        } catch (IllegalArgumentException exception) {
            return fallback;
        }
    }
}
