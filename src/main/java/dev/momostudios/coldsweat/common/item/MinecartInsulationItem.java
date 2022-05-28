package dev.momostudios.coldsweat.common.item;

import dev.momostudios.coldsweat.util.registries.ModBlocks;
import net.minecraft.client.audio.SoundSource;
import net.minecraft.entity.item.minecart.MinecartEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileHelper;
import net.minecraft.item.Item;
import dev.momostudios.coldsweat.core.itemgroup.ColdSweatGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.EntityRayTraceResult;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraftforge.client.model.animation.Animation;
import net.minecraftforge.common.ForgeMod;

public class MinecartInsulationItem extends Item
{
    public MinecartInsulationItem()
    {
        super(new Properties().group(ColdSweatGroup.COLD_SWEAT).maxStackSize(1));
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, PlayerEntity player, Hand hand)
    {
        ItemStack itemStack = player.getHeldItem(hand);
        double reachDistance = player.getAttribute(ForgeMod.REACH_DISTANCE.get()).getValue();

        Vector3d eye = player.getEyePosition(Animation.getPartialTickTime());
        Vector3d look = eye.add(player.getLookVec().scale(reachDistance));
        EntityRayTraceResult entityHitResult = ProjectileHelper.rayTraceEntities(world, player, eye, look, new AxisAlignedBB(eye, look).grow(1.0D),
                                                                                 entity -> entity instanceof MinecartEntity);

        if (entityHitResult != null && entityHitResult.getType() == RayTraceResult.Type.ENTITY)
        {
            if (entityHitResult.getEntity() instanceof MinecartEntity)
            {
                MinecartEntity minecart = (MinecartEntity) entityHitResult.getEntity();
                if (minecart.getDisplayTile().getBlock() != ModBlocks.MINECART_INSULATION)
                {
                    if (!player.isCreative())
                    {
                        player.getHeldItemMainhand().shrink(1);
                    }
                    player.swing(Hand.MAIN_HAND, true);
                    world.playSound(null,
                                    minecart.getPosition(),
                                    SoundEvents.ENTITY_LLAMA_SWAG,
                                    SoundCategory.PLAYERS,
                                    1f,
                                    (float) ((Math.random() / 5) + 0.9));
                    minecart.setDisplayTile(ModBlocks.MINECART_INSULATION.getDefaultState());
                    minecart.setDisplayTileOffset(5);
                    return ActionResult.func_233538_a_(itemStack, world.isRemote());
                }
            }
        }

        return ActionResult.resultPass(itemStack);
    }
}
