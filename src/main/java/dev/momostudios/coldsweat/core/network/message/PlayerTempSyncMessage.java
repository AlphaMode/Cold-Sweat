package dev.momostudios.coldsweat.core.network.message;

import dev.momostudios.coldsweat.api.temperature.Temperature;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.network.NetworkEvent;
import dev.momostudios.coldsweat.common.capability.PlayerTempCapability;

import java.util.function.Supplier;

public class PlayerTempSyncMessage
{
    public double body;
    public double base;
    public double ambient;
    public double max;
    public double min;

    public PlayerTempSyncMessage(double body, double base, double ambient, double max, double min)
    {
        this.body = body;
        this.base = base;
        this.ambient = ambient;
    }

    public static void encode(PlayerTempSyncMessage message, PacketBuffer buffer)
    {
        buffer.writeDouble(message.body);
        buffer.writeDouble(message.base);
        buffer.writeDouble(message.ambient);
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

        context.enqueueWork(() -> DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> syncTemperature(message.body, message.base, message.ambient, message.max, message.min)));

        context.setPacketHandled(true);
    }

    public static DistExecutor.SafeRunnable syncTemperature(double body, double base, double ambient, double max, double min)
    {
        return new DistExecutor.SafeRunnable()
        {
            @Override
            public void run()
            {
                ClientPlayerEntity player = Minecraft.getInstance().player;

                if (player != null && !player.isSpectator())
                {
                    player.getCapability(PlayerTempCapability.TEMPERATURE).ifPresent(cap ->
                    {
                        cap.set(Temperature.Types.CORE, body);
                        cap.set(Temperature.Types.BASE, base);
                        cap.set(Temperature.Types.WORLD, ambient);
                        cap.set(Temperature.Types.HOTTEST, max);
                        cap.set(Temperature.Types.COLDEST, min);
                    });
                }
            }
        };
    }
}