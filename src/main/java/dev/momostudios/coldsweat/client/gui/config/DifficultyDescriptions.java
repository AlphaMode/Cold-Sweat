package dev.momostudios.coldsweat.client.gui.config;

import dev.momostudios.coldsweat.api.util.Temperature;
import net.minecraft.ChatFormatting;
import dev.momostudios.coldsweat.util.math.CSMath;
import net.minecraft.network.chat.Component;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class DifficultyDescriptions
{
    private static final String bl = ChatFormatting.BLUE.toString();
    private static final String rd = ChatFormatting.RED.toString();
    private static final String ye = ChatFormatting.YELLOW.toString();
    private static final String rs = ChatFormatting.RESET.toString();
    private static final String bold = ChatFormatting.BOLD.toString();
    private static final String under = ChatFormatting.UNDERLINE.toString();

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
                getTrans("cold_sweat.config.difficulty.description.min_temp", getTemp(40, bl)),
                getTrans("cold_sweat.config.difficulty.description.max_temp", getTemp(120, rd)),
                getTrans("cold_sweat.config.difficulty.description.rate.decrease", ye+"50%"+rs),
                Component.translatable("cold_sweat.config.difficulty.description.world_temp_on", bold + under, rs).getString(),
                Component.translatable("cold_sweat.config.difficulty.description.scaling_off", bold + under, rs).getString(),
                Component.translatable("cold_sweat.config.difficulty.description.potions_on", bold + under, rs).getString()
        );
    }

    public static List<String> easyDescription()
    {
        return Arrays.asList(
                getTrans("cold_sweat.config.difficulty.description.min_temp", getTemp(45, bl)),
                getTrans("cold_sweat.config.difficulty.description.max_temp", getTemp(110, rd)),
                getTrans("cold_sweat.config.difficulty.description.rate.decrease", ye+"25%"+rs),
                Component.translatable("cold_sweat.config.difficulty.description.world_temp_on", bold + under, rs).getString(),
                Component.translatable("cold_sweat.config.difficulty.description.scaling_off", bold + under, rs).getString(),
                Component.translatable("cold_sweat.config.difficulty.description.potions_on", bold + under, rs).getString()
        );
    }

    public static List<String> normalDescription()
    {
        return Arrays.asList(
                getTrans("cold_sweat.config.difficulty.description.min_temp", getTemp(50, bl)),
                getTrans("cold_sweat.config.difficulty.description.max_temp", getTemp(100, rd)),
                Component.translatable("cold_sweat.config.difficulty.description.rate.normal").getString(),
                Component.translatable("cold_sweat.config.difficulty.description.world_temp_off", bold + under, rs).getString(),
                Component.translatable("cold_sweat.config.difficulty.description.scaling_on", bold + under, rs).getString(),
                Component.translatable("cold_sweat.config.difficulty.description.potions_off", bold + under, rs).getString()
        );
    }

    public static List<String> hardDescription()
    {
        return Arrays.asList(
                getTrans("cold_sweat.config.difficulty.description.min_temp", getTemp(60, bl)),
                getTrans("cold_sweat.config.difficulty.description.max_temp", getTemp(90, rd)),
                getTrans("cold_sweat.config.difficulty.description.rate.increase", ye+"50%"+rs),
                Component.translatable("cold_sweat.config.difficulty.description.world_temp_off", bold + under, rs).getString(),
                Component.translatable("cold_sweat.config.difficulty.description.scaling_on", bold + under, rs).getString(),
                Component.translatable("cold_sweat.config.difficulty.description.potions_off", bold + under, rs).getString()
        );
    }

    public static List<String> customDescription()
    {
        return Collections.singletonList(
                Component.translatable("cold_sweat.config.difficulty.description.custom").getString()
        );
    }

    private static String getTrans(String key, Object... args)
    {
        return Component.translatable(key, args).getString();
    }

    private static String getTemp(double temp, String color)
    {
        DecimalFormat df = new DecimalFormat("#.##");
        return color + temp + rs + " °F / " + color + df.format(CSMath.convertTemp(temp, Temperature.Units.F, Temperature.Units.C, true)) + rs + " °C";
    }
}
