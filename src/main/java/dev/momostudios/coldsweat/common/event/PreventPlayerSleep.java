package dev.momostudios.coldsweat.common.event;

import dev.momostudios.coldsweat.api.temperature.Temperature;
import dev.momostudios.coldsweat.config.ConfigCache;
import dev.momostudios.coldsweat.util.entity.TempHelper;
import dev.momostudios.coldsweat.util.math.CSMath;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.event.entity.player.PlayerSleepInBedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class PreventPlayerSleep
{
    @SubscribeEvent
    public static void onTrySleep(PlayerSleepInBedEvent event)
    {
        PlayerEntity player = event.getPlayer();
        double bodyTemp = TempHelper.getTemperature(player, Temperature.Types.BODY).get();
        double worldTemp = TempHelper.getTemperature(player, Temperature.Types.WORLD).get();
        double minTemp = ConfigCache.getInstance().minTemp + TempHelper.getTemperature(player, Temperature.Types.MIN).get();
        double maxTemp = ConfigCache.getInstance().maxTemp + TempHelper.getTemperature(player, Temperature.Types.MAX).get();

        if (!CSMath.isBetween((int) bodyTemp, -99, 99))
        {
            player.sendStatusMessage(new TranslationTextComponent("cold_sweat.message.sleep.body",
            new TranslationTextComponent(bodyTemp > 99 ? "cold_sweat.message.sleep.hot" : "cold_sweat.message.sleep.cold").getString()), true);
            event.getPlayer().swingArm(event.getPlayer().swingingHand);
            event.setCanceled(true);
        }
        else if (!CSMath.isBetween(worldTemp, minTemp, maxTemp))
        {
            player.sendStatusMessage(new TranslationTextComponent("cold_sweat.message.sleep.world",
            new TranslationTextComponent(worldTemp > maxTemp ? "cold_sweat.message.sleep.hot" : "cold_sweat.message.sleep.cold").getString()), true);
            event.getPlayer().swingArm(event.getPlayer().swingingHand);
            event.setCanceled(true);
        }
    }
}
