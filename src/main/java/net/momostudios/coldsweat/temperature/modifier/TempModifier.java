package net.momostudios.coldsweat.temperature.modifier;

import net.minecraft.entity.player.PlayerEntity;
import net.momostudios.coldsweat.temperature.Temperature;

/**
 * This is the basis of all ways that a Temperature can be changed.
 * For example, biome temperature, time of day, depth, and waterskins are all {@link net.momostudios.coldsweat.temperature.modifier.TempModifier}
 *
 * It is up to you to apply and remove these modifiers manually.
 *
 * If you want a modifier to be instant (like the waterskin), you can remove it
 * from the player in {@code calculate()} via
 */
public class TempModifier
{
    /**
     * Determines what the provided temperature would be given the player it is being applied to
     * @param temp should usually represent the player's body temperature or ambient temperature
     */
    public double calculate(Temperature temp, PlayerEntity player)
    {
        return temp.get();
    }
}
