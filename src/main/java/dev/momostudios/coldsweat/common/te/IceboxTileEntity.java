package dev.momostudios.coldsweat.common.te;

import dev.momostudios.coldsweat.common.block.IceboxBlock;
import dev.momostudios.coldsweat.core.init.ParticleTypesInit;
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
import net.minecraft.util.Direction;
import net.minecraft.util.IIntArray;
import net.minecraft.util.NonNullList;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import dev.momostudios.coldsweat.ColdSweat;
import dev.momostudios.coldsweat.common.container.IceboxContainer;
import dev.momostudios.coldsweat.config.ItemSettingsConfig;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Random;

public class IceboxTileEntity extends LockableLootTileEntity implements ITickableTileEntity, ISidedInventory
{
    public static int[] WATERSKIN_SLOTS = {1, 2, 3, 4, 5, 6, 7, 8, 9};
    public static int[] FUEL_SLOT = {0};
    public static int slots = 10;
    public static int MAX_FUEL = 1000;
    protected NonNullList<ItemStack> items = NonNullList.withSize(slots, ItemStack.EMPTY);

    public int ticksExisted;
    private int fuel;

    public static LoadedValue<Map<Item, Number>> VALID_FUEL = LoadedValue.of(() -> ConfigHelper.getItemsWithValues(ItemSettingsConfig.getInstance().iceboxItems()));

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

    public IceboxTileEntity(TileEntityType<?> tileEntityTypeIn) {
        super(tileEntityTypeIn);
    }

    @Override
    protected ITextComponent getDefaultName() {
        return new TranslationTextComponent("container." + ColdSweat.MOD_ID + ".icebox");
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

    public IceboxTileEntity()
    {
        this(TileEntityInit.ICEBOX_TILE_ENTITY_TYPE.get());
    }

    public void tick()
    {
        this.ticksExisted++;

        if (world != null && !world.isRemote)
        {
            if (this.getFuel() > 0)
            {
                // Set state to lit
                if (!world.getBlockState(pos).get(IceboxBlock.FROSTED))
                    world.setBlockState(pos, world.getBlockState(pos).with(IceboxBlock.FROSTED, true));

                // Cool down waterskins
                if (ticksExisted % 20 == 0)
                {
                    boolean hasItemStacks = false;
                    for (int i = 1; i < 10; i++)
                    {
                        ItemStack stack = getItemInSlot(i);
                        int itemTemp = stack.getOrCreateTag().getInt("temperature");

                        if (stack.getItem() == ModItems.FILLED_WATERSKIN && itemTemp > -50)
                        {
                            hasItemStacks = true;
                            stack.getOrCreateTag().putInt("temperature", itemTemp - 1);
                        }
                    }
                    if (hasItemStacks) setFuel(getFuel() - 1);
                }
            }
            // if no fuel, set state to unlit
            else if (world.getBlockState(pos).get(IceboxBlock.FROSTED))
            {
                world.setBlockState(pos, world.getBlockState(pos).with(IceboxBlock.FROSTED, false));
            }

            // Input fuel
            if (this.ticksExisted % 10 == 0)
            {
                ItemStack fuelStack = this.getItemInSlot(0);
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
        else
        {
            if (world.getBlockState(pos).get(IceboxBlock.FROSTED) && world.getGameTime() % 3 == 0 &&  Math.random() < 0.5)
            {
                double d0 = pos.getX() + 0.5;
                double d1 = pos.getY();
                double d2 = pos.getZ() + 0.5;
                boolean side = new Random().nextBoolean();
                double d5 = side ? Math.random() * 2 - 1 : (Math.random() < 0.5 ? 0.55 : -0.55);
                double d6 = Math.random() * 0.3;
                double d7 = !side ? Math.random() * 2 - 1 : (Math.random() < 0.5 ? 0.55 : -0.55);
                world.addParticle(ParticleTypesInit.MIST.get(), d0 + d5, d1 + d6, d2 + d7, d5 / 40, 0.0D, d7 / 40);
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
        return slots;
    }

    @Override
    protected Container createMenu(int id, PlayerInventory player)
    {
        return new IceboxContainer(id, player, this, fuelData);
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
