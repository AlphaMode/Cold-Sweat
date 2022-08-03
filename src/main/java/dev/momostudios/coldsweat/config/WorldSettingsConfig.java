package dev.momostudios.coldsweat.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class WorldSettingsConfig
{
    private static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.ConfigValue<List<? extends List<?>>> biomeOffsets;
    public static final ForgeConfigSpec.ConfigValue<List<? extends List<?>>> biomeTemps;
    public static final ForgeConfigSpec.ConfigValue<List<? extends List<?>>> dimensionOffsets;
    public static final ForgeConfigSpec.ConfigValue<List<? extends List<?>>> dimensionTemps;

    public static ForgeConfigSpec.ConfigValue<List<? extends Number>> summerTemps = null;
    public static ForgeConfigSpec.ConfigValue<List<? extends Number>> autumnTemps = null;
    public static ForgeConfigSpec.ConfigValue<List<? extends Number>> winterTemps = null;
    public static ForgeConfigSpec.ConfigValue<List<? extends Number>> springTemps = null;


    public static final WorldSettingsConfig INSTANCE = new WorldSettingsConfig();

    static
    {
        /*
         Dimensions
         */
        BUILDER.comment("Format: [[\"dimension-1\", temperature-1], [\"dimension-2\", temperature-2]... etc]",
                        "Common dimension IDs: minecraft:overworld, minecraft:the_nether, minecraft:the_end",
                        "Note: all temperatures are in Minecraft units",
                        "°F to MC = (x - 32) / 42",
                        "°C to MC = x / 23.3")
               .push("Dimensions");

        dimensionOffsets = BUILDER
                .comment("Applies an offset to the world's temperature across an entire dimension")
                .defineList("Dimension Temperature Offsets", Arrays.asList(
                        Arrays.asList("minecraft:the_nether", 1.0),
                        Arrays.asList("minecraft:the_end", -0.1)
                ), it -> it instanceof List && ((List<?>) it).get(0) instanceof String && ((List<?>) it).get(1) instanceof Number);

        dimensionTemps = BUILDER
                .comment("Overrides existing dimension temperatures & offsets",
                        "Also overrides temperatures of all biomes in the dimension")
                .defineList("Dimension Temperatures", Arrays.asList(
                        // No default values
                ), it -> it instanceof List && ((List<?>) it).get(0) instanceof String && ((List<?>) it).get(1) instanceof Number);

        BUILDER.pop();

        /*
         Biomes
         */
        BUILDER.comment("Format: [[\"biome-1\", temperature-1], [\"biome-2\", temperature-2]... etc]",
                        "Note: all temperatures are in Minecraft units")
               .push("Biomes");

        BUILDER.push("Biome Temperature Offsets");
        biomeOffsets = BUILDER
                .comment("Applies an offset to the temperature of a biome")
                .defineList("Biome Temperature Offsets", Arrays.asList(
                    Arrays.asList("minecraft:soul_sand_valley", -0.5),

                    Arrays.asList("minecraft:plains", 0.3),

                    Arrays.asList("minecraft:bamboo_jungle", 0.2),
                    Arrays.asList("minecraft:bamboo_jungle_hills", 0.2),
                    Arrays.asList("minecraft:jungle", 0.2),
                    Arrays.asList("minecraft:jungle_hills", 0.2),
                    Arrays.asList("minecraft:jungle_edge", 0.2),
                    Arrays.asList("minecraft:modified_jungle", 0.2),
                    Arrays.asList("minecraft:modified_jungle_edge", 0.2),

                    Arrays.asList("minecraft:desert", -0.2),
                    Arrays.asList("minecraft:desert_hills", -0.2),
                    Arrays.asList("minecraft:desert_lakes", -0.3),

                    Arrays.asList("minecraft:giant_spruce_taiga", 0.2),
                    Arrays.asList("minecraft:giant_spruce_taiga_hills", 0.2),

                    Arrays.asList("minecraft:savanna", 0.2),
                    Arrays.asList("minecraft:savanna_plateau", 0.2),
                    Arrays.asList("minecraft:shattered_savanna_plateau", 0.2),

                    Arrays.asList("minecraft:taiga", 0.2),
                    Arrays.asList("minecraft:taiga_hills", 0.2),
                    Arrays.asList("minecraft:taiga_mountains", 0.2),
                    Arrays.asList("minecraft:snowy_taiga", 0.2),
                    Arrays.asList("minecraft:snowy_taiga_hills", 0.2),
                    Arrays.asList("minecraft:snowy_taiga_mountains", 0.2),
                    Arrays.asList("minecraft:snowy_mountains", 0.2)
            ), it -> ((List<?>) it).get(0) instanceof String && ((List<?>) it).get(1) instanceof Number);
        BUILDER.pop();

        BUILDER.push("Biome Temperatures");
        biomeTemps = BUILDER
            .comment("Overrides existing biome temperatures & offsets")
            .defineList("Biome Temperatures", Arrays.asList(
                    // No default values
            ), it -> ((List<?>) it).get(0) instanceof String && ((List<?>) it).get(1) instanceof Number);
        BUILDER.pop();

        BUILDER.pop();

        /* Seasons config */
        if (ModList.get().isLoaded("sereneseasons") || ModList.get().isLoaded("betterweather"))
        {
            BUILDER.comment("Format: [season-start, season-mid, season-end]",
                           "Applied as an offset to the world's temperature")
                   .push("Season Temperatures");

            summerTemps = BUILDER
                    .defineList("Summer", Arrays.asList(
                            0.4, 0.6, 0.4
                    ), it -> it instanceof List && ((List<?>) it).get(0) instanceof Number
                            && ((List<?>) it).get(1) instanceof Number
                            && ((List<?>) it).get(2) instanceof Number);

            autumnTemps = BUILDER
                    .defineList("Autumn", Arrays.asList(
                            0.2, 0, -0.2
                    ), it -> it instanceof List && ((List<?>) it).get(0) instanceof Number
                            && ((List<?>) it).get(1) instanceof Number
                            && ((List<?>) it).get(2) instanceof Number);

            winterTemps = BUILDER
                    .defineList("Winter", Arrays.asList(
                            -0.4, -0.6, -0.4
                    ), it -> it instanceof List && ((List<?>) it).get(0) instanceof Number
                            && ((List<?>) it).get(1) instanceof Number
                            && ((List<?>) it).get(2) instanceof Number);

            springTemps = BUILDER
                    .defineList("Spring", Arrays.asList(
                            -0.2, 0, 0.2
                    ), it -> it instanceof List && ((List<?>) it).get(0) instanceof Number
                            && ((List<?>) it).get(1) instanceof Number
                            && ((List<?>) it).get(2) instanceof Number);

            BUILDER.pop();

        }

        SPEC = BUILDER.build();
    }

    public static void setup()
    {
        Path configPath = FMLPaths.CONFIGDIR.get();
        Path csConfigPath = Paths.get(configPath.toAbsolutePath().toString(), "coldsweat");

        // Create the config folder
        try
        {
            Files.createDirectory(csConfigPath);
        }
        catch (Exception e)
        {
            // Do nothing
        }

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, SPEC, "coldsweat/world_settings.toml");
    }

    public static WorldSettingsConfig getInstance()
    {
        return INSTANCE;
    }

    /*
     * Non-private values for use elsewhere
     */
    public List<? extends List<?>> biomeOffsets()
    {
        return biomeOffsets.get();
    }
    public List<? extends List<?>> biomeTemperatures()
    {
        return biomeTemps.get();
    }

    public List<? extends List<?>> dimensionOffsets()
    {
        return dimensionOffsets.get();
    }
    public List<? extends List<?>> dimensionTemperatures()
    {
        return dimensionTemps.get();
    }

    public Double[] summerTemps()
    {
        return summerTemps.get().stream().map(Number::doubleValue).toArray(Double[]::new);
    }
    public Double[] autumnTemps()
    {
        return autumnTemps.get().stream().map(Number::doubleValue).toArray(Double[]::new);
    }
    public Double[] winterTemps()
    {
        return winterTemps.get().stream().map(Number::doubleValue).toArray(Double[]::new);
    }
    public Double[] springTemps()
    {
        return springTemps.get().stream().map(Number::doubleValue).toArray(Double[]::new);
    }

    public void setBiomeOffsets(List<? extends List<?>> list) {
        biomeOffsets.set(list);
    }
    public void setDimensionOffsets(List<? extends List<?>> list) {
        dimensionOffsets.set(list);
    }
    public void setBiomeTemperatures(List<? extends List<?>> list) {
        biomeTemps.set(list);
    }
    public void setDimensionTemperatures(List<? extends List<?>> list) {
        dimensionTemps.set(list);
    }
}
