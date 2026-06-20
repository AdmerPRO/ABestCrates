package pl.admerpro.aBestCrates.model;

public enum ParticleEffectType {
    NONE,
    RING,
    SPIRAL,
    HELIX,
    DYSON_SPHERE;

    public ParticleEffectType next() {
        ParticleEffectType[] values = values();
        return values[(ordinal() + 1) % values.length];
    }
}
