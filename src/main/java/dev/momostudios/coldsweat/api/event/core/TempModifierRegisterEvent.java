package dev.momostudios.coldsweat.api.event.core;

import dev.momostudios.coldsweat.api.event.common.TempModifierEvent;
import dev.momostudios.coldsweat.api.registry.TempModifierRegistry;
import dev.momostudios.coldsweat.api.temperature.modifier.TempModifier;
import net.minecraftforge.common.MinecraftForge;

/**
 * Fired when the {@link TempModifier} registry is being built ({@link TempModifierRegistry}). <br>
 * The event is fired during {@link net.minecraftforge.event.world.WorldEvent.Load}. <br>
 * <br>
 * Use {@code TempModifierRegistry.flush()} if calling manually to prevent duplicates. <br>
 * (You probably shouldn't ever do that anyway) <br>
 * <br>
 * This event is not {@link net.minecraftforge.eventbus.api.Cancelable}. <br>
 * <br>
 * This event is fired on the {@link MinecraftForge#EVENT_BUS}.
 */
public class TempModifierRegisterEvent extends TempModifierEvent
{
    /**
     * Adds a new {@link TempModifier} to the registry.
     *
     * @param modifier the {@link TempModifier} to add.
     * @throws InstantiationException If the TempModifier has no default constructor.
     * @throws IllegalAccessException If the default constructor is not accessible.
     */
    public void register(TempModifier modifier)
    {
        TempModifierRegistry.register(modifier);
    }
}
