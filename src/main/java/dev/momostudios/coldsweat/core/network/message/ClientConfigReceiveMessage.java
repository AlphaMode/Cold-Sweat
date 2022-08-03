package dev.momostudios.coldsweat.core.network.message;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;
import dev.momostudios.coldsweat.util.config.ConfigCache;
import dev.momostudios.coldsweat.core.network.ColdSweatPacketHandler;

import java.lang.reflect.Constructor;
import java.util.function.Supplier;

public class ClientConfigReceiveMessage
{
    ConfigCache configCache;
    boolean onJoin;

    public ClientConfigReceiveMessage(ConfigCache configCache, boolean onJoin)
    {
        this.configCache = configCache;
        this.onJoin = onJoin;
    }

    public static void encode(ClientConfigReceiveMessage message, PacketBuffer buffer)
    {
        buffer.writeBoolean(message.onJoin);
        ColdSweatPacketHandler.writeConfigCacheToBuffer(message.configCache, buffer);
    }

    public static ClientConfigReceiveMessage decode(PacketBuffer buffer)
    {
        boolean onJoin = buffer.readBoolean();
        ConfigCache configCache = ColdSweatPacketHandler.readConfigCacheFromBuffer(buffer);

        return new ClientConfigReceiveMessage(configCache, onJoin);
    }

    public static void handle(ClientConfigReceiveMessage message, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() ->
        {
            if (context.getDirection().getReceptionSide().isClient())
            {
                if (message.onJoin)
                {
                    ConfigCache.setInstance(message.configCache);
                }
                else
                {
                    try
                    {
                        ClientPlayerEntity clientPlayer = Minecraft.getInstance().player;
                        if (clientPlayer != null)
                        {
                            Constructor configScreen = Class.forName("dev.momostudios.coldsweat.client.gui.config.pages.ConfigPageOne").getConstructor(Class.forName("net.minecraft.client.gui.screen.Screen"), ConfigCache.class);
                            Minecraft.getInstance().displayGuiScreen((Screen) configScreen.newInstance(Minecraft.getInstance().currentScreen, message.configCache));
                        }
                    }
                    catch (Exception ignored) {}
                }
            }
        });
        context.setPacketHandled(true);
    }
}
