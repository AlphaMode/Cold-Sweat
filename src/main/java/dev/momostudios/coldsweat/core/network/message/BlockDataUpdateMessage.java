package dev.momostudios.coldsweat.core.network.message;

import net.minecraft.client.Minecraft;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Server -> Client
 */
public class BlockDataUpdateMessage
{
    BlockPos blockPos;
    TileEntity tileEntity;
    CompoundNBT nbt;

    public BlockDataUpdateMessage(TileEntity tileEntity)
    {
        this.blockPos = tileEntity.getPos();
        this.tileEntity = tileEntity;
    }

    public BlockDataUpdateMessage(BlockPos pos, CompoundNBT nbt)
    {
        this.blockPos = pos;
        this.nbt = nbt;
    }

    public static void encode(BlockDataUpdateMessage message, PacketBuffer buffer)
    {
        buffer.writeBlockPos(message.blockPos);
        buffer.writeCompoundTag(message.tileEntity.write(new CompoundNBT()));
    }

    public static BlockDataUpdateMessage decode(PacketBuffer buffer)
    {
        return new BlockDataUpdateMessage(buffer.readBlockPos(), buffer.readCompoundTag());
    }

    public static void handle(BlockDataUpdateMessage message, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        if (context.getDirection().getReceptionSide().isClient())
        {
            context.enqueueWork(() ->
            {
                ClientWorld world = Minecraft.getInstance().world;
                if (world != null)
                {
                    TileEntity te = world.getTileEntity(message.blockPos);
                    if (te != null)
                    {
                        te.read(null, message.nbt);
                    }
                }
            });
        }
        context.setPacketHandled(true);
    }
}
