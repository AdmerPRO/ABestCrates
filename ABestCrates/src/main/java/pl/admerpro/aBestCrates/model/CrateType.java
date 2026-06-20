package pl.admerpro.aBestCrates.model;

public enum CrateType {
    GAMBLE,
    CHOOSE;

    public CrateType next() {
        CrateType[] values = values();
        return values[(ordinal() + 1) % values.length];
    }
}
