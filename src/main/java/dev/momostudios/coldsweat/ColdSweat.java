package dev.momostudios.coldsweat;

import dev.momostudios.coldsweat.common.temperature.Temperature;
import dev.momostudios.coldsweat.config.*;
import dev.momostudios.coldsweat.common.capability.*;
import dev.momostudios.coldsweat.core.init.*;
import dev.momostudios.coldsweat.core.network.ColdSweatPacketHandler;
import dev.momostudios.coldsweat.util.entity.PlayerHelper;
import dev.momostudios.coldsweat.util.registries.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(ColdSweat.MOD_ID)
public class ColdSweat
{
    public static final Logger LOGGER = LogManager.getLogger();
    public static final String MOD_ID = "cold_sweat";
    public static final boolean remapMixins = false;

    public ColdSweat()
    {
        MinecraftForge.EVENT_BUS.register(this);
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();

        bus.addListener(this::commonSetup);
        bus.addListener(this::clientSetup);
        BlockInit.BLOCKS.register(bus);
        BlockEntityInit.BLOCK_ENTITY_TYPES.register(bus);
        ContainerInit.CONTAINER_TYPES.register(bus);
        ItemInit.ITEMS.register(bus);
        EffectInit.EFFECTS.register(bus);
        ParticleTypesInit.PARTICLES.register(bus);
        PotionInit.POTIONS.register(bus);
        SoundInit.SOUNDS.register(bus);

        // Setup configs
        WorldTemperatureConfig.setup();
        ItemSettingsConfig.setup();
        ColdSweatConfig.setup();
        ClientSettingsConfig.setup();
        EntitySettingsConfig.setup();
    }

    // Register Commands
    @SubscribeEvent
    public void onCommandRegister(final RegisterCommandsEvent event)
    {
        CommandInit.registerCommands(event);
    }

    // Register Packet
    @SubscribeEvent
    public void commonSetup(final FMLCommonSetupEvent event)
    {
        ColdSweatPacketHandler.init();
    }

    // Fix Hearth transparency
    @SubscribeEvent
    public void clientSetup(final FMLClientSetupEvent event)
    {
        ItemBlockRenderTypes.setRenderLayer(BlockInit.HEARTH.get(), RenderType.cutoutMipped());

        event.enqueueWork(() ->
        {
            ItemProperties.register(ModItems.HELLSPRING_LAMP, new ResourceLocation(ColdSweat.MOD_ID, "hellspring_state"), (stack, level, entity, id) ->
            {
                if (stack.getOrCreateTag().getBoolean("isOn"))
                {
                    return stack.getOrCreateTag().getInt("fuel") > 43 ? 3 :
                            stack.getOrCreateTag().getInt("fuel") > 22 ? 2 : 1;
                }
                return 0;
            });

            ItemProperties.register(ModItems.THERMOMETER, new ResourceLocation(ColdSweat.MOD_ID, "temperature"), (stack, level, entity, id) ->
            {
                Player player = Minecraft.getInstance().player;
                if (player != null)
                {
                    ConfigCache config = ConfigCache.getInstance();
                    float minTemp = (float) config.minTemp;
                    float maxTemp = (float) config.maxTemp;

                    float ambientTemp = (float) PlayerHelper.getTemperature(player, Temperature.Types.AMBIENT).get();

                    float ambientAdjusted = ambientTemp - minTemp;
                    float tempScaleFactor = 1 / ((maxTemp - minTemp) / 2);

                    return ambientAdjusted * tempScaleFactor - 1;
                }
                return 1;
            });
        });
    }

    @SubscribeEvent
    public static void onCapInit(RegisterCapabilitiesEvent event)
    {
        event.register(ITemperatureCap.class);
        event.register(IBlockStorageCap.class);
    }
}
