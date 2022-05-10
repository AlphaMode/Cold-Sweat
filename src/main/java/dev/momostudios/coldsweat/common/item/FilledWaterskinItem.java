package dev.momostudios.coldsweat.common.item;

import dev.momostudios.coldsweat.api.temperature.Temperature;
import dev.momostudios.coldsweat.core.init.ItemInit;
import dev.momostudios.coldsweat.core.itemgroup.ColdSweatGroup;
import dev.momostudios.coldsweat.util.math.CSMath;
import dev.momostudios.coldsweat.util.registries.ModItems;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.*;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import dev.momostudios.coldsweat.api.temperature.modifier.WaterskinTempModifier;
import dev.momostudios.coldsweat.config.ConfigCache;
import dev.momostudios.coldsweat.util.entity.TempHelper;

public class FilledWaterskinItem extends Item
{
    public FilledWaterskinItem()
    {
        super(new Properties().group(ColdSweatGroup.COLD_SWEAT).maxStackSize(1).containerItem(ItemInit.WATERSKIN_REGISTRY.get()));
    }

    @Override
    public void inventoryTick(ItemStack itemstack, World world, Entity entity, int slot, boolean selected)
    {
        super.inventoryTick(itemstack, world, entity, slot, selected);
        if (entity instanceof PlayerEntity)
        {
            PlayerEntity player = (PlayerEntity) entity;
            double itemTemp = itemstack.getOrCreateTag().getDouble("temperature");
            if (CSMath.isBetween(itemTemp, -1, 1))
            {
                if (itemTemp != 0)
                    itemstack.getOrCreateTag().putDouble("temperature", 0);
            }
            else if (slot <= 8)
            {
                double temp = 0.03 * ConfigCache.getInstance().rate * CSMath.normalize(itemTemp);
                double newTemp = itemTemp - temp;

                itemstack.getOrCreateTag().putDouble("temperature", newTemp);

                TempHelper.addModifier(player, new WaterskinTempModifier(temp * 1.5), Temperature.Types.CORE, false);
            }
        }
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, PlayerEntity player, Hand hand)
    {
        ActionResult<ItemStack> ar = super.onItemRightClick(world, player, hand);
        ItemStack itemstack = ar.getResult();

        TempHelper.addModifier(player, new WaterskinTempModifier(itemstack.getOrCreateTag().getDouble("temperature")).expires(1), Temperature.Types.CORE, true);

        world.playSound(player.getPosX(), player.getPosY(), player.getPosZ(), SoundEvents.AMBIENT_UNDERWATER_EXIT,
            SoundCategory.PLAYERS, 1, (float) ((Math.random() / 5) + 0.9), false);

        ItemStack emptyWaterskin = new ItemStack(ModItems.WATERSKIN);
        emptyWaterskin.setDisplayName(itemstack.getDisplayName());
        if (player.inventory.hasItemStack(emptyWaterskin))
        {
            player.addItemStackToInventory(emptyWaterskin);
        }
        else
        {
            player.setHeldItem(hand, emptyWaterskin);
        }
        player.swingArm(hand);

        if (world instanceof ServerWorld)
            ((ServerWorld) world).spawnParticle(ParticleTypes.FALLING_WATER, player.getPosX(), (player.getPosY() + (player.getHeight())), player.getPosZ(), 50, 0.3, 0.3, 0.3, 0.05);
        return ar;
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged)
    {
        return slotChanged;
    }

    public String getTranslationKey(ItemStack stack)
    {
        return "item.cold_sweat.waterskin";
    }
}
