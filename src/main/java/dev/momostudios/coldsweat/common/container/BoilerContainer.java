package dev.momostudios.coldsweat.common.container;

import dev.momostudios.coldsweat.common.item.FilledWaterskinItem;
import dev.momostudios.coldsweat.common.blockentity.BoilerBlockEntity;
import dev.momostudios.coldsweat.core.init.MenuInit;
import dev.momostudios.coldsweat.util.math.CSMath;
import dev.momostudios.coldsweat.util.registries.ModItems;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Objects;

public class BoilerContainer extends AbstractContainerMenu
{
    public final BoilerBlockEntity te;

    public BoilerContainer(final int windowId, final Inventory playerInv, final BoilerBlockEntity te)
    {
        super(MenuInit.BOILER_CONTAINER_TYPE.get(), windowId);
        this.te = te;

        // Fuel slot
        this.addSlot(new Slot(te, 0, 80, 62)
        {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return te.getItemFuel(stack) > 0;
            }
        });

        // Waterskins
        for (int in = 1; in < 10; in++)
        {
            this.addSlot(new Slot(te, in, -10 + in * 18, 35)
            {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return stack.getItem() instanceof FilledWaterskinItem;
                }
            });
        }

        // Main player inventory
        for (int row = 0; row < 3; row++)
        {
            for (int col = 0; col < 9; col++)
            {
                this.addSlot(new Slot(playerInv, col + (9 * row) + 9, 8 + col * 18, 163 - (4 - row) * 18));
            }
        }

        // Player Hotbar
        for (int col = 0; col < 9; col++)
        {
            this.addSlot(new Slot(playerInv, col, 8 + col * 18, 149));
        }
    }

    public BoilerContainer(final int windowId, final Inventory playerInv, final FriendlyByteBuf data)
    {
        this(windowId, playerInv, getTileEntity(playerInv, data));
    }

    public int getFuel()
    {
        return this.te.getFuel();
    }


    private static BoilerBlockEntity getTileEntity(final Inventory playerInv, final FriendlyByteBuf data)
    {
        Objects.requireNonNull(playerInv, "Player inventory cannot be null");
        Objects.requireNonNull(data, "PacketBuffer inventory cannot be null");
        final BlockEntity te = playerInv.player.level.getBlockEntity(data.readBlockPos());
        if (te instanceof BoilerBlockEntity)
        {
            return (BoilerBlockEntity) te;
        }
        throw new IllegalStateException("Tile Entity is not correct");
    }

    @Override
    public boolean stillValid(Player playerIn)
    {
        return playerIn.distanceToSqr(this.te.getBlockPos().getX(), this.te.getBlockPos().getY(), this.te.getBlockPos().getZ()) <= 64.0D;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index)
    {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem())
        {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();
            if (CSMath.withinRange(index, 0, 9))
            {
                if (!this.moveItemStackTo(itemstack1, 10, 46, true))
                {
                    return ItemStack.EMPTY;
                }

                slot.onQuickCraft(itemstack1, itemstack);
            }
            else
            {
                if (itemstack.getItem() == ModItems.FILLED_WATERSKIN)
                {
                    if (!this.moveItemStackTo(itemstack1, 1, 10, false))
                    {
                        slot.onQuickCraft(itemstack1, itemstack);
                        return ItemStack.EMPTY;
                    }
                }
                else if (this.te.getItemFuel(itemstack) > 0)
                {
                    if (!this.moveItemStackTo(itemstack1, 0, 1, false))
                    {
                        slot.onQuickCraft(itemstack1, itemstack);
                        return ItemStack.EMPTY;
                    }
                }
                else if (CSMath.withinRange(index, slots.size() - 9, slots.size()))
                {
                    if (!this.moveItemStackTo(itemstack1, 10, 36, false))
                    {
                        slot.onQuickCraft(itemstack1, itemstack);
                        return ItemStack.EMPTY;
                    }
                }
                else if (CSMath.withinRange(index, 10, slots.size() - 9))
                {
                    if (!this.moveItemStackTo(itemstack1, slots.size() - 9, slots.size(), false))
                    {
                        slot.onQuickCraft(itemstack1, itemstack);
                        return ItemStack.EMPTY;
                    }
                }
                return ItemStack.EMPTY;
            }

            if (itemstack1.isEmpty())
            {
                slot.set(ItemStack.EMPTY);
            }
            else
            {
                slot.setChanged();
            }

            slot.onTake(player, itemstack1);
        }

        return itemstack;
    }
}
