package dev.momostudios.coldsweat.common.event;

import dev.momostudios.coldsweat.common.te.HearthTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class HearthPathManagement
{
    public static LinkedHashMap<BlockPos, Integer> HEARTH_POSITIONS = new LinkedHashMap<>();

    public static final Set<Pair<BlockPos, String>> DISABLED_HEARTHS = new HashSet<>();

    // When a block update happens in the world, store the position of the chunk so nearby Hearths will be notified
    @SubscribeEvent
    public static void onBlockUpdated(BlockChangedEvent event)
    {
        BlockPos pos = event.getPos();
        World world = event.getWorld();
        if (event.getPrevState().getShape(world, pos) != event.getNewState().getShape(world, pos))
        {
            for (Map.Entry<BlockPos, Integer> entry : HEARTH_POSITIONS.entrySet())
            {
                BlockPos hearthPos = entry.getKey();
                int range = entry.getValue();
                TileEntity te = event.getWorld().getTileEntity(hearthPos);
                if (pos.withinDistance(hearthPos, range) && te instanceof HearthTileEntity)
                {
                    ((HearthTileEntity) te).sendBlockUpdate(pos);
                }
            }
        }
    }

    /**
     * Save the player's disabled hearths on logout
     */
    @SubscribeEvent
    public static void onBlockUpdated(BlockEvent.NeighborNotifyEvent event)
    {
        int chunkX = (event.getPos().getX() >> 4);
        int chunkZ = (event.getPos().getZ() >> 4);

        for (int x = -1; x < 1; x++)
        {
            for (int z = -1; z < 1; z++)
            {
                Chunk chunk = event.getWorld().getChunkProvider().getChunkNow(chunkX + x, chunkZ + z);

                if (chunk != null)
                {
                    for (TileEntity te : chunk.getTileEntityMap().values())
                    {
                        if (te instanceof HearthTileEntity)
                        {
                            ((HearthTileEntity) te).attemptReset();
                        }
                    }
                }
            }
        }
    }
}
