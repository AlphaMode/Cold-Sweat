package dev.momostudios.coldsweat.api.temperature.modifier;

import net.minecraft.entity.player.PlayerEntity;
import dev.momostudios.coldsweat.api.temperature.Temperature;

public class HellLampTempModifier extends TempModifier
{
    @Override
    public Temperature getResult(Temperature temp, PlayerEntity player)
    {
        return temp.add(0.8);
    }

    @Override
    public String getID() {
        return "cold_sweat:hellspring_lamp";
    }
}
