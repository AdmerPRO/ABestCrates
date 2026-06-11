package pl.admerpro.aBestCrates.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class MenuHolder implements InventoryHolder {
    private final MenuType type;
    private final String crateId;
    private final String rewardId;
    private Inventory inventory;

    public MenuHolder(MenuType type, String crateId) {
        this(type, crateId, null);
    }

    public MenuHolder(MenuType type, String crateId, String rewardId) {
        this.type = type;
        this.crateId = crateId;
        this.rewardId = rewardId;
    }

    public MenuType getType() {
        return type;
    }

    public String getCrateId() {
        return crateId;
    }

    public String getRewardId() {
        return rewardId;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
