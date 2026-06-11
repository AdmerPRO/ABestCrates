package pl.admerpro.aBestCrates.util;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class ItemBuilder {
    private final ItemStack itemStack;
    private final ItemMeta itemMeta;

    public ItemBuilder(Material material) {
        this(new ItemStack(material == null ? Material.STONE : material));
    }

    public ItemBuilder(ItemStack itemStack) {
        this.itemStack = itemStack == null ? new ItemStack(Material.STONE) : itemStack.clone();
        this.itemMeta = this.itemStack.getItemMeta();
    }

    public ItemBuilder name(String name) {
        if (itemMeta != null) {
            itemMeta.setDisplayName(ColorUtil.color(name));
        }
        return this;
    }

    public ItemBuilder lore(List<String> lore) {
        if (itemMeta != null) {
            itemMeta.setLore(ColorUtil.color(lore == null ? new ArrayList<>() : lore));
        }
        return this;
    }

    public ItemBuilder addLore(String line) {
        if (itemMeta != null) {
            List<String> lore = itemMeta.hasLore() ? itemMeta.getLore() : new ArrayList<>();
            lore.add(ColorUtil.color(line));
            itemMeta.setLore(lore);
        }
        return this;
    }

    public ItemBuilder hideFlags() {
        if (itemMeta != null) {
            itemMeta.addItemFlags(ItemFlag.values());
        }
        return this;
    }

    public ItemStack build() {
        if (itemMeta != null) {
            itemStack.setItemMeta(itemMeta);
        }
        return itemStack;
    }
}
