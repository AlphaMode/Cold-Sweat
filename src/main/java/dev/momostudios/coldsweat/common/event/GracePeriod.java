package dev.momostudios.coldsweat.common.event;

import dev.momostudios.coldsweat.util.config.ConfigSettings;
import dev.momostudios.coldsweat.util.registries.ModEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.potion.EffectInstance;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class GracePeriod
{
    @SubscribeEvent
    public static void onSpawn(EntityJoinWorldEvent event)
    {
        if (event.getEntity() instanceof PlayerEntity && ConfigSettings.getInstance().graceEnabled)
        {
            if (!event.getWorld().isRemote && !event.getEntity().getPersistentData().getBoolean("givenGracePeriod"))
            {
                event.getEntity().getPersistentData().putBoolean("givenGracePeriod", true);
                ((PlayerEntity) event.getEntity()).addPotionEffect(new EffectInstance(ModEffects.GRACE, ConfigSettings.getInstance().graceLength, 0, false, false, true));
            }
        }
    }
}
