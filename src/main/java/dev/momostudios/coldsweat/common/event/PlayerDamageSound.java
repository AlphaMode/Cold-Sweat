package dev.momostudios.coldsweat.common.event;

import dev.momostudios.coldsweat.util.registries.ModSounds;
import dev.momostudios.coldsweat.util.world.WorldHelper;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.SoundCategory;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import dev.momostudios.coldsweat.util.entity.ModDamageSources;

@Mod.EventBusSubscriber
public class PlayerDamageSound
{
    @SubscribeEvent
    public static void onPlayerHurt(LivingHurtEvent event)
    {
        if (event.getSource().equals(ModDamageSources.COLD))
        {
            if (event.getEntity() instanceof PlayerEntity && !event.getEntity().world.isRemote)
            {
                WorldHelper.playEntitySound(ModSounds.FREEZE, event.getEntity(), SoundCategory.PLAYERS, 2f, (float) Math.random() * 0.3f + 0.85f);
            }
        }
    }
}
