package dev.momostudios.coldsweat.common.event;

import dev.momostudios.coldsweat.common.te.HearthTileEntity;
import dev.momostudios.coldsweat.core.network.ColdSweatPacketHandler;
import dev.momostudios.coldsweat.core.network.message.HearthResetMessage;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.server.ServerChunkProvider;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.network.PacketDistributor;

@Mod.EventBusSubscriber
public class HearthPathManagement
{
    @SubscribeEvent
    public static void onBlockUpdated(BlockEvent.NeighborNotifyEvent event)
    {
        int chunkX = (event.getPos().getX() >> 4) - 1;
        int chunkZ = (event.getPos().getZ() >> 4) - 1;

        for (int x = -1; x < 1; x++)
        {
            for (int z = -1; z < 1; z++)
            {
                Chunk chunk = event.getWorld().getChunkProvider().getChunkNow(chunkX + x, chunkZ + z);

                if (chunk != null && event.getWorld().getChunkProvider() instanceof ServerChunkProvider)
                chunk.getTileEntitiesPos().forEach(pos ->
                {
                    TileEntity block = event.getWorld().getTileEntity(pos);
                    if (block instanceof HearthTileEntity && !((HearthTileEntity) block).shouldRebuild())
                    {
                        ((HearthTileEntity) block).setShouldRebuild(true);
                        ColdSweatPacketHandler.INSTANCE.send(PacketDistributor.TRACKING_CHUNK.with(() -> chunk), new HearthResetMessage(pos));
                    }
                });
            }
        }
    }
}
