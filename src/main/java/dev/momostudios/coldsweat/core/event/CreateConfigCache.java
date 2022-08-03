package dev.momostudios.coldsweat.core.event;

import dev.momostudios.coldsweat.config.ColdSweatConfig;
import dev.momostudios.coldsweat.util.config.ConfigCache;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.server.FMLServerStartedEvent;

@Mod.EventBusSubscriber
public class CreateConfigCache
{
    @SubscribeEvent
    public static void onServerStart(FMLServerStartedEvent event)
    {
        ConfigCache.getInstance().readValues(ColdSweatConfig.getInstance());
    }
}
