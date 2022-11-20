package dev.momostudios.coldsweat.core.network.message;

import dev.momostudios.coldsweat.api.temperature.Temperature;
import dev.momostudios.coldsweat.client.event.SelfTempDisplay;
import dev.momostudios.coldsweat.common.capability.ModCapabilities;
import dev.momostudios.coldsweat.common.capability.PlayerTempCap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public class PlayerTempSyncMessage
{
    CompoundNBT temps;
    boolean instant;

    public PlayerTempSyncMessage(CompoundNBT temps, boolean instant)
    {
        this.temps = temps;
        this.instant = instant;
    }

    public static void encode(PlayerTempSyncMessage message, PacketBuffer buffer)
    {
        buffer.writeCompoundTag(message.temps);
        buffer.writeBoolean(message.instant);
    }

    public static PlayerTempSyncMessage decode(PacketBuffer buffer)
    {
        return new PlayerTempSyncMessage(buffer.readCompoundTag(), buffer.readBoolean());
    }

    public static void handle(PlayerTempSyncMessage message, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();

        if (context.getDirection().getReceptionSide().isClient())
        {
            context.enqueueWork(() ->
            {
                ClientPlayerEntity player = Minecraft.getInstance().player;

                if (player != null)
                {
                    player.getCapability(ModCapabilities.PLAYER_TEMPERATURE).ifPresent(cap ->
                    {
                        if (cap instanceof PlayerTempCap)
                        {
                            ((PlayerTempCap) cap).deserializeTemps(message.temps);

                            if (message.instant)
                            {
                                SelfTempDisplay.setBodyTemp(cap.getTemp(Temperature.Type.BODY));
                            }
                        }
                    });
                }
            });
        }

        context.setPacketHandled(true);
    }
}