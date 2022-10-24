package dev.momostudios.coldsweat.core.event;

import dev.momostudios.coldsweat.config.*;
import dev.momostudios.coldsweat.util.config.ConfigSettings;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.server.FMLServerStartedEvent;

@Mod.EventBusSubscriber
public class CreateConfigSettings
{
    @SubscribeEvent
    public static void onServerStart(FMLServerStartedEvent event)
    {
        ConfigSettings.getInstance().readValues(ColdSweatConfig.getInstance());
    }
}
