package dev.momostudios.coldsweat.api.event.core;

import dev.momostudios.coldsweat.api.temperature.block_effect.BlockEffect;
import dev.momostudios.coldsweat.api.registry.BlockEffectRegistry;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.Event;

/**
 * Fired when the {@link BlockEffect} registry is being built ({@link BlockEffectRegistry}). <br>
 * The event is fired during {@link net.minecraftforge.event.world.WorldEvent.Load}. <br>
 * <br>
 * Use {@code BlockEffectRegistry.flush()} if calling manually to prevent duplicates. <br>
 * (You probably shouldn't ever do that anyway) <br>
 * <br>
 * This event is not {@link net.minecraftforge.eventbus.api.Cancelable}. <br>
 * <br>
 * This event is fired on the {@link MinecraftForge#EVENT_BUS}.
 */
public class BlockEffectRegisterEvent extends Event
{
    /**
     * Adds a new {@link BlockEffect} to the registry.
     *
     * @param blockEffect The BlockEffect to add.
     * @throws InstantiationException If the BlockEffect doesn't have a default constructor.
     * @throws IllegalAccessException If the default constructor is not accessible.
     */
    public void register(BlockEffect blockEffect)
    {
        BlockEffectRegistry.register(blockEffect);
    }
}
