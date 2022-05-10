package dev.momostudios.coldsweat.client.event;

import com.mojang.blaze3d.matrix.MatrixStack;
import dev.momostudios.coldsweat.util.entity.PlayerHelper;
import dev.momostudios.coldsweat.util.registries.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.AbstractClientPlayerEntity;
import net.minecraft.client.renderer.FirstPersonRenderer;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.entity.PlayerRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.HandSide;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;

import java.lang.reflect.Method;

@Mod.EventBusSubscriber(Dist.CLIENT)
public class RenderLampHand
{
    static Method renderItem = ObfuscationReflectionHelper.findMethod(FirstPersonRenderer.class, "func_228405_a_",
                                                               AbstractClientPlayerEntity.class, float.class, float.class,
                                                               Hand.class, float.class, ItemStack.class,
                                                               float.class, MatrixStack.class, IRenderTypeBuffer.class, int.class);
    static
    {
        renderItem.setAccessible(true);
    }

    @SubscribeEvent
    public static void onHandRender(RenderHandEvent event)
    {
        if (event.getItemStack().getItem() == ModItems.HELLSPRING_LAMP)
        {
            MatrixStack ms = event.getMatrixStack();
            AbstractClientPlayerEntity player = Minecraft.getInstance().player;
            boolean isRightHand = PlayerHelper.getHandSide(event.getHand(), player) == HandSide.RIGHT;

            event.setCanceled(true);

            ms.push();
            ms.rotate(Vector3f.YP.rotationDegrees(-((float) Math.cos(Math.min(event.getSwingProgress() * 1.3, 1) * Math.PI * 2) * 5 - 5)));
            ms.rotate(Vector3f.ZP.rotationDegrees(-((float) Math.cos(Math.min(event.getSwingProgress() * 1.3, 1) * Math.PI * 2) * 10 - 10)));

            ms.translate
            (
                    0.0d,
                    Math.cos(Math.min(event.getSwingProgress() * 1.1, 1) * Math.PI * 2 - Math.PI * 0.5) * 0.1
                            + (event.getEquipProgress() == 0 ? (Math.cos(event.getSwingProgress() * Math.PI * 2) - 1) * 0.2 : 0),
                    Math.cos(Math.min(event.getSwingProgress() * 1.1, 1) * Math.PI * 2) * -0.0 - 0
            );

            /*
             Render the hand itself
             */
            ms.push();

            if (isRightHand)
            {
                ms.translate(0.75, -0.3, -0.31);
            }
            else
            {
                ms.translate(-0.75, -0.3, -0.31);
            }

            ms.scale(0.75f, 0.8f, 0.72f);

            PlayerRenderer handRenderer = (PlayerRenderer) Minecraft.getInstance().getRenderManager().getRenderer(player);
            if (isRightHand)
            {
                ms.rotate(Vector3f.ZP.rotationDegrees(100));
                ms.rotate(Vector3f.YP.rotationDegrees(170.0F));
                ms.rotate(Vector3f.XP.rotationDegrees(90.0F));
                ms.translate(event.getEquipProgress() * 1.5, -event.getEquipProgress() * 0.5, -event.getEquipProgress() * 0.2);
                handRenderer.renderRightArm(ms, event.getBuffers(), event.getLight(), player);
            }
            else
            {
                ms.rotate(Vector3f.ZP.rotationDegrees(-100));
                ms.rotate(Vector3f.YP.rotationDegrees(190.0F));
                ms.rotate(Vector3f.XP.rotationDegrees(90.0F));
                ms.translate(-event.getEquipProgress() * 1.5, -event.getEquipProgress() * 0.5, -event.getEquipProgress() * 0.2);
                handRenderer.renderLeftArm(ms, event.getBuffers(), event.getLight(), player);
            }
            ms.pop();

            /*
             Render the lamp
             */
            ms.push();
            ms.translate(event.getEquipProgress() * 0.05, -event.getEquipProgress() * 0.575, event.getEquipProgress() * 0.25);
            try
            {
                renderItem.invoke(Minecraft.getInstance().getFirstPersonRenderer(),
                                  Minecraft.getInstance().player,
                                  event.getPartialTicks(),
                                  event.getInterpolatedPitch(),
                                  event.getHand(),
                                  0,
                                  event.getItemStack(),
                                  event.getEquipProgress(),
                                  ms,
                                  event.getBuffers(),
                                  event.getLight());
            }
            catch (Exception e) {}
            ms.pop();
            ms.pop();
        }
    }
}
