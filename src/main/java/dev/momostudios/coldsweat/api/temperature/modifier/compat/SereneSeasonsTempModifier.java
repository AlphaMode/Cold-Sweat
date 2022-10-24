package dev.momostudios.coldsweat.api.temperature.modifier.compat;

import dev.momostudios.coldsweat.util.config.ConfigSettings;
import dev.momostudios.coldsweat.util.math.CSMath;
import net.minecraft.entity.player.PlayerEntity;
import dev.momostudios.coldsweat.api.temperature.Temperature;
import dev.momostudios.coldsweat.api.temperature.modifier.TempModifier;
import sereneseasons.api.season.ISeasonState;
import sereneseasons.api.season.SeasonHelper;

import java.util.function.Function;

public class SereneSeasonsTempModifier extends TempModifier
{
    @Override
    public Function<Temperature, Temperature> calculate(PlayerEntity player)
    {
        if (player.world.getDimensionType().isNatural())
        {
            ISeasonState season = SeasonHelper.getSeasonState(player.world);
            double startValue;
            double endValue;
            switch (season.getSubSeason())
            {
                case EARLY_AUTUMN : { startValue = ConfigSettings.SS_AUTUMN_TEMPS.get()[0]; endValue = ConfigSettings.SS_AUTUMN_TEMPS.get()[1]; break; }
                case MID_AUTUMN   : { startValue = ConfigSettings.SS_AUTUMN_TEMPS.get()[1]; endValue = ConfigSettings.SS_AUTUMN_TEMPS.get()[2]; break; }
                case LATE_AUTUMN  : { startValue = ConfigSettings.SS_AUTUMN_TEMPS.get()[2]; endValue = ConfigSettings.SS_WINTER_TEMPS.get()[0]; break; }

                case EARLY_WINTER : { startValue = ConfigSettings.SS_WINTER_TEMPS.get()[0]; endValue = ConfigSettings.SS_WINTER_TEMPS.get()[1]; break; }
                case MID_WINTER   : { startValue = ConfigSettings.SS_WINTER_TEMPS.get()[1]; endValue = ConfigSettings.SS_WINTER_TEMPS.get()[2]; break; }
                case LATE_WINTER  : { startValue = ConfigSettings.SS_WINTER_TEMPS.get()[2]; endValue = ConfigSettings.SS_SPRING_TEMPS.get()[0]; break; }

                case EARLY_SPRING : { startValue = ConfigSettings.SS_SPRING_TEMPS.get()[0]; endValue = ConfigSettings.SS_SPRING_TEMPS.get()[1]; break; }
                case MID_SPRING   : { startValue = ConfigSettings.SS_SPRING_TEMPS.get()[1]; endValue = ConfigSettings.SS_SPRING_TEMPS.get()[2]; break; }
                case LATE_SPRING  : { startValue = ConfigSettings.SS_SPRING_TEMPS.get()[2]; endValue = ConfigSettings.SS_SUMMER_TEMPS.get()[0]; break; }

                case EARLY_SUMMER : { startValue = ConfigSettings.SS_SUMMER_TEMPS.get()[0]; endValue = ConfigSettings.SS_SUMMER_TEMPS.get()[1]; break; }
                case MID_SUMMER   : { startValue = ConfigSettings.SS_SUMMER_TEMPS.get()[1]; endValue = ConfigSettings.SS_SUMMER_TEMPS.get()[2]; break; }
                case LATE_SUMMER  : { startValue = ConfigSettings.SS_SUMMER_TEMPS.get()[2]; endValue = ConfigSettings.SS_AUTUMN_TEMPS.get()[0]; break; }
                default : return temp -> temp;
            }
            double finalStartValue = startValue;
            double finalEndValue = endValue;
            return temp -> temp.add(CSMath.blend(finalStartValue, finalEndValue, season.getDay() % (season.getSubSeasonDuration() / season.getDayDuration()), 0, 8));
        }
        else
            return temp -> temp;
    }

    @Override
    public String getID() {
        return "sereneseasons:season";
    }
}
