package dev.momostudios.coldsweat.common.event;

import dev.momostudios.coldsweat.ColdSweat;
import dev.momostudios.coldsweat.api.temperature.Temperature;
import dev.momostudios.coldsweat.api.temperature.modifier.*;
import dev.momostudios.coldsweat.api.registry.TempModifierRegistry;
import dev.momostudios.coldsweat.util.entity.TempHelper;
import dev.momostudios.coldsweat.util.registries.ModEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraftforge.event.TickEvent;
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

            // Nether Lamp
            if (player.getPersistentData().getInt("soulLampTimeout") <= 0 && TempHelper.hasModifier(player, HellLampTempModifier.class, Temperature.Types.WORLD))
            {
                TempHelper.removeModifiers(player, Temperature.Types.WORLD, 1, modifier -> modifier instanceof HellLampTempModifier);
            }
            else
            {
                player.getPersistentData().putInt("soulLampTimeout", player.getPersistentData().getInt("soulLampTimeout") - 1);
            }
        }
    }

    @SubscribeEvent
    public static void onSleep(SleepFinishedTimeEvent event)
    {
        event.getWorld().getPlayers().forEach(player ->
        {
            if (player.isSleeping())
            {
                Temperature temp = TempHelper.getTemperature(player, Temperature.Types.CORE);
                TempHelper.setTemperature(player, new Temperature(temp.get() / 4), Temperature.Types.CORE);
            }
        });
    }
}