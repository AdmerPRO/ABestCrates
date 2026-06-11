package pl.admerpro.aBestCrates.util;

import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
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
            itemMeta.displayName(ColorUtil.component(name));
        }
        return this;
    }

    public ItemBuilder lore(List<String> lore) {
        if (itemMeta != null) {
            itemMeta.lore(ColorUtil.components(lore == null ? new ArrayList<>() : lore));
        }
        return this;
    }

    public ItemBuilder addLore(String line) {
        if (itemMeta != null) {
            List<Component> lore = itemMeta.hasLore() && itemMeta.lore() != null
                ? new ArrayList<>(itemMeta.lore())
                : new ArrayList<>();
            lore.add(ColorUtil.component(line));
            itemMeta.lore(lore);
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
