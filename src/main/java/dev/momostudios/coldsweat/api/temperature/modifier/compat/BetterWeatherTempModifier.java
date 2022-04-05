package dev.momostudios.coldsweat.api.temperature.modifier.compat;

import corgitaco.betterweather.api.season.Season;
import net.minecraft.entity.player.PlayerEntity;
import dev.momostudios.coldsweat.api.temperature.Temperature;
import dev.momostudios.coldsweat.api.temperature.modifier.TempModifier;

public class BetterWeatherTempModifier extends TempModifier
{
    @Override
    public Temperature getResult(Temperature temp, PlayerEntity player)
    {
        Season season = Season.getSeason(player.world);
        if (season != null && player.world.getDimensionType().isNatural())
        {
            switch (season.getKey())
            {
                case AUTUMN:
                    switch (season.getPhase())
                    {
                        case START: return temp.add(0.2);
                        case MID:   return temp;
                        case END:   return temp.add(-0.2);
                    }

                case WINTER:
                    switch (season.getPhase())
                    {
                        case START: return temp.add(-0.4);
                        case MID:   return temp.add(-0.6);
                        case END:   return temp.add(-0.4);
                    }

                case SPRING:
                    switch (season.getPhase())
                    {
                        case START: return temp.add(-0.2);
                        case MID:   return temp;
                        case END:   return temp.add(0.2);
                    }

                case SUMMER:
                    switch (season.getPhase())
                    {
                        case START: return temp.add(0.4);
                        case MID:   return temp.add(0.6);
                        case END:   return temp.add(0.4);
                    }

                default:
                    return temp;
            }
        }
        else
            return temp;
    }

    @Override
    public String getID() {
        return "betterweather:season";
    }
}
