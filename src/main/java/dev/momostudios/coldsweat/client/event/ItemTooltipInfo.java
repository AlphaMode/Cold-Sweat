package dev.momostudios.coldsweat.client.event;

import dev.momostudios.coldsweat.api.temperature.Temperature;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.BucketItem;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import dev.momostudios.coldsweat.ColdSweat;
import dev.momostudios.coldsweat.config.ClientSettingsConfig;
import dev.momostudios.coldsweat.util.math.CSMath;
import dev.momostudios.coldsweat.util.registries.ModItems;

@Mod.EventBusSubscriber
public class ItemTooltipInfo
{
    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void addTTInfo(ItemTooltipEvent event)
    {
        if (event.getItemStack().getItem() == ModItems.FILLED_WATERSKIN && event.getPlayer() != null)
        {
            boolean celsius = ClientSettingsConfig.getInstance().celsius();
            double temp = event.getItemStack().getOrCreateTag().getDouble("temperature");
            String color = temp == 0 ? "7" : (temp < 0 ? "9" : "c");
            String tempUnits = celsius ? "C" : "F";
            temp = temp / 2 + 95;
            if (celsius) temp = CSMath.convertUnits(temp, Temperature.Units.F, Temperature.Units.C, true);
            temp += ClientSettingsConfig.getInstance().tempOffset() / 2.0;

            event.getToolTip().add(1, new StringTextComponent("\u00a77" + new TranslationTextComponent(
                "item." + ColdSweat.MOD_ID + ".waterskin.filled").getString() + " (\u00a7" + color + (int) temp + " \u00b0" + tempUnits + "\u00a77)\u00a7r"));
        }
        else if ((event.getItemStack().getItem() instanceof ArmorItem || event.getItemStack().getItem() instanceof BucketItem) &&
                  event.getItemStack().getOrCreateTag().getBoolean("insulated"))
        {
            event.getToolTip().add(1, new StringTextComponent("\u00a7d" +
                new TranslationTextComponent("modifier." + ColdSweat.MOD_ID + ".insulated").getString() + "\u00a7r"));
        }
        else if (event.getItemStack().getItem() == ModItems.SOULSPRING_LAMP)
        {
            event.getToolTip().add(1, new StringTextComponent("             "));
            if (Minecraft.getInstance().gameSettings.advancedItemTooltips)
            {
                event.getToolTip().add(Math.max(event.getToolTip().size() - 2, 2),
                                       new StringTextComponent("§fFuel: " + (int) event.getItemStack().getOrCreateTag().getDouble("fuel") + " / " + 64));
            }
        }
    }
}
