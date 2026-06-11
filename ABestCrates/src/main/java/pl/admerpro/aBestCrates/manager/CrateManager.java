package pl.admerpro.aBestCrates.manager;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import pl.admerpro.aBestCrates.model.Crate;
import pl.admerpro.aBestCrates.model.Reward;
import pl.admerpro.aBestCrates.storage.CrateStorage;

public class CrateManager {
    private final CrateStorage storage;
    private final Map<String, Crate> crates = new LinkedHashMap<>();

    public CrateManager(JavaPlugin plugin) {
        this.storage = new CrateStorage(plugin);
    }

    public void load() {
        crates.clear();
        for (Crate crate : storage.load()) {
            crates.put(key(crate.getId()), crate);
        }
    }

    public void save() {
        storage.save(crates.values());
    }

    public Optional<Crate> getCrate(String id) {
        return Optional.ofNullable(crates.get(key(id)));
    }

    public Collection<Crate> getCrates() {
        return crates.values();
    }

    public boolean exists(String id) {
        return crates.containsKey(key(id));
    }

    public Crate createCrate(String id) {
        Crate crate = new Crate(Crate.normalizeId(id));
        Reward reward = new Reward("default");
        reward.setDisplayItem(new ItemStack(Material.DIAMOND));
        reward.setItemReward(new ItemStack(Material.DIAMOND));
        reward.setRealChance(100.0D);
        reward.setDisplayChance(100.0D);
        crate.addReward(reward);
        crates.put(key(crate.getId()), crate);
        save();
        return crate;
    }

    public boolean deleteCrate(String id) {
        boolean removed = crates.remove(key(id)) != null;
        if (removed) {
            save();
        }
        return removed;
    }

    public Optional<Crate> renameCrate(String currentId, String newId) {
        String normalizedNewId = Crate.normalizeId(newId);
        if (normalizedNewId.isBlank() || exists(normalizedNewId)) {
            return Optional.empty();
        }

        Crate crate = crates.remove(key(currentId));
        if (crate == null) {
            return Optional.empty();
        }

        crate.setId(normalizedNewId);
        crates.put(key(crate.getId()), crate);
        save();
        return Optional.of(crate);
    }

    public String nextGeneratedName() {
        int index = 1;
        while (exists("Crate" + index)) {
            index++;
        }
        return "Crate" + index;
    }

    private String key(String id) {
        return Crate.normalizeId(id).toLowerCase(Locale.ROOT);
    }
}
