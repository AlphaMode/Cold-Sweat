package dev.momostudios.coldsweat.api.event.common;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.eventbus.api.Event;

public class BlockChangedEvent extends Event
{
    BlockPos pos;
    BlockState prevState;
    BlockState newState;
    World world;

    public BlockChangedEvent(BlockPos pos, BlockState prevState, BlockState newState, World world)
    {
        this.pos = pos;
        this.prevState = prevState;
        this.newState = newState;
        this.world = world;
    }

    public BlockPos getPos()
    {
        return pos;
    }

    public BlockState getPrevState()
    {
        return prevState;
    }

    public BlockState getNewState()
    {
        return newState;
    }

    public World getWorld()
    {
        return world;
    }
}
