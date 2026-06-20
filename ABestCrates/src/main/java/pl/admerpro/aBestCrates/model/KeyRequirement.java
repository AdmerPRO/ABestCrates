package pl.admerpro.aBestCrates.model;

public record KeyRequirement(String crateId, int amount) {
    public KeyRequirement {
        crateId = Crate.normalizeId(crateId);
        amount = Math.max(1, amount);
    }
}
