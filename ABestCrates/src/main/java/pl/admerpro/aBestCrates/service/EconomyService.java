package pl.admerpro.aBestCrates.service;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class EconomyService {
    private final JavaPlugin plugin;
    private EconomyHook hook;

    public EconomyService(JavaPlugin plugin) {
        this.plugin = plugin;
        connect();
    }

    public boolean isAvailable() {
        return hook() != null && hook.isAvailable();
    }

    public boolean has(Player player, double amount) {
        EconomyHook economy = hook();
        return economy != null && economy.has(player, amount);
    }

    public boolean withdraw(Player player, double amount) {
        if (amount <= 0.0D) {
            return true;
        }
        EconomyHook economy = hook();
        return economy != null && economy.withdraw(player, amount);
    }

    public boolean deposit(Player player, double amount) {
        if (amount <= 0.0D) {
            return true;
        }
        EconomyHook economy = hook();
        if (economy == null || !economy.deposit(player, amount)) {
            plugin.getLogger().severe("Vault refund failed for " + player.getName() + ".");
            return false;
        }
        return true;
    }

    private EconomyHook hook() {
        if (hook == null) {
            connect();
        }
        return hook;
    }

    private void connect() {
        if (!Bukkit.getPluginManager().isPluginEnabled("Vault")) {
            return;
        }
        try {
            hook = new VaultEconomyHook(plugin);
        } catch (LinkageError error) {
            plugin.getLogger().warning("Vault is enabled but its economy API could not be loaded: " + error.getMessage());
        }
    }
}
