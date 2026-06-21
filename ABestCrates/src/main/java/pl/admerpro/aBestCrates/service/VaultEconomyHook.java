package pl.admerpro.aBestCrates.service;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

final class VaultEconomyHook implements EconomyHook {
    private final JavaPlugin plugin;

    VaultEconomyHook(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean isAvailable() {
        return provider() != null;
    }

    @Override
    public boolean has(Player player, double amount) {
        Economy economy = provider();
        return economy != null && economy.has(player, amount);
    }

    @Override
    public boolean withdraw(Player player, double amount) {
        Economy economy = provider();
        if (economy == null) {
            return false;
        }
        EconomyResponse response = economy.withdrawPlayer(player, amount);
        if (!response.transactionSuccess()) {
            plugin.getLogger().warning("Vault withdrawal failed for " + player.getName() + ": " + response.errorMessage);
        }
        return response.transactionSuccess();
    }

    @Override
    public boolean deposit(Player player, double amount) {
        Economy economy = provider();
        if (economy == null) {
            return false;
        }
        EconomyResponse response = economy.depositPlayer(player, amount);
        if (!response.transactionSuccess()) {
            plugin.getLogger().severe("Vault refund failed for " + player.getName() + ": " + response.errorMessage);
        }
        return response.transactionSuccess();
    }

    private Economy provider() {
        RegisteredServiceProvider<Economy> registration =
            Bukkit.getServicesManager().getRegistration(Economy.class);
        return registration == null ? null : registration.getProvider();
    }
}
