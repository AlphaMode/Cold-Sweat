package dev.momostudios.coldsweat.common.item;

import dev.momostudios.coldsweat.api.temperature.Temperature;
import dev.momostudios.coldsweat.core.itemgroup.ColdSweatGroup;
import dev.momostudios.coldsweat.core.network.message.PlaySoundMessage;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.PacketDistributor;
import dev.momostudios.coldsweat.api.temperature.modifier.HellLampTempModifier;
import dev.momostudios.coldsweat.config.ConfigCache;
import dev.momostudios.coldsweat.config.ItemSettingsConfig;
import dev.momostudios.coldsweat.core.network.ColdSweatPacketHandler;
import dev.momostudios.coldsweat.util.math.CSMath;
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
            double max = ConfigCache.getInstance().maxTemp;
            double temp = TempHelper.hasModifier(player, HellLampTempModifier.class, Temperature.Types.WORLD) ?
                    player.getPersistentData().getDouble("preLampTemp") : TempHelper.getTemperature(player, Temperature.Types.WORLD).get();

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

            if ((isSelected || player.getHeldItemOffhand() == stack) && validDimension && temp > max)
            {
                if (getFuel(stack) > 0)
                {
                    // Drain fuel
                    if (player.ticksExisted % 10 == 0 && !(player.isCreative() || player.isSpectator()))
                        addFuel(stack, -0.02f * (float) CSMath.clamp(temp - ConfigCache.getInstance().maxTemp, 1, 3));

                    // Give effect to nearby players
                    AxisAlignedBB bb = new AxisAlignedBB(player.getPosX() - 3.5, player.getPosY() - 3.5, player.getPosZ() - 3.5,
                                                         player.getPosX() + 3.5, player.getPosY() + 3.5, player.getPosZ() + 3.5);

                    worldIn.getEntitiesWithinAABB(PlayerEntity.class, bb).forEach(e ->
                    {
                        TempHelper.addModifier(e, new HellLampTempModifier(), Temperature.Types.WORLD, false);

                        e.getPersistentData().putInt("soulLampTimeout", 5);
                    });
                }
            }

            // Handle state changes & sounds
            if (stack.getOrCreateTag().getInt("stateChangeTimer") > 0)
            {
                stack.getOrCreateTag().putInt("stateChangeTimer", stack.getOrCreateTag().getInt("stateChangeTimer") - 1);
            }

            if (stack.getOrCreateTag().getInt("fuel") > 0 && validDimension && temp > max &&
            (isSelected || player.getHeldItemOffhand() == stack))
            {
                if (stack.getOrCreateTag().getInt("stateChangeTimer") <= 0 && !stack.getOrCreateTag().getBoolean("isOn"))
                {
                    stack.getOrCreateTag().putInt("stateChangeTimer", 10);
                    stack.getOrCreateTag().putBoolean("isOn", true);

                    ColdSweatPacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(), new PlaySoundMessage(1, 1.5f, (float) Math.random() / 5f + 0.9f, player.getUniqueID()));
                }
            }
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
