package com.stealthsense.config;

public class MobDetectionProfile {
    public String id;
    public String displayName;
    public double maxRange;
    public double fovDegrees;
    public double clarityThreshold;
    public double speedBlurFactor;
    public double sneakMultiplier;
    public double intelligence;
    public int memoryTicks;

    public MobDetectionProfile() {}

    public MobDetectionProfile(String id, String displayName, double maxRange, double fovDegrees,
                               double clarityThreshold, double speedBlurFactor, double sneakMultiplier,
                               double intelligence, int memoryTicks) {
        this.id = id;
        this.displayName = displayName;
        this.maxRange = maxRange;
        this.fovDegrees = fovDegrees;
        this.clarityThreshold = clarityThreshold;
        this.speedBlurFactor = speedBlurFactor;
        this.sneakMultiplier = sneakMultiplier;
        this.intelligence = intelligence;
        this.memoryTicks = memoryTicks;
    }

    public MobDetectionProfile copy() {
        return new MobDetectionProfile(id, displayName, maxRange, fovDegrees,
                clarityThreshold, speedBlurFactor, sneakMultiplier, intelligence, memoryTicks);
    }
}
