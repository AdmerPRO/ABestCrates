package pl.admerpro.aBestCrates.service;

import java.lang.reflect.Method;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class EconomyService {
    private final JavaPlugin plugin;

    public EconomyService(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean isAvailable() {
        return provider() != null;
    }

    public boolean has(Player player, double amount) {
        Object provider = provider();
        if (provider == null) {
            return false;
        }
        try {
            Method has = provider.getClass().getMethod("has", OfflinePlayer.class, double.class);
            return Boolean.TRUE.equals(has.invoke(provider, player, amount));
        } catch (ReflectiveOperationException exception) {
            plugin.getLogger().warning("Vault economy check failed: " + exception.getMessage());
            return false;
        }
    }

    public boolean withdraw(Player player, double amount) {
        if (amount <= 0.0D) {
            return true;
        }
        Object provider = provider();
        if (provider == null) {
            return false;
        }
        try {
            Method withdraw = provider.getClass().getMethod("withdrawPlayer", OfflinePlayer.class, double.class);
            Object response = withdraw.invoke(provider, player, amount);
            Method success = response.getClass().getMethod("transactionSuccess");
            return Boolean.TRUE.equals(success.invoke(response));
        } catch (ReflectiveOperationException exception) {
            plugin.getLogger().warning("Vault withdrawal failed: " + exception.getMessage());
            return false;
        }
    }

    private Object provider() {
        try {
            Class<?> economyClass = Class.forName("net.milkbowl.vault.economy.Economy");
            @SuppressWarnings({"rawtypes", "unchecked"})
            RegisteredServiceProvider<?> registration = Bukkit.getServicesManager().getRegistration((Class) economyClass);
            return registration == null ? null : registration.getProvider();
        } catch (ClassNotFoundException exception) {
            return null;
        }
    }
}
