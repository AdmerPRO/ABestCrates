package pl.admerpro.aBestCrates.manager;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import pl.admerpro.aBestCrates.model.Crate;

public class CrateLocationManager {
    private final JavaPlugin plugin;
    private final CrateManager crateManager;
    private final File file;
    private final Map<BlockKey, String> crateLocations = new HashMap<>();

    public CrateLocationManager(JavaPlugin plugin, CrateManager crateManager) {
        this.plugin = plugin;
        this.crateManager = crateManager;
        this.file = new File(plugin.getDataFolder(), "locations.yml");
    }

    public void load() {
        crateLocations.clear();
        if (!file.exists()) {
            return;
        }

        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = configuration.getConfigurationSection("locations");
        if (section == null) {
            return;
        }

        for (String key : section.getKeys(false)) {
            BlockKey blockKey = BlockKey.fromString(key);
            if (blockKey != null) {
                crateLocations.put(blockKey, section.getString(key));
            }
        }
    }

    public void save() {
        YamlConfiguration configuration = new YamlConfiguration();
        for (Map.Entry<BlockKey, String> entry : crateLocations.entrySet()) {
            configuration.set("locations." + entry.getKey().asString(), entry.getValue());
        }

        try {
            configuration.save(file);
        } catch (IOException exception) {
            plugin.getLogger().severe("Could not save locations.yml: " + exception.getMessage());
        }
    }

    public void placeCrate(Player player, Crate crate) {
        Block block = player.getLocation().getBlock();
        block.setType(crate.getBlockMaterial());
        crateLocations.put(BlockKey.fromLocation(block.getLocation()), crate.getId());
        save();
    }

    public Optional<Crate> getCrateAt(Block block) {
        if (block == null) {
            return Optional.empty();
        }
        String crateId = crateLocations.get(BlockKey.fromLocation(block.getLocation()));
        return crateId == null ? Optional.empty() : crateManager.getCrate(crateId);
    }

    public void removeCrateAt(Block block) {
        if (block == null) {
            return;
        }
        crateLocations.remove(BlockKey.fromLocation(block.getLocation()));
        save();
    }

    public void renameCrate(String oldId, String newId) {
        crateLocations.replaceAll((location, crateId) -> crateId.equalsIgnoreCase(oldId) ? newId : crateId);
        save();
    }

    private record BlockKey(String world, int x, int y, int z) {
        private static BlockKey fromLocation(Location location) {
            return new BlockKey(location.getWorld().getName(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
        }

        private static BlockKey fromString(String value) {
            String[] parts = value.split(",");
            if (parts.length != 4) {
                return null;
            }

            World world = Bukkit.getWorld(parts[0]);
            if (world == null) {
                return null;
            }

            try {
                return new BlockKey(world.getName(), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), Integer.parseInt(parts[3]));
            } catch (NumberFormatException exception) {
                return null;
            }
        }

        private String asString() {
            return world + "," + x + "," + y + "," + z;
        }
    }
}
