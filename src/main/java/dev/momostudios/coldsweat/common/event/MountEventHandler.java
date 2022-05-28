package dev.momostudios.coldsweat.common.event;

import dev.momostudios.coldsweat.api.temperature.Temperature;
import dev.momostudios.coldsweat.util.registries.ModItems;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.minecart.MinecartEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import dev.momostudios.coldsweat.api.temperature.modifier.MountTempModifier;
import dev.momostudios.coldsweat.config.EntitySettingsConfig;
import dev.momostudios.coldsweat.core.init.BlockInit;
import dev.momostudios.coldsweat.util.entity.TempHelper;

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
                    TempHelper.addModifier(player, new MountTempModifier(1).expires(1), Temperature.Types.RATE, false);
                }
                else
                {
                    for (List<Object> entity : EntitySettingsConfig.INSTANCE.insulatedEntities())
                    {
                        if (player.getRidingEntity().getType().getRegistryName().toString().equals(entity.get(0)))
                        {
                            Number number = (Number) entity.get(1);
                            double value = number.doubleValue();
                            TempHelper.addModifier(player, new MountTempModifier(value).expires(1), Temperature.Types.RATE, false);
                        }
                    }
                }
            }
        }
    }
}
