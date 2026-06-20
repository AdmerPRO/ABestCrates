package pl.admerpro.aBestCrates.integration;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pl.admerpro.aBestCrates.Main;
import pl.admerpro.aBestCrates.manager.CrateManager;
import pl.admerpro.aBestCrates.manager.KeyManager;
import pl.admerpro.aBestCrates.service.PlayerDataService;

public class ABestCratesExpansion extends PlaceholderExpansion {
    private final Main plugin;
    private final CrateManager crateManager;
    private final KeyManager keyManager;
    private final PlayerDataService playerData;

    public ABestCratesExpansion(Main plugin, CrateManager crateManager, KeyManager keyManager,
                                PlayerDataService playerData) {
        this.plugin = plugin;
        this.crateManager = crateManager;
        this.keyManager = keyManager;
        this.playerData = playerData;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "abestcrates";
    }

    @Override
    public @NotNull String getAuthor() {
        return String.join(", ", plugin.getPluginMeta().getAuthors());
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) {
            return "0";
        }
        if (params.startsWith("virtual_")) {
            return String.valueOf(keyManager.getVirtualKeys(player.getUniqueId(), params.substring("virtual_".length())));
        }
        if (params.startsWith("opened_")) {
            return String.valueOf(playerData.getOpenCount(player.getUniqueId(), params.substring("opened_".length())));
        }
        if (params.startsWith("physical_") && player instanceof Player onlinePlayer) {
            return crateManager.getCrate(params.substring("physical_".length()))
                .map(crate -> String.valueOf(keyManager.countPhysicalKeys(onlinePlayer, crate))).orElse("0");
        }
        if (params.startsWith("total_")) {
            String crateId = params.substring("total_".length());
            int virtual = keyManager.getVirtualKeys(player.getUniqueId(), crateId);
            int physical = player instanceof Player onlinePlayer
                ? crateManager.getCrate(crateId).map(crate -> keyManager.countPhysicalKeys(onlinePlayer, crate)).orElse(0) : 0;
            return String.valueOf(virtual + physical);
        }
        return null;
    }
}
