package pl.admerpro.aBestCrates.model;

public enum AnimationType {
    CLASSIC,
    FAST,
    SLOT_MACHINE,
    SPIN,
    INSTANT;

    public AnimationType next() {
        AnimationType[] values = values();
        return values[(ordinal() + 1) % values.length];
    }
}
