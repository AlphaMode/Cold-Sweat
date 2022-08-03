package dev.momostudios.coldsweat.client.event;

import dev.momostudios.coldsweat.core.network.message.ClientConfigAskMessage;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.EntityLeaveWorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import dev.momostudios.coldsweat.core.network.ColdSweatPacketHandler;

@Mod.EventBusSubscriber(Dist.CLIENT)
public class SyncConfigOnJoin
{
    static boolean GENERATED = false;

    @SubscribeEvent
    public static void onJoin(EntityJoinWorldEvent event)
    {
        if (!GENERATED && event.getWorld().isRemote && event.getEntity() == Minecraft.getInstance().player)
        {
            GENERATED = true;
            ColdSweatPacketHandler.INSTANCE.sendToServer(new ClientConfigAskMessage(true));
        }
    }

    @SubscribeEvent
    public static void onLeave(EntityLeaveWorldEvent event)
    {
        if (GENERATED && event.getWorld().isRemote && event.getEntity() == Minecraft.getInstance().player)
        {
            GENERATED = false;
        }
    }
}
