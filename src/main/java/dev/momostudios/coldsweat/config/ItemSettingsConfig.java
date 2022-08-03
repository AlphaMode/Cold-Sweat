package dev.momostudios.coldsweat.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

public final class ItemSettingsConfig
{
    private static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.ConfigValue<List<? extends List<?>>> boilerItems;
    private static final ForgeConfigSpec.ConfigValue<List<? extends List<?>>> iceBoxItems;
    private static final ForgeConfigSpec.ConfigValue<List<? extends List<?>>> hearthItems;
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> hellLampItems;
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> hellLampDimensions;
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> insulatingItems;
    private static final ForgeConfigSpec.ConfigValue<List<? extends List<?>>> insulatingArmor;
    private static final ForgeConfigSpec.ConfigValue<List<? extends List<?>>> temperatureFoods;
    private static final ForgeConfigSpec.IntValue waterskinStrength;

    public static final ItemSettingsConfig INSTANCE = new ItemSettingsConfig();

    static
    {
        /*
          Boiler Items
         */
        BUILDER.push("Fuel Items")
                .comment("Defines items that can use as fuel",
                         "Format: [[\"item-id-1\", amount-1], [\"item-id-2\", amount-2], ...etc]");
        boilerItems = BUILDER
                .defineList("Boiler", Arrays.asList
                                (
                                        Arrays.asList("minecraft:coal",         37),
                                        Arrays.asList("minecraft:charcoal",     37),
                                        Arrays.asList("minecraft:coal_block",   333),
                                        Arrays.asList("minecraft:magma_block",  333),
                                        Arrays.asList("minecraft:lava_bucket",  1000)
                                ),
                        it -> it instanceof List && ((List<?>) it).get(0) instanceof String && ((List<?>) it).get(1) instanceof Number);

        iceBoxItems = BUILDER
                .defineList("Icebox", Arrays.asList
                                (
                                        Arrays.asList("minecraft:snowball",     37),
                                        Arrays.asList("minecraft:clay",         37),
                                        Arrays.asList("minecraft:snow_block",   333),
                                        Arrays.asList("minecraft:water_bucket", 333),
                                        Arrays.asList("minecraft:ice",          333),
                                        Arrays.asList("minecraft:packed_ice",   1000)
                                ),
                        it -> it instanceof List && ((List<?>) it).get(0) instanceof String && ((List<?>) it).get(1) instanceof Number);

        hearthItems = BUILDER
                .comment("Negative values indicate cold fuel")
                .defineList("Hearth", Arrays.asList
                                (
                                        // Hot
                                        Arrays.asList("minecraft:coal",         37),
                                        Arrays.asList("minecraft:charcoal",     37),
                                        Arrays.asList("minecraft:coal_block",   333),
                                        Arrays.asList("minecraft:magma_block",  333),
                                        Arrays.asList("minecraft:lava_bucket",  1000),

                                        // Cold
                                        Arrays.asList("minecraft:snowball",     -37),
                                        Arrays.asList("minecraft:clay",         -37),
                                        Arrays.asList("minecraft:snow_block",   -333),
                                        Arrays.asList("minecraft:water_bucket", -333),
                                        Arrays.asList("minecraft:ice",          -333),
                                        Arrays.asList("minecraft:packed_ice",   -1000)
                                ),
                        it -> it instanceof List && ((List<?>) it).get(0) instanceof String && ((List<?>) it).get(1) instanceof Number);
        BUILDER.pop();

        /*
          Hellspring Lamp Items
         */
        BUILDER.push("Hellspring Lamp");
        hellLampItems = BUILDER
                .comment("Defines the items that the Hellspring Lamp can use as fuel and their values",
                        "Format: [\"item-id-1\", \"item-id-2\", ...etc]")
                .defineList("Fuel Items", Arrays.asList
                                (
                                        "minecraft:warped_stem",
                                        "minecraft:warped_hyphae",
                                        "minecraft:stripped_warped_stem",
                                        "minecraft:stripped_warped_hyphae",
                                        "minecraft:crimson_stem",
                                        "minecraft:crimson_hyphae",
                                        "minecraft:stripped_crimson_stem",
                                        "minecraft:stripped_crimson_hyphae"
                                ),
                        it -> it instanceof String);

        hellLampDimensions = BUILDER
                .comment("Defines the dimensions that the Hellspring Lamp can be used in",
                        "Format: [\"dimension-id-1\", \"dimension-id-2\", ...etc]")
                .defineList("Valid Dimensions", Arrays.asList
                                (
                                        "minecraft:the_nether"
                                ),
                        it -> it instanceof String);
        BUILDER.pop();

        /*
         Insulator Items
         */
        BUILDER.push("Insulation");
        insulatingItems = BUILDER
                .comment("Defines the items that can be used for insulating armor in the Sewing Table",
                        "Format: [[\"item-id-1\"], [\"item-id-2\"], ...etc]")
                .defineList("Insulation Ingredients", Arrays.asList
                                (
                                        "minecraft:leather_helmet",
                                        "minecraft:leather_chestplate",
                                        "minecraft:leather_leggings",
                                        "minecraft:leather_boots"
                                ),
                        it -> it instanceof String);

        insulatingArmor = BUILDER
                .comment("Defines the items that provide insulation when worn",
                        "Format: [[\"item-id-1\", amount-1], [\"item-id-2\", amount-2], ...etc]")
                .defineList("Insulated Armor", Arrays.asList
                                (
                                        Arrays.asList("minecraft:leather_helmet", 4),
                                        Arrays.asList("minecraft:leather_chestplate", 7),
                                        Arrays.asList("minecraft:leather_leggings", 5),
                                        Arrays.asList("minecraft:leather_boots", 4)
                                ),
                        it -> it instanceof List && ((List<?>) it).get(0) instanceof String && ((List<?>) it).get(1) instanceof Number);
        BUILDER.pop();

        /*
         Temperature-Affecting Foods
         */
        BUILDER.push("Consumables");
        temperatureFoods = BUILDER
                .comment("Defines items that affect the player's temperature when consumed",
                         "Format: [[item-id-1, amount-1], [item-id-2, amount-2], ...etc]",
                         "Negative values are cold foods, positive values are hot foods")
                .defineList("Temperature-Affecting Foods", Arrays.asList
                                 (
                                         // nothing here
                                 ),
                        it -> it instanceof List && ((List<?>) it).get(0) instanceof String && ((List<?>) it).get(1) instanceof Number);
        waterskinStrength = BUILDER
                .comment("Defines the amount a player's body temperature will change by when using a waterskin")
                .defineInRange("Waterskin Strength", 50, 0, Integer.MAX_VALUE);
        BUILDER.pop();

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
        catch (Exception e) {}

        ModLoadingContext.get().registerConfig(net.minecraftforge.fml.config.ModConfig.Type.COMMON, SPEC, "coldsweat/item_settings.toml");
    }

    public static ItemSettingsConfig getInstance()
    {
        return new ItemSettingsConfig();
    }

    public List<? extends List<?>> boilerItems()
    {
        return boilerItems.get();
    }

    public List<? extends List<?>> iceboxItems()
    {
        return iceBoxItems.get();
    }

    public List<? extends List<?>> hearthItems()
    {
        return hearthItems.get();
    }

    public List<? extends String> insulatingItems()
    {
        return insulatingItems.get();
    }

    public List<? extends List<?>> insulatingArmor()
    {
        return insulatingArmor.get();
    }

    public List<? extends String> soulLampItems()
    {
        return hellLampItems.get();
    }

    public List<? extends List<?>> temperatureFoods()
    {
        return temperatureFoods.get();
    }

    public List<? extends String> soulLampDimensions()
    {
        return hellLampDimensions.get();
    }

    public int waterskinStrength()
    {
        return waterskinStrength.get();
    }
}
