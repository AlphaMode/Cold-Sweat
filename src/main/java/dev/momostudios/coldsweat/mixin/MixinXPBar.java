package dev.momostudios.coldsweat.mixin;

import com.mojang.blaze3d.matrix.MatrixStack;
import dev.momostudios.coldsweat.ColdSweat;
import dev.momostudios.coldsweat.config.ClientSettingsConfig;
import net.minecraft.client.gui.IngameGui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(IngameGui.class)
public class MixinXPBar
{
    @Inject(method = "renderExpBar(Lcom/mojang/blaze3d/matrix/MatrixStack;I)V",
            at = @At
            (
                value = "INVOKE",
                target = "Lnet/minecraft/profiler/IProfiler;startSection(Ljava/lang/String;)V",
                shift = At.Shift.AFTER
            ),
            slice = @Slice
            (
                from = @At(value = "INVOKE", target = "Lnet/minecraft/profiler/IProfiler;endSection()V"),
                to   = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/FontRenderer;getStringWidth(Ljava/lang/String;)I")
            ),
            remap = ColdSweat.REMAP_MIXINS)
    public void renderExperienceBar1(MatrixStack poseStack, int xPos, CallbackInfo ci)
    {
        // Render XP bar
        if (ClientSettingsConfig.getInstance().customHotbar())
        {
            poseStack.translate(0.0D, 4.0D, 0.0D);
        }
    }

    @Inject(method = "renderExpBar(Lcom/mojang/blaze3d/matrix/MatrixStack;I)V",
            at = @At
            (
                value = "INVOKE",
                target = "Lnet/minecraft/profiler/IProfiler;endSection()V"
            ),
            slice = @Slice
            (
                from = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/FontRenderer;getStringWidth(Ljava/lang/String;)I"),
                to   = @At(value = "RETURN")
            ),
            remap = ColdSweat.REMAP_MIXINS)
    public void renderExperienceBar2(MatrixStack poseStack, int xPos, CallbackInfo ci)
    {
        // Render XP bar
        if (ClientSettingsConfig.getInstance().customHotbar())
        {
            poseStack.translate(0.0D, -4.0D, 0.0D);
        }
    }

    @Mixin(IngameGui.class)
    public static class MixinItemLabel
    {
        @Inject(method = "renderItemName(Lcom/mojang/blaze3d/matrix/MatrixStack;)V",
                at = @At
                (
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/systems/RenderSystem;pushMatrix()V",
                    shift = At.Shift.AFTER
                ), remap = ColdSweat.REMAP_MIXINS)
        public void renderItemNamePre(MatrixStack matrixStack, CallbackInfo ci)
        {
            if (ClientSettingsConfig.getInstance().customHotbar())
            {
                matrixStack.translate(0, -4, 0);
            }
        }

        @Inject(method = "renderItemName(Lcom/mojang/blaze3d/matrix/MatrixStack;)V",
                at = @At
                (
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/systems/RenderSystem;popMatrix()V",
                    shift = At.Shift.BEFORE
                ), remap = ColdSweat.REMAP_MIXINS)
        public void renderItemNamePost(MatrixStack matrixStack, CallbackInfo ci)
        {
            if (ClientSettingsConfig.getInstance().customHotbar())
            {
                matrixStack.translate(0, 4, 0);
            }
        }
    }
}
