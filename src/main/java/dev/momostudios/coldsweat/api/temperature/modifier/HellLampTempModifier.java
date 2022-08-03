package dev.momostudios.coldsweat.api.temperature.modifier;

import dev.momostudios.coldsweat.util.config.ConfigCache;
import net.minecraft.entity.player.PlayerEntity;
import dev.momostudios.coldsweat.api.temperature.Temperature;

import java.util.function.Function;

public class HellLampTempModifier extends TempModifier
{
    @Override
    public Function<Temperature, Temperature> calculate(PlayerEntity player)
    {
        double almostMox = ConfigCache.getInstance().maxTemp * 0.99;
        return temp ->
        {
            if (temp.get() < almostMox) return temp;

            return temp.multiply(Math.max(0.4, almostMox / temp.get()));
        };
    }

    @Override
    public String getID() {
        return "cold_sweat:hellspring_lamp";
    }
}
