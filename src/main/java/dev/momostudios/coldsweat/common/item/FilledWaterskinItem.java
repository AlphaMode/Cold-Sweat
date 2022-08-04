package dev.momostudios.coldsweat.common.item;

import dev.momostudios.coldsweat.api.temperature.Temperature;
import dev.momostudios.coldsweat.config.ItemSettingsConfig;
import dev.momostudios.coldsweat.core.init.ItemInit;
import dev.momostudios.coldsweat.core.itemgroup.ColdSweatGroup;
import dev.momostudios.coldsweat.util.config.LoadedValue;
import dev.momostudios.coldsweat.util.math.CSMath;
import dev.momostudios.coldsweat.util.registries.ModItems;
import dev.momostudios.coldsweat.util.world.WorldHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.*;
import net.minecraft.world.World;
import dev.momostudios.coldsweat.api.temperature.modifier.WaterskinTempModifier;
import dev.momostudios.coldsweat.util.config.ConfigCache;
import dev.momostudios.coldsweat.util.entity.TempHelper;

public class FilledWaterskinItem extends Item
{
    static LoadedValue<Integer> WATERSKIN_STRENGTH = LoadedValue.of(() -> ItemSettingsConfig.getInstance().waterskinStrength());

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
            if (itemTemp != 0)
            {
                if (CSMath.isInRange(itemTemp, -1, 1))
                {
                    itemstack.getOrCreateTag().putDouble("temperature", 0);
                    return;
                }

                if (slot <= 8 || player.getHeldItemOffhand().equals(itemstack))
                {
                    double temp = 0.03 * ConfigCache.getInstance().rate * CSMath.getSign(itemTemp);
                    double newTemp = itemTemp - temp;

                    itemstack.getOrCreateTag().putDouble("temperature", newTemp);

                    TempHelper.addModifier(player, new WaterskinTempModifier(temp * 1.5).expires(1), Temperature.Type.CORE, true);
                }
            }
        }
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, PlayerEntity player, Hand hand)
    {
        ActionResult<ItemStack> ar = super.onItemRightClick(world, player, hand);
        ItemStack itemstack = ar.getResult();

        double amount = itemstack.getOrCreateTag().getDouble("temperature") * (WATERSKIN_STRENGTH.get() / 50d);
        if (player.ticksExisted % 5 == 0)
        TempHelper.addModifier(player, new WaterskinTempModifier(amount).expires(5), Temperature.Type.CORE, true);

        // Play empty sound
        world.playSound(player.getPosX(), player.getPosY(), player.getPosZ(), SoundEvents.AMBIENT_UNDERWATER_EXIT,
                             SoundCategory.PLAYERS, 1, (float) ((Math.random() / 5) + 0.9), false);

        // Create empty waterskin item
        ItemStack emptyWaterskin = new ItemStack(ModItems.WATERSKIN);

        // Preserve NBT (except temperature)
        emptyWaterskin.setTag(itemstack.getTag());
        emptyWaterskin.removeChildTag("temperature");

        // Add the item to the player's inventory
        if (player.inventory.hasItemStack(emptyWaterskin))
        {
            player.addItemStackToInventory(emptyWaterskin);
            player.setHeldItem(hand, ItemStack.EMPTY);
        }
        else
        {
            player.setHeldItem(hand, emptyWaterskin);
        }

        player.swingArm(hand);

        for (int i = 0; i < 10; i++)
        {
            WorldHelper.schedule(() ->
            {
                for (int p = 0; p < 5; p++)
                {
                    world.addParticle(ParticleTypes.FALLING_WATER,
                            player.getPosX() + Math.random() * player.getWidth() - (player.getWidth() / 2),
                            player.getPosY() + player.getHeight() + Math.random() * 0.5,
                            player.getPosZ() + Math.random() * player.getWidth() - (player.getWidth() / 2), 0.3, 0.3, 0.3);
                }
            }, i);
        }
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
