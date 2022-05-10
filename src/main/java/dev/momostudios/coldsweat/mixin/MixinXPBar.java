package dev.momostudios.coldsweat.mixin;

import com.mojang.blaze3d.matrix.MatrixStack;
import dev.momostudios.coldsweat.ColdSweat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.IngameGui;
import dev.momostudios.coldsweat.client.event.RearrangeHotbar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(IngameGui.class)
public class MixinXPBar
{
    IngameGui gui = (IngameGui) (Object) this;

    @Shadow
    protected int scaledWidth;
    @Shadow
    protected int scaledHeight;

    /**
     * @author iMikul
     * @reason Move XP bar elements to make room for body temperature readout
     */
    @Inject(method = "renderExpBar(Lcom/mojang/blaze3d/matrix/MatrixStack;I)V",
            at = @At
                    (
                            value = "INVOKE",
                            target = "Lnet/minecraft/profiler/IProfiler;startSection(Ljava/lang/String;)V", shift = At.Shift.AFTER
                    ),
            slice = @Slice
                    (
                            from = @At(value = "INVOKE", target = "Lnet/minecraft/profiler/IProfiler;endSection()V"),
                            to   = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/FontRenderer;getStringWidth(Ljava/lang/String;)I")
                    ),
            cancellable = true,
            remap = ColdSweat.remapMixins)
    public void renderExperienceBar(MatrixStack matrixStack, int xPos, CallbackInfo ci)
    {
        Minecraft mc = Minecraft.getInstance();
        FontRenderer font = gui.getFontRenderer();

        // Render XP bar
        if (RearrangeHotbar.customHotbar)
        {
            String s = "" + mc.player.experienceLevel;
            int i1 = (scaledWidth - font.getStringWidth(s)) / 2;
            int j1 = scaledHeight - 31;
            font.drawString(matrixStack, s, (float)(i1 + 1), (float)j1, 0);
            font.drawString(matrixStack, s, (float)(i1 - 1), (float)j1, 0);
            font.drawString(matrixStack, s, (float)i1, (float)(j1 + 1), 0);
            font.drawString(matrixStack, s, (float)i1, (float)(j1 - 1), 0);
            font.drawString(matrixStack, s, (float)i1, (float)j1, 8453920);
            mc.getProfiler().endSection();
            ci.cancel();
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
                ))
        public void renderItemNamePre(MatrixStack matrixStack, CallbackInfo ci)
        {
            if (RearrangeHotbar.customHotbar)
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
                        ))
        public void renderItemNamePost(MatrixStack matrixStack, CallbackInfo ci)
        {
            if (RearrangeHotbar.customHotbar)
            {
                matrixStack.translate(0, 4, 0);
            }
        }
    }
}
