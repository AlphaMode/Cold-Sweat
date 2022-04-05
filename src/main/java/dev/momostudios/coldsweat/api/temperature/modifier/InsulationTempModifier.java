package dev.momostudios.coldsweat.api.temperature.modifier;

import net.minecraft.entity.player.PlayerEntity;
import dev.momostudios.coldsweat.api.temperature.Temperature;

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
    public Temperature getResult(Temperature temp, PlayerEntity player)
    {
        return temp.divide(Math.max(1d, this.<Integer>getArgument("warmth") / 10d));
    }

    public String getID()
    {
        return "cold_sweat:insulated_armor";
    }
}