package dev.momostudios.coldsweat.common.te;

import com.mojang.datafixers.util.Pair;
import dev.momostudios.coldsweat.ColdSweat;
import dev.momostudios.coldsweat.common.container.HearthContainer;
import dev.momostudios.coldsweat.api.temperature.Temperature;
import dev.momostudios.coldsweat.config.ConfigCache;
import dev.momostudios.coldsweat.core.init.ParticleTypesInit;
import dev.momostudios.coldsweat.core.init.TileEntityInit;
import dev.momostudios.coldsweat.core.network.ColdSweatPacketHandler;
import dev.momostudios.coldsweat.core.network.message.BlockDataUpdateMessage;
import dev.momostudios.coldsweat.util.entity.NBTHelper;
import dev.momostudios.coldsweat.util.entity.TempHelper;
import dev.momostudios.coldsweat.util.math.CSMath;
import dev.momostudios.coldsweat.util.registries.ModEffects;
import dev.momostudios.coldsweat.util.registries.ModSounds;
import dev.momostudios.coldsweat.util.world.SpreadPath;
import dev.momostudios.coldsweat.util.world.WorldHelper;
import net.minecraft.block.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.IntNBT;
import net.minecraft.particles.BasicParticleType;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.potion.EffectInstance;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.LockableLootTileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.fml.network.PacketDistributor;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.registries.ForgeRegistries;
import dev.momostudios.coldsweat.common.block.HearthBottomBlock;
import dev.momostudios.coldsweat.config.ItemSettingsConfig;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public class HearthTileEntity extends LockableLootTileEntity implements ITickableTileEntity
{
    ConfigCache config = ConfigCache.getInstance();


    LinkedHashMap<BlockPos, SpreadPath> paths = new LinkedHashMap<>();
    HashMap<Pair<Integer, Integer>, Chunk> loadedChunks = new HashMap<>();

    public static final int MAX_PATHS = 6000;
    public static int MAX_DISTANCE = 20;

    static int INSULATION_TIME = 1200;

    public static int SLOT_COUNT = 1;
    protected NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    BlockPos pos = this.getPos();

    boolean shouldUseHotFuel = false;
    boolean shouldUseColdFuel = false;
    boolean hasHotFuel = false;
    boolean hasColdFuel = false;
    boolean hasFuelItem = false;

    boolean isPlayerNearby = false;
    boolean shouldRebuild = false;
    int rebuildCooldown = 0;

    public int ticksExisted;

    private Chunk workingChunk = null;
    private Pair<Integer, Integer> workingCoords = new Pair<>(pos.getX() >> 4, pos.getZ() >> 4);

    public static final int MAX_FUEL = 1000;

    public HearthTileEntity(TileEntityType<?> tileEntityTypeIn)
    {
        super(tileEntityTypeIn);
        this.addPath(new SpreadPath(this.pos));
    }

    public HearthTileEntity()
    {
        this(TileEntityInit.HEARTH_TILE_ENTITY_TYPE.get());
    }

    @Override
    protected ITextComponent getDefaultName() {
        return new TranslationTextComponent("container." + ColdSweat.MOD_ID + ".hearth");
    }

    @Override
    public ITextComponent getDisplayName()
    {
        return this.getCustomName() != null ? this.getCustomName() : this.getDefaultName();
    }

    @Override
    public CompoundNBT getUpdateTag()
    {
        CompoundNBT tag = super.getUpdateTag();
        tag.putInt("hotFuel",  this.getHotFuel());
        tag.putInt("coldFuel", this.getColdFuel());
        return tag;
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

    @Override
    public void tick()
    {
        pos = this.getPos();

        this.ticksExisted++;

        int hotFuel = this.getHotFuel();
        int coldFuel = this.getColdFuel();

        if (rebuildCooldown > 0) rebuildCooldown--;

        // Gradually increases insulation amount
        int insulationLevel = NBTHelper.incrementNBT(this, "insulationLevel", 1, (nbt) -> nbt < INSULATION_TIME);

        if (this.world != null && this.ticksExisted % 20 == 0)
        {
            this.isPlayerNearby = false;
            for (PlayerEntity player : this.world.getPlayers())
            {
                if (player.getDistanceSq(this.pos.getX() + 0.5, this.pos.getY() + 0.5, this.pos.getZ() + 0.5) < 400)
                {
                    this.isPlayerNearby = true;
                    break;
                }
            }
        }

        // Reset if a nearby block has been updated
        if (rebuildCooldown <= 0 && this.shouldRebuild)
        {
            this.shouldRebuild = false;
            this.resetPaths();
        }

        if (world != null && (hotFuel > 0 || coldFuel > 0) && this.isPlayerNearby)
        {
            boolean showParticles = world.isRemote && this.getTileData().getBoolean("showRadius");
            int pathCount = paths.size();

            // Create temporary list to add back to the master path list
            HashMap<BlockPos, SpreadPath> newPaths = new HashMap<>();

            /*
             Partition all points into multiple lists (max of 19)
            */
            int index = 0;
            // Size of each partition
            int partSize = 400;
            // Number of partitions
            int partCount = (int) Math.ceil(pathCount / (double) partSize);
            // Index of the last point being worked on this tick
            int lastIndex = Math.min(pathCount, partSize * (this.ticksExisted % partCount + 1) + 1);
            // Index of the first point being worked on this tick
            int firstIndex = Math.max(0, lastIndex - partSize);

            // Iterate over the specified partition of paths
            for (Map.Entry<BlockPos, SpreadPath> entry : paths.entrySet())
            {
                // Skip until we reach the first index of this partition
                if (index > lastIndex) break;

                if (index < firstIndex)
                {
                    index++;
                    continue;
                }

                index++;

                SpreadPath spreadPath = entry.getValue();

                int x = spreadPath.getX();
                int y = spreadPath.getY();
                int z = spreadPath.getZ();

                // Air Particles
                if (showParticles)
                {
                    // Spawn particles if enabled
                    if (Minecraft.getInstance().gameSettings.showDebugInfo)
                    {
                        world.addParticle(ParticleTypes.FLAME, false, x + 0.5, y + 0.5, z + 0.5, 0, 0, 0);
                    }
                    else if (Math.random() < 0.016)
                    {
                        double xr = Math.random();
                        double yr = Math.random();
                        double zr = Math.random();
                        double xm = Math.random() / 20 - 0.025;
                        double zm = Math.random() / 20 - 0.025;

                        world.addParticle(ParticleTypesInit.HEARTH_AIR.get(), false, x + xr, y + yr, z + zr, xm, 0, zm);
                    }
                }

                // Give insulation to players
                if (!world.isRemote)
                {
                    for (PlayerEntity player : world.getPlayers())
                    {
                        if (player.getDistanceSq(x, y, z) < 1.2)
                        {
                            EffectInstance effect = player.getActivePotionEffect(ModEffects.INSULATION);
                            boolean hasEffect = effect != null;

                            if (!hasEffect || effect.getDuration() < 60)
                            {
                                // Get the player's temperature
                                double temp = hasEffect ? player.getPersistentData().getDouble("preHearthTemp") :
                                        TempHelper.getTemperature(player, Temperature.Types.WORLD).get();

                                // Tell the hearth to use hot fuel
                                shouldUseHotFuel = shouldUseHotFuel || (temp < config.minTemp);

                                // Tell the hearth to use cold fuel
                                shouldUseColdFuel = shouldUseColdFuel || (temp > config.maxTemp);

                                if (shouldUseHotFuel || shouldUseColdFuel)
                                {
                                    int effectLevel = Math.max(0, (int) ((insulationLevel / (double) INSULATION_TIME) * 9));
                                    player.addPotionEffect(new EffectInstance(ModEffects.INSULATION, 100, effectLevel, false, false));
                                }
                            }
                            break;
                        }
                    }
                }

                if (spreadPath.isFrozen)
                {
                    continue;
                }

                // Try to spread to new blocks
                if (pathCount < MAX_PATHS && spreadPath.withinDistance(pos, MAX_DISTANCE))
                {
                    // Get the chunk at this position
                    Pair<Integer, Integer> chunkPos = Pair.of(x >> 4, z >> 4);

                    Chunk chunk;

                    if (chunkPos.equals(workingCoords))
                    {
                        chunk = workingChunk;
                    }
                    else
                    {
                        workingChunk = chunk = loadedChunks.get(chunkPos);
                        workingCoords = chunkPos;
                    }

                    if (chunk == null)
                    {
                        loadedChunks.put(chunkPos, chunk = world.getChunkProvider().getChunkNow(x >> 4, z >> 4));
                        workingChunk = chunk;
                    }
                    if (chunk == null) continue;

                    if (!WorldHelper.canSeeSky(chunk, world, spreadPath.getPos()))
                    {
                        for (Direction direction : Direction.values())
                        {
                            SpreadPath tryPath = spreadPath.offset(direction);
                            BlockPos tryPos = tryPath.getPos();

                            SpreadPath existingPath = paths.get(tryPos);

                            if (existingPath == null)
                            {
                                if (WorldHelper.canSpreadThrough(chunk, spreadPath.getPos(), tryPath.origin))
                                    newPaths.put(tryPos, tryPath);
                            }
                        }
                    }
                }
                spreadPath.isFrozen = true;
            }

            // Add new positions
            paths.putAll(newPaths);

            // Drain fuel
            if (!world.isRemote && this.ticksExisted % 80 == 0)
            {
                if (shouldUseColdFuel)
                {
                    this.setColdFuel(coldFuel - 1);
                }
                if (shouldUseHotFuel)
                {
                    this.setHotFuel(hotFuel - 1);
                }

                shouldUseColdFuel = false;
                shouldUseHotFuel = false;
            }
        }

        // Input fuel types
        if (this.ticksExisted % 20 == 0)
        {
            this.hasFuelItem = !getItemInSlot(0).isEmpty();
        }
        if (this.hasFuelItem)
        {
            ItemStack fuelItem = this.getItemInSlot(0);
            int fuel = getItemFuel(fuelItem);
            if (fuel != 0)
            {
                if ((fuel > 0 ? hotFuel : coldFuel) <= MAX_FUEL - Math.abs(fuel) * 0.75)
                {
                    if (fuelItem.hasContainerItem())
                    {
                        if (fuelItem.getCount() == 1)
                        {
                            this.setItemInSlot(0, fuelItem.getContainerItem());
                        }
                    }
                    else fuelItem.shrink(1);
                    this.addFuel(fuel, hotFuel, coldFuel);
                }
            }
        }

        // Particles
        if (Math.random() < coldFuel / 3000d)
        {
            double d0 = pos.getX() + 0.5d;
            double d1 = pos.getY() + 1.8d;
            double d2 = pos.getZ() + 0.5d;
            double d3 = (Math.random() - 0.5) / 4;
            double d4 = (Math.random() - 0.5) / 4;
            double d5 = (Math.random() - 0.5) / 4;
            world.addParticle(ParticleTypesInit.STEAM.get(), d0 + d3, d1 + d4, d2 + d5, 0.0D, 0.04D, 0.0D);
        }
        if (Math.random() < hotFuel / 3000d)
        {
            double d0 = pos.getX() + 0.5d;
            double d1 = pos.getY() + 1.8d;
            double d2 = pos.getZ() + 0.5d;
            double d3 = (Math.random() - 0.5) / 2;
            double d4 = (Math.random() - 0.5) / 2;
            double d5 = (Math.random() - 0.5) / 2;
            BasicParticleType particle = Math.random() < 0.5 ? ParticleTypes.LARGE_SMOKE : ParticleTypes.SMOKE;
            world.addParticle(particle, d0 + d3, d1 + d4, d2 + d5, 0.0D, 0.0D, 0.0D);
        }
    }

    public void resetPaths()
    {
        Map<BlockPos, SpreadPath> newlist = new HashMap<>();
        SpreadPath path = new SpreadPath(pos);
        newlist.put(pos, path);
        this.replacePaths(newlist);
    }

    public static int getItemFuel(ItemStack item)
    {
        for (List<?> iterator : new ItemSettingsConfig().hearthItems())
        {
            String testItem = (String) iterator.get(0);

            if (new ResourceLocation(testItem).equals(ForgeRegistries.ITEMS.getKey(item.getItem())))
            {
                return ((Number) iterator.get(1)).intValue();
            }
        }
        return 0;
    }

    public ItemStack getItemInSlot(int index)
    {
        AtomicReference<ItemStack> stack = new AtomicReference<>(ItemStack.EMPTY);
        this.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null).ifPresent(capability ->
        {
            stack.set(capability.getStackInSlot(index));
        });
        return stack.get();
    }

    public void setItemInSlot(int index, ItemStack stack)
    {
        this.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null).ifPresent(capability ->
        {
            if (stack != null && stack != capability.getStackInSlot(index))
            {
                capability.extractItem(index, capability.getStackInSlot(index).getCount(), false);
            }
            capability.insertItem(index, stack, false);
        });
    }

    public int getHotFuel()
    {
        return this.getTileData().getInt("hotFuel");
    }

    public int getColdFuel()
    {
        return this.getTileData().getInt("coldFuel");
    }

    public void setHotFuel(int amount)
    {
        this.getTileData().putInt("hotFuel", (int) CSMath.clamp(amount, 0, MAX_FUEL));
        this.updateFuelState();

        if (amount == 0 && hasHotFuel)
        {
            hasHotFuel = false;
            world.playSound(null, pos, ModSounds.HEARTH_FUEL, SoundCategory.BLOCKS, 1, (float) Math.random() * 0.2f + 0.9f);
        }
        else hasHotFuel = true;
    }

    public void setColdFuel(int amount)
    {
        this.getTileData().putInt("coldFuel", (int) CSMath.clamp(amount, 0, MAX_FUEL));
        this.updateFuelState();

        if (amount == 0 && hasColdFuel)
        {
            hasColdFuel = false;
            world.playSound(null, pos, ModSounds.HEARTH_FUEL, SoundCategory.BLOCKS, 1, (float) Math.random() * 0.2f + 0.9f);
        }
        else hasColdFuel = true;
    }

    public void addFuel(int amount)
    {
        this.addFuel(amount, this.getHotFuel(), this.getColdFuel());
    }

    public void addFuel(int amount, int hotFuel, int coldFuel)
    {
        if (amount > 0)
        {
            setHotFuel(hotFuel + Math.abs(amount));
        }
        else if (amount < 0)
        {
            setColdFuel(coldFuel + Math.abs(amount));
        }
    }

    public void updateFuelState()
    {
        if (world != null && !world.isRemote)
        {
            int hotFuel = this.getHotFuel();
            int coldFuel = this.getColdFuel();

            BlockState state = world.getBlockState(pos);
            int waterLevel = coldFuel == 0 ? 0 : (coldFuel < MAX_FUEL / 2 ? 1 : 2);
            int lavaLevel = hotFuel == 0 ? 0 : (hotFuel < MAX_FUEL / 2 ? 1 : 2);

            BlockState desiredState = state.with(HearthBottomBlock.WATER, waterLevel).with(HearthBottomBlock.LAVA, lavaLevel);
            if (state.get(HearthBottomBlock.WATER) != waterLevel || state.get(HearthBottomBlock.LAVA) != lavaLevel)
            {
                world.setBlockState(pos, desiredState, 3);
            }
            this.markDirty();

            ColdSweatPacketHandler.INSTANCE.send(PacketDistributor.TRACKING_CHUNK.with(() -> world.getChunkAt(pos)),
                    new BlockDataUpdateMessage(pos, Arrays.asList("hotFuel", "coldFuel"), Arrays.asList(IntNBT.valueOf(hotFuel), IntNBT.valueOf(coldFuel))));
        }
    }

    @Override
    public int getSizeInventory()
    {
        return SLOT_COUNT;
    }

    @Override
    protected Container createMenu(int id, PlayerInventory playerInv)
    {
        return new HearthContainer(id, playerInv, this);
    }

    @Override
    public void read(BlockState state, CompoundNBT nbt)
    {
        super.read(state, nbt);
        this.setColdFuel(nbt.getInt("coldFuel"));
        this.setHotFuel(nbt.getInt("hotFuel"));
        this.items = NonNullList.withSize(this.getSizeInventory(), ItemStack.EMPTY);
    }

    @Override
    public CompoundNBT write(CompoundNBT nbt)
    {
        super.write(nbt);
        nbt.putInt("coldFuel", this.getColdFuel());
        nbt.putInt("hotFuel", this.getHotFuel());
        return nbt;
    }

    public void replacePaths(Map<BlockPos, SpreadPath> newPaths)
    {
        paths.clear();
        this.addPaths(newPaths);
    }

    public void addPath(SpreadPath path) {
        paths.put(path.getPos(), path);
    }

    public void addPath(BlockPos pos, SpreadPath path) {
        paths.put(pos, path);
    }

    public void addPaths(Map<BlockPos, SpreadPath> newPaths)
    {
        for (Map.Entry<BlockPos, SpreadPath> entry : newPaths.entrySet())
        {
            this.addPath(entry.getKey(), entry.getValue());
        }
    }

    public boolean shouldRebuild()
    {
        return shouldRebuild;
    }

    public void setShouldRebuild(boolean shouldRebuild)
    {
        this.shouldRebuild = shouldRebuild;
    }
}