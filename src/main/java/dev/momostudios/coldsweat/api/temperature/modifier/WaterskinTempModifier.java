package dev.momostudios.coldsweat.api.temperature.modifier;

import net.minecraft.entity.player.PlayerEntity;
import dev.momostudios.coldsweat.api.temperature.Temperature;

import java.util.function.Function;

public class WaterskinTempModifier extends TempModifier
{
    public WaterskinTempModifier()
    {
        this(0.0);
    }

    public WaterskinTempModifier(double temp)
    {
        addArgument("temperature", temp);
    }

    @Override
    public Function<Temperature, Temperature> calculate(PlayerEntity player)
    {
        return temp -> temp.add(this.<Double>getArgument("temperature"));
    }

    public String getID()
    {
        return "cold_sweat:waterskin";
    }
}