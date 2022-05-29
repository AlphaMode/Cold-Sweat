package dev.momostudios.coldsweat.core.event;

import dev.momostudios.coldsweat.common.capability.ModCapabilities;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import dev.momostudios.coldsweat.config.ColdSweatConfig;
import dev.momostudios.coldsweat.config.ConfigCache;
import dev.momostudios.coldsweat.util.entity.TempHelper;

@Mod.EventBusSubscriber
public class PlayerTempUpdater
{
    static int WORLD_TIME = 0;
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event)
    {
        if (event.phase == TickEvent.Phase.START)
        {
            if (!event.player.world.isRemote)
            {
                PlayerEntity player = event.player;
                player.getCapability(ModCapabilities.PLAYER_TEMPERATURE).ifPresent(cap ->
                {
                    cap.tickUpdate(player);

                    if (player.ticksExisted % 60 == 0)
                    {
                        TempHelper.updateModifiers(player, cap);
                    }
                });
            }
            else
            {
                event.player.getCapability(ModCapabilities.PLAYER_TEMPERATURE).ifPresent(cap -> cap.tickClient(event.player));
            }
        }
    }

    @SubscribeEvent
    public static void serverSyncConfigToCache(TickEvent.WorldTickEvent event)
    {
        // Syncs the server's config files to the cache
        if (!event.world.isRemote && WORLD_TIME % 200 == 0)
            ConfigCache.getInstance().writeValues(ColdSweatConfig.getInstance());

        WORLD_TIME++;
    }
}