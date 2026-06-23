package pl.admerpro.aBestCrates.service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
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

    public int recordOpen(Player player, Crate crate) {
        rememberPlayer(player);
        return recordOpen(player.getUniqueId(), crate);
    }

    public int getOpenCount(UUID uuid, String crateId) {
        return data.getInt(playerPath(uuid) + ".opens." + crateId.toLowerCase(Locale.ROOT), 0);
    }

    public Map<String, Integer> getPlayerOpenCounts(UUID uuid) {
        ConfigurationSection opensSection = data.getConfigurationSection(playerPath(uuid) + ".opens");
        if (opensSection == null) {
            return Map.of();
        }

        Map<String, Integer> counts = new LinkedHashMap<>();
        for (String crateId : opensSection.getKeys(false)) {
            int amount = Math.max(0, opensSection.getInt(crateId));
            if (amount > 0) {
                counts.put(crateId, amount);
            }
        }
        return sortedByValueDesc(counts);
    }

    public int getTotalOpenCount(UUID uuid) {
        return getPlayerOpenCounts(uuid).values().stream()
            .mapToInt(Integer::intValue)
            .sum();
    }

    public Map<UUID, Integer> getPlayerTotals() {
        ConfigurationSection playersSection = data.getConfigurationSection("players");
        if (playersSection == null) {
            return Map.of();
        }

        Map<UUID, Integer> totals = new LinkedHashMap<>();
        for (String uuidValue : playersSection.getKeys(false)) {
            UUID uuid = parseUuid(uuidValue).orElse(null);
            if (uuid == null) {
                continue;
            }
            int amount = getTotalOpenCount(uuid);
            if (amount > 0) {
                totals.put(uuid, amount);
            }
        }
        return sortedByValueDesc(totals);
    }

    public Map<UUID, Integer> getPlayersForCrate(String crateId) {
        ConfigurationSection playersSection = data.getConfigurationSection("players");
        if (playersSection == null) {
            return Map.of();
        }

        String crateKey = key(crateId);
        Map<UUID, Integer> totals = new LinkedHashMap<>();
        for (String uuidValue : playersSection.getKeys(false)) {
            UUID uuid = parseUuid(uuidValue).orElse(null);
            if (uuid == null) {
                continue;
            }
            int amount = data.getInt(playerPath(uuid) + ".opens." + crateKey, 0);
            if (amount > 0) {
                totals.put(uuid, amount);
            }
        }
        return sortedByValueDesc(totals);
    }

    public Map<String, Integer> getCrateTotals() {
        ConfigurationSection playersSection = data.getConfigurationSection("players");
        if (playersSection == null) {
            return Map.of();
        }

        Map<String, Integer> totals = new LinkedHashMap<>();
        for (String uuidValue : playersSection.getKeys(false)) {
            ConfigurationSection opensSection = playersSection.getConfigurationSection(uuidValue + ".opens");
            if (opensSection == null) {
                continue;
            }
            for (String crateId : opensSection.getKeys(false)) {
                int amount = Math.max(0, opensSection.getInt(crateId));
                if (amount > 0) {
                    totals.merge(crateId, amount, Integer::sum);
                }
            }
        }
        return sortedByValueDesc(totals);
    }

    public String getKnownPlayerName(UUID uuid) {
        String name = data.getString(playerPath(uuid) + ".name");
        if (name != null && !name.isBlank()) {
            return name;
        }

        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
        if (offlinePlayer.getName() != null && !offlinePlayer.getName().isBlank()) {
            return offlinePlayer.getName();
        }
        String uuidValue = uuid.toString();
        return uuidValue.substring(0, Math.min(8, uuidValue.length()));
    }

    public Optional<UUID> findKnownPlayerUuid(String input) {
        if (input == null || input.isBlank()) {
            return Optional.empty();
        }

        String name = input.trim();
        Player onlinePlayer = Bukkit.getPlayerExact(name);
        if (onlinePlayer != null) {
            return Optional.of(onlinePlayer.getUniqueId());
        }

        Optional<UUID> uuidInput = parseUuid(name);
        if (uuidInput.isPresent()) {
            return uuidInput;
        }

        ConfigurationSection playersSection = data.getConfigurationSection("players");
        if (playersSection == null) {
            return Optional.empty();
        }
        for (String uuidValue : playersSection.getKeys(false)) {
            String knownName = playersSection.getString(uuidValue + ".name", "");
            if (knownName.equalsIgnoreCase(name)) {
                return parseUuid(uuidValue);
            }
        }
        return Optional.empty();
    }

    public boolean resetCooldowns(UUID uuid, String crateId) {
        String path = playerPath(uuid) + ".cooldowns";
        if (isAll(crateId)) {
            return clearPath(path);
        }
        return clearPath(path + "." + key(crateId));
    }

    public boolean resetStats(UUID uuid, String crateId) {
        boolean changed;
        String path = playerPath(uuid);
        if (isAll(crateId)) {
            changed = clearPath(path + ".opens");
            changed |= clearPath(path + ".streak");
            return changed;
        }

        String normalizedCrate = key(crateId);
        changed = clearPath(path + ".opens." + normalizedCrate);
        if (data.getString(path + ".streak.crate", "").equalsIgnoreCase(normalizedCrate)) {
            changed |= clearPath(path + ".streak");
        }
        return changed;
    }

    public boolean resetPlayerRewardLimits(UUID uuid, String crateId, String rewardId) {
        String path = playerPath(uuid) + ".rewards";
        if (isAll(crateId)) {
            return clearPath(path);
        }
        path += "." + key(crateId);
        if (isAll(rewardId)) {
            return clearPath(path);
        }
        return clearPath(path + "." + key(rewardId));
    }

    public boolean resetGlobalRewardLimits(String crateId, String rewardId) {
        String path = "global-rewards";
        if (isAll(crateId)) {
            return clearPath(path);
        }
        path += "." + key(crateId);
        if (isAll(rewardId)) {
            return clearPath(path);
        }
        return clearPath(path + "." + key(rewardId));
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
        rememberPlayer(player);
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

    private void rememberPlayer(Player player) {
        String path = playerPath(player.getUniqueId()) + ".name";
        if (!player.getName().equals(data.getString(path))) {
            data.set(path, player.getName());
        }
    }

    private Optional<UUID> parseUuid(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(UUID.fromString(value));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    private <T> Map<T, Integer> sortedByValueDesc(Map<T, Integer> values) {
        return values.entrySet().stream()
            .sorted(Map.Entry.<T, Integer>comparingByValue().reversed()
                .thenComparing(entry -> String.valueOf(entry.getKey())))
            .collect(LinkedHashMap::new,
                (map, entry) -> map.put(entry.getKey(), entry.getValue()),
                LinkedHashMap::putAll);
    }

    private boolean clearPath(String path) {
        if (!data.contains(path)) {
            return false;
        }
        data.set(path, null);
        markDirty();
        return true;
    }

    private boolean isAll(String value) {
        return value == null || value.isBlank() || value.equalsIgnoreCase("all") || value.equals("*");
    }

    private String key(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }
}
