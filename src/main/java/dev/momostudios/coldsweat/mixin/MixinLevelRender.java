package dev.momostudios.coldsweat.mixin;

import com.mojang.blaze3d.matrix.MatrixStack;
import dev.momostudios.coldsweat.ColdSweat;
import dev.momostudios.coldsweat.api.event.client.RenderWorldEvent;
import net.minecraft.client.renderer.*;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = WorldRenderer.class, priority = 1001)
public class MixinLevelRender
{
    @Shadow
    private ViewFrustum viewFrustum;

    @Inject(method = "updateCameraAndRender", at = @At("HEAD"), remap = ColdSweat.REMAP_MIXINS, cancellable = true)
    void renderLevel(MatrixStack ps, float partialTicks, long finishTimeNano, boolean renderBlockOutline, ActiveRenderInfo camera,
                     GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f matrix4f, CallbackInfo ci)
    {
        RenderWorldEvent event = new RenderWorldEvent(ps, partialTicks, finishTimeNano, renderBlockOutline, camera,
                (WorldRenderer) (Object) this, gameRenderer, lightTexture, matrix4f, viewFrustum);
        MinecraftForge.EVENT_BUS.post(event);
        if (event.isCanceled()) ci.cancel();
    }
}
