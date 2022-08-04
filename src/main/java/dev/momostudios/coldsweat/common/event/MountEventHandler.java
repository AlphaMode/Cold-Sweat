package dev.momostudios.coldsweat.common.event;

import dev.momostudios.coldsweat.api.temperature.Temperature;
import net.minecraft.entity.item.minecart.MinecartEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import dev.momostudios.coldsweat.api.temperature.modifier.MountTempModifier;
import dev.momostudios.coldsweat.config.EntitySettingsConfig;
import dev.momostudios.coldsweat.core.init.BlockInit;
import dev.momostudios.coldsweat.api.util.TempHelper;

import java.util.List;

@Mod.EventBusSubscriber
public class MountEventHandler
{
    @SubscribeEvent
    public static void playerRiding(TickEvent.PlayerTickEvent event)
    {
        if (event.phase == TickEvent.Phase.END)
        {
            PlayerEntity player = event.player;
            if (player.getRidingEntity() != null)
            {
                if (player.getRidingEntity() instanceof MinecartEntity && ((MinecartEntity) player.getRidingEntity()).getDisplayTile().getBlock() == BlockInit.MINECART_INSULATION.get())
                {
                    TempHelper.addModifier(player, new MountTempModifier(1).expires(1), Temperature.Type.RATE, false);
                }
                else
                {
                    for (List<Object> entity : EntitySettingsConfig.INSTANCE.insulatedEntities())
                    {
                        if (player.getRidingEntity().getType().getRegistryName().toString().equals(entity.get(0)))
                        {
                            Number number = (Number) entity.get(1);
                            double value = number.doubleValue();
                            TempHelper.addModifier(player, new MountTempModifier(value).expires(1), Temperature.Type.RATE, false);
                        }
                    }
                }
            }
        }
    }
}
