package dev.momostudios.coldsweat.common.event;

import dev.momostudios.coldsweat.api.temperature.Temperature;
import dev.momostudios.coldsweat.util.config.ConfigCache;
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
        // There's already something blocking the player from sleeping
        if (event.getResultStatus() != null) return;

        PlayerEntity player = event.getPlayer();
        double bodyTemp = TempHelper.getTemperature(player, Temperature.Types.BODY).get();
        double worldTemp = TempHelper.getTemperature(player, Temperature.Types.WORLD).get();
        double minTemp = ConfigCache.getInstance().minTemp + TempHelper.getTemperature(player, Temperature.Types.MIN).get();
        double maxTemp = ConfigCache.getInstance().maxTemp + TempHelper.getTemperature(player, Temperature.Types.MAX).get();

        // If the player's body temperature is critical
        if (!CSMath.isBetween(bodyTemp, -100, 100))
        {
            player.sendStatusMessage(new TranslationTextComponent("cold_sweat.message.sleep.body." + (bodyTemp > 99 ? "hot" : "cold")), true);
            event.setResult(PlayerEntity.SleepResult.OTHER_PROBLEM);
        }
        // If the player's world temperature is critical
        else if (!CSMath.isInRange(worldTemp, minTemp, maxTemp))
        {
            player.sendStatusMessage(new TranslationTextComponent("cold_sweat.message.sleep.world." + (worldTemp > maxTemp ? "hot" : "cold")), true);
            event.setResult(PlayerEntity.SleepResult.OTHER_PROBLEM);
        }
    }
}
