package dev.momostudios.coldsweat.mixin;

import com.mojang.datafixers.util.Pair;
import dev.momostudios.coldsweat.ColdSweat;
import dev.momostudios.coldsweat.client.event.HandleHellLampAnim;
import dev.momostudios.coldsweat.util.entity.PlayerHelper;
import dev.momostudios.coldsweat.util.math.CSMath;
import net.minecraft.client.renderer.entity.model.BipedModel;
import net.minecraft.client.renderer.model.ModelRenderer;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.HandSide;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.client.model.animation.Animation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BipedModel.class)
public class MixinBipedModel
{
    @Shadow
    public BipedModel.ArmPose rightArmPose;
    @Shadow
    public BipedModel.ArmPose leftArmPose;
    @Shadow
    public ModelRenderer bipedRightArm;
    @Shadow
    public ModelRenderer bipedLeftArm;

    /**
     * @author iMikul
     * @reason Adds functions for the Soulfire Lamp
     */
    @Inject(method = "func_241654_b_",
            at = @At("TAIL"), remap = ColdSweat.REMAP_MIXINS)
    public void func_241654_b_(LivingEntity entity, CallbackInfo ci)
    {
        if (entity instanceof PlayerEntity)
        {
            PlayerEntity player = (PlayerEntity) entity;
            boolean holdingLamp = PlayerHelper.holdingLamp(player, HandSide.RIGHT);
            Pair<Float, Float> armRot = HandleHellLampAnim.RIGHT_ARM_ROTATIONS.getOrDefault(player, Pair.of(0f, 0f));
            float rightArmRot = CSMath.toRadians(MathHelper.lerp(Animation.getPartialTickTime(), armRot.getSecond(), armRot.getFirst()));

            switch (this.rightArmPose)
            {
                case EMPTY:
                    this.bipedRightArm.rotateAngleX = this.bipedRightArm.rotateAngleX - rightArmRot;
                    this.bipedRightArm.rotateAngleY = 0.0F;
                    break;
                case BLOCK:
                    this.bipedRightArm.rotateAngleX = this.bipedRightArm.rotateAngleX * 0.5F - 0.9424779F - rightArmRot;
                    this.bipedRightArm.rotateAngleY = -(float) Math.PI / 6F;
                    break;
                case ITEM:
                    this.bipedRightArm.rotateAngleX = this.bipedRightArm.rotateAngleX * (holdingLamp ? 0.15F : 0.5f) - ((float) Math.PI / 10F) - rightArmRot;
                    this.bipedRightArm.rotateAngleZ = this.bipedRightArm.rotateAngleZ * (holdingLamp ? 0.15F : 0.5f);
                    this.bipedRightArm.rotateAngleY = 0.0F;
                    break;
            }
        }
    }

    /**
     * @author iMikul
     * @reason Adds functions for the Soulfire Lamp
     */
    @Inject(method = "func_241655_c_",
            at = @At("TAIL"), remap = ColdSweat.REMAP_MIXINS)
    public void func_241655_c_(LivingEntity entity, CallbackInfo ci)
    {
        if (entity instanceof PlayerEntity)
        {
            PlayerEntity player = (PlayerEntity) entity;
            boolean holdingLamp = PlayerHelper.holdingLamp(player, HandSide.LEFT);
            Pair<Float, Float> armRot = HandleHellLampAnim.LEFT_ARM_ROTATIONS.getOrDefault(player, Pair.of(0f, 0f));
            float leftArmRot = CSMath.toRadians(MathHelper.lerp(Animation.getPartialTickTime(), armRot.getSecond(), armRot.getFirst()));

            switch (this.leftArmPose)
            {
                case EMPTY:
                    this.bipedLeftArm.rotateAngleX = this.bipedLeftArm.rotateAngleX - leftArmRot;
                    this.bipedLeftArm.rotateAngleY = 0.0F;
                    break;
                case BLOCK:
                    this.bipedLeftArm.rotateAngleX = this.bipedLeftArm.rotateAngleX * 0.5f - 0.9424779F - leftArmRot;
                    this.bipedLeftArm.rotateAngleY = (float) Math.PI / 6F;
                    break;
                case ITEM:
                    this.bipedLeftArm.rotateAngleX = this.bipedLeftArm.rotateAngleX * (holdingLamp ? 0.15F : 0.5f) - (float) Math.PI / 10F - leftArmRot;
                    this.bipedLeftArm.rotateAngleZ = this.bipedLeftArm.rotateAngleZ * (holdingLamp ? 0.15F : 0.5f);
                    this.bipedLeftArm.rotateAngleY = 0.0F;
                    break;
            }
        }
    }
}
