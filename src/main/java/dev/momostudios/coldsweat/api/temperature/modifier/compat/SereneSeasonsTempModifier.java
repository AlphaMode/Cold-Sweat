package dev.momostudios.coldsweat.api.temperature.modifier.compat;

import dev.momostudios.coldsweat.config.WorldSettingsConfig;
import dev.momostudios.coldsweat.util.config.LoadedValue;
import net.minecraft.entity.player.PlayerEntity;
import dev.momostudios.coldsweat.api.temperature.Temperature;
import dev.momostudios.coldsweat.api.temperature.modifier.TempModifier;
import sereneseasons.api.season.SeasonHelper;

import java.util.function.Function;

public class SereneSeasonsTempModifier extends TempModifier
{
    static LoadedValue<Double[]> SUMMER_TEMPS = LoadedValue.of(() -> WorldSettingsConfig.getInstance().summerTemps());
    static LoadedValue<Double[]> AUTUMN_TEMPS = LoadedValue.of(() -> WorldSettingsConfig.getInstance().autumnTemps());
    static LoadedValue<Double[]> WINTER_TEMPS = LoadedValue.of(() -> WorldSettingsConfig.getInstance().winterTemps());
    static LoadedValue<Double[]> SPRING_TEMPS = LoadedValue.of(() -> WorldSettingsConfig.getInstance().springTemps());

    @Override
    public Function<Temperature, Temperature> calculate(PlayerEntity player)
    {
        if (player.world.getDimensionType().isNatural())
        {
            double value;
            switch (SeasonHelper.getSeasonState(player.world).getSubSeason())
            {
                case EARLY_AUTUMN : value = AUTUMN_TEMPS.get()[0]; break;
                case MID_AUTUMN   : value = AUTUMN_TEMPS.get()[1]; break;
                case LATE_AUTUMN  : value = AUTUMN_TEMPS.get()[2]; break;

                case EARLY_WINTER : value = WINTER_TEMPS.get()[0]; break;
                case MID_WINTER   : value = WINTER_TEMPS.get()[1]; break;
                case LATE_WINTER  : value = WINTER_TEMPS.get()[2]; break;

                case EARLY_SPRING : value = SPRING_TEMPS.get()[0]; break;
                case MID_SPRING   : value = SPRING_TEMPS.get()[1]; break;
                case LATE_SPRING  : value = SPRING_TEMPS.get()[2]; break;

                case EARLY_SUMMER : value = SUMMER_TEMPS.get()[0]; break;
                case MID_SUMMER   : value = SUMMER_TEMPS.get()[1]; break;
                case LATE_SUMMER  : value = SUMMER_TEMPS.get()[2]; break;

                default: return temp -> temp;
            }
            return temp -> temp.add(value);
        }
        else
            return temp -> temp;
    }

    @Override
    public String getID() {
        return "sereneseasons:season";
    }
}
