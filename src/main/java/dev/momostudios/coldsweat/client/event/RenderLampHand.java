package dev.momostudios.coldsweat.client.event;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import dev.momostudios.coldsweat.util.entity.PlayerHelper;
import dev.momostudios.coldsweat.util.registries.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.AbstractClientPlayerEntity;
import net.minecraft.client.renderer.FirstPersonRenderer;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.model.PlayerModel;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.entity.player.PlayerEntity;
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
    @SubscribeEvent
    public static void onHandRender(RenderHandEvent event)
    {
        if (event.getItemStack().getItem() == ModItems.HELLSPRING_LAMP)
        {
            MatrixStack ms = event.getMatrixStack();
            IVertexBuilder playerSkin = event.getBuffers().getBuffer(RenderType.getEntityCutout(Minecraft.getInstance().player.getLocationSkin()));
            boolean isRightHand = PlayerHelper.getHandSide(event.getHand(), Minecraft.getInstance().player) == HandSide.RIGHT;

            event.setCanceled(true);

            ms.push();
            ms.rotate(Vector3f.YP.rotationDegrees(-((float) Math.cos(Math.min(event.getSwingProgress() * 1.3, 1) * Math.PI * 2) * 5 - 5)));
            ms.rotate(Vector3f.ZP.rotationDegrees(-((float) Math.cos(Math.min(event.getSwingProgress() * 1.3, 1) * Math.PI * 2) * 10 - 10)));

            ms.translate(0.0d,
                         event.getEquipProgress() == 0 ? Math.cos(Math.min(event.getSwingProgress() * 1.3, 1) * Math.PI * 2 - Math.PI * 0.5) * 0.1 : 0,
                         Math.cos(Math.min(event.getSwingProgress() * 1.3, 1) * Math.PI * 2) * 0 - 0);

            ms.push();

            if (isRightHand)
            {
                ms.translate(0.73, -0.12, -0.45);
            }
            else
            {
                ms.translate(-0.73, -0.12, -0.45);
            }

            ms.scale(0.45f, 0.4f, 0.5f);

            if (isRightHand)
            {
                ms.rotate(Vector3f.ZP.rotationDegrees(100));
                ms.rotate(Vector3f.YP.rotationDegrees(160.0F));
                ms.rotate(Vector3f.XP.rotationDegrees(90.0F));
                ms.translate(event.getEquipProgress() * 1.5, -event.getEquipProgress() * 0.5, -event.getEquipProgress() * 0.2);
                new PlayerModel<PlayerEntity>(1, false).bipedRightArm.render(ms, playerSkin, event.getLight(), OverlayTexture.NO_OVERLAY);
            }
            else
            {
                ms.rotate(Vector3f.ZP.rotationDegrees(-100));
                ms.rotate(Vector3f.YP.rotationDegrees(200.0F));
                ms.rotate(Vector3f.XP.rotationDegrees(90.0F));
                ms.translate(-event.getEquipProgress() * 1.5, -event.getEquipProgress() * 0.5, -event.getEquipProgress() * 0.2);
                new PlayerModel<PlayerEntity>(1, false).bipedLeftArm.render(ms, playerSkin, event.getLight(), OverlayTexture.NO_OVERLAY);
            }
            ms.pop();

            ms.push();
            Method renderItem = ObfuscationReflectionHelper.findMethod(FirstPersonRenderer.class, "renderItemInFirstPerson",
                                                                       AbstractClientPlayerEntity.class, float.class, float.class,
                                                                       Hand.class, float.class, ItemStack.class,
                                                                       float.class, MatrixStack.class, IRenderTypeBuffer.class, int.class);
            renderItem.setAccessible(true);
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
