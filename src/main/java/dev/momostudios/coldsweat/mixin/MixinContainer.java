package dev.momostudios.coldsweat.mixin;

import dev.momostudios.coldsweat.ColdSweat;
import dev.momostudios.coldsweat.config.ItemSettingsConfig;
import dev.momostudios.coldsweat.util.config.ConfigEntry;
import dev.momostudios.coldsweat.util.registries.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.container.ClickType;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Container.class)
public class MixinContainer
{
    Container menu = (Container) (Object) this;

    @Inject(method = "func_241440_b_(IILnet/minecraft/inventory/container/ClickType;Lnet/minecraft/entity/player/PlayerEntity;)Lnet/minecraft/item/ItemStack;", at = @At("HEAD"),
    remap = ColdSweat.remapMixins, cancellable = true)
    public void slotClicked(int slotId, int mouseButton, ClickType clickType, PlayerEntity player, CallbackInfoReturnable<ItemStack> ci)
    {
        try
        {
            Slot slot = menu.getSlot(slotId);
            ItemStack stack = slot.getStack();
            if (stack.getItem() == ModItems.HELLSPRING_LAMP)
            {
                double fuel = stack.getOrCreateTag().getDouble("fuel");
                ItemStack holdingStack = player.inventory.getItemStack();
                if (fuel <= 63 && clickType == ClickType.PICKUP && getItemEntry(holdingStack).value > 0)
                {
                    stack.getOrCreateTag().putDouble("fuel", Math.min(64, fuel + (mouseButton == 1 ? 1 : holdingStack.getCount())));
                    holdingStack.shrink(mouseButton == 1 ? 1 : 64 - (int) fuel);
                    ci.setReturnValue(holdingStack);
                }
            }
        } catch (Exception e) {}
    }

    private static ConfigEntry getItemEntry(ItemStack stack)
    {
        for (String entry : ItemSettingsConfig.getInstance().soulLampItems())
        {
            if (entry.equals(ForgeRegistries.ITEMS.getKey(stack.getItem()).toString()))
            {
                return new ConfigEntry(entry, 1);
            }
        }
        return new ConfigEntry("minecraft:air", 0);
    }
}
