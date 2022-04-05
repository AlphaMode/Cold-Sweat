package dev.momostudios.coldsweat.core.network.message;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class BlockDataUpdateMessage
{
    BlockPos blockPos;
    List<INBT> tagValues;
    List<String> blockTags;

    public BlockDataUpdateMessage() {
    }

    public BlockDataUpdateMessage(BlockPos blockPos, List<String> blockTags, List<INBT> tagValues) {
        this.blockPos = blockPos;
        this.blockTags = blockTags;
        this.tagValues = tagValues;
    }

    public static void encode(BlockDataUpdateMessage message, PacketBuffer buffer)
    {
        buffer.writeBlockPos(message.blockPos);
        if (message.blockTags.size() == message.tagValues.size())
        {
            CompoundNBT tags = new CompoundNBT();
            for (int i = 0; i < message.blockTags.size(); i++)
            {
                tags.put(message.blockTags.get(i), message.tagValues.get(i));
            }
            buffer.writeCompoundTag(tags);
        }
    }

    public static BlockDataUpdateMessage decode(PacketBuffer buffer)
    {
        BlockPos blockPos = buffer.readBlockPos();

        CompoundNBT tags = buffer.readCompoundTag();
        Set<String> blockTags = tags.keySet();
        List<INBT> tagValues = new ArrayList<>(blockTags.size());
        for (String blockTag : blockTags)
        {
            tagValues.add(tags.get(blockTag));
        }
        return new BlockDataUpdateMessage(blockPos, new ArrayList<>(blockTags), tagValues);
    }

    public static void handle(BlockDataUpdateMessage message, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        if (context.getDirection().getReceptionSide().isClient())
        {
            context.enqueueWork(() ->
            {
                TileEntity te = Minecraft.getInstance().world.getTileEntity(message.blockPos);
                if (te != null)
                {
                    for (int i = 0; i < message.blockTags.size(); i++)
                    {
                        te.getTileData().put(message.blockTags.get(i), message.tagValues.get(i));
                    }
                }
            });
        }
        context.setPacketHandled(true);
    }
}
