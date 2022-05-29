package dev.momostudios.coldsweat.core.network.message;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.EntityTickableSound;
import net.minecraft.entity.Entity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.UUID;
import java.util.function.Supplier;

public class PlaySoundMessage
{
    String sound;
    float volume;
    float pitch;
    int entityID;

    public PlaySoundMessage(String sound, float volume, float pitch, int entityID)
    {
        this.sound = sound;
        this.volume = volume;
        this.pitch = pitch;
        this.entityID = entityID;
    }

    public static void encode(PlaySoundMessage message, PacketBuffer buffer) {
        buffer.writeString(message.sound);
        buffer.writeFloat(message.volume);
        buffer.writeFloat(message.pitch);
        buffer.writeInt(message.entityID);
    }

    public static PlaySoundMessage decode(PacketBuffer buffer)
    {
        return new PlaySoundMessage(buffer.readString(), buffer.readFloat(), buffer.readFloat(), buffer.readInt());
    }

    public static void handle(PlaySoundMessage message, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() ->
        {
            if (context.getDirection().getReceptionSide().isClient())
            {
                SoundEvent sound = ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation(message.sound));
                Entity entity = Minecraft.getInstance().world.getEntityByID(message.entityID);

                if (entity != null && sound != null)
                {
                    Minecraft.getInstance().getSoundHandler().play(new EntityTickableSound(sound, SoundCategory.PLAYERS,
                            message.volume, message.pitch, entity));
                }
            }
        });
        context.setPacketHandled(true);
    }
}
