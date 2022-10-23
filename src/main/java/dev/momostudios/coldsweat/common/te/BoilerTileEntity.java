package dev.momostudios.coldsweat.common.te;

import dev.momostudios.coldsweat.core.init.TileEntityInit;
import dev.momostudios.coldsweat.util.config.ConfigHelper;
import dev.momostudios.coldsweat.util.config.LoadedValue;
import dev.momostudios.coldsweat.util.registries.ModItems;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.inventory.container.Container;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.LockableLootTileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.*;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.registries.ForgeRegistries;
import dev.momostudios.coldsweat.ColdSweat;
import dev.momostudios.coldsweat.common.block.BoilerBlock;
import dev.momostudios.coldsweat.common.container.BoilerContainer;
import dev.momostudios.coldsweat.config.ItemSettingsConfig;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

public class BoilerTileEntity extends LockableLootTileEntity implements ITickableTileEntity, ISidedInventory
{
    public static int[] WATERSKIN_SLOTS = {1, 2, 3, 4, 5, 6, 7, 8, 9};
    public static int[] FUEL_SLOT = {0};
    public static int SLOTS = 10;
    public static int MAX_FUEL = 1000;
    protected NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);

    public int ticksExisted;
    private int fuel;

    protected final IIntArray fuelData = new IIntArray() {
        public int get(int index) {
            return fuel;
        }

        public void set(int index, int value) {
            fuel = value;
        }

        public int size() {
            return 1;
        }
    };

    public BoilerTileEntity(TileEntityType<?> tileEntityTypeIn) {
        super(tileEntityTypeIn);
    }

    @Override
    protected ITextComponent getDefaultName() {
        return new TranslationTextComponent("container." + ColdSweat.MOD_ID + ".boiler");
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return this.items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> itemsIn)
    {
        this.items = itemsIn;
    }

    public BoilerTileEntity()
    {
        this(TileEntityInit.BOILER_TILE_ENTITY_TYPE.get());
    }

    public void tick()
    {
        this.ticksExisted++;

        if (world != null && !world.isRemote)
        {
            if (this.getFuel() > 0)
            {
                // Set state to lit
                if (!world.getBlockState(pos).get(BoilerBlock.LIT))
                    world.setBlockState(pos, world.getBlockState(pos).with(BoilerBlock.LIT, true));

                // Warm up waterskins
                if (ticksExisted % 20 == 0)
                {
                    boolean hasItemStacks = false;
                    for (int i = 1; i < 10; i++)
                    {
                        ItemStack stack = getItemInSlot(i);
                        int itemTemp = stack.getOrCreateTag().getInt("temperature");

                        if (stack.getItem() == ModItems.FILLED_WATERSKIN && itemTemp < 50)
                        {
                            hasItemStacks = true;
                            stack.getOrCreateTag().putInt("temperature", itemTemp + 1);
                        }
                    }
                    if (hasItemStacks) setFuel(getFuel() - 1);
                }

                if (world.getGameTime() % 20 == 0 && Math.random() < 0.15)
                {
                    world.playSound(null, pos, SoundEvents.BLOCK_FURNACE_FIRE_CRACKLE, SoundCategory.BLOCKS, 2, (float) Math.random() * 0.4f + 0.8f);
                }
            }
            // if no fuel, set state to unlit
            else if (world.getBlockState(pos).get(BoilerBlock.LIT))
            {
                world.setBlockState(pos, world.getBlockState(pos).with(BoilerBlock.LIT, false));
            }

            // Input fuel
            if (this.ticksExisted % 10 == 0)
            {
                ItemStack fuelStack = this.getItemInSlot(0);
                if (!fuelStack.isEmpty())
                {
                    int itemFuel = getItemFuel(fuelStack);

                    if (itemFuel != 0 && this.getFuel() < MAX_FUEL - itemFuel / 2)
                    {
                        if (fuelStack.hasContainerItem() && fuelStack.getCount() == 1)
                        {
                            this.setItemInSlot(0, fuelStack.getContainerItem());
                            setFuel(this.getFuel() + itemFuel);
                        }
                        else
                        {
                            int consumeCount = Math.min((int) Math.floor((MAX_FUEL - fuel) / (double) Math.abs(itemFuel)), fuelStack.getCount());
                            fuelStack.shrink(consumeCount);
                            setFuel(this.getFuel() + itemFuel * consumeCount);
                        }
                    }
                }
            }
        }
    }

    public int getItemFuel(ItemStack item)
    {
        return VALID_FUEL.get().getOrDefault(item.getItem(), 0).intValue();
    }

    public ItemStack getItemInSlot(int index)
    {
        return getCap().map(c -> c.getStackInSlot(index)).orElse(ItemStack.EMPTY);
    }

    public void setItemInSlot(int index, ItemStack stack)
    {
        getCap().ifPresent(capability ->
        {
            capability.getStackInSlot(index).shrink(capability.getStackInSlot(index).getCount());
            capability.insertItem(index, stack, false);
        });
    }

    public int getFuel()
    {
        return fuelData.get(0);
    }

    public void setFuel(int amount)
    {
        fuelData.set(0, Math.min(amount, MAX_FUEL));
    }

    public LazyOptional<IItemHandler> getCap()
    {
        return this.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY);
    }

    public int[] getSlotsForFace(Direction side)
    {
        if (side == Direction.DOWN)
        {
            return WATERSKIN_SLOTS;
        }
        else
        {
            return side == Direction.UP ? WATERSKIN_SLOTS : FUEL_SLOT;
        }
    }

    @Override
    public boolean canInsertItem(int index, ItemStack itemStackIn, @Nullable Direction direction)
    {
        return index == 0 || getCap().map(h -> canInsertItem(index, itemStackIn, direction)).orElse(false);
    }

    @Override
    public boolean canExtractItem(int index, ItemStack stack, Direction direction)
    {
        return getItemFuel(stack) == 0;
    }

    @Override
    public boolean isItemValidForSlot(int index, ItemStack stack)
    {
        return true;
    }

    @Override
    public int getSizeInventory() {
        return SLOTS;
    }

    @Override
    protected Container createMenu(int id, PlayerInventory player)
    {
        return new BoilerContainer(id, player, this, fuelData);
    }

    @Override
    public void read(BlockState state, CompoundNBT nbt)
    {
        super.read(state, nbt);
        this.setFuel(nbt.getInt("fuel"));
        this.items = NonNullList.withSize(this.getSizeInventory(), ItemStack.EMPTY);
        ItemStackHelper.loadAllItems(nbt, this.items);
    }

    @Override
    public CompoundNBT write(CompoundNBT compound)
    {
        super.write(compound);
        compound.putInt("fuel", this.getFuel());
        ItemStackHelper.saveAllItems(compound, items);
        return compound;
    }
}
