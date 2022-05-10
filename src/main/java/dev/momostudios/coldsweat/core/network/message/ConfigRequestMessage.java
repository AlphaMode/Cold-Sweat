package dev.momostudios.coldsweat.core.network.message;

import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.network.PacketDistributor;
import dev.momostudios.coldsweat.config.ColdSweatConfig;
import dev.momostudios.coldsweat.config.ConfigCache;
import dev.momostudios.coldsweat.config.ItemSettingsConfig;
import dev.momostudios.coldsweat.config.WorldTemperatureConfig;
import dev.momostudios.coldsweat.core.network.ColdSweatPacketHandler;

import java.util.function.Supplier;

public class ConfigRequestMessage
{
    boolean onJoin;

    public ConfigRequestMessage(boolean onJoin) {
        this.onJoin = onJoin;
    }

    public static void encode(ConfigRequestMessage message, PacketBuffer buffer) {
        buffer.writeBoolean(message.onJoin);
    }

    public static ConfigRequestMessage decode(PacketBuffer buffer)
    {
        return new ConfigRequestMessage(buffer.readBoolean());
    }

    public static void handle(ConfigRequestMessage message, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() ->
        {
            ConfigCache cache = new ConfigCache();
            cache.writeValues(ColdSweatConfig.getInstance());
            cache.worldOptionsReference.putAll(WorldTemperatureConfig.INSTANCE.getConfigMap());
            cache.itemSettingsReference = ItemSettingsConfig.INSTANCE;

            ColdSweatPacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(context::getSender), new ConfigReceiveMessage(cache, message.onJoin));
        });
        context.setPacketHandled(true);
    }
}