package pl.admerpro.aBestCrates.service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import java.util.logging.Level;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import pl.admerpro.aBestCrates.model.Crate;
import pl.admerpro.aBestCrates.model.Reward;

public class PlayerDataService {
    private final JavaPlugin plugin;
    private final File dataFile;
    private final File logFile;
    private YamlConfiguration data;
    private int batchDepth;
    private boolean batchDirty;
    private final StringBuilder batchedLog = new StringBuilder();

    public PlayerDataService(JavaPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "player-data.yml");
        this.logFile = new File(plugin.getDataFolder(), "reward-rolls.log");
        this.data = YamlConfiguration.loadConfiguration(dataFile);
    }

    public void reload() {
        data = YamlConfiguration.loadConfiguration(dataFile);
    }

    public void save() {
        try {
            data.save(dataFile);
        } catch (IOException exception) {
            plugin.getLogger().log(Level.SEVERE, "Could not save player-data.yml.", exception);
        }
    }

    public long remainingCooldown(UUID uuid, Crate crate) {
        long expiresAt = data.getLong(playerPath(uuid) + ".cooldowns." + crate.getId().toLowerCase(Locale.ROOT), 0L);
        return Math.max(0L, (expiresAt - System.currentTimeMillis() + 999L) / 1000L);
    }

    public void startCooldown(UUID uuid, Crate crate) {
        if (crate.getCooldownSeconds() <= 0L) {
            return;
        }
        data.set(playerPath(uuid) + ".cooldowns." + crate.getId().toLowerCase(Locale.ROOT),
            System.currentTimeMillis() + crate.getCooldownSeconds() * 1000L);
        markDirty();
    }

    public int recordOpen(UUID uuid, Crate crate) {
        String path = playerPath(uuid);
        String lastCrate = data.getString(path + ".streak.crate", "");
        int streak = lastCrate.equalsIgnoreCase(crate.getId()) ? data.getInt(path + ".streak.amount", 0) + 1 : 1;
        data.set(path + ".streak.crate", crate.getId());
        data.set(path + ".streak.amount", streak);
        String totalPath = path + ".opens." + crate.getId().toLowerCase(Locale.ROOT);
        data.set(totalPath, data.getInt(totalPath, 0) + 1);
        markDirty();
        return streak;
    }

    public int getOpenCount(UUID uuid, String crateId) {
        return data.getInt(playerPath(uuid) + ".opens." + crateId.toLowerCase(Locale.ROOT), 0);
    }

    public boolean canReceive(Player player, Crate crate, Reward reward) {
        return canReceive(player, crate, reward, 0);
    }

    public boolean canReceive(Player player, Crate crate, Reward reward, int reservedAmount) {
        String rewardKey = crate.getId().toLowerCase(Locale.ROOT) + "."
            + reward.getId().toLowerCase(Locale.ROOT);
        int reserved = Math.max(0, reservedAmount);
        if (reward.getGlobalLimit() > 0
            && data.getInt("global-rewards." + rewardKey, 0) + reserved >= reward.getGlobalLimit()) {
            return false;
        }
        return reward.getPlayerLimit() <= 0
            || data.getInt(playerPath(player.getUniqueId()) + ".rewards." + rewardKey, 0) + reserved
                < reward.getPlayerLimit();
    }

    public void recordReward(Player player, Crate crate, Reward reward) {
        String rewardKey = crate.getId().toLowerCase(Locale.ROOT) + "."
            + reward.getId().toLowerCase(Locale.ROOT);
        String playerRewardPath = playerPath(player.getUniqueId()) + ".rewards." + rewardKey;
        data.set(playerRewardPath, data.getInt(playerRewardPath, 0) + 1);
        data.set("global-rewards." + rewardKey, data.getInt("global-rewards." + rewardKey, 0) + 1);
        markDirty();
        appendLog(player, crate, reward);
    }

    public void beginBatch() {
        batchDepth++;
    }

    public void endBatch() {
        if (batchDepth <= 0) {
            return;
        }
        batchDepth--;
        if (batchDepth != 0) {
            return;
        }
        if (batchDirty) {
            batchDirty = false;
            save();
        }
        flushLog();
    }

    private synchronized void appendLog(Player player, Crate crate, Reward reward) {
        String line = "%s player=%s uuid=%s crate=%s reward=%s%n".formatted(
            Instant.now(), player.getName(), player.getUniqueId(), crate.getId(), reward.getId());
        if (batchDepth > 0) {
            batchedLog.append(line);
        } else {
            writeLog(line);
        }
    }

    private void markDirty() {
        if (batchDepth > 0) {
            batchDirty = true;
        } else {
            save();
        }
    }

    private synchronized void flushLog() {
        if (batchedLog.isEmpty()) {
            return;
        }
        String content = batchedLog.toString();
        batchedLog.setLength(0);
        writeLog(content);
    }

    private void writeLog(String content) {
        try {
            Files.writeString(logFile.toPath(), content, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException exception) {
            plugin.getLogger().log(Level.WARNING, "Could not append reward-rolls.log.", exception);
        }
    }

    private String playerPath(UUID uuid) {
        return "players." + uuid;
    }
}
