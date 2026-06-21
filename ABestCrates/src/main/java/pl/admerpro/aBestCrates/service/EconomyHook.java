package pl.admerpro.aBestCrates.service;

import org.bukkit.entity.Player;

interface EconomyHook {
    boolean isAvailable();

    boolean has(Player player, double amount);

    boolean withdraw(Player player, double amount);

    boolean deposit(Player player, double amount);
}
