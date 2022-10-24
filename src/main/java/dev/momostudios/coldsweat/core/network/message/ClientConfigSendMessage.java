package dev.momostudios.coldsweat.core.network.message;

import dev.momostudios.coldsweat.util.config.ConfigHelper;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.network.PacketDistributor;
import dev.momostudios.coldsweat.config.ColdSweatConfig;
import dev.momostudios.coldsweat.util.config.ConfigSettings;
import dev.momostudios.coldsweat.core.network.ColdSweatPacketHandler;

import java.util.function.Supplier;

/**
 * Send new config settings to clients.<br>
 * If this packet is sent to the server, the server broadcasts the new settings to all clients.<br>
 * Client -> Server OR Server -> Client
 */
public class ClientConfigSendMessage
{
    ConfigSettings configSettings;

    public ClientConfigSendMessage(ConfigSettings config)
    {
        this.configSettings = config;
    }

    public static void encode(ClientConfigSendMessage message, PacketBuffer buffer)
    {
        buffer.writeCompoundTag(ConfigHelper.writeConfigSettingsToNBT(message.configSettings));
    }

    public static ClientConfigSendMessage decode(PacketBuffer buffer)
    {
        return new ClientConfigSendMessage(ConfigHelper.readConfigSettingsFromNBT(buffer.readCompoundTag()));
    }

    public static void handle(ClientConfigSendMessage message, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() ->
        {
            if (context.getDirection().getReceptionSide().isServer())
            {
                ColdSweatConfig.getInstance().writeValues(message.configSettings);
                ColdSweatConfig.getInstance().save();

                ColdSweatPacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(), new ClientConfigSendMessage(message.configSettings));
            }

            ConfigSettings.setInstance(message.configSettings);
        });
        context.setPacketHandled(true);
    }
}
