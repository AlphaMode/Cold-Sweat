package dev.momostudios.coldsweat.common.event;

import dev.momostudios.coldsweat.util.world.WorldHelper;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.material.Material;
import net.minecraft.util.Direction;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class PlaceCaveAir
{
    @SubscribeEvent
    public static void onRemoveBlock(BlockEvent.NeighborNotifyEvent event)
    {
        ChunkPos chunkPos = new ChunkPos(event.getPos());
        Chunk chunk = event.getWorld().getChunkProvider().getChunkNow(chunkPos.x, chunkPos.z);

        if (event.getWorld() instanceof World && !WorldHelper.canSeeSky(chunk, (World) event.getWorld(), event.getPos()))
        {
            WorldHelper.schedule(() ->
            {
                if (chunk != null)
                {
                    BlockState state = chunk.getBlockState(event.getPos());
                    if (state.getMaterial() == Material.AIR && state.getBlock() != Blocks.CAVE_AIR)
                    for (Direction direction : event.getNotifiedSides())
                    {
                        if (event.getWorld().getBlockState(event.getPos().offset(direction)).getBlock().getBlock() == Blocks.CAVE_AIR)
                        {
                            event.getWorld().setBlockState(event.getPos(), Blocks.CAVE_AIR.getDefaultState(), 2);
                            break;
                        }
                    }
                }
            }, 1);
        }
    }
}
