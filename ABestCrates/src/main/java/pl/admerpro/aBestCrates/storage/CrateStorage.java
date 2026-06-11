package pl.admerpro.aBestCrates.storage;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import pl.admerpro.aBestCrates.model.AnimationType;
import pl.admerpro.aBestCrates.model.Crate;
import pl.admerpro.aBestCrates.model.KeyDefinition;
import pl.admerpro.aBestCrates.model.Reward;

public class CrateStorage {
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

        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(file);
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
        Material material = value == null ? null : Material.matchMaterial(value);
        return material == null ? fallback : material;
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
