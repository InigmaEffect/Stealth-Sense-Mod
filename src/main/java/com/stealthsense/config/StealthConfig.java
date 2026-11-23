package com.stealthsense.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.stealthsense.StealthSenseMod;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public class StealthConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("stealthsense.json");

    public static class Data {
        public boolean modEnabled = true;
        public boolean showDebugActionbar = false;
        public double globalSpeedBypassThreshold = 0.42;
        public Map<String, MobDetectionProfile> profiles = new LinkedHashMap<>();
    }

    public static Data INSTANCE = new Data();

    public static void load() {
        if (Files.exists(CONFIG_PATH)) {
            try {
                String json = Files.readString(CONFIG_PATH);
                Data loaded = GSON.fromJson(json, Data.class);
                if (loaded != null && loaded.profiles != null) {
                    INSTANCE = loaded;
                }
            } catch (Exception e) {
                StealthSenseMod.LOGGER.error("Failed to load stealthsense.json, regenerating defaults", e);
            }
        }
        populateMissingProfiles();
        save();
    }

    public static void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            Files.writeString(CONFIG_PATH, GSON.toJson(INSTANCE));
        } catch (IOException e) {
            StealthSenseMod.LOGGER.error("Failed to save stealthsense.json", e);
        }
    }

    public static void resetDefaults() {
        INSTANCE.profiles.clear();
        populateMissingProfiles();
        save();
    }

    public static void populateMissingProfiles() {
        addDefault("minecraft:zombie", "Zombie", 16.0, 95.0, 0.35, 1.35, 0.45, 0.15, 100);
        addDefault("minecraft:husk", "Husk", 16.0, 95.0, 0.35, 1.35, 0.45, 0.15, 100);
        addDefault("minecraft:drowned", "Drowned", 18.0, 100.0, 0.40, 1.30, 0.45, 0.20, 110);
        addDefault("minecraft:skeleton", "Skeleton", 26.0, 115.0, 0.25, 0.85, 0.55, 0.65, 220);
        addDefault("minecraft:stray", "Stray", 26.0, 115.0, 0.25, 0.85, 0.55, 0.65, 220);
        addDefault("minecraft:bogged", "Bogged", 24.0, 110.0, 0.30, 0.90, 0.50, 0.60, 200);
        addDefault("minecraft:creeper", "Creeper", 18.0, 105.0, 0.30, 1.15, 0.40, 0.35, 140);
        addDefault("minecraft:spider", "Spider", 20.0, 160.0, 0.20, 0.70, 0.60, 0.40, 160);
        addDefault("minecraft:cave_spider", "Cave Spider", 18.0, 165.0, 0.20, 0.65, 0.60, 0.45, 160);
        addDefault("minecraft:enderman", "Enderman (Long-Sight)", 64.0, 130.0, 0.12, 0.45, 0.75, 0.85, 360);
        addDefault("minecraft:pillager", "Pillager", 30.0, 120.0, 0.22, 0.80, 0.60, 0.80, 280);
        addDefault("minecraft:vindicator", "Vindicator", 22.0, 115.0, 0.28, 0.95, 0.55, 0.75, 240);
        addDefault("minecraft:evoker", "Evoker", 28.0, 125.0, 0.20, 0.75, 0.65, 0.95, 320);
        addDefault("minecraft:witch", "Witch", 22.0, 115.0, 0.25, 0.90, 0.55, 0.85, 260);
        addDefault("minecraft:blaze", "Blaze", 32.0, 135.0, 0.20, 0.75, 0.65, 0.60, 240);
        addDefault("minecraft:ghast", "Ghast", 56.0, 140.0, 0.18, 0.60, 0.70, 0.50, 220);
        addDefault("minecraft:piglin", "Piglin", 22.0, 110.0, 0.28, 0.95, 0.50, 0.70, 220);
        addDefault("minecraft:piglin_brute", "Piglin Brute", 24.0, 120.0, 0.24, 0.85, 0.60, 0.80, 280);
        addDefault("minecraft:warden", "Warden (Vanilla Sound/Smell)", 24.0, 360.0, 0.0, 0.0, 1.0, 0.90, 400);

        for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
            Identifier key = BuiltInRegistries.ENTITY_TYPE.getKey(type);
            String id = key.toString();
            if (!INSTANCE.profiles.containsKey(id)) {
                MobCategory cat = type.getCategory();
                if (cat == MobCategory.MONSTER) {
                    addDefault(id, formatName(key.getPath()), 20.0, 110.0, 0.30, 1.00, 0.50, 0.50, 180);
                } else if (cat == MobCategory.CREATURE || cat == MobCategory.AMBIENT || cat == MobCategory.AXOLOTLS) {
                    addDefault(id, formatName(key.getPath()), 14.0, 130.0, 0.35, 1.20, 0.45, 0.25, 80);
                }
            }
        }
    }

    private static void addDefault(String id, String name, double range, double fov,
                                   double clarity, double speedBlur, double sneak, double intel, int memory) {
        if (!INSTANCE.profiles.containsKey(id)) {
            INSTANCE.profiles.put(id, new MobDetectionProfile(id, name, range, fov, clarity, speedBlur, sneak, intel, memory));
        }
    }

    public static MobDetectionProfile getProfile(Mob mob) {
        Identifier key = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType());
        String id = key.toString();
        MobDetectionProfile profile = INSTANCE.profiles.get(id);
        if (profile == null) {
            profile = new MobDetectionProfile(id, formatName(key.getPath()), 20.0, 110.0, 0.30, 1.00, 0.50, 0.50, 180);
            INSTANCE.profiles.put(id, profile);
        }
        return profile;
    }

    private static String formatName(String path) {
        String[] parts = path.split("_");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (!p.isEmpty()) {
                sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1)).append(" ");
            }
        }
        return sb.toString().trim();
    }
}
