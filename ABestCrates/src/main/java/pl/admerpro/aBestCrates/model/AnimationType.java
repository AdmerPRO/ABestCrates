package pl.admerpro.aBestCrates.model;

public enum AnimationType {
    CLASSIC,
    FAST,
    INSTANT,
    ROLL,
    CSGO,
    CASINO,
    COSMIC,
    ROULETTE,
    WHEEL,
    WONDER;

    public AnimationType next() {
        AnimationType[] values = values();
        return values[(ordinal() + 1) % values.length];
    }
}
