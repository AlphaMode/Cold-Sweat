package dev.momostudios.coldsweat.mixin;

import dev.momostudios.coldsweat.ColdSweat;
import net.minecraft.entity.item.minecart.AbstractMinecartEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.DamageSource;
import net.minecraft.world.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractMinecartEntity.class)
public class MixinMinecart
{
    AbstractMinecartEntity minecart = (AbstractMinecartEntity) (Object) this;

    @Inject(method = "attackEntityFrom(Lnet/minecraft/util/DamageSource;F)Z",
            at = @At
            (
                value = "INVOKE",
                target = "Lnet/minecraft/entity/item/minecart/AbstractMinecartEntity;killMinecart(Lnet/minecraft/util/DamageSource;)V"
            ),
            remap = ColdSweat.REMAP_MIXINS,
            cancellable = true)
    public void killMinecart(DamageSource source, float amount, CallbackInfoReturnable<Boolean> ci)
    {
        ItemStack carryStack = minecart.getDisplayTile().getBlock().asItem().getDefaultInstance();
        if (!carryStack.isEmpty())
        {
            if (minecart.world.getGameRules().getBoolean(GameRules.DO_ENTITY_DROPS))
            {
                ItemStack itemstack = new ItemStack(Items.MINECART);
                if (minecart.hasCustomName())
                {
                    itemstack.setDisplayName(minecart.getCustomName());
                }

                minecart.entityDropItem(itemstack);
                minecart.entityDropItem(carryStack);
            }
            minecart.remove();
            ci.cancel();
        }
    }
}
