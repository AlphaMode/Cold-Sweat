package dev.momostudios.coldsweat.core.event;

import dev.momostudios.coldsweat.common.capability.PlayerTempCapability;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import dev.momostudios.coldsweat.config.ColdSweatConfig;
import dev.momostudios.coldsweat.config.ConfigCache;
import dev.momostudios.coldsweat.util.entity.TempHelper;

@Mod.EventBusSubscriber
public class PlayerTempUpdater
{
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event)
    {
        if (event.phase == TickEvent.Phase.START)
        {
            if (!event.player.world.isRemote)
            {
                if (event.player.ticksExisted % 20 == 0)
                {
                    event.player.getCapability(PlayerTempCapability.TEMPERATURE).ifPresent(cap ->
                    {
                        TempHelper.updateModifiers(event.player, cap);
                    });
                }

                event.player.getCapability(PlayerTempCapability.TEMPERATURE).ifPresent(cap -> cap.tickUpdate(event.player));
            }
            else
            {
                event.player.getCapability(PlayerTempCapability.TEMPERATURE).ifPresent(cap -> cap.tickClient(event.player));
            }
        }
    }


    @SubscribeEvent
    public static void serverSyncConfigToCache(TickEvent.WorldTickEvent event)
    {
        // Syncs the server's config files to the cache
        if (!event.world.isRemote && event.world.getGameTime() % 100 == 0)
            ConfigCache.getInstance().writeValues(ColdSweatConfig.getInstance());
    }
}