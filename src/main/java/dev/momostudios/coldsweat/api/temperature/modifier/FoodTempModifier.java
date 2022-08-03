package dev.momostudios.coldsweat.api.temperature.modifier;

import dev.momostudios.coldsweat.api.temperature.Temperature;
import net.minecraft.entity.player.PlayerEntity;

import java.util.function.Function;

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
    public Function<Temperature, Temperature> calculate(PlayerEntity player)
    {
        return temp -> temp.add(this.<Double>getArgument("effect"));
    }

    @Override
    public String getID()
    {
        return "cold_sweat:food";
    }
}
