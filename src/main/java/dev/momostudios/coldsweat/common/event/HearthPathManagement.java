package dev.momostudios.coldsweat.common.event;

import dev.momostudios.coldsweat.common.te.HearthTileEntity;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class HearthPathManagement
{
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
                chunk.getTileEntityMap().forEach((blockPos, tileEntity) ->
                {
                    if (tileEntity instanceof HearthTileEntity && !((HearthTileEntity) tileEntity).shouldRebuild())
                    {
                        ((HearthTileEntity) tileEntity).attemptReset(true);
                    }
                });
            }
        }
    }
}
