package dev.momostudios.coldsweat.common.event;

import dev.momostudios.coldsweat.ColdSweat;
import dev.momostudios.coldsweat.api.temperature.Temperature;
import dev.momostudios.coldsweat.api.temperature.modifier.InsulationTempModifier;
import dev.momostudios.coldsweat.api.temperature.modifier.TempModifier;
import dev.momostudios.coldsweat.config.ItemSettingsConfig;
import dev.momostudios.coldsweat.util.config.ConfigOption;
import dev.momostudios.coldsweat.util.config.ConfigHelper;
import dev.momostudios.coldsweat.util.config.LoadedValue;
import dev.momostudios.coldsweat.util.entity.TempHelper;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.Map;

@Mod.EventBusSubscriber(modid = ColdSweat.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ArmorInsulation
{
    public static LoadedValue<Map<Item, Number>> INSULATING_ARMORS = LoadedValue.of(() ->
            ConfigHelper.getItemsWithValues(ItemSettingsConfig.getInstance().insulatingArmor()));

    @SubscribeEvent
    public static void addLeatherModifiers(TickEvent.PlayerTickEvent event)
    {
        PlayerEntity player = event.player;
        if (event.phase == TickEvent.Phase.END && !player.world.isRemote && player.ticksExisted % 10 == 0)
        {
            Map<Item, Number> insulatingArmors = INSULATING_ARMORS.get();

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

            if (insulation > 0)
            {
                TempModifier modifier = TempHelper.getModifier(player, Temperature.Types.RATE, InsulationTempModifier.class);

                if (modifier != null)
                    modifier.setArgument("warmth", insulation);
                else
                    TempHelper.replaceModifier(player, new InsulationTempModifier(insulation).expires(10).tickRate(10), Temperature.Types.RATE);
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