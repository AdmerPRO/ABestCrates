package pl.admerpro.aBestCrates.model;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Material;

public class KeyDefinition {
    private Material material = Material.TRIPWIRE_HOOK;
    private String displayName = "&6%crate_displayname% &fKey";
    private List<String> lore = new ArrayList<>(List.of("&7Use this key to open", "&f%crate_displayname%"));
    private boolean glow = true;
    private Integer customModelData;

    public Material getMaterial() {
        return material;
    }

    public void setMaterial(Material material) {
        this.material = material == null ? Material.TRIPWIRE_HOOK : material;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName == null || displayName.isBlank() ? "&6%crate_displayname% &fKey" : displayName;
    }

    public List<String> getLore() {
        return lore;
    }

    public void setLore(List<String> lore) {
        this.lore = lore == null ? new ArrayList<>() : new ArrayList<>(lore);
    }

    public boolean isGlow() {
        return glow;
    }

    public void setGlow(boolean glow) {
        this.glow = glow;
    }

    public Integer getCustomModelData() {
        return customModelData;
    }

    public void setCustomModelData(Integer customModelData) {
        this.customModelData = customModelData;
    }
}
