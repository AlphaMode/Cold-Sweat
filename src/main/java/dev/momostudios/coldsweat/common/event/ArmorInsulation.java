package dev.momostudios.coldsweat.common.event;

import dev.momostudios.coldsweat.api.temperature.Temperature;
import dev.momostudios.coldsweat.api.temperature.modifier.InsulationTempModifier;
import dev.momostudios.coldsweat.api.temperature.modifier.TempModifier;
import dev.momostudios.coldsweat.util.config.ConfigSettings;
import dev.momostudios.coldsweat.api.util.TempHelper;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;

@Mod.EventBusSubscriber
public class ArmorInsulation
{
    @SubscribeEvent
    public static void addLeatherModifiers(TickEvent.PlayerTickEvent event)
    {
        PlayerEntity player = event.player;
        if (event.phase == TickEvent.Phase.END && !player.world.isRemote && player.ticksExisted % 10 == 0)
        {
            Map<Item, Double> insulatingArmors = ConfigSettings.INSULATING_ARMORS.get();

            int insulation = 0;
            for (EquipmentSlotType slot : EquipmentSlotType.values())
            {
                if (slot == EquipmentSlotType.MAINHAND || slot == EquipmentSlotType.OFFHAND) continue;

                ItemStack armorStack = player.getItemStackFromSlot(slot);
                if (armorStack.getItem() instanceof ArmorItem)
                {
                    // Add the armor's defense value to the insulation value.
                    insulation += ((ArmorItem) armorStack.getItem()).getDamageReduceAmount();

                    // Add the armor's intrinsic insulation value (defined in configs)
                    // Mutually exclusive with Sewing Table insulation
                    Number insulationValue = insulatingArmors.get(armorStack.getItem());
                    if (insulationValue != null)
                    {
                        insulation += insulationValue.intValue();
                    }
                    // Add the armor's insulation value from the Sewing Table
                    else if (armorStack.getOrCreateTag().getBoolean("insulated"))
                    {
                        insulation += getSlotWeight(slot);
                    }
                }
            }

            TempModifier currentMod = TempHelper.getModifier(player, Temperature.Type.RATE, InsulationTempModifier.class);
            if (currentMod == null || (int) currentMod.getArgument("warmth") != insulation)
            {
                if (insulation == 0 && currentMod != null)
                    TempHelper.removeModifiers(player, Temperature.Type.RATE, (mod) -> mod instanceof InsulationTempModifier);
                else
                    TempHelper.replaceModifier(player, new InsulationTempModifier(insulation).tickRate(10), Temperature.Type.RATE);
            }
        }
    }

    static int getSlotWeight(EquipmentSlotType slot)
    {
        switch (slot)
        {
            case HEAD  : return 4;
            case CHEST : return 7;
            case LEGS  : return 6;
            case FEET  : return 3;
            default    : return 0;
        }
    }
}