package dev.momostudios.coldsweat.api.temperature.modifier;

import net.minecraft.entity.player.PlayerEntity;
import dev.momostudios.coldsweat.api.temperature.Temperature;

public class MountTempModifier extends TempModifier
{
    public MountTempModifier() {
        this(1.0);
    }

    public MountTempModifier(double strength) {
        addArgument("strength", strength);
    }

    @Override
    public Temperature getResult(Temperature temp, PlayerEntity player)
    {
        return temp.multiply(1.0D - this.<Double>getArgument("strength"));
    }

    public String getID()
    {
        return "cold_sweat:insulated_mount";
    }
}