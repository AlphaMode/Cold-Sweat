package dev.momostudios.coldsweat.common.block;

import dev.momostudios.coldsweat.common.te.HearthTileEntity;
import dev.momostudios.coldsweat.core.init.BlockInit;
import dev.momostudios.coldsweat.core.init.TileEntityInit;
import dev.momostudios.coldsweat.core.itemgroup.ColdSweatGroup;
import dev.momostudios.coldsweat.util.math.CSMath;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.loot.LootContext;
import net.minecraft.state.DirectionProperty;
import net.minecraft.state.IntegerProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3i;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraftforge.common.ToolType;
import net.minecraftforge.fml.network.NetworkHooks;

import java.util.*;

public class HearthBottomBlock extends Block
{
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final IntegerProperty WATER = IntegerProperty.create("water", 0, 2);
    public static final IntegerProperty LAVA = IntegerProperty.create("lava", 0, 2);

    private static final Map<Direction, VoxelShape> SHAPES = new HashMap<Direction, VoxelShape>();

    public static Properties getProperties()
    {
        return Properties
                .create(Material.ROCK)
                .sound(SoundType.STONE)
                .hardnessAndResistance(2f, 10f)
                .harvestTool(ToolType.PICKAXE)
                .harvestLevel(1)
                .notSolid()
                .setLightLevel(s -> 0)
                .setOpaque((bs, br, bp) -> false);
    }

    public static Item.Properties getItemProperties()
    {
        return new Item.Properties().group(ColdSweatGroup.COLD_SWEAT).maxStackSize(1);
    }

    public HearthBottomBlock(Properties properties)
    {
        super(properties);
        this.setDefaultState(this.stateContainer.getBaseState().with(FACING, Direction.NORTH).with(WATER, 0).with(LAVA, 0));
        runCalculation(VoxelShapes.or(
            makeCuboidShape(3, 0, 3.5, 13, 18, 12.5), // Shell
            makeCuboidShape(4, 18, 5, 9, 27, 10), // Exhaust
            makeCuboidShape(-1, 3, 6, 17, 11, 10))); // Canisters
    }

    static void calculateShapes(Direction to, VoxelShape shape)
    {
        VoxelShape[] buffer = new VoxelShape[] { shape, VoxelShapes.empty() };

        int times = (to.getHorizontalIndex() - Direction.NORTH.getHorizontalIndex() + 4) % 4;
        for (int i = 0; i < times; i++)
        {
            buffer[0].forEachBox((minX, minY, minZ, maxX, maxY, maxZ) -> buffer[1] = VoxelShapes.or(buffer[1],
                VoxelShapes.create(1 - maxZ, minY, minX, 1 - minZ, maxY, maxX)));
            buffer[0] = buffer[1];
            buffer[1] = VoxelShapes.empty();
        }

        SHAPES.put(to, buffer[0]);
    }

    static void runCalculation(VoxelShape shape)
    {
        for (Direction direction : Direction.values())
        {
            calculateShapes(direction, shape);
        }
    }

    @Override
    public VoxelShape getShape(BlockState state, IBlockReader world, BlockPos pos, ISelectionContext context)
    {
        return SHAPES.get(state.get(FACING));
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state, IBlockReader reader, BlockPos pos) {
        return true;
    }

