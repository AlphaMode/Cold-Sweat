package dev.momostudios.coldsweat.api.temperature.modifier;

import dev.momostudios.coldsweat.api.temperature.Temperature;
import net.minecraft.entity.player.PlayerEntity;

public class FoodTempModifier extends TempModifier
{
    public FoodTempModifier()
    {
        this(0);
    }

    public FoodTempModifier(double effect)
    {
        addArgument("effect", effect);
    }

    @Override
    public Temperature getResult(Temperature temp, PlayerEntity player)
    {
        return temp.add(this.<Double>getArgument("effect"));
    }

    @Override
    public String getID()
    {
        return "cold_sweat:food";
    }
}
