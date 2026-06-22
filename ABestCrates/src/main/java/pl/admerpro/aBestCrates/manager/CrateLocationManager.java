package pl.admerpro.aBestCrates.manager;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import pl.admerpro.aBestCrates.model.Crate;
import pl.admerpro.aBestCrates.util.ColorUtil;

public class CrateLocationManager {
    private final JavaPlugin plugin;
    private final CrateManager crateManager;
    private final File file;
    private final NamespacedKey hologramKey;
    private final Map<BlockKey, String> crateLocations = new HashMap<>();
    private Runnable visualRefresh = () -> { };

    public CrateLocationManager(JavaPlugin plugin, CrateManager crateManager) {
        this.plugin = plugin;
        this.crateManager = crateManager;
        this.file = new File(plugin.getDataFolder(), "locations.yml");
        this.hologramKey = new NamespacedKey(plugin, "crate_hologram");
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
            plugin.getLogger().log(Level.SEVERE, "Could not save locations.yml.", exception);
        }
    }

    public void placeCrate(Player player, Crate crate) {
        Block block = player.getLocation().getBlock();
        block.setType(crate.getBlockMaterial());
        linkCrateBlock(block, crate);
    }

    public void linkCrateBlock(Block block, Crate crate) {
        if (block == null || crate == null) {
            return;
        }
        crateLocations.put(BlockKey.fromLocation(block.getLocation()), crate.getId());
        save();
        refreshHolograms();
    }

    public Optional<Crate> getCrateAt(Block block) {
        if (block == null) {
            return Optional.empty();
        }
        String crateId = crateLocations.get(BlockKey.fromLocation(block.getLocation()));
        return crateId == null ? Optional.empty() : crateManager.getCrate(crateId);
    }

    public boolean isCrateBlock(Block block) {
        return block != null && crateLocations.containsKey(BlockKey.fromLocation(block.getLocation()));
    }

    public boolean isCrateHologram(Entity entity) {
        return entity instanceof ArmorStand armorStand
            && armorStand.getPersistentDataContainer().has(hologramKey, PersistentDataType.BYTE);
    }

    public void removeCrateAt(Block block) {
        if (block == null) {
            return;
        }
        crateLocations.remove(BlockKey.fromLocation(block.getLocation()));
        save();
        refreshHolograms();
    }

    public boolean removePlacedCrate(Block block) {
        if (!isCrateBlock(block)) {
            return false;
        }

        crateLocations.remove(BlockKey.fromLocation(block.getLocation()));
        block.setType(Material.AIR);
        save();
        refreshHolograms();
        return true;
    }

    public void removeCratePlacements(String crateId) {
        crateLocations.entrySet().removeIf(entry -> {
            if (!entry.getValue().equalsIgnoreCase(crateId)) {
                return false;
            }
            Location location = entry.getKey().toLocation();
            if (location != null) {
                location.getBlock().setType(Material.AIR);
            }
            return true;
        });
        save();
        refreshHolograms();
    }

    public void renameCrate(String oldId, String newId) {
        crateLocations.replaceAll((location, crateId) -> crateId.equalsIgnoreCase(oldId) ? newId : crateId);
        save();
        refreshHolograms();
    }

    public void updatePlacedBlocks(Crate crate) {
        for (Map.Entry<BlockKey, String> entry : crateLocations.entrySet()) {
            if (!entry.getValue().equalsIgnoreCase(crate.getId())) {
                continue;
            }
            Location location = entry.getKey().toLocation();
            if (location != null) {
                location.getBlock().setType(crate.getBlockMaterial());
            }
        }
        refreshHolograms();
    }

    public void refreshHolograms() {
        clearHolograms();
        for (Map.Entry<BlockKey, String> entry : crateLocations.entrySet()) {
            Location location = entry.getKey().toLocation();
            if (location == null) {
                continue;
            }
            crateManager.getCrate(entry.getValue()).ifPresent(crate -> spawnHologram(location, crate));
        }
        visualRefresh.run();
    }

    public List<PlacedCrate> getPlacedCrates() {
        List<PlacedCrate> placedCrates = new ArrayList<>();
        for (Map.Entry<BlockKey, String> entry : crateLocations.entrySet()) {
            Location location = entry.getKey().toLocation();
            Crate crate = crateManager.getCrate(entry.getValue()).orElse(null);
            if (location != null && crate != null) {
                placedCrates.add(new PlacedCrate(location, crate));
            }
        }
        return placedCrates;
    }

    public void setVisualRefresh(Runnable visualRefresh) {
        this.visualRefresh = visualRefresh == null ? () -> { } : visualRefresh;
    }

    public void clearHolograms() {
        for (World world : Bukkit.getWorlds()) {
            for (ArmorStand armorStand : world.getEntitiesByClass(ArmorStand.class)) {
                if (armorStand.getPersistentDataContainer().has(hologramKey, PersistentDataType.BYTE)) {
                    armorStand.remove();
                }
            }
        }
    }

    private void spawnHologram(Location blockLocation, Crate crate) {
        List<String> lines = hologramLines(crate);
        for (int index = 0; index < lines.size(); index++) {
            double yOffset = 1.35D + ((lines.size() - 1 - index) * 0.28D);
            Location hologramLocation = blockLocation.clone().add(0.5D, yOffset, 0.5D);
            ArmorStand armorStand = blockLocation.getWorld().spawn(hologramLocation, ArmorStand.class);
            armorStand.setInvisible(true);
            armorStand.setMarker(true);
            armorStand.setGravity(false);
            armorStand.setPersistent(false);
            armorStand.setCustomNameVisible(true);
            armorStand.customName(ColorUtil.component(lines.get(index)));
            armorStand.getPersistentDataContainer().set(hologramKey, PersistentDataType.BYTE, (byte) 1);
        }
    }

    private List<String> hologramLines(Crate crate) {
        List<String> lines = new ArrayList<>();
        lines.add(crate.getColor() + ColorUtil.removeColor(crate.getDisplayName()));
        String displayNameText = ColorUtil.removeColor(crate.getDisplayName());
        for (String line : crate.getHologram()) {
            if (line == null || line.isBlank()) {
                continue;
            }
            if (ColorUtil.removeColor(line).equalsIgnoreCase(displayNameText)) {
                continue;
            }
            lines.add(line);
        }
        return lines;
    }

    private record BlockKey(String world, int x, int y, int z) {
        private static BlockKey fromLocation(Location location) {
            return new BlockKey(location.getWorld().getName(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
        }

        private static BlockKey fromString(String value) {
            String[] parts = value.split(",");
            if (parts.length != 4 || parts[0].isBlank()) {
                return null;
            }

            try {
                return new BlockKey(parts[0], Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), Integer.parseInt(parts[3]));
            } catch (NumberFormatException exception) {
                return null;
            }
        }

        private String asString() {
            return world + "," + x + "," + y + "," + z;
        }

        private Location toLocation() {
            World bukkitWorld = Bukkit.getWorld(world);
            return bukkitWorld == null ? null : new Location(bukkitWorld, x, y, z);
        }
    }

    public record PlacedCrate(Location location, Crate crate) {
        public PlacedCrate {
            location = location.clone();
        }
    }
}
