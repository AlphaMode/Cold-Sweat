package dev.momostudios.coldsweat.util.config;

import com.mojang.datafixers.util.Pair;
import dev.momostudios.coldsweat.config.ColdSweatConfig;
import dev.momostudios.coldsweat.config.ItemSettingsConfig;
import dev.momostudios.coldsweat.config.WorldSettingsConfig;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.ModList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConfigSettings
{
    // Settings saved during runtime for easy access
    public static ConfigValue<Map<ResourceLocation, Pair<Double, Double>>> BIOME_TEMPS;
    public static ConfigValue<Map<ResourceLocation, Pair<Double, Double>>> BIOME_OFFSETS;
    public static ConfigValue<Map<ResourceLocation, Double>> DIMENSION_TEMPS;
    public static ConfigValue<Map<ResourceLocation, Double>> DIMENSION_OFFSETS;

    public static ConfigValue<Double[]> BW_SUMMER_TEMPS;
    public static ConfigValue<Double[]> BW_AUTUMN_TEMPS;
    public static ConfigValue<Double[]> BW_WINTER_TEMPS;
    public static ConfigValue<Double[]> BW_SPRING_TEMPS;

    public static ConfigValue<Double[]> SS_SUMMER_TEMPS;
    public static ConfigValue<Double[]> SS_AUTUMN_TEMPS;
    public static ConfigValue<Double[]> SS_WINTER_TEMPS;
    public static ConfigValue<Double[]> SS_SPRING_TEMPS;

    public static ConfigValue<List<Item>> INSULATING_ITEMS;
    public static ConfigValue<Map<Item, Double>> INSULATING_ARMORS;

    public static ConfigValue<Map<Item, Double>> VALID_FOODS;

    public static ConfigValue<Integer> WATERSKIN_STRENGTH;

    public static ConfigValue<List<Item>> LAMP_FUEL_ITEMS;

    public static ConfigValue<List<String>> LAMP_DIMENSIONS;

    public static ConfigValue<Map<Item, Double>> BOILER_FUEL_ITEMS;
    public static ConfigValue<Map<Item, Double>> ICEBOX_FUEL_ITEMS;
    public static ConfigValue<Map<Item, Double>> HEARTH_FUEL_ITEMS;

    // Makes the settings instantiation collapsible & easier to read
    static
    {
        BIOME_TEMPS = ConfigValue.of(() ->
                ConfigHelper.getBiomeTemps(WorldSettingsConfig.getInstance().biomeTemperatures(), true));

        BIOME_OFFSETS = ConfigValue.of(() ->
                ConfigHelper.getBiomeTemps(WorldSettingsConfig.getInstance().biomeOffsets(), false));

        DIMENSION_TEMPS = ConfigValue.of(() ->
        {
            Map<ResourceLocation, Double> map = new HashMap<>();
            for (List<?> entry : WorldSettingsConfig.getInstance().dimensionTemperatures())
            {
                map.put(new ResourceLocation((String) entry.get(0)), (Double) entry.get(1));
            }
            return map;
        });

        DIMENSION_OFFSETS = ConfigValue.of(() ->
        {
            Map<ResourceLocation, Double> map = new HashMap<>();
            for (List<?> entry : WorldSettingsConfig.getInstance().dimensionOffsets())
            {
                map.put(new ResourceLocation((String) entry.get(0)), (Double) entry.get(1));
            }
            return map;
        });

        BOILER_FUEL_ITEMS = ConfigValue.of(() -> ConfigHelper.getItemsWithValues(ItemSettingsConfig.getInstance().boilerItems()));
        ICEBOX_FUEL_ITEMS = ConfigValue.of(() -> ConfigHelper.getItemsWithValues(ItemSettingsConfig.getInstance().iceboxItems()));
        HEARTH_FUEL_ITEMS = ConfigValue.of(() -> ConfigHelper.getItemsWithValues(ItemSettingsConfig.getInstance().hearthItems()));

        INSULATING_ITEMS = ConfigValue.of(() ->
        {
            List<Item> list = new ArrayList<>();
            for (String itemID : ItemSettingsConfig.getInstance().insulatingItems())
            {
                list.addAll(ConfigHelper.getItems(itemID));
            }
            return list;
        });

        INSULATING_ARMORS = ConfigValue.of(() ->
                ConfigHelper.getItemsWithValues(ItemSettingsConfig.getInstance().insulatingArmor()));

        VALID_FOODS = ConfigValue.of(() -> ConfigHelper.getItemsWithValues(ItemSettingsConfig.getInstance().temperatureFoods()));

        WATERSKIN_STRENGTH = ConfigValue.of(() -> ItemSettingsConfig.getInstance().waterskinStrength());

        LAMP_FUEL_ITEMS = ConfigValue.of(() ->
        {
            List<Item> list = new ArrayList<>();
            for (String itemID : ItemSettingsConfig.getInstance().soulLampItems())
            {
                list.addAll(ConfigHelper.getItems(itemID));
            }
            return list;
        });

        LAMP_DIMENSIONS = ConfigValue.of(() -> new ArrayList<>(ItemSettingsConfig.getInstance().soulLampDimensions()));

        if (ModList.get().isLoaded("sereneseasons"))
        {
            SS_SUMMER_TEMPS = ConfigValue.of(() -> WorldSettingsConfig.getInstance().summerTemps());
            SS_AUTUMN_TEMPS = ConfigValue.of(() -> WorldSettingsConfig.getInstance().autumnTemps());
            SS_WINTER_TEMPS = ConfigValue.of(() -> WorldSettingsConfig.getInstance().winterTemps());
            SS_SPRING_TEMPS = ConfigValue.of(() -> WorldSettingsConfig.getInstance().springTemps());
        }

        if (ModList.get().isLoaded("betterweather"))
        {
            BW_SUMMER_TEMPS = ConfigValue.of(() -> WorldSettingsConfig.getInstance().summerTemps());
            BW_AUTUMN_TEMPS = ConfigValue.of(() -> WorldSettingsConfig.getInstance().autumnTemps());
            BW_WINTER_TEMPS = ConfigValue.of(() -> WorldSettingsConfig.getInstance().winterTemps());
            BW_SPRING_TEMPS = ConfigValue.of(() -> WorldSettingsConfig.getInstance().springTemps());
        }
    }

    public int difficulty;
    public double maxTemp;
    public double minTemp;
    public double rate;
    public boolean fireRes;
    public boolean iceRes;
    public boolean damageScaling;
    public boolean requireThermometer;
    public int graceLength;
    public boolean graceEnabled;

    private static ConfigSettings INSTANCE = new ConfigSettings();

    public ConfigSettings() {}

    public static ConfigSettings getInstance()
    {
        return INSTANCE;
    }

    public static void setInstance(ConfigSettings instance)
    {
        INSTANCE = instance;
    }

    public ConfigSettings(ColdSweatConfig config)
    {
        readValues(config);
    }

    public void readValues(ColdSweatConfig config)
    {
        difficulty = config.getDifficulty();
        maxTemp = config.getMaxTempHabitable();
        minTemp = config.getMinTempHabitable();
        rate = config.getRateMultiplier();
        fireRes = config.isFireResistanceEnabled();
        iceRes = config.isIceResistanceEnabled();
        damageScaling = config.doDamageScaling();
        requireThermometer = config.isWorldTempShowing();
        graceLength = config.getGracePeriodLength();
        graceEnabled = config.isGracePeriodEnabled();
    }
}
