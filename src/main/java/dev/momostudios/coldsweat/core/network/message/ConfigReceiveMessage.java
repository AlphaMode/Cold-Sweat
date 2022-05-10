package dev.momostudios.coldsweat.core.network.message;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;
import dev.momostudios.coldsweat.config.ConfigCache;
import dev.momostudios.coldsweat.core.network.ColdSweatPacketHandler;

import java.lang.reflect.Constructor;
import java.util.function.Supplier;

public class ConfigReceiveMessage
{
    ConfigCache configCache;
    boolean onJoin;

    public ConfigReceiveMessage(ConfigCache configCache, boolean onJoin)
    {
        this.configCache = configCache;
        this.onJoin = onJoin;
    }

    public static void encode(ConfigReceiveMessage message, PacketBuffer buffer)
    {
        buffer.writeBoolean(message.onJoin);
        ColdSweatPacketHandler.writeConfigCacheToBuffer(message.configCache, buffer);
        // WorldTempConfig
        buffer.writeCompoundTag(ColdSweatPacketHandler.writeListOfLists(message.configCache.worldOptionsReference.get("biome_offsets")));
        buffer.writeCompoundTag(ColdSweatPacketHandler.writeListOfLists(message.configCache.worldOptionsReference.get("dimension_offsets")));
        buffer.writeCompoundTag(ColdSweatPacketHandler.writeListOfLists(message.configCache.worldOptionsReference.get("biome_temperatures")));
        buffer.writeCompoundTag(ColdSweatPacketHandler.writeListOfLists(message.configCache.worldOptionsReference.get("dimension_temperatures")));
    }

    public static ConfigReceiveMessage decode(PacketBuffer buffer)
    {
        boolean onJoin = buffer.readBoolean();
        ConfigCache configCache = ColdSweatPacketHandler.readConfigCacheFromBuffer(buffer);
        configCache.worldOptionsReference.put("biome_offsets", ColdSweatPacketHandler.readListOfLists(buffer.readCompoundTag()));
        configCache.worldOptionsReference.put("dimension_offsets", ColdSweatPacketHandler.readListOfLists(buffer.readCompoundTag()));
        configCache.worldOptionsReference.put("biome_temperatures", ColdSweatPacketHandler.readListOfLists(buffer.readCompoundTag()));
        configCache.worldOptionsReference.put("dimension_temperatures", ColdSweatPacketHandler.readListOfLists(buffer.readCompoundTag()));

        return new ConfigReceiveMessage(configCache, onJoin);
    }

    public static void handle(ConfigReceiveMessage message, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() ->
        {
            if (context.getDirection().getReceptionSide().isClient())
            {
                if (message.onJoin)
                {
                    ConfigCache.setInstance(message.configCache);
                }
                else
                {
                    try
                    {
                        ClientPlayerEntity localPlayer = Minecraft.getInstance().player;
                        if (localPlayer != null)
                        {
                            Constructor configScreen = Class.forName("dev.momostudios.coldsweat.client.gui.config.pages.ConfigPageOne").getConstructor(Class.forName("net.minecraft.client.gui.screen.Screen"), ConfigCache.class);
                            Minecraft.getInstance().displayGuiScreen((Screen) configScreen.newInstance(Minecraft.getInstance().currentScreen, message.configCache));
                        }
                    }
                    catch (Exception e) {}
                }
            }
        });
        context.setPacketHandled(true);
    }
}
