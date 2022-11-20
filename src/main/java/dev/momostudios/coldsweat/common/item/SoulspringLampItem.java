package dev.momostudios.coldsweat.common.item;

import dev.momostudios.coldsweat.api.temperature.Temperature;
import dev.momostudios.coldsweat.api.temperature.modifier.SoulLampTempModifier;
import dev.momostudios.coldsweat.api.temperature.modifier.TempModifier;
import dev.momostudios.coldsweat.util.config.ConfigSettings;
import dev.momostudios.coldsweat.core.itemgroup.ColdSweatGroup;
import dev.momostudios.coldsweat.util.entity.NBTHelper;
import dev.momostudios.coldsweat.util.math.CSMath;
import dev.momostudios.coldsweat.util.registries.ModSounds;
import dev.momostudios.coldsweat.util.world.WorldHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.World;
import dev.momostudios.coldsweat.api.util.TempHelper;

public class SoulspringLampItem extends Item
{
    public SoulspringLampItem()
    {
        super(new Properties().group(ColdSweatGroup.COLD_SWEAT).maxStackSize(1).isImmuneToFire());
    }

    @Override
    public void inventoryTick(ItemStack stack, World worldIn, Entity entityIn, int itemSlot, boolean isSelected)
    {
        if (entityIn instanceof PlayerEntity && !worldIn.isRemote)
        {
            PlayerEntity player = (PlayerEntity) entityIn;
            double max = ConfigSettings.getInstance().maxTemp;
            double temp;

            // Is holding
            if ((isSelected || player.getHeldItemOffhand() == stack))
            {
                TempModifier lampMod = TempHelper.getModifier(player, Temperature.Type.WORLD, SoulLampTempModifier.class);
                // Is world temp more than max
                if (ConfigSettings.LAMP_DIMENSIONS.get().contains(worldIn.getDimensionKey().getLocation().toString())
                // Is in valid dimension
                && (temp = lampMod != null ? lampMod.getLastInput().get() : TempHelper.getTemperature(player, Temperature.Type.WORLD).get()) > max && getFuel(stack) > 0)
                {
                    // Drain fuel
                    if (player.ticksExisted % 5 == 0)
                    {
                        if (!(player.isCreative() || player.isSpectator()))
                        {
                            addFuel(stack, -0.01d * CSMath.clamp(temp - max, 1d, 3d));
                        }

                        AxisAlignedBB bb = new AxisAlignedBB(player.getPosX() - 3.5, player.getPosY() - 3.5, player.getPosZ() - 3.5,
                                player.getPosX() + 3.5, player.getPosY() + 3.5, player.getPosZ() + 3.5);
                        for (PlayerEntity entity : worldIn.getEntitiesWithinAABB(PlayerEntity.class, bb))
                        {
                            SoulLampTempModifier modifier = TempHelper.getModifier(entity, Temperature.Type.WORLD, SoulLampTempModifier.class);
                            if (modifier != null)
                                modifier.expires(modifier.getTicksExisted() + 5);
                            else
                                TempHelper.addModifier(entity, new SoulLampTempModifier().expires(5).tickRate(5), Temperature.Type.WORLD, false);
                        }
                    }

                    // If the conditions are met, turn on the lamp
                    if (stack.getOrCreateTag().getInt("stateChangeTimer") <= 0 && !stack.getOrCreateTag().getBoolean("isOn"))
                    {
                        stack.getOrCreateTag().putInt("stateChangeTimer", 10);
                        stack.getOrCreateTag().putBoolean("isOn", true);

                        WorldHelper.playEntitySound(ModSounds.NETHER_LAMP_ON, player, SoundCategory.PLAYERS, 1.5f, (float) Math.random() / 5f + 0.9f);
                    }
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

                    WorldHelper.playEntitySound(ModSounds.NETHER_LAMP_OFF, player, SoundCategory.PLAYERS, 1.5f, (float) Math.random() / 5f + 0.9f);
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

    private void setFuel(ItemStack stack, double fuel)
    {
        stack.getOrCreateTag().putDouble("fuel", fuel);
    }
    private void addFuel(ItemStack stack, double fuel)
    {
        setFuel(stack, getFuel(stack) + fuel);
    }
    private float getFuel(ItemStack stack)
    {
        return stack.getOrCreateTag().getFloat("fuel");
    }

    @Override
    public void fillItemGroup(ItemGroup tab, NonNullList<ItemStack> itemList)
    {
        if (this.isInGroup(tab))
        {
            ItemStack stack = new ItemStack(this);
            stack.getOrCreateTag().putBoolean("isOn", true);
            stack.getOrCreateTag().putDouble("fuel", 64);
            itemList.add(stack);
        }
    }
}
