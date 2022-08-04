package dev.momostudios.coldsweat.common.item;

import dev.momostudios.coldsweat.api.temperature.Temperature;
import dev.momostudios.coldsweat.api.temperature.modifier.TempModifier;
import dev.momostudios.coldsweat.util.config.ConfigCache;
import dev.momostudios.coldsweat.core.itemgroup.ColdSweatGroup;
import dev.momostudios.coldsweat.util.config.LoadedValue;
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
import dev.momostudios.coldsweat.api.temperature.modifier.HellLampTempModifier;
import dev.momostudios.coldsweat.config.ItemSettingsConfig;
import dev.momostudios.coldsweat.api.util.TempHelper;

import java.util.ArrayList;
import java.util.List;

public class HellspringLampItem extends Item
{
    static LoadedValue<List<String>> VALID_DIMENSIONS = LoadedValue.of(() -> new ArrayList<>(ItemSettingsConfig.getInstance().soulLampDimensions()));

    public HellspringLampItem()
    {
        super(new Properties().group(ColdSweatGroup.COLD_SWEAT).maxStackSize(1).isImmuneToFire());
    }

    @Override
    public void inventoryTick(ItemStack stack, World worldIn, Entity entityIn, int itemSlot, boolean isSelected)
    {
        if (entityIn instanceof PlayerEntity && !worldIn.isRemote)
        {
            PlayerEntity player = (PlayerEntity) entityIn;
            double max = ConfigCache.getInstance().maxTemp;
            TempModifier lampMod = TempHelper.getModifier(player, Temperature.Type.WORLD, HellLampTempModifier.class);
            double temp;

            // Is holding
            if ((isSelected || player.getHeldItemOffhand() == stack)
            // Is world temp more than max
            && (temp = lampMod != null ? lampMod.getLastInput().get() : TempHelper.getTemperature(player, Temperature.Type.WORLD).get()) > max && getFuel(stack) > 0
            // Is in valid dimension
            && VALID_DIMENSIONS.get().contains(worldIn.getDimensionKey().getLocation().toString()))
            {
                // Drain fuel
                if (player.ticksExisted % 5 == 0 && !(player.isCreative() || player.isSpectator()))
                {
                    addFuel(stack, -0.01d * CSMath.clamp(temp - ConfigCache.getInstance().maxTemp, 1d, 3d));

                    AxisAlignedBB bb = new AxisAlignedBB(player.getPosX() - 3.5, player.getPosY() - 3.5, player.getPosZ() - 3.5,
                                                         player.getPosX() + 3.5, player.getPosY() + 3.5, player.getPosZ() + 3.5);
                    for (PlayerEntity entity : worldIn.getEntitiesWithinAABB(PlayerEntity.class, bb))
                    {
                        HellLampTempModifier modifier = TempHelper.getModifier(entity, Temperature.Type.WORLD, HellLampTempModifier.class);
                        if (modifier != null)
                            modifier.expires(modifier.getTicksExisted() + 5);
                        else
                            TempHelper.addModifier(entity, new HellLampTempModifier().expires(5).tickRate(5), Temperature.Type.WORLD, false);
                    }
                }

                // If the conditions are met, turn on the lamp
                if (stack.getOrCreateTag().getInt("stateChangeTimer") <= 0 && !stack.getOrCreateTag().getBoolean("isOn"))
                {
                    stack.getOrCreateTag().putInt("stateChangeTimer", 10);
                    stack.getOrCreateTag().putBoolean("isOn", true);

                    WorldHelper.playEntitySound(ModSounds.NETHER_LAMP_ON, SoundCategory.PLAYERS, player, 1.5f, (float) Math.random() / 5f + 0.9f);
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

                    WorldHelper.playEntitySound(ModSounds.NETHER_LAMP_OFF, SoundCategory.PLAYERS, player, 1.5f, (float) Math.random() / 5f + 0.9f);
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
