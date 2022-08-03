package dev.momostudios.coldsweat.api.temperature.modifier;

import net.minecraft.entity.player.PlayerEntity;
import dev.momostudios.coldsweat.api.temperature.Temperature;

import java.util.function.Function;

public class InsulationTempModifier extends TempModifier
{
    public InsulationTempModifier()
    {
        addArgument("warmth", 0);
    }

    public InsulationTempModifier(int amount)
    {
        addArgument("warmth", amount);
    }

    @Override
    public Function<Temperature, Temperature> calculate(PlayerEntity player)
    {
        return temp -> temp.divide(Math.max(1d, this.<Integer>getArgument("warmth") / 10d));
    }

    public String getID()
    {
        return "cold_sweat:insulated_armor";
    }
}