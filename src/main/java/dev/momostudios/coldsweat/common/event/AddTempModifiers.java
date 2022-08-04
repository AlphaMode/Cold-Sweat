package dev.momostudios.coldsweat.common.event;

import dev.momostudios.coldsweat.api.temperature.Temperature;
import dev.momostudios.coldsweat.api.temperature.modifier.*;
import dev.momostudios.coldsweat.api.registry.TempModifierRegistry;
import dev.momostudios.coldsweat.common.capability.ModCapabilities;
import dev.momostudios.coldsweat.api.util.TempHelper;
import dev.momostudios.coldsweat.util.registries.ModEffects;
import dev.momostudios.coldsweat.util.world.WorldHelper;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.potion.EffectInstance;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.PotionEvent;
import net.minecraftforge.event.world.SleepFinishedTimeEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class AddTempModifiers
{
    @SubscribeEvent
    public static void onPlayerCreated(EntityJoinWorldEvent event)
    {
        if (event.getEntity() instanceof PlayerEntity && !event.getWorld().isRemote)
        {
            WorldHelper.schedule(() ->
            {
                PlayerEntity player = (PlayerEntity) event.getEntity();

                TempHelper.addModifier(player, new BiomeTempModifier().tickRate(8), Temperature.Type.WORLD, false);
                TempHelper.addModifier(player, new TimeTempModifier().tickRate(20), Temperature.Type.WORLD, false);
                if (ModList.get().isLoaded("sereneseasons"))
                    TempHelper.addModifier(player, TempModifierRegistry.get("sereneseasons:season").tickRate(20), Temperature.Type.WORLD, false);
                else if (ModList.get().isLoaded("betterweather"))
                    TempHelper.addModifier(player, TempModifierRegistry.get("betterweather:season").tickRate(20), Temperature.Type.WORLD, false);
                TempHelper.addModifier(player, new DepthTempModifier().tickRate(10), Temperature.Type.WORLD, false);
                TempHelper.addModifier(player, new BlockTempModifier().tickRate(5),  Temperature.Type.WORLD, false);
            }, 10);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event)
    {
        PlayerEntity player = event.player;

        if (player.world.isRemote && player.ticksExisted % 5 == 0 && player.isInWaterRainOrBubbleColumn())
        {
            TempHelper.addModifier(player, new WaterTempModifier(0.01), Temperature.Type.WORLD, false);
        }
    }

    /**
     * Check for updates to the player's Insulation effect
     */
    @SubscribeEvent
    public static void onInsulationUpdate(PotionEvent event)
    {
        if (!event.getEntity().world.isRemote && event.getEntity() instanceof PlayerEntity && event.getPotionEffect() != null
        && event.getPotionEffect().getPotion() == ModEffects.INSULATION)
        {
            PlayerEntity player = (PlayerEntity) event.getEntity();
            if (event instanceof PotionEvent.PotionAddedEvent)
            {
                EffectInstance effect = event.getPotionEffect();
                TempHelper.replaceModifier(player, new HearthTempModifier(effect.getAmplifier() + 1).expires(effect.getDuration()), Temperature.Type.WORLD);
            }
            else if (event instanceof PotionEvent.PotionRemoveEvent)
            {
                TempHelper.removeModifiers(player, Temperature.Type.WORLD, 1, mod -> mod instanceof HearthTempModifier);
            }
        }
    }

    @SubscribeEvent
    public static void onSleep(SleepFinishedTimeEvent event)
    {
        if (!event.getWorld().isRemote())
        {
            event.getWorld().getPlayers().forEach(player ->
            {
                if (player.isSleeping())
                {
                    player.getCapability(ModCapabilities.PLAYER_TEMPERATURE).ifPresent(cap ->
                    {
                        double temp = cap.get(Temperature.Type.CORE);
                        cap.set(Temperature.Type.CORE, temp / 4d);
                        TempHelper.updateTemperature(player, cap, true);
                    });
                }
            });
        }
    }
}
