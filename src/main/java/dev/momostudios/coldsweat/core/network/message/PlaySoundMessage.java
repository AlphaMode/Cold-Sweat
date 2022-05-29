package dev.momostudios.coldsweat.core.network.message;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.function.Supplier;

public class PlaySoundMessage
{
    static Constructor<?> SOUND_MAKER;
    static Method PLAY_METHOD;

    static
    {
        try
        {
            SOUND_MAKER = ObfuscationReflectionHelper.findConstructor(Class.forName("net.minecraft.client.audio.EntityTickableSound"),
                    SoundEvent.class, SoundCategory.class, float.class, float.class, Entity.class);
            PLAY_METHOD = ObfuscationReflectionHelper.findMethod(Class.forName("net.minecraft.client.audio.SoundHandler"), "func_147682_a",
                    Class.forName("net.minecraft.client.audio.ISound"));
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    String sound;
    SoundCategory category;
    float volume;
    float pitch;
    int entityID;

    public PlaySoundMessage(String sound, SoundCategory category, float volume, float pitch, int entityID)
    {
        this.sound = sound;
        this.category = category;
        this.volume = volume;
        this.pitch = pitch;
        this.entityID = entityID;
    }

    public static void encode(PlaySoundMessage message, PacketBuffer buffer) {
        buffer.writeString(message.sound);
        buffer.writeEnumValue(message.category);
        buffer.writeFloat(message.volume);
        buffer.writeFloat(message.pitch);
        buffer.writeInt(message.entityID);
    }

    public static PlaySoundMessage decode(PacketBuffer buffer)
    {
        return new PlaySoundMessage(buffer.readString(), buffer.readEnumValue(SoundCategory.class), buffer.readFloat(), buffer.readFloat(), buffer.readInt());
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
                    try
                    {
                        PLAY_METHOD.invoke(Minecraft.getInstance().getSoundHandler(), SOUND_MAKER.newInstance(sound,
                                message.category, message.volume, message.pitch, entity));
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                }
            }
        });
        context.setPacketHandled(true);
    }
}
