package dev.momostudios.coldsweat.mixin;

import dev.momostudios.coldsweat.ColdSweat;
import dev.momostudios.coldsweat.api.event.common.BlockChangedEvent;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Called on the server side when a block state is changed.<br>
 * This event is not {@link net.minecraftforge.eventbus.api.Cancelable}.<br>
 * <br>
 * {@link net.minecraftforge.event.world.BlockEvent.NeighborNotifyEvent} is only called on break/place. <br>
 * It doesn't trigger when a state is updated, nor does it know what the previous state was.<br>
 */
@Mixin(ServerWorld.class)
public class MixinBlockUpdate
{
    World world = (World) (Object) this;

    @Inject(method = "onBlockStateChange(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;Lnet/minecraft/block/BlockState;)V",
            at = @At("HEAD"), remap = ColdSweat.REMAP_MIXINS)
    private void onBlockUpdate(BlockPos pos, BlockState oldState, BlockState newState, CallbackInfo ci)
    {
        if (world.getChunkProvider().isChunkLoaded(new ChunkPos(pos)) && oldState != newState)
        {
            BlockChangedEvent event = new BlockChangedEvent(pos, oldState, newState, (ServerWorld) (Object) this);
            MinecraftForge.EVENT_BUS.post(event);
        }
    }
}
