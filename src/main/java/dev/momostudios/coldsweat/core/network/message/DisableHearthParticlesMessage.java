package dev.momostudios.coldsweat.core.network.message;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.LogicalSidedProvider;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.network.NetworkEvent;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.function.Supplier;

public class DisableHearthParticlesMessage
{
    CompoundNBT nbt;
    int entityID;
    String worldKey;

    public DisableHearthParticlesMessage(PlayerEntity player, CompoundNBT nbt)
    {
        this.nbt = nbt;
        this.entityID = player.getEntityId();
        this.worldKey = player.world.getDimensionKey().getLocation().toString();
    }

    DisableHearthParticlesMessage(int entityID, String worldKey, CompoundNBT nbt)
    {
        this.nbt = nbt;
        this.entityID = entityID;
        this.worldKey = worldKey;
    }

    public static void encode(DisableHearthParticlesMessage message, PacketBuffer buffer)
    {
        buffer.writeInt(message.entityID);
        buffer.writeString(message.worldKey);
        buffer.writeCompoundTag(message.nbt);
    }

    public static DisableHearthParticlesMessage decode(PacketBuffer buffer)
    {
        return new DisableHearthParticlesMessage(buffer.readInt(), buffer.readString(), buffer.readCompoundTag());
    }

    static Class MINECRAFT = null;
    static Method GET_INSTANCE = null;
    static Field CLIENT_WORLD = null;
    static
    {
        try
        {
            MINECRAFT = Class.forName("net.minecraft.client.Minecraft");
            GET_INSTANCE = ObfuscationReflectionHelper.findMethod(MINECRAFT, "func_71410_x");
            CLIENT_WORLD = ObfuscationReflectionHelper.findField(MINECRAFT, " field_71441_e");
        } catch (Exception ignored) {}
    }

    public static void handle(DisableHearthParticlesMessage message, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() ->
        {
            try
            {
                World world = (context.getDirection().getReceptionSide().isClient() && ((World) CLIENT_WORLD.get(GET_INSTANCE.invoke(null))).getDimensionKey().getLocation().toString().equals(message.worldKey))
                        ? (World) CLIENT_WORLD.get(GET_INSTANCE.invoke(null))
                        : ((MinecraftServer) LogicalSidedProvider.INSTANCE.get(LogicalSide.SERVER)).getWorld(RegistryKey.getOrCreateKey(Registry.WORLD_KEY, new ResourceLocation(message.worldKey)));
                if (world != null)
                {
                    Entity entity = world.getEntityByID(message.entityID);
                    if (entity instanceof PlayerEntity)
                    {
                        entity.getPersistentData().put("disabledHearths", message.nbt);
                    }
                }
            } catch (Exception ignored) {}
        });
        context.setPacketHandled(true);
    }
}
