package net.momostudios.coldsweat.core.event;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.potion.Effects;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.momostudios.coldsweat.ColdSweat;
import net.momostudios.coldsweat.common.temperature.Temperature;
import net.momostudios.coldsweat.common.temperature.modifier.TempModifier;
import net.momostudios.coldsweat.config.ColdSweatConfig;
import net.momostudios.coldsweat.config.ConfigCache;
import net.momostudios.coldsweat.core.util.CustomDamageTypes;
import net.momostudios.coldsweat.core.util.registrylists.ModEffects;
import net.momostudios.coldsweat.core.util.PlayerTemp;

import java.util.List;

@Mod.EventBusSubscriber(modid = ColdSweat.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class PlayerTempUpdater
{
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event)
    {
        if (event.phase == TickEvent.Phase.END)
        {
            PlayerEntity player = event.player;

            /*
             * Runs the calculate() method for every TempModifier on the player
             */
            double ambientTemp = new Temperature().with(PlayerTemp.getModifiers(player, PlayerTemp.Types.AMBIENT), player).get();
            double bodyTemp = PlayerTemp.getTemperature(player, PlayerTemp.Types.BODY).get();
            ConfigCache config = ConfigCache.getInstance();

            PlayerTemp.setTemperature(player, new Temperature(ambientTemp), PlayerTemp.Types.AMBIENT);

            double maxTemp = config.maxTemp;
            double minTemp = config.minTemp;

            Temperature temp = PlayerTemp.getTemperature(player, PlayerTemp.Types.BODY);

            //Increase body temperature when ambientTemp is above maximum (with rate modifiers)
            if (ambientTemp > maxTemp && !player.isCreative())
            {
                temp.add((float) new Temperature((float) Math.abs(maxTemp - ambientTemp) / 7f)
                        .with(PlayerTemp.getModifiers(player, PlayerTemp.Types.RATE), player).get() * config.rate);
            }
            //Return the player's temperature back to 0
            else if (bodyTemp > 0)
            {
                temp.add((float) -Math.min(Math.max(0.1, Math.abs(ambientTemp - maxTemp) / 5f) * config.rate, bodyTemp));
            }

            //Decrease body temperature when ambientTemp is below minimum (with rate modifiers)
            if (ambientTemp < minTemp && !player.isCreative())
            {
                temp.add((float) -new Temperature((float) Math.abs(minTemp - ambientTemp) / 7f)
                        .with(PlayerTemp.getModifiers(player, PlayerTemp.Types.RATE), player).get() * config.rate);
            }
            //Return the player's temperature back to 0
            else if (bodyTemp < 0)
            {
                temp.add((float) Math.min(Math.max(0.1, Math.abs(ambientTemp - minTemp) / 5f) * config.rate, -bodyTemp));
            }

            //Calculates the player's temperature
            PlayerTemp.setTemperature
            (
                player,
                temp.with(PlayerTemp.getModifiers(player, PlayerTemp.Types.BODY), player),
                PlayerTemp.Types.BODY
            );
            PlayerTemp.setTemperature
            (
                player,
                new Temperature().with(PlayerTemp.getModifiers(player, PlayerTemp.Types.BASE), player),
                PlayerTemp.Types.BASE
            );
            PlayerTemp.setTemperature
            (
                player,
                PlayerTemp.getTemperature(player, PlayerTemp.Types.BASE).with(PlayerTemp.getModifiers(player, PlayerTemp.Types.BASE), player).add(
                        PlayerTemp.getTemperature(player, PlayerTemp.Types.BODY).with(PlayerTemp.getModifiers(player, PlayerTemp.Types.BODY), player)),
                PlayerTemp.Types.COMPOSITE
            );

            //Ensure a maximum and minimum cap of 150 or -150 for body temperature (does not include base offset)
            if (PlayerTemp.getTemperature(player, PlayerTemp.Types.BODY).get() > 150)
            {
                PlayerTemp.setTemperature(player, new Temperature(150), PlayerTemp.Types.BODY);
            }
            if (PlayerTemp.getTemperature(player, PlayerTemp.Types.BODY).get() < -150)
            {
                PlayerTemp.setTemperature(player, new Temperature(-150), PlayerTemp.Types.BODY);
            }

            //Deal damage to the player if temperature is critical
            boolean hasFireResistance = player.isPotionActive(Effects.FIRE_RESISTANCE) && config.fireRes;
            boolean hasIceResistance = player.isPotionActive(ModEffects.ICE_RESISTANCE) && config.iceRes;
            if (player.ticksExisted % 40 == 0)
            {
                boolean scales = config.damageScaling;
                if (PlayerTemp.getTemperature(player, PlayerTemp.Types.COMPOSITE).get() >= 100 && !hasFireResistance && !player.isPotionActive(ModEffects.GRACE))
                {
                    player.attackEntityFrom(scales ? CustomDamageTypes.HOT_SCALED : CustomDamageTypes.HOT, 2);
                }
                if (PlayerTemp.getTemperature(player, PlayerTemp.Types.COMPOSITE).get() <= -100 && !hasIceResistance && !player.isPotionActive(ModEffects.GRACE))
                {
                    player.attackEntityFrom(scales ? CustomDamageTypes.COLD_SCALED : CustomDamageTypes.COLD, 2);
                }
            }
        }
    }

    @SubscribeEvent
    public static void worldTickEvent(TickEvent.WorldTickEvent event)
    {
        if (!event.world.isRemote && event.world.getGameTime() % 20 == 0)
            ConfigCache.getInstance().writeValues(ColdSweatConfig.getInstance());
    }
}