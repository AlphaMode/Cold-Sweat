package dev.momostudios.coldsweat.common.item;

import dev.momostudios.coldsweat.api.temperature.Temperature;
import dev.momostudios.coldsweat.core.init.ItemInit;
import dev.momostudios.coldsweat.core.itemgroup.ColdSweatGroup;
import dev.momostudios.coldsweat.util.registries.ModItems;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.*;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.registries.ForgeRegistries;
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
            double itemTemp = itemstack.getOrCreateTag().getDouble("temperature");
            if (itemTemp != 0 && slot <= 8)
            {
                double temp = 0;
                if (itemTemp > 0)
                {
                    itemstack.getOrCreateTag().putDouble("temperature", itemTemp - Math.min(itemTemp, 0.03));
                    temp = 0.03;
                }
                else if (itemTemp < 0)
                {
                    itemstack.getOrCreateTag().putDouble("temperature", itemTemp + Math.min(-itemTemp, 0.03));
                    temp = -0.03;
                }

                TempHelper.addModifier((PlayerEntity) entity, new WaterskinTempModifier(temp * ConfigCache.getInstance().rate).expires(1), Temperature.Types.CORE, true);
            }
        }
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, PlayerEntity entity, Hand hand)
    {
        ActionResult<ItemStack> ar = super.onItemRightClick(world, entity, hand);
        ItemStack itemstack = ar.getResult();

        TempHelper.addModifier(entity, new WaterskinTempModifier(itemstack.getOrCreateTag().getDouble("temperature")).expires(1), Temperature.Types.CORE, true);

        world.playSound(entity.getPosX(), entity.getPosY(), entity.getPosZ(), SoundEvents.AMBIENT_UNDERWATER_EXIT,
            SoundCategory.PLAYERS, 1, (float) ((Math.random() / 5) + 0.9), false);

        if (!entity.inventory.hasItemStack(ModItems.WATERSKIN.getDefaultInstance()))
        {
            entity.setHeldItem(hand, ModItems.WATERSKIN.getDefaultInstance());
        }
        else
        {
            entity.addItemStackToInventory(ModItems.WATERSKIN.getDefaultInstance());
            itemstack.shrink(1);
        }
        entity.swingArm(hand);

        if (world instanceof ServerWorld)
            ((ServerWorld) world).spawnParticle(ParticleTypes.FALLING_WATER, entity.getPosX(), (entity.getPosY() + (entity.getHeight())), entity.getPosZ(), 50, 0.3, 0.3, 0.3, 0.05);
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