    @SuppressWarnings("deprecation")
    @Override
    public ActionResultType onBlockActivated(BlockState state, World worldIn, BlockPos pos, PlayerEntity player, Hand hand, BlockRayTraceResult rayTraceResult)
    {
        if (!worldIn.isRemote)
        {
            if (worldIn.getTileEntity(pos) instanceof HearthTileEntity)
            {
                HearthTileEntity te = (HearthTileEntity) worldIn.getTileEntity(pos);
                ItemStack stack = player.getHeldItem(hand);

                // If the held item is a bucket, try to extract fluids
                if (stack.getItem() == Items.BUCKET)
                {
                    int lavaFuel = Math.abs(HearthTileEntity.getItemFuel(Items.LAVA_BUCKET.getDefaultInstance()));
                    if (te.getHotFuel() >= lavaFuel * 0.99)
                    {
                        Vector3i lavaSideOffset = state.get(FACING).rotateY().getDirectionVec();
                        Vector3d lavaSidePos = CSMath.getMiddle(pos)
                                                     .add(lavaSideOffset.getX() * 0.65, lavaSideOffset.getY() * 0.65, lavaSideOffset.getZ() * 0.65);

                        if (rayTraceResult.getHitVec().isWithinDistanceOf(lavaSidePos, 0.4))
                        {
                            if (lavaFuel > 0)
                            {
                                // Remove fuel
                                te.setHotFuel(te.getHotFuel() - lavaFuel);
                                // Give filled bucket item
                                if (stack.getCount() == 1)
                                {
                                    player.setHeldItem(hand, Items.LAVA_BUCKET.getDefaultInstance());
                                }
                                else
                                {
                                    stack.shrink(1);
                                    player.inventory.addItemStackToInventory(Items.LAVA_BUCKET.getDefaultInstance());
                                }
                                // Play bucket sound
                                worldIn.playSound(null, pos, SoundEvents.ITEM_BUCKET_FILL_LAVA, SoundCategory.BLOCKS, 1.0F, 0.9f + new Random().nextFloat() * 0.2F);

                                return ActionResultType.SUCCESS;
                            }
                        }
                    }
                    int waterFuel = Math.abs(HearthTileEntity.getItemFuel(Items.WATER_BUCKET.getDefaultInstance()));
                    if (te.getColdFuel() >= waterFuel * 0.99)
                    {
                        Vector3i waterSideOffset = state.get(FACING).rotateYCCW().getDirectionVec();
                        Vector3d waterSidePos = CSMath.getMiddle(pos)
                                                     .add(waterSideOffset.getX() * 0.65, waterSideOffset.getY() * 0.65, waterSideOffset.getZ() * 0.65);

                        if (rayTraceResult.getHitVec().isWithinDistanceOf(waterSidePos, 0.4))
                        {
                            if (waterFuel > 0)
                            {
                                // Remove fuel
                                te.setColdFuel(te.getColdFuel() - waterFuel);
                                // Give filled bucket item
                                if (stack.getCount() == 1)
                                {
                                    player.setHeldItem(hand, Items.WATER_BUCKET.getDefaultInstance());
                                }
                                else
                                {
                                    stack.shrink(1);
                                    player.inventory.addItemStackToInventory(Items.WATER_BUCKET.getDefaultInstance());
                                }
                                // Play bucket sound
                                worldIn.playSound(null, pos, SoundEvents.ITEM_BUCKET_FILL, SoundCategory.BLOCKS, 1.0F, 0.9f + new Random().nextFloat() * 0.2F);

                                return ActionResultType.SUCCESS;
                            }
                        }
                    }
                }

                // If the held item is fuel, try to insert the fuel
                int itemFuel = te.getItemFuel(stack);
                int hearthFuel = itemFuel > 0 ? te.getHotFuel() : te.getColdFuel();

                if (itemFuel != 0 && hearthFuel + Math.abs(itemFuel) * 0.75 < HearthTileEntity.MAX_FUEL)
                {
                    // Consume the item if not in creative
                    if (!player.isCreative())
                    {
                        if (stack.hasContainerItem())
                        {
                            ItemStack container = stack.getContainerItem();
                            player.setHeldItem(hand, container);
                        }
                        else
                        {
                            stack.shrink(1);
                        }
                    }
                    // Add the fuel
                    te.addFuel(itemFuel);

                    // Play the fuel filling sound
                    worldIn.playSound(null, pos, itemFuel > 0 ? SoundEvents.ITEM_BUCKET_EMPTY_LAVA : SoundEvents.ITEM_BUCKET_EMPTY,
                            SoundCategory.BLOCKS, 1.0F, 0.9f + new Random().nextFloat() * 0.2F);
                }
                // Open the GUI
                else
                {
                    NetworkHooks.openGui((ServerPlayerEntity) player, te, pos);
                }
            }
        }
        return ActionResultType.SUCCESS;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onBlockAdded(BlockState state, World worldIn, BlockPos pos, BlockState oldState, boolean isMoving)
    {
        if (worldIn.isAirBlock(pos.up()))
        {
            worldIn.setBlockState(pos.up(), BlockInit.HEARTH_TOP.get().getDefaultState().with(HearthTopBlock.FACING, state.get(FACING)), 2);
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public void neighborChanged(BlockState state, World worldIn, BlockPos pos, Block blockIn, BlockPos fromPos, boolean isMoving)
    {
        super.neighborChanged(state, worldIn, pos, blockIn, fromPos, isMoving);
        if (worldIn.getBlockState(pos.up()).getBlock() != BlockInit.HEARTH_TOP.get())
        {
            worldIn.destroyBlock(pos, false);
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public INamedContainerProvider getContainer(BlockState state, World worldIn, BlockPos pos) {
        TileEntity tileEntity = worldIn.getTileEntity(pos);
        return tileEntity instanceof INamedContainerProvider ? (INamedContainerProvider) tileEntity : null;
    }

    @Override
    public boolean hasTileEntity(BlockState state) {
        return true;
    }

    @Override
    public TileEntity createTileEntity(BlockState state, IBlockReader world) {
        return TileEntityInit.HEARTH_TILE_ENTITY_TYPE.get().create();
    }

    @SuppressWarnings("deprecation")
    @Override
    public List<ItemStack> getDrops(BlockState state, LootContext.Builder builder) {
        List<ItemStack> dropsOriginal = super.getDrops(state, builder);
        if (!dropsOriginal.isEmpty())
            return dropsOriginal;
        return Collections.singletonList(new ItemStack(this, 1));
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean isMoving)
    {
        if (!state.matchesBlock(newState.getBlock()))
        {
            if (world.getBlockState(pos.up()).getBlock() == BlockInit.HEARTH_TOP.get())
            {
                world.destroyBlock(pos.up(), false);
            }

            TileEntity tileentity = world.getTileEntity(pos);
            if (tileentity instanceof HearthTileEntity)
            {
                InventoryHelper.dropInventoryItems(world, pos, (HearthTileEntity) tileentity);
                world.updateComparatorOutputLevel(pos, this);
            }
        }
        super.onReplaced(state, world, pos, newState, isMoving);
    }

    @Override
    public BlockState rotate(BlockState state, IWorld world, BlockPos pos, Rotation direction)
    {
        return state.with(FACING, direction.rotate(state.get(FACING)));
    }

    @Override
    protected void fillStateContainer(StateContainer.Builder<Block, BlockState> builder)
    {
        builder.add(FACING, WATER, LAVA);
    }

    @Override
    public BlockState getStateForPlacement(BlockItemUseContext context)
    {
        World level = context.getWorld();
        BlockPos topPos = context.getPos().up();
        return level.getBlockState(topPos).isReplaceable(context) && level.getWorldBorder().contains(topPos)
                ? this.getDefaultState().with(FACING, context.getPlacementHorizontalFacing().getOpposite()).with(WATER, 0).with(LAVA, 0)
                : null;
    }
}
