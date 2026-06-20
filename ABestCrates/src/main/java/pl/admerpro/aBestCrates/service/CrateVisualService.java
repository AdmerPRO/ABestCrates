package pl.admerpro.aBestCrates.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import pl.admerpro.aBestCrates.manager.CrateLocationManager;
import pl.admerpro.aBestCrates.manager.KeyManager;
import pl.admerpro.aBestCrates.model.Crate;
import pl.admerpro.aBestCrates.model.ParticleEffectType;
import pl.admerpro.aBestCrates.util.ColorUtil;

public class CrateVisualService implements Listener {
    private final JavaPlugin plugin;
    private final CrateLocationManager locationManager;
    private final KeyManager keyManager;
    private final NamespacedKey virtualDisplayKey;
    private final Map<UUID, List<ArmorStand>> virtualDisplays = new HashMap<>();
    private BukkitTask particleTask;
    private double animation;

    public CrateVisualService(JavaPlugin plugin, CrateLocationManager locationManager, KeyManager keyManager) {
        this.plugin = plugin;
        this.locationManager = locationManager;
        this.keyManager = keyManager;
        this.virtualDisplayKey = new NamespacedKey(plugin, "virtual_key_display");
    }

    public void start() {
        clearVirtualDisplays();
        refreshVirtualDisplays();
        long interval = Math.max(1L, plugin.getConfig().getLong("particles.tick-interval", 2L));
        particleTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tickParticles, interval, interval);
    }

    public void stop() {
        if (particleTask != null) {
            particleTask.cancel();
            particleTask = null;
        }
        clearVirtualDisplays();
    }

    public void reload() {
        stop();
        start();
    }

    public void refreshVirtualDisplays() {
        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(plugin, this::refreshVirtualDisplays);
            return;
        }
        clearVirtualDisplays();
        if (!plugin.getConfig().getBoolean("virtual-key-display.enabled", true)) {
            return;
        }
        for (Player owner : Bukkit.getOnlinePlayers()) {
            spawnVirtualDisplays(owner);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Bukkit.getScheduler().runTask(plugin, this::refreshVirtualDisplays);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        removeDisplays(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        Bukkit.getScheduler().runTask(plugin, this::refreshVirtualDisplays);
    }

    @EventHandler(ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        if (event.getTo().getWorld() != event.getFrom().getWorld()) {
            Bukkit.getScheduler().runTask(plugin, this::refreshVirtualDisplays);
        }
    }

    private void spawnVirtualDisplays(Player owner) {
        List<ArmorStand> displays = new ArrayList<>();
        for (CrateLocationManager.PlacedCrate placedCrate : locationManager.getPlacedCrates()) {
            Crate crate = placedCrate.crate();
            if (!crate.isVirtualKeyDisplay() || placedCrate.location().getWorld() != owner.getWorld()) {
                continue;
            }
            int keys = keyManager.getVirtualKeys(owner.getUniqueId(), crate.getId());
            String format = plugin.getConfig().getString("virtual-key-display.line", "&eVirtual Keys: &f%keys%");
            String line = format.replace("%keys%", String.valueOf(keys))
                .replace("%crate%", crate.getId())
                .replace("%crate_displayname%", crate.getDisplayName());
            Location location = placedCrate.location().clone().add(0.5D,
                plugin.getConfig().getDouble("virtual-key-display.height", 1.05D), 0.5D);
            ArmorStand stand = location.getWorld().spawn(location, ArmorStand.class);
            stand.setInvisible(true);
            stand.setMarker(true);
            stand.setGravity(false);
            stand.setPersistent(false);
            stand.setCustomNameVisible(true);
            stand.customName(ColorUtil.component(line));
            stand.getPersistentDataContainer().set(virtualDisplayKey, PersistentDataType.BYTE, (byte) 1);
            for (Player viewer : Bukkit.getOnlinePlayers()) {
                if (!viewer.getUniqueId().equals(owner.getUniqueId())) {
                    viewer.hideEntity(plugin, stand);
                }
            }
            owner.showEntity(plugin, stand);
            displays.add(stand);
        }
        virtualDisplays.put(owner.getUniqueId(), displays);
    }

    private void clearVirtualDisplays() {
        virtualDisplays.values().stream().flatMap(List::stream).filter(stand -> !stand.isDead()).forEach(ArmorStand::remove);
        virtualDisplays.clear();
        for (World world : Bukkit.getWorlds()) {
            for (ArmorStand stand : world.getEntitiesByClass(ArmorStand.class)) {
                if (stand.getPersistentDataContainer().has(virtualDisplayKey, PersistentDataType.BYTE)) {
                    stand.remove();
                }
            }
        }
    }

    private void removeDisplays(UUID uuid) {
        List<ArmorStand> displays = virtualDisplays.remove(uuid);
        if (displays != null) {
            displays.stream().filter(stand -> !stand.isDead()).forEach(ArmorStand::remove);
        }
    }

    private void tickParticles() {
        animation += 0.18D;
        double viewDistance = Math.max(8.0D, plugin.getConfig().getDouble("particles.view-distance", 32.0D));
        double viewDistanceSquared = viewDistance * viewDistance;
        for (CrateLocationManager.PlacedCrate placedCrate : locationManager.getPlacedCrates()) {
            Crate crate = placedCrate.crate();
            if (crate.getParticleEffect() == ParticleEffectType.NONE) {
                continue;
            }
            Location center = placedCrate.location().clone().add(0.5D, 0.65D, 0.5D);
            boolean hasViewer = center.getWorld().getPlayers().stream()
                .anyMatch(player -> player.getLocation().distanceSquared(center) <= viewDistanceSquared);
            if (!hasViewer) {
                continue;
            }
            drawEffect(center, crate.getParticleEffect());
        }
    }

    private void drawEffect(Location center, ParticleEffectType effect) {
        switch (effect) {
            case RING -> drawHorizontalRing(center, 0.9D, animation, Particle.END_ROD);
            case SPIRAL -> {
                for (int index = 0; index < 5; index++) {
                    double angle = animation + index * 1.25D;
                    particle(center, Math.cos(angle) * 0.75D, index * 0.25D, Math.sin(angle) * 0.75D, Particle.ENCHANT);
                }
            }
            case HELIX -> {
                for (int index = 0; index < 6; index++) {
                    double angle = animation + index * 0.9D;
                    double y = index * 0.2D;
                    particle(center, Math.cos(angle) * 0.7D, y, Math.sin(angle) * 0.7D, Particle.ELECTRIC_SPARK);
                    particle(center, Math.cos(angle + Math.PI) * 0.7D, y, Math.sin(angle + Math.PI) * 0.7D, Particle.ELECTRIC_SPARK);
                }
            }
            case DYSON_SPHERE -> drawDysonSphere(center);
            case NONE -> { }
        }
    }

    private void drawDysonSphere(Location center) {
        double radius = 1.05D;
        for (int index = 0; index < 8; index++) {
            double angle = animation + index * (Math.PI / 4.0D);
            particle(center, Math.cos(angle) * radius, Math.sin(angle) * radius + 0.35D, 0.0D, Particle.END_ROD);
            particle(center, 0.0D, Math.sin(angle) * radius + 0.35D, Math.cos(angle) * radius, Particle.ELECTRIC_SPARK);
            particle(center, Math.cos(angle) * radius, 0.35D, Math.sin(angle) * radius, Particle.ENCHANT);
        }
    }

    private void drawHorizontalRing(Location center, double radius, double phase, Particle particle) {
        for (int index = 0; index < 8; index++) {
            double angle = phase + index * (Math.PI / 4.0D);
            particle(center, Math.cos(angle) * radius, 0.25D, Math.sin(angle) * radius, particle);
        }
    }

    private void particle(Location center, double x, double y, double z, Particle particle) {
        center.getWorld().spawnParticle(particle, center.getX() + x, center.getY() + y, center.getZ() + z,
            1, 0.0D, 0.0D, 0.0D, 0.0D);
    }
}
