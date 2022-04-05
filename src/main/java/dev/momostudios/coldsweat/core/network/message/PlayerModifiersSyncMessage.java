package dev.momostudios.coldsweat.core.network.message;

import dev.momostudios.coldsweat.api.temperature.Temperature;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.network.NetworkEvent;
import dev.momostudios.coldsweat.api.temperature.modifier.TempModifier;
import dev.momostudios.coldsweat.common.capability.PlayerTempCapability;
import dev.momostudios.coldsweat.util.entity.NBTHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class PlayerModifiersSyncMessage
{
    public List<TempModifier> ambient;
    public List<TempModifier> body;
    public List<TempModifier> base;
    public List<TempModifier> rate;
    public List<TempModifier> max;
    public List<TempModifier> min;

    public PlayerModifiersSyncMessage(List<TempModifier> ambient, List<TempModifier> body, List<TempModifier> base, List<TempModifier> rate,
                                      List<TempModifier> max, List<TempModifier> min)
    {
        this.ambient = ambient;
        this.body = body;
        this.base = base;
        this.rate = rate;
        this.max = max;
        this.min = min;
    }

    public static void encode(PlayerModifiersSyncMessage message, PacketBuffer buffer)
    {
        buffer.writeCompoundTag(writeToNBT(message, Temperature.Types.WORLD));
        buffer.writeCompoundTag(writeToNBT(message, Temperature.Types.CORE));
        buffer.writeCompoundTag(writeToNBT(message, Temperature.Types.BASE));
        buffer.writeCompoundTag(writeToNBT(message, Temperature.Types.RATE));
        buffer.writeCompoundTag(writeToNBT(message, Temperature.Types.HOTTEST));
        buffer.writeCompoundTag(writeToNBT(message, Temperature.Types.COLDEST));
    }

    public static PlayerModifiersSyncMessage decode(PacketBuffer buffer)
    {
        return new PlayerModifiersSyncMessage(
                readFromNBT(buffer.readCompoundTag()),
                readFromNBT(buffer.readCompoundTag()),
                readFromNBT(buffer.readCompoundTag()),
                readFromNBT(buffer.readCompoundTag()),
                readFromNBT(buffer.readCompoundTag()),
                readFromNBT(buffer.readCompoundTag()));
    }

    private static CompoundNBT writeToNBT(PlayerModifiersSyncMessage message, Temperature.Types type)
    {
        CompoundNBT nbt = new CompoundNBT();
        List<TempModifier> referenceList =
                type == Temperature.Types.WORLD ? message.ambient :
                type == Temperature.Types.CORE ? message.body :
                type == Temperature.Types.BASE ? message.base :
                type == Temperature.Types.HOTTEST ? message.max :
                type == Temperature.Types.COLDEST ? message.min :
                message.rate;

        // Iterate modifiers and write to NBT
        for (int i = 0; i < referenceList.size(); i++)
        {
            TempModifier modifier = referenceList.get(i);

            if (modifier != null && modifier.getID() != null)
            {
                nbt.put(String.valueOf(i), NBTHelper.modifierToNBT(modifier));
            }
        }

        return nbt;
    }

    private static List<TempModifier> readFromNBT(CompoundNBT nbt)
    {
        List<TempModifier> modifiers = new ArrayList<>();
        for (String key : nbt.keySet())
        {
            modifiers.add(NBTHelper.NBTToModifier(nbt.getCompound(key)));
        }
        return modifiers;
    }

    public static void handle(PlayerModifiersSyncMessage message, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();

        if (context.getDirection().getReceptionSide().isClient())
        context.enqueueWork(() ->
        {
            ClientPlayerEntity player = Minecraft.getInstance().player;

            if (player != null)
            {
                player.getCapability(PlayerTempCapability.TEMPERATURE).ifPresent(cap ->
                {
                    cap.clearModifiers(Temperature.Types.WORLD);
                    cap.getModifiers(Temperature.Types.WORLD).addAll(message.ambient);

                    cap.clearModifiers(Temperature.Types.CORE);
                    cap.getModifiers(Temperature.Types.CORE).addAll(message.body);

                    cap.clearModifiers(Temperature.Types.BASE);
                    cap.getModifiers(Temperature.Types.BASE).addAll(message.base);

                    cap.clearModifiers(Temperature.Types.RATE);
                    cap.getModifiers(Temperature.Types.RATE).addAll(message.rate);
                });
            }
        });

        context.setPacketHandled(true);
    }
}