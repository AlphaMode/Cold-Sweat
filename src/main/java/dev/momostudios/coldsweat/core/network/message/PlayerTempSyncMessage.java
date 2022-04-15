package dev.momostudios.coldsweat.core.network.message;

import dev.momostudios.coldsweat.api.temperature.Temperature;
import dev.momostudios.coldsweat.common.capability.ModCapabilities;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public class PlayerTempSyncMessage
{
    public double body;
    public double base;
    public double world;
    public double max;
    public double min;

    public PlayerTempSyncMessage(double body, double base, double world, double max, double min)
    {
        this.body = body;
        this.base = base;
        this.world = world;
        this.max = max;
        this.min = min;
    }

    public static void encode(PlayerTempSyncMessage message, PacketBuffer buffer)
    {
        buffer.writeDouble(message.body);
        buffer.writeDouble(message.base);
        buffer.writeDouble(message.world);
        buffer.writeDouble(message.max);
        buffer.writeDouble(message.min);
    }

    public static PlayerTempSyncMessage decode(PacketBuffer buffer)
    {
        return new PlayerTempSyncMessage(buffer.readDouble(), buffer.readDouble(), buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
    }

    public static void handle(PlayerTempSyncMessage message, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();

        if (context.getDirection().getReceptionSide().isClient())
        context.enqueueWork(() ->
        {
            ClientPlayerEntity player = Minecraft.getInstance().player;

            if (player != null && !player.isSpectator())
            {
                player.world.getPlayerByUuid(player.getUniqueID()).getCapability(ModCapabilities.PLAYER_TEMPERATURE).ifPresent(cap ->
                {
                    cap.set(Temperature.Types.CORE, message.body);
                    cap.set(Temperature.Types.BASE, message.base);
                    cap.set(Temperature.Types.WORLD, message.world);
                    cap.set(Temperature.Types.MAX, message.max);
                    cap.set(Temperature.Types.MIN, message.min);
                });
            }
        });

        context.setPacketHandled(true);
    }
}