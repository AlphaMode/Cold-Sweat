package dev.momostudios.coldsweat.core.network;

import dev.momostudios.coldsweat.core.network.message.*;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.StringNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;
import dev.momostudios.coldsweat.ColdSweat;
import dev.momostudios.coldsweat.config.ConfigCache;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ColdSweatPacketHandler
{
    private static final String PROTOCOL_VERSION = "0.1.1";
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(ColdSweat.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    public static void init()
    {
        INSTANCE.registerMessage(0, PlayerTempSyncMessage.class, PlayerTempSyncMessage::encode, PlayerTempSyncMessage::decode, PlayerTempSyncMessage::handle);
        INSTANCE.registerMessage(1, PlayerModifiersSyncMessage.class, PlayerModifiersSyncMessage::encode, PlayerModifiersSyncMessage::decode, PlayerModifiersSyncMessage::handle);
        INSTANCE.registerMessage(2, SoulLampInputMessage.class, SoulLampInputMessage::encode, SoulLampInputMessage::decode, SoulLampInputMessage::handle);
        INSTANCE.registerMessage(3, SoulLampInputClientMessage.class, SoulLampInputClientMessage::encode, SoulLampInputClientMessage::decode, SoulLampInputClientMessage::handle);
        INSTANCE.registerMessage(4, ClientConfigSendMessage.class, ClientConfigSendMessage::encode, ClientConfigSendMessage::decode, ClientConfigSendMessage::handle);
        INSTANCE.registerMessage(5, ClientConfigAskMessage.class, ClientConfigAskMessage::encode, ClientConfigAskMessage::decode, ClientConfigAskMessage::handle);
        INSTANCE.registerMessage(6, ClientConfigRecieveMessage.class, ClientConfigRecieveMessage::encode, ClientConfigRecieveMessage::decode, ClientConfigRecieveMessage::handle);
        INSTANCE.registerMessage(7, PlaySoundMessage.class, PlaySoundMessage::encode, PlaySoundMessage::decode, PlaySoundMessage::handle);
        INSTANCE.registerMessage(8, HearthFuelSyncMessage.class, HearthFuelSyncMessage::encode, HearthFuelSyncMessage::decode, HearthFuelSyncMessage::handle);
        INSTANCE.registerMessage(9, BlockDataUpdateMessage.class, BlockDataUpdateMessage::encode, BlockDataUpdateMessage::decode, BlockDataUpdateMessage::handle);
    }

    public static void writeConfigCacheToBuffer(ConfigCache config, PacketBuffer buffer)
    {
        buffer.writeInt(config.difficulty);
        buffer.writeDouble(config.minTemp);
        buffer.writeDouble(config.maxTemp);
        buffer.writeDouble(config.rate);
        buffer.writeBoolean(config.fireRes);
        buffer.writeBoolean(config.iceRes);
        buffer.writeBoolean(config.damageScaling);
        buffer.writeBoolean(config.showWorldTemp);
        buffer.writeInt(config.graceLength);
        buffer.writeBoolean(config.graceEnabled);
    }

    public static ConfigCache readConfigCacheFromBuffer(PacketBuffer buffer)
    {
        ConfigCache config = new ConfigCache();
        config.difficulty = buffer.readInt();
        config.minTemp = buffer.readDouble();
        config.maxTemp = buffer.readDouble();
        config.rate = buffer.readDouble();
        config.fireRes = buffer.readBoolean();
        config.iceRes = buffer.readBoolean();
        config.damageScaling = buffer.readBoolean();
        config.showWorldTemp = buffer.readBoolean();
        config.graceLength = buffer.readInt();
        config.graceEnabled = buffer.readBoolean();
        return config;
    }

    public static CompoundNBT writeListOfLists(List<? extends List<?>> list)
    {
        CompoundNBT tag = new CompoundNBT();
        for (int i = 0; i < list.size(); i++)
        {
            List<?> sublist = list.get(i);
            ListNBT subtag = new ListNBT();
            for (Object o : sublist)
            {
                subtag.add(StringNBT.valueOf(o.toString()));
            }
            tag.put("" + i, subtag);
        }
        return tag;
    }

    public static List<List<Object>> readListOfLists(CompoundNBT tag)
    {
        List<List<Object>> list = new ArrayList<>();
        for (int i = 0; i < tag.size(); i++)
        {
            ListNBT subtag = tag.getList("" + i, 8);
            List<Object> sublist = IntStream.range(0, subtag.size()).mapToObj(j ->
            {
                String string = subtag.getString(j);
                try
                {
                    return Double.parseDouble(string);
                }
                catch (Exception e)
                {
                    return string;
                }
            }).collect(Collectors.toList());
            list.add(sublist);
        }
        return list;
    }
}
