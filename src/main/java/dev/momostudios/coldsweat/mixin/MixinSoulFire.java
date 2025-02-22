package dev.momostudios.coldsweat.mixin;

import dev.momostudios.coldsweat.ColdSweat;
import dev.momostudios.coldsweat.config.ColdSweatConfig;
import dev.momostudios.coldsweat.util.compat.CompatManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.SoulFireBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BaseFireBlock.class)
public class MixinSoulFire
{
    @Inject(method = "entityInside(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/Entity;)V",
    at = @At("HEAD"), cancellable = true, remap = ColdSweat.REMAP_MIXINS)
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, CallbackInfo ci)
    {
        if (state.getBlock() instanceof SoulFireBlock && ColdSweatConfig.getInstance().isSoulFireCold()
        && !(entity instanceof ItemEntity && CompatManager.isSpiritLoaded()))
        {
            entity.setIsInPowderSnow(true);
            entity.clearFire();
            ci.cancel();
        }
    }
}
