package dev.momostudios.coldsweat.common.event;

import dev.momostudios.coldsweat.api.temperature.Temperature;
import dev.momostudios.coldsweat.config.ItemSettingsConfig;
import dev.momostudios.coldsweat.util.config.ConfigHelper;
import dev.momostudios.coldsweat.util.config.LoadedValue;
import dev.momostudios.coldsweat.api.util.TempHelper;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import dev.momostudios.coldsweat.api.temperature.modifier.FoodTempModifier;

import java.util.Map;

@Mod.EventBusSubscriber
public class PlayerEatFood
{
    public static LoadedValue<Map<Item, Number>> VALID_FOODS = LoadedValue.of(() -> ConfigHelper.getItemsWithValues(ItemSettingsConfig.getInstance().temperatureFoods()));

    @SubscribeEvent
    public static void onEatFood(LivingEntityUseItemEvent.Finish event)
    {
        if (event.getEntityLiving() instanceof PlayerEntity && event.getItem().isFood() && !event.getEntityLiving().world.isRemote)
        {
            double foodTemp = VALID_FOODS.get().getOrDefault(event.getItem().getItem(), 0).doubleValue();
            if (foodTemp != 0)
            {
                PlayerEntity player = (PlayerEntity) event.getEntityLiving();
                TempHelper.addModifier(player, new FoodTempModifier(foodTemp).expires(1), Temperature.Type.CORE, true);
            }
        }
    }
}
