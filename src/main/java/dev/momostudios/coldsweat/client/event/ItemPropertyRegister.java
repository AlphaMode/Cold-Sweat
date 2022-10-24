package dev.momostudios.coldsweat.client.event;

import dev.momostudios.coldsweat.api.temperature.Temperature;
import dev.momostudios.coldsweat.config.ClientSettingsConfig;
import dev.momostudios.coldsweat.util.config.ConfigSettings;
import dev.momostudios.coldsweat.util.math.CSMath;
import dev.momostudios.coldsweat.util.registries.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.item.ItemModelsProperties;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ItemPropertyRegister
{
    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event)
    {
        event.enqueueWork(ItemPropertyRegister::registerPropertyOverride);
    }

    public static void registerPropertyOverride()
    {
        ItemModelsProperties.registerProperty(ModItems.HELLSPRING_LAMP, new ResourceLocation("cold_sweat:soulfire_state"), (stack, world, entity) ->
        {
            if (stack.getOrCreateTag().getBoolean("isOn"))
            {
                return stack.getOrCreateTag().getInt("fuel") > 43 ? 3 :
                       stack.getOrCreateTag().getInt("fuel") > 22 ? 2 : 1;
            }
            return 0;
        });
        ItemModelsProperties.registerProperty(ModItems.THERMOMETER, new ResourceLocation("cold_sweat:temperature"), (stack, level, entity) ->
        {
            ClientPlayerEntity player = Minecraft.getInstance().player;
            if (player != null)
            {
                ConfigSettings config = ConfigSettings.getInstance();
                double minTemp = config.minTemp;
                double maxTemp = config.maxTemp;

                double worldTemp = (float) CSMath.convertUnits(WorldTempGaugeDisplay.WORLD_TEMP,
                        ClientSettingsConfig.getInstance().celsius() ? Temperature.Units.C : Temperature.Units.F, Temperature.Units.MC, true);

                double worldTempAdjusted = CSMath.blend(-1.01d, 1d, worldTemp, minTemp, maxTemp);
                return (float) worldTempAdjusted;
            }
            return 1;
        });
    }
}
