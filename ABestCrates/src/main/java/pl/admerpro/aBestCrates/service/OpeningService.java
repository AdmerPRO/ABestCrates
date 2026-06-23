package pl.admerpro.aBestCrates.service;

import org.bukkit.entity.Player;
import pl.admerpro.aBestCrates.model.Crate;

public interface OpeningService {
    void open(Player player, Crate crate);

    void forceOpen(Player player, Crate crate);

    void openAllKeys(Player player, Crate crate);
}
