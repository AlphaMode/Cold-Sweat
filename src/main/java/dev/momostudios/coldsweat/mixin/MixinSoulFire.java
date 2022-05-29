package dev.momostudios.coldsweat.mixin;

import dev.momostudios.coldsweat.ColdSweat;
import dev.momostudios.coldsweat.config.ColdSweatConfig;
import dev.momostudios.coldsweat.util.entity.ModDamageSources;
import dev.momostudios.coldsweat.util.registries.ModSounds;
import dev.momostudios.coldsweat.util.world.WorldHelper;
import net.minecraft.block.AbstractFireBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.SoulFireBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractFireBlock.class)
public class MixinSoulFire
{
    @Inject(method = "onEntityCollision(Lnet/minecraft/block/BlockState;Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/entity/Entity;)V",
    at = @At("HEAD"), cancellable = true, remap = ColdSweat.REMAP_MIXINS)
    public void entityInside(BlockState state, World level, BlockPos pos, Entity entity, CallbackInfo ci)
    {
        if (state.getBlock() instanceof SoulFireBlock && ColdSweatConfig.getInstance().isSoulFireCold())
        {
            entity.extinguish();
            ci.cancel();
            if (entity.attackEntityFrom(ModDamageSources.COLD.setDamageBypassesArmor(), 1.0F))
            {
                WorldHelper.playEntitySound(ModSounds.FREEZE, entity, 1.0F, (float) Math.random() * 0.2f + 0.9f);
            }
        }
    }
}
