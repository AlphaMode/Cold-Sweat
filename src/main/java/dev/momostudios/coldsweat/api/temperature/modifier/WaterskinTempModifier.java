package dev.momostudios.coldsweat.api.temperature.modifier;

import net.minecraft.entity.player.PlayerEntity;
import dev.momostudios.coldsweat.api.temperature.Temperature;

public class WaterskinTempModifier extends TempModifier
{
    public WaterskinTempModifier()
    {
        addArgument("temperature", 0);
    }

    public WaterskinTempModifier(double temp)
    {
        addArgument("temperature", temp);
    }

    @Override
    public Temperature getResult(Temperature temp, PlayerEntity player)
    {
        return temp.add(this.<Double>getArgument("temperature"));
    }

    public String getID()
    {
        return "cold_sweat:waterskin";
    }
}