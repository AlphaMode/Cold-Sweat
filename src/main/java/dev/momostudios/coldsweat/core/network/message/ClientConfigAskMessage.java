package dev.momostudios.coldsweat.core.network.message;

import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.network.PacketDistributor;
import dev.momostudios.coldsweat.util.config.ConfigSettings;
import dev.momostudios.coldsweat.core.network.ColdSweatPacketHandler;

import java.util.function.Supplier;

/**
 * Requests config settings from the server<br>
 * Client -> Server
 */
public class ClientConfigAskMessage
{
    boolean onJoin;

    public ClientConfigAskMessage(boolean onJoin) {
        this.onJoin = onJoin;
    }

    public static void encode(ClientConfigAskMessage message, PacketBuffer buffer) {
        buffer.writeBoolean(message.onJoin);
    }

    public static ClientConfigAskMessage decode(PacketBuffer buffer)
    {
        return new ClientConfigAskMessage(buffer.readBoolean());
    }

    public static void handle(ClientConfigAskMessage message, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() ->
        {
            ColdSweatPacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(context::getSender), new ClientConfigReceiveMessage(ConfigSettings.getInstance(), message.onJoin));
        });
        context.setPacketHandled(true);
    }
}