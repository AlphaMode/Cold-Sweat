package dev.momostudios.coldsweat.core.network.message;

import dev.momostudios.coldsweat.util.config.ConfigHelper;
import dev.momostudios.coldsweat.util.config.ConfigSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.lang.reflect.Constructor;
import java.util.function.Supplier;

/**
 * Applies config settings from the server<br>
 * Server -> Client
 */
public class ClientConfigReceiveMessage
{
    ConfigSettings configSettings;
    boolean onJoin;

    public ClientConfigReceiveMessage(ConfigSettings configSettings, boolean onJoin)
    {
        this.configSettings = configSettings;
        this.onJoin = onJoin;
    }

    public static void encode(ClientConfigReceiveMessage message, PacketBuffer buffer)
    {
        buffer.writeBoolean(message.onJoin);
        buffer.writeCompoundTag(ConfigHelper.writeConfigSettingsToNBT(message.configSettings));
    }

    public static ClientConfigReceiveMessage decode(PacketBuffer buffer)
    {
        boolean onJoin = buffer.readBoolean();
        ConfigSettings configSettings = ConfigHelper.readConfigSettingsFromNBT(buffer.readCompoundTag());

        return new ClientConfigReceiveMessage(configSettings, onJoin);
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
                    ConfigSettings.setInstance(message.configSettings);
                }
                else
                {
                    try
                    {
                        ClientPlayerEntity clientPlayer = Minecraft.getInstance().player;
                        if (clientPlayer != null)
                        {
                            Constructor configScreen = Class.forName("dev.momostudios.coldsweat.client.gui.config.pages.ConfigPageOne").getConstructor(Class.forName("net.minecraft.client.gui.screen.Screen"), ConfigSettings.class);
                            Minecraft.getInstance().displayGuiScreen((Screen) configScreen.newInstance(Minecraft.getInstance().currentScreen, message.configSettings));
                        }
                    }
                    catch (Exception ignored) {}
                }
            }
        });
        context.setPacketHandled(true);
    }
}
