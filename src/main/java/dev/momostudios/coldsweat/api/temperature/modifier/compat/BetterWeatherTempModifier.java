package dev.momostudios.coldsweat.api.temperature.modifier.compat;

import corgitaco.betterweather.api.season.Season;
import dev.momostudios.coldsweat.util.config.ConfigSettings;
import net.minecraft.entity.player.PlayerEntity;
import dev.momostudios.coldsweat.api.temperature.Temperature;
import dev.momostudios.coldsweat.api.temperature.modifier.TempModifier;

import java.util.function.Function;

public class BetterWeatherTempModifier extends TempModifier
{
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
                {
                    switch (season.getPhase())
                    {
                        case START: seasonEffect = ConfigSettings.BW_AUTUMN_TEMPS.get()[0]; break;
                        case MID:   seasonEffect = ConfigSettings.BW_AUTUMN_TEMPS.get()[1]; break;
                        case END:   seasonEffect = ConfigSettings.BW_AUTUMN_TEMPS.get()[2]; break;
                    }
                    break;
                }

                case WINTER:
                {
                    switch (season.getPhase())
                    {
                        case START: seasonEffect = ConfigSettings.BW_WINTER_TEMPS.get()[0]; break;
                        case MID:   seasonEffect = ConfigSettings.BW_WINTER_TEMPS.get()[1]; break;
                        case END:   seasonEffect = ConfigSettings.BW_WINTER_TEMPS.get()[2]; break;
                    }
                    break;
                }

                case SPRING:
                {
                    switch (season.getPhase())
                    {
                        case START: seasonEffect = ConfigSettings.BW_SPRING_TEMPS.get()[0]; break;
                        case MID:   seasonEffect = ConfigSettings.BW_SPRING_TEMPS.get()[1]; break;
                        case END:   seasonEffect = ConfigSettings.BW_SPRING_TEMPS.get()[2]; break;
                    }
                    break;
                }

                case SUMMER:
                {
                    switch (season.getPhase())
                    {
                        case START: seasonEffect = ConfigSettings.BW_SUMMER_TEMPS.get()[0]; break;
                        case MID:   seasonEffect = ConfigSettings.BW_SUMMER_TEMPS.get()[1]; break;
                        case END:   seasonEffect = ConfigSettings.BW_SUMMER_TEMPS.get()[2]; break;
                    }
                    break;
                }
            }

            double finalSeasonEffect = seasonEffect;
            return temp -> temp.add(finalSeasonEffect);
        }
        else
        {   return temp -> temp;
        }
    }

    @Override
    public String getID() {
        return "betterweather:season";
    }
}
