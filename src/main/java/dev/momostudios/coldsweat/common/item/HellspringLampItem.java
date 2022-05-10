package dev.momostudios.coldsweat.common.item;

import dev.momostudios.coldsweat.api.temperature.Temperature;
import dev.momostudios.coldsweat.config.ConfigCache;
import dev.momostudios.coldsweat.core.itemgroup.ColdSweatGroup;
import dev.momostudios.coldsweat.core.network.message.PlaySoundMessage;
import dev.momostudios.coldsweat.util.entity.NBTHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.PacketDistributor;
import dev.momostudios.coldsweat.api.temperature.modifier.HellLampTempModifier;
import dev.momostudios.coldsweat.config.ItemSettingsConfig;
import dev.momostudios.coldsweat.core.network.ColdSweatPacketHandler;
import dev.momostudios.coldsweat.util.entity.TempHelper;

public class HellspringLampItem extends Item
{
    public HellspringLampItem()
    {
        super(new Properties().group(ColdSweatGroup.COLD_SWEAT).maxStackSize(1));
    }

    @Override
    public void inventoryTick(ItemStack stack, World worldIn, Entity entityIn, int itemSlot, boolean isSelected)
    {
        if (entityIn instanceof PlayerEntity && !worldIn.isRemote)
        {
            PlayerEntity player = (PlayerEntity) entityIn;

            // Fuel the item on creation
            if (!stack.getOrCreateTag().getBoolean("hasTicked"))
            {
                stack.getOrCreateTag().putBoolean("hasTicked", true);
                setFuel(stack, 64);
            }

            boolean validDimension = false;
            for (String id : ItemSettingsConfig.getInstance().hellLampDimensions())
            {
                if (worldIn.getDimensionKey().getLocation().toString().equals(id))
                {
                    validDimension = true;
                    break;
                }
            }

            if ((isSelected || player.getHeldItemOffhand() == stack) && validDimension && getFuel(stack) > 0
            && TempHelper.getTemperature(player, Temperature.Types.WORLD).get() > ConfigCache.getInstance().maxTemp)
            {
                // Drain fuel
                if (player.ticksExisted % 10 == 0 && !(player.isCreative() || player.isSpectator()))
                    addFuel(stack, -0.02f);

                // Give effect to nearby players
                AxisAlignedBB bb = new AxisAlignedBB(player.getPosX() - 3.5, player.getPosY() - 3.5, player.getPosZ() - 3.5,
                                                     player.getPosX() + 3.5, player.getPosY() + 3.5, player.getPosZ() + 3.5);
                worldIn.getEntitiesWithinAABB(PlayerEntity.class, bb).forEach(e ->
                {
                    TempHelper.addOrReplaceModifier(e, new HellLampTempModifier().expires(5), Temperature.Types.MAX);
                });

                // If the conditions are met, turn on the lamp
                if (stack.getOrCreateTag().getInt("stateChangeTimer") <= 0 && !stack.getOrCreateTag().getBoolean("isOn"))
                {
                    stack.getOrCreateTag().putInt("stateChangeTimer", 10);
                    stack.getOrCreateTag().putBoolean("isOn", true);

                    ColdSweatPacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(), new PlaySoundMessage(1, 1.5f, (float) Math.random() / 5f + 0.9f, player.getUniqueID()));
                }
            }
            // If the conditions are not met, turn off the lamp
            else
            {
                if (stack.getOrCreateTag().getInt("stateChangeTimer") <= 0 && stack.getOrCreateTag().getBoolean("isOn"))
                {
                    stack.getOrCreateTag().putInt("stateChangeTimer", 10);
                    stack.getOrCreateTag().putBoolean("isOn", false);

                    if (getFuel(stack) < 0.5)
                        setFuel(stack, 0);

                    ColdSweatPacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(), new PlaySoundMessage(2, 1.5f, (float) Math.random() / 5f + 0.9f, player.getUniqueID()));
                }
            }

            // Decrement the state change timer
            NBTHelper.incrementNBT(stack, "stateChangeTimer", -1, nbt -> nbt > 0);
        }
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged)
    {
        return slotChanged;
    }

    private void setFuel(ItemStack stack, float fuel)
    {
        stack.getOrCreateTag().putFloat("fuel", fuel);
    }
    private void addFuel(ItemStack stack, float fuel)
    {
        setFuel(stack, getFuel(stack) + fuel);
    }
    private float getFuel(ItemStack stack)
    {
        return stack.getOrCreateTag().getFloat("fuel");
    }
}
