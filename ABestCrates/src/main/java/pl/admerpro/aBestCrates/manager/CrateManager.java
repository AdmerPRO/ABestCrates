package pl.admerpro.aBestCrates.manager;

import java.io.File;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import pl.admerpro.aBestCrates.model.Crate;
import pl.admerpro.aBestCrates.model.KeyRequirement;
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
        String normalizedId = Crate.normalizeId(id);
        if (!Crate.isValidId(normalizedId) || exists(normalizedId)) {
            throw new IllegalArgumentException("Invalid or duplicate crate id: " + id);
        }
        Crate crate = new Crate(normalizedId);
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
        if (!Crate.isValidId(normalizedNewId) || exists(normalizedNewId)) {
            return Optional.empty();
        }

        Crate crate = crates.remove(key(currentId));
        if (crate == null) {
            return Optional.empty();
        }

        crate.setId(normalizedNewId);
        crates.put(key(crate.getId()), crate);
        for (Crate configuredCrate : crates.values()) {
            boolean referencesRenamedCrate = configuredCrate.getKeyRequirements().stream()
                .anyMatch(requirement -> requirement.crateId().equalsIgnoreCase(currentId));
            if (referencesRenamedCrate) {
                configuredCrate.setKeyRequirements(configuredCrate.getKeyRequirements().stream()
                    .map(requirement -> requirement.crateId().equalsIgnoreCase(currentId)
                        ? new KeyRequirement(normalizedNewId, requirement.amount()) : requirement)
                    .toList());
            }
        }
        save();
        return Optional.of(crate);
    }

    public Optional<Crate> duplicateCrate(String sourceId, String newId) {
        String normalizedNewId = Crate.normalizeId(newId);
        Crate source = crates.get(key(sourceId));
        if (source == null || !Crate.isValidId(normalizedNewId) || exists(normalizedNewId)) {
            return Optional.empty();
        }

        Crate copy = copyCrate(source, normalizedNewId);
        crates.put(key(copy.getId()), copy);
        save();
        return Optional.of(copy);
    }

    public boolean exportTemplate(String sourceId, File templateFile) {
        Crate source = crates.get(key(sourceId));
        return source != null && storage.exportTemplate(source, templateFile);
    }

    public Optional<Crate> importTemplate(File templateFile, String newId) {
        Optional<Crate> imported = storage.importTemplate(templateFile, newId);
        if (imported.isEmpty()) {
            return Optional.empty();
        }

        Crate crate = imported.get();
        if (!Crate.isValidId(crate.getId()) || exists(crate.getId())) {
            return Optional.empty();
        }
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

    private Crate copyCrate(Crate source, String newId) {
        Crate copy = new Crate(newId);
        copy.setDisplayName(source.getDisplayName());
        copy.setColor(source.getColor());
        copy.setBlockMaterial(source.getBlockMaterial());
        copy.setHologram(source.getHologram());
        copy.setNoKeyMessage(source.getNoKeyMessage());
        copy.setAnimationType(source.getAnimationType());
        copy.setCrateType(source.getCrateType());
        copy.setParticleEffect(source.getParticleEffect());
        copy.setVirtualKeyDisplay(source.isVirtualKeyDisplay());
        copy.setPermission(source.getPermission());
        copy.setCooldownSeconds(source.getCooldownSeconds());
        copy.setOpenCost(source.getOpenCost());
        copy.setPushback(source.isPushback());
        copy.setPreviewTitle(source.getPreviewTitle());
        copy.setOpeningTitle(source.getOpeningTitle());
        copy.setRewardRolls(source.getRewardRolls());
        copy.setKeyRequirements(source.getKeyRequirements().stream()
            .map(requirement -> new KeyRequirement(
                requirement.crateId().equalsIgnoreCase(source.getId()) ? newId : requirement.crateId(),
                requirement.amount()))
            .toList());
        copy.setMilestones(source.getMilestones());
        copy.setKeyDefinition(copyKey(source));
        for (Reward reward : source.getRewards()) {
            copy.addReward(copyReward(reward));
        }
        return copy;
    }

    private pl.admerpro.aBestCrates.model.KeyDefinition copyKey(Crate source) {
        pl.admerpro.aBestCrates.model.KeyDefinition sourceKey = source.getKeyDefinition();
        pl.admerpro.aBestCrates.model.KeyDefinition copy = new pl.admerpro.aBestCrates.model.KeyDefinition();
        copy.setMaterial(sourceKey.getMaterial());
        copy.setDisplayName(sourceKey.getDisplayName());
        copy.setLore(sourceKey.getLore());
        copy.setGlow(sourceKey.isGlow());
        copy.setCustomModelData(sourceKey.getCustomModelData());
        copy.setTemplateItem(sourceKey.getTemplateItem());
        return copy;
    }

    private Reward copyReward(Reward source) {
        Reward copy = new Reward(source.getId());
        copy.setDisplayItem(source.getDisplayItem());
        copy.setItemRewards(source.getItemRewards());
        copy.setCommands(source.getCommands());
        copy.setRealChance(source.getRealChance());
        copy.setDisplayChance(source.getDisplayChance());
        copy.setRarity(source.getRarity());
        copy.setBroadcast(source.isBroadcast());
        copy.setFireworks(source.isFireworks());
        copy.setWeight(source.getWeight());
        copy.setRequiredPermissions(source.getRequiredPermissions());
        copy.setBlockedPermissions(source.getBlockedPermissions());
        copy.setGlobalLimit(source.getGlobalLimit());
        copy.setPlayerLimit(source.getPlayerLimit());
        return copy;
    }
}
