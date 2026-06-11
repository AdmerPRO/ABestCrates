package pl.admerpro.aBestCrates.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class MenuHolder implements InventoryHolder {
    private final MenuType type;
    private final String crateId;
    private Inventory inventory;

    public MenuHolder(MenuType type, String crateId) {
        this.type = type;
        this.crateId = crateId;
    }

    public MenuType getType() {
        return type;
    }

    public String getCrateId() {
        return crateId;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
