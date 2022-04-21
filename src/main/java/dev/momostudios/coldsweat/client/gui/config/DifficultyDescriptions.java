package dev.momostudios.coldsweat.client.gui.config;

import dev.momostudios.coldsweat.api.temperature.Temperature;
import dev.momostudios.coldsweat.util.math.CSMath;
import net.minecraft.util.text.TranslationTextComponent;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class DifficultyDescriptions
{
    private static final String bl    = "§9";
    private static final String rd    = "§c";
    private static final String ye    = "§e";
    private static final String rs    = "§r";
    private static final String bold  = "§l";
    private static final String under = "§n";

    public static List<String> getListFor(int difficulty)
    {
        switch (difficulty)
        {
            case 0:
                return superEasyDescription();
            case 1:
                return easyDescription();
            case 2:
                return normalDescription();
            case 3:
                return hardDescription();
            default:
                return customDescription();
        }
    }

    public static List<String> superEasyDescription()
    {
        return Arrays.asList(
                getText("cold_sweat.config.difficulty.description.min_temp", getTemp(40, bl)),
                getText("cold_sweat.config.difficulty.description.max_temp", getTemp(120, rd)),
                getText("cold_sweat.config.difficulty.description.rate.decrease", ye+"50%"+rs),
                getText("cold_sweat.config.difficulty.description.world_temp_on", bold + under, rs),
                getText("cold_sweat.config.difficulty.description.scaling_off", bold + under, rs),
                getText("cold_sweat.config.difficulty.description.potions_on", bold + under, rs)
        );
    }

    public static List<String> easyDescription()
    {
        return Arrays.asList(
                getText("cold_sweat.config.difficulty.description.min_temp", getTemp(45, bl)),
                getText("cold_sweat.config.difficulty.description.max_temp", getTemp(110, rd)),
                getText("cold_sweat.config.difficulty.description.rate.decrease", ye+"25%"+rs),
                getText("cold_sweat.config.difficulty.description.world_temp_on", bold + under, rs),
                getText("cold_sweat.config.difficulty.description.scaling_off", bold + under, rs),
                getText("cold_sweat.config.difficulty.description.potions_on", bold + under, rs)
        );
    }

    public static List<String> normalDescription()
    {
        return Arrays.asList(
                getText("cold_sweat.config.difficulty.description.min_temp", getTemp(50, bl)),
                getText("cold_sweat.config.difficulty.description.max_temp", getTemp(100, rd)),
                getText("cold_sweat.config.difficulty.description.rate.normal"),
                getText("cold_sweat.config.difficulty.description.world_temp_off", bold + under, rs),
                getText("cold_sweat.config.difficulty.description.scaling_on", bold + under, rs),
                getText("cold_sweat.config.difficulty.description.potions_off", bold + under, rs)
        );
    }

    public static List<String> hardDescription()
    {
        return Arrays.asList(
                getText("cold_sweat.config.difficulty.description.min_temp", getTemp(60, bl)),
                getText("cold_sweat.config.difficulty.description.max_temp", getTemp(90, rd)),
                getText("cold_sweat.config.difficulty.description.rate.increase", ye+"50%"+rs),
                getText("cold_sweat.config.difficulty.description.world_temp_off", bold + under, rs),
                getText("cold_sweat.config.difficulty.description.scaling_on", bold + under, rs),
                getText("cold_sweat.config.difficulty.description.potions_off", bold + under, rs)
        );
    }

    public static List<String> customDescription()
    {
        return Collections.singletonList(
                new TranslationTextComponent("cold_sweat.config.difficulty.description.custom").getString()
        );
    }

    private static String getText(String key, Object... args)
    {
        return new TranslationTextComponent(key, args).getString();
    }

    private static String getTemp(double temp, String color)
    {
        DecimalFormat df = new DecimalFormat("#.##");
        return color + temp + rs + " °F / " + color + df.format(CSMath.convertUnits(temp, Temperature.Units.F, Temperature.Units.C, true)) + rs + " °C";
    }
}
