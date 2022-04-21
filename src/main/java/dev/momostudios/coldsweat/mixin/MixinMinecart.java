package dev.momostudios.coldsweat.mixin;

import dev.momostudios.coldsweat.ColdSweat;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.item.minecart.AbstractMinecartEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.world.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractMinecartEntity.class)
public class MixinMinecart
{
    AbstractMinecartEntity minecart = (AbstractMinecartEntity) (Object) this;

    @Inject(method = "killMinecart(Lnet/minecraft/util/DamageSource;)V", at = @At("HEAD"), remap = ColdSweat.remapMixins)
    public void killMinecart(DamageSource source, CallbackInfo ci)
    {
        Block block = minecart.getDisplayTile().getBlock();

        if (minecart.world.getGameRules().getBoolean(GameRules.DO_ENTITY_DROPS)
        && block != Blocks.HOPPER
        && block != Blocks.CHEST
        && block != Blocks.TNT
        && block != Blocks.FURNACE)
        {
            ItemStack itemStack = new ItemStack(block.asItem());
            minecart.entityDropItem(itemStack);
        }
    }
}
