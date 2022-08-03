package dev.momostudios.coldsweat.api.temperature.modifier.compat;

import corgitaco.betterweather.api.season.Season;
import dev.momostudios.coldsweat.config.WorldSettingsConfig;
import dev.momostudios.coldsweat.util.config.LoadedValue;
import net.minecraft.entity.player.PlayerEntity;
import dev.momostudios.coldsweat.api.temperature.Temperature;
import dev.momostudios.coldsweat.api.temperature.modifier.TempModifier;

import java.util.function.Function;

public class BetterWeatherTempModifier extends TempModifier
{
    static LoadedValue<Double[]> SUMMER_TEMPS = LoadedValue.of(() -> WorldSettingsConfig.getInstance().summerTemps());
    static LoadedValue<Double[]> AUTUMN_TEMPS = LoadedValue.of(() -> WorldSettingsConfig.getInstance().autumnTemps());
    static LoadedValue<Double[]> WINTER_TEMPS = LoadedValue.of(() -> WorldSettingsConfig.getInstance().winterTemps());
    static LoadedValue<Double[]> SPRING_TEMPS = LoadedValue.of(() -> WorldSettingsConfig.getInstance().springTemps());

    @Override
    public Function<Temperature, Temperature> calculate(PlayerEntity player)
    {
        Season season = Season.getSeason(player.world);
        if (season != null && player.world.getDimensionType().isNatural())
        {
            double seasonEffect = 0;
            switch (season.getKey())
            {
                case AUTUMN:
                    switch (season.getPhase())
                    {
                        case START: seasonEffect = AUTUMN_TEMPS.get()[0]; break;
                        case MID:   seasonEffect = AUTUMN_TEMPS.get()[1]; break;
                        case END:   seasonEffect = AUTUMN_TEMPS.get()[2]; break;
                    }

                case WINTER:
                    switch (season.getPhase())
                    {
                        case START: seasonEffect = WINTER_TEMPS.get()[0]; break;
                        case MID:   seasonEffect = WINTER_TEMPS.get()[1]; break;
                        case END:   seasonEffect = WINTER_TEMPS.get()[2]; break;
                    }

                case SPRING:
                    switch (season.getPhase())
                    {
                        case START: seasonEffect = SPRING_TEMPS.get()[0]; break;
                        case MID:   seasonEffect = SPRING_TEMPS.get()[1]; break;
                        case END:   seasonEffect = SPRING_TEMPS.get()[2]; break;
                    }

                case SUMMER:
                    switch (season.getPhase())
                    {
                        case START: seasonEffect = SUMMER_TEMPS.get()[0]; break;
                        case MID:   seasonEffect = SUMMER_TEMPS.get()[1]; break;
                        case END:   seasonEffect = SUMMER_TEMPS.get()[2]; break;
                    }
            }

            double finalSeasonEffect = seasonEffect;
            return temp -> temp.add(finalSeasonEffect);
        }
        else
        {
            return temp -> temp;
        }
    }

    @Override
    public String getID() {
        return "betterweather:season";
    }
}
