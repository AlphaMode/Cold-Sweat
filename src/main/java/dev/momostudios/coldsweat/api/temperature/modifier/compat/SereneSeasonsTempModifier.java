package dev.momostudios.coldsweat.api.temperature.modifier.compat;

import net.minecraft.entity.player.PlayerEntity;
import dev.momostudios.coldsweat.api.temperature.Temperature;
import dev.momostudios.coldsweat.api.temperature.modifier.TempModifier;
import sereneseasons.api.season.SeasonHelper;

public class SereneSeasonsTempModifier extends TempModifier
{
    @Override
    public Temperature getResult(Temperature temp, PlayerEntity player)
    {
        if (player.world.getDimensionType().isNatural())
            switch (SeasonHelper.getSeasonState(player.world).getSubSeason())
            {
                case EARLY_AUTUMN: return temp.add(0.2);
                case MID_AUTUMN:   return temp;
                case LATE_AUTUMN:  return temp.add(-0.2);

                case EARLY_WINTER: return temp.add(-0.4);
                case MID_WINTER:   return temp.add(-0.6);
                case LATE_WINTER:  return temp.add(-0.4);

                case EARLY_SPRING: return temp.add(-0.2);
                case MID_SPRING:   return temp;
                case LATE_SPRING:  return temp.add(0.2);

                case EARLY_SUMMER: return temp.add(0.4f);
                case MID_SUMMER:   return temp.add(0.6f);
                case LATE_SUMMER:  return temp.add(0.4f);

                default: return temp;
            }
        else
            return temp;
    }

    @Override
    public String getID() {
        return "sereneseasons:season";
    }
}
