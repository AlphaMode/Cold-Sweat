package dev.momostudios.coldsweat.common.event;

import dev.momostudios.coldsweat.ColdSweat;
import dev.momostudios.coldsweat.api.temperature.Temperature;
import dev.momostudios.coldsweat.api.temperature.modifier.*;
import dev.momostudios.coldsweat.api.registry.TempModifierRegistry;
import dev.momostudios.coldsweat.config.ConfigCache;
import dev.momostudios.coldsweat.util.entity.TempHelper;
import dev.momostudios.coldsweat.util.math.CSMath;
import dev.momostudios.coldsweat.util.registries.ModEffects;
import net.minecraft.block.BedBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.SleepFinishedTimeEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ColdSweat.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class AddTempModifiers
{
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event)
    {
        if (event.phase == TickEvent.Phase.END)
        {
            PlayerEntity player = event.player;

            /*
             * Add TempModifiers if not present
             */
            if (player.ticksExisted % 20 == 0)
            {
                TempHelper.addModifier(player, new BiomeTempModifier().tickRate(5), Temperature.Types.WORLD, false);
                TempHelper.addModifier(player, new TimeTempModifier().tickRate(20), Temperature.Types.WORLD, false);
                TempHelper.addModifier(player, new DepthTempModifier().tickRate(5), Temperature.Types.WORLD, false);
                TempHelper.addModifier(player, new BlockTempModifier().tickRate(5), Temperature.Types.WORLD, false);
                if (ModList.get().isLoaded("sereneseasons"))
                    TempHelper.addModifier(player, TempModifierRegistry.getEntryFor("sereneseasons:season").tickRate(20), Temperature.Types.WORLD, false);
                if (ModList.get().isLoaded("betterweather"))
                    TempHelper.addModifier(player, TempModifierRegistry.getEntryFor("betterweather:season").tickRate(20), Temperature.Types.WORLD, false);

                // Hearth
                if (player.isPotionActive(ModEffects.INSULATION))
                {
                    int potionLevel = player.getActivePotionEffect(ModEffects.INSULATION).getAmplifier() + 1;
                    TempHelper.removeModifiers(player, Temperature.Types.WORLD, 1, mod -> mod instanceof HearthTempModifier);
                    TempHelper.addModifier(player, new HearthTempModifier(potionLevel).expires(20), Temperature.Types.WORLD, false);
                }
            }

            // Water / Rain
            if (player.ticksExisted % 5 == 0)
            {
                if (player.isInWaterRainOrBubbleColumn())
                {
                    TempHelper.addModifier(player, new WaterTempModifier(0.01), Temperature.Types.WORLD, false);
                }
                else if (TempHelper.hasModifier(player, WaterTempModifier.class, Temperature.Types.WORLD))
                {
                    TempHelper.removeModifiers(player, Temperature.Types.WORLD, 1, modifier ->
                            modifier instanceof WaterTempModifier && (double) modifier.getArgument("strength") <= 0);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onTrySleep(PlayerInteractEvent.RightClickBlock event)
    {
        PlayerEntity player = event.getPlayer();
        if (player.world.getBlockState(event.getPos()).getBlock() instanceof BedBlock)
        {
            double bodyTemp = TempHelper.getTemperature(player, Temperature.Types.BODY).get();
            double worldTemp = TempHelper.getTemperature(player, Temperature.Types.WORLD).get();
            double minTemp = ConfigCache.getInstance().minTemp;
            double maxTemp = ConfigCache.getInstance().maxTemp;
            System.out.println(bodyTemp + " " + worldTemp + " " + minTemp + " " + maxTemp);

            if (!CSMath.isBetween((int) bodyTemp, -99, 99))
            {
                player.sendStatusMessage(new TranslationTextComponent("cold_sweat.message.sleep.body",
                        new TranslationTextComponent(bodyTemp > 99 ? "cold_sweat.message.sleep.hot" : "cold_sweat.message.sleep.cold").getString()), true);
                event.setCanceled(true);
            }
            else if (!CSMath.isBetween(worldTemp, minTemp, maxTemp))
            {
                player.sendStatusMessage(new TranslationTextComponent("cold_sweat.message.sleep.world",
                        new TranslationTextComponent(worldTemp > maxTemp ? "cold_sweat.message.sleep.hot" : "cold_sweat.message.sleep.cold").getString()), true);
                event.setCanceled(true);
            }
        }
    }
}