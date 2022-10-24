package dev.momostudios.coldsweat.common.item;

import dev.momostudios.coldsweat.api.temperature.modifier.BlockTempModifier;
import dev.momostudios.coldsweat.core.itemgroup.ColdSweatGroup;
import dev.momostudios.coldsweat.util.config.ConfigSettings;
import dev.momostudios.coldsweat.util.math.CSMath;
import dev.momostudios.coldsweat.util.registries.ModItems;
import net.minecraft.block.BlockState;
import net.minecraft.block.material.Material;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.*;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import dev.momostudios.coldsweat.api.temperature.Temperature;
import dev.momostudios.coldsweat.api.temperature.modifier.BiomeTempModifier;

import java.util.Arrays;

public class WaterskinItem extends Item
{
    public WaterskinItem()
    {
        super(new Properties().group(ColdSweatGroup.COLD_SWEAT).maxStackSize(16));
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, PlayerEntity player, Hand hand)
    {
        ActionResult<ItemStack> ar = super.onItemRightClick(world, player, hand);
        ItemStack itemstack = ar.getResult();

        //Get the block the player is looking at
        Vector3d lookPos = player.getEyePosition(1f).add(
                player.getLook(1f).x * 5,
                player.getLook(1f).y * 5,
                player.getLook(1f).z * 5);
        BlockState lookingAt = world.getFluidState(world.rayTraceBlocks(new RayTraceContext(player.getEyePosition(1f), lookPos,
                RayTraceContext.BlockMode.COLLIDER, RayTraceContext.FluidMode.SOURCE_ONLY, player)).getPos()).getBlockState();

        if (lookingAt.getMaterial() == Material.WATER)
        {
            ItemStack filledWaterskin = ModItems.FILLED_WATERSKIN.getDefaultInstance();
            filledWaterskin.setTag(itemstack.getTag());
            filledWaterskin.getOrCreateTag().putDouble("temperature", CSMath.clamp((new Temperature().with(player, Arrays.asList(
                    new BiomeTempModifier(),
                    new BlockTempModifier()
            )).get() - (CSMath.average(ConfigSettings.getInstance().maxTemp, ConfigSettings.getInstance().minTemp))) * 15, -50, 50));

            //Replace 1 of the stack with a FilledWaterskinItem
            if (itemstack.getCount() > 1)
            {
                if (!player.addItemStackToInventory(filledWaterskin))
                {
                    ItemEntity itementity = player.dropItem(filledWaterskin, false);
                    if (itementity != null)
                    {
                        itementity.setNoPickupDelay();
                        itementity.setOwnerId(player.getUniqueID());
                    }
                }
                itemstack.setCount(itemstack.getCount() - 1);
            }
            else
            {
                player.setHeldItem(hand, filledWaterskin);
            }
            //Play filling sound
            world.playSound(null, player.getPosition(), SoundEvents.AMBIENT_UNDERWATER_ENTER, SoundCategory.PLAYERS, 1, (float) Math.random() / 5 + 0.9f);
            player.swingArm(hand);
        }
        return ar;
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged)
    {
        return slotChanged;
    }
}
