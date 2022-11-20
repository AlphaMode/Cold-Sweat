package dev.momostudios.coldsweat.common.te;

import com.mojang.datafixers.util.Pair;
import dev.momostudios.coldsweat.ColdSweat;
import dev.momostudios.coldsweat.api.temperature.modifier.HearthTempModifier;
import dev.momostudios.coldsweat.common.container.HearthContainer;
import dev.momostudios.coldsweat.api.temperature.Temperature;
import dev.momostudios.coldsweat.common.event.HearthPathManagement;
import dev.momostudios.coldsweat.util.world.TaskScheduler;
import dev.momostudios.coldsweat.core.init.ParticleTypesInit;
import dev.momostudios.coldsweat.core.init.TileEntityInit;
import dev.momostudios.coldsweat.core.network.ColdSweatPacketHandler;
import dev.momostudios.coldsweat.core.network.message.BlockDataUpdateMessage;
import dev.momostudios.coldsweat.core.network.message.HearthResetMessage;
import dev.momostudios.coldsweat.util.config.ConfigSettings;
import dev.momostudios.coldsweat.api.util.TempHelper;
import dev.momostudios.coldsweat.util.math.CSMath;
import dev.momostudios.coldsweat.util.registries.ModEffects;
import dev.momostudios.coldsweat.util.registries.ModSounds;
import dev.momostudios.coldsweat.util.world.SpreadPath;
import dev.momostudios.coldsweat.util.world.WorldHelper;
import net.minecraft.block.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.ParticleStatus;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.inventory.container.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.potion.EffectInstance;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.LockableLootTileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;
import net.minecraft.util.NonNullList;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraftforge.fml.network.PacketDistributor;
import dev.momostudios.coldsweat.common.block.HearthBottomBlock;

import java.util.*;
import java.util.stream.Collectors;

public class HearthTileEntity extends LockableLootTileEntity implements ITickableTileEntity
{
    public final HashMap<BlockPos, Float> renderPoints = new HashMap<>();

    ConfigSettings config = ConfigSettings.getInstance();

    ArrayList<SpreadPath> paths = new ArrayList<>();
    // Used as a lookup table for detecting duplicate paths (faster than ArrayList#contains())
    HashSet<BlockPos> pathLookup = new HashSet<>();

    HashMap<ChunkPos, Chunk> loadedChunks = new HashMap<>();

    static int INSULATION_TIME = 1200;

    public static int SLOT_COUNT = 1;
    protected NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    BlockPos pos = new BlockPos(0, 0, 0);

    int coldFuel = 0;
    int hotFuel = 0;
    boolean shouldUseHotFuel = false;
    boolean shouldUseColdFuel = false;
    boolean hasHotFuel = false;
    boolean hasColdFuel = false;
    int insulationLevel = 0;

    boolean isPlayerNearby = false;
    int rebuildCooldown = 0;
    boolean forceRebuild = false;
    LinkedList<BlockPos> notifyQueue = new LinkedList<>();

    public int ticksExisted;

    private Chunk workingChunk = null;
    private ChunkPos workingCoords = new ChunkPos(this.getPos().getX() >> 4, this.getPos().getZ() >> 4);

    boolean showParticles = true;
    int frozenPaths = 0;
    boolean spreading = true;

    public static final int MAX_FUEL = 1000;

    public HearthTileEntity(TileEntityType<?> tileEntityTypeIn)
    {
        super(tileEntityTypeIn);
        TaskScheduler.scheduleServer(() -> HearthPathManagement.HEARTH_POSITIONS.put(this.getPos(), this.spreadRange()), 1);
    }

    public HearthTileEntity()
    {
        this(TileEntityInit.HEARTH_TILE_ENTITY_TYPE.get());
    }

    public int spreadRange()
    {
        return 20;
    }

    public int maxPaths()
    {
        return 6000;
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
        tag.putInt("insulationLevel", insulationLevel);
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
        if (pos.equals(new BlockPos(0, 0, 0))) pos = this.getPos();
        if (paths.isEmpty())
        {
            addPath(new SpreadPath(this.getPos()));
            addPath(new SpreadPath(this.getPos().up()));
        }

        this.ticksExisted++;

        if (rebuildCooldown > 0) rebuildCooldown--;

        List<? extends PlayerEntity> players = new ArrayList<>(this.world.getPlayers());

        if (this.world != null && this.ticksExisted % 20 == 0)
        {
            this.isPlayerNearby = false;
            for (PlayerEntity player : this.world.getPlayers())
            {
                if (player.getPosition().withinDistance(pos, this.spreadRange()))
                {
                    this.isPlayerNearby = true;
                    break;
                }
            }
        }

        // Reset if a nearby block has been updated
        if (forceRebuild || (rebuildCooldown <= 0 && !notifyQueue.isEmpty()))
        {
            boolean shouldRebuild = false;

            // If the rebuild is forced, skip the queue and rebuild immediately
            if (forceRebuild)
                shouldRebuild = true;
            else
            {
                // Iterate over every position in the queue
                // If any of them are in the path lookup, rebuild
                for (BlockPos notifyPos : notifyQueue)
                {
                    if (pathLookup.contains(notifyPos))
                    {
                        shouldRebuild = true;
                        break;
                    }
                }
            }

            if (shouldRebuild)
            {
                // Reset cooldown
                this.rebuildCooldown = 100;

                // Reset paths
                this.replacePaths(Arrays.asList(new SpreadPath(pos), new SpreadPath(pos.up())));
                frozenPaths = 0;
                spreading = true;

                // Tell client to reset paths too
                if (!world.isRemote)
                    ColdSweatPacketHandler.INSTANCE.send(PacketDistributor.TRACKING_CHUNK.with(() -> world.getChunkProvider().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4)), new HearthResetMessage(pos));
            }
            notifyQueue.clear();
            forceRebuild = false;
        }

        if (world != null && (hotFuel > 0 || coldFuel > 0))
        {
            // Gradually increases insulation amount
            if (insulationLevel < INSULATION_TIME)
                insulationLevel++;

            if (this.isPlayerNearby)
            {
                if (this.ticksExisted % 10 == 0)
                {
                    showParticles = world.isRemote
                            && Minecraft.getInstance().gameSettings.particles == ParticleStatus.ALL
                            && !HearthPathManagement.DISABLED_HEARTHS.contains(Pair.of(pos, world.getDimensionKey().getLocation().toString()));
                }

                /*
                 Partition the points into logical "sub-maps" to be iterated over separately each tick
                */
                int pathCount = paths.size();
                // Size of each partition (defaults to 1/30th of the total paths)
                int partSize = CSMath.clamp(pathCount / 30, 10, 200);
                // Number of partitions
                int partCount = (int) CSMath.ceil(pathCount / (double) partSize);
                // Index of the last point being worked on this tick
                int lastIndex = Math.min(pathCount, partSize * ((this.ticksExisted % partCount) + 1));
                // Index of the first point being worked on this tick
                int firstIndex = Math.max(0, lastIndex - partSize);

                /*
                 Iterate over the specified partition of paths
                 */
                for (int i = firstIndex; i < Math.min(paths.size(), lastIndex); i++)
                {
                    SpreadPath spreadPath = paths.get(i);
                    renderPoints.put(spreadPath.getPos(), 0.2f);

                    int x = spreadPath.getX();
                    int y = spreadPath.getY();
                    int z = spreadPath.getZ();

                    try
                    {
                        // Don't try to spread if the path is frozen
                        if (spreadPath.isFrozen())
                        {
                            // Remove a 3D checkerboard pattern of paths after the Hearth is finished spreading
                            // (Halves the number of iterations)
                            // The Hearth is "finished spreading" when all paths are frozen
                            if (!spreading && ((Math.abs(y % 2) == 0) == (Math.abs(x % 2) == Math.abs(z % 2))))
                            {
                                paths.remove(i);
                                // Go back and iterate over the new path at this index
                                i--;
                            }
                            continue;
                        }

                        /*
                         Try to spread to new blocks
                         */
                        if (pathCount < this.maxPaths() && spreadPath.withinDistance(pos, this.spreadRange()))
                        {
                            /*
                             Get the chunk at this position
                             */
                            ChunkPos chunkPos = new ChunkPos(x >> 4, z >> 4);
                            Chunk chunk;

                            if (chunkPos == workingCoords)
                                chunk = workingChunk;
                            else
                            {
                                if ((chunk = loadedChunks.get(chunkPos)) == null)
                                {
                                    loadedChunks.put(chunkPos, workingChunk = chunk = (Chunk) world.getChunkProvider().getChunk(x >> 4, z >> 4, ChunkStatus.FULL, false));
                                    if (chunk == null) continue;
                                }
                                workingCoords = chunkPos;
                                workingChunk = chunk;
                            }

                            /*
                             Spreading algorithm
                             */
                            BlockPos pathPos = spreadPath.getPos();
                            BlockState blockState = WorldHelper.getBlockState(chunk, pathPos);

                            if (!WorldHelper.canSeeSky(chunk, world, pathPos.up(), 64))
                            {
                                // Try to spread in every direction from the current position
                                for (Direction direction : Direction.values())
                                {
                                    SpreadPath tryPath = spreadPath.offset(direction);

                                    // Avoid duplicate paths (ArrayList isn't duplicate-safe like Sets/Maps)
                                    if (pathLookup.add(tryPath.getPos()) && !WorldHelper.isSpreadBlocked(world, blockState, pathPos, direction, spreadPath.getDirection()))
                                    {
                                        // Add the new path to the temporary list and lookup table
                                        paths.add(tryPath);
                                    }
                                }
                            }
                            // Remove this path if it has skylight access
                            else
                            {
                                pathLookup.remove(pathPos);
                                paths.remove(i);
                                i--;
                                continue;
                            }
                        }
                        if (!spreadPath.isFrozen())
                        {
                            // Track frozen paths to know when the Hearth is done spreading
                            spreadPath.freeze();
                            this.frozenPaths++;
                        }
                        if (this.frozenPaths >= pathCount)
                            this.spreading = false;
                    }
                    /*
                     Give insulation & spawn particles
                     */
                    finally
                    {
                        // Air Particles
                        if (world.isRemote && showParticles)
                        {
                            Random rand = new Random();
                            if (!Minecraft.getInstance().gameSettings.showDebugInfo && rand.nextFloat() < (spreading ? 0.016f : 0.032f))
                            {
                                float xr = rand.nextFloat();
                                float yr = rand.nextFloat();
                                float zr = rand.nextFloat();
                                float xm = rand.nextFloat() / 20 - 0.025f;
                                float zm = rand.nextFloat() / 20 - 0.025f;

                                world.addParticle(ParticleTypesInit.HEARTH_AIR.get(), false, x + xr, y + yr, z + zr, xm, 0, zm);
                            }
                        }

                        // Give insulation to players
                        if (!world.isRemote)
                        {
                            for (int p = 0; p < players.size(); p++)
                            {
                                PlayerEntity player = players.get(p);
                                // If player is null or not in range, skip
                                if (player == null || CSMath.getDistance(spreadPath.getPos(), player.getPosition()) > 1)
                                    continue;

                                EffectInstance effect = player.getActivePotionEffect(ModEffects.INSULATION);

                                if (effect == null || effect.getDuration() < 60
                                && !WorldHelper.canSeeSky(player.world, new BlockPos(player.getPosX(), CSMath.ceil(player.getPosY()), player.getPosZ()), 64))
                                {
                                    this.insulatePlayer(player);
                                    break;
                                }

                                players.remove(p);
                                p--;
                            }
                        }
                    }
                }
            }

            // Drain fuel
            if (this.ticksExisted % 80 == 0)
            {
                if (shouldUseColdFuel)
                    this.setColdFuel(coldFuel - 1);
                if (shouldUseHotFuel)
                    this.setHotFuel(hotFuel - 1);

                shouldUseColdFuel = false;
                shouldUseHotFuel = false;
            }
        }

        // Input fuel
        if (this.ticksExisted % 10 == 0)
        {
            ItemStack fuelStack = this.getItemInSlot(0);
            int itemFuel = getItemFuel(fuelStack);
            if (itemFuel != 0)
            {
                int fuel = itemFuel > 0 ? hotFuel : coldFuel;
                if (fuel < MAX_FUEL - Math.abs(itemFuel) * 0.75)
                {
                    if (fuelStack.hasContainerItem())
                    {
                        if (fuelStack.getCount() == 1)
                        {
                            this.setItemInSlot(0, fuelStack.getContainerItem());
                            addFuel(itemFuel, hotFuel, coldFuel);
                        }
                    }
                    else
                    {
                        int consumeCount = Math.min((int) Math.floor((MAX_FUEL - fuel) / (double) Math.abs(itemFuel)), fuelStack.getCount());
                        fuelStack.shrink(consumeCount);
                        addFuel(itemFuel * consumeCount, hotFuel, coldFuel);
                    }
                }
            }
        }

        // Particles
        if (world.isRemote)
        {
            Random rand = new Random();
            if (rand.nextDouble() < coldFuel / 3000d)
            {
                double d0 = pos.getX() + 0.5d;
                double d1 = pos.getY() + 1.8d;
                double d2 = pos.getZ() + 0.5d;
                double d3 = (rand.nextDouble() - 0.5) / 4;
                double d4 = (rand.nextDouble() - 0.5) / 4;
                double d5 = (rand.nextDouble() - 0.5) / 4;
                world.addParticle(ParticleTypesInit.STEAM.get(), d0 + d3, d1 + d4, d2 + d5, 0.0D, 0.04D, 0.0D);
            }
            if (rand.nextDouble() < hotFuel / 3000d)
            {
                double d0 = pos.getX() + 0.5d;
                double d1 = pos.getY() + 1.8d;
                double d2 = pos.getZ() + 0.5d;
                double d3 = (rand.nextDouble() - 0.5) / 2;
                double d4 = (rand.nextDouble() - 0.5) / 2;
                double d5 = (rand.nextDouble() - 0.5) / 2;
                world.addParticle(rand.nextDouble() < 0.5 ? ParticleTypes.LARGE_SMOKE : ParticleTypes.SMOKE, d0 + d3, d1 + d4, d2 + d5, 0.0D, 0.0D, 0.0D);
            }
        }
    }

    boolean insulatePlayer(PlayerEntity player)
    {
        // Get the player's temperature
        if (!(shouldUseHotFuel && shouldUseColdFuel))
        {
            HearthTempModifier mod = TempHelper.getModifier(player, Temperature.Type.WORLD, HearthTempModifier.class);
            double temp = (mod != null) ? mod.getLastInput().get() : TempHelper.getTemperature(player, Temperature.Type.WORLD).get();

            // Tell the hearth to use hot fuel
            shouldUseHotFuel = shouldUseHotFuel || (hotFuel > 0 && temp < config.minTemp);
            // Tell the hearth to use cold fuel
            shouldUseColdFuel = shouldUseColdFuel || (coldFuel > 0 && temp > config.maxTemp);
        }

        if (shouldUseHotFuel || shouldUseColdFuel)
        {
            int effectLevel = Math.min(9, (int) ((insulationLevel / (double) INSULATION_TIME) * 9));
            player.addPotionEffect(new EffectInstance(ModEffects.INSULATION, 100, effectLevel, false, false));
            return true;
        }
        return false;
    }

    public static int getItemFuel(ItemStack item)
    {
        return ConfigSettings.HEARTH_FUEL_ITEMS.get().getOrDefault(item.getItem(), 0d).intValue();
    }

    public ItemStack getItemInSlot(int index)
    {
        return items.get(index);
    }

    public void setItemInSlot(int index, ItemStack stack)
    {
        items.set(index, stack);
    }

    public int getHotFuel()
    {
        return hotFuel;
    }

    public int getColdFuel()
    {
        return coldFuel;
    }

    public void setHotFuel(int amount)
    {
        this.hotFuel = amount;
        this.markDirty();
        this.updateFuelState();

        if (amount == 0)
        {
            if (hasHotFuel)
                world.playSound(null, pos, ModSounds.HEARTH_FUEL, SoundCategory.BLOCKS, 1, (float) Math.random() * 0.2f + 0.9f);
            hasHotFuel = false;
        }
        else hasHotFuel = true;
    }

    public void setColdFuel(int amount)
    {
        this.coldFuel = amount;
        this.markDirty();
        this.updateFuelState();

        if (amount == 0)
        {
            if (hasColdFuel)
                world.playSound(null, pos, ModSounds.HEARTH_FUEL, SoundCategory.BLOCKS, 1, (float) Math.random() * 0.2f + 0.9f);
            hasColdFuel = false;
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

            BlockState state = world.getBlockState(this.getPos());
            int waterLevel = coldFuel == 0 ? 0 : (coldFuel < MAX_FUEL / 2 ? 1 : 2);
            int lavaLevel = hotFuel == 0 ? 0 : (hotFuel < MAX_FUEL / 2 ? 1 : 2);

            BlockState desiredState = state.with(HearthBottomBlock.WATER, waterLevel).with(HearthBottomBlock.LAVA, lavaLevel);
            if (state.get(HearthBottomBlock.WATER) != waterLevel || state.get(HearthBottomBlock.LAVA) != lavaLevel)
                world.setBlockState(this.getPos(), desiredState, 3);

            this.markDirty();

            CompoundNBT tag = new CompoundNBT();
            this.write(tag);
            ColdSweatPacketHandler.INSTANCE.send(PacketDistributor.TRACKING_CHUNK.with(() -> world.getChunkAt(pos)), new BlockDataUpdateMessage(this));
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
        this.items = NonNullList.withSize(this.getSizeInventory(), ItemStack.EMPTY);
        super.read(state, nbt);
        this.setColdFuel(nbt.getInt("coldFuel"));
        this.setHotFuel(nbt.getInt("hotFuel"));
        this.insulationLevel = nbt.getInt("insulationLevel");
        ItemStackHelper.loadAllItems(nbt, items);
    }

    @Override
    public CompoundNBT write(CompoundNBT nbt)
    {
        super.write(nbt);
        nbt.putInt("coldFuel", this.getColdFuel());
        nbt.putInt("hotFuel", this.getHotFuel());
        nbt.putInt("insulationLevel", this.insulationLevel);
        ItemStackHelper.saveAllItems(nbt, this.items);
        return nbt;
    }

    void replacePaths(Collection<SpreadPath> newPaths)
    {
        paths.clear();
        this.addPaths(newPaths);
        pathLookup.clear();
        pathLookup.addAll(newPaths.stream().map(SpreadPath::getPos).collect(Collectors.toList()));
    }

    void addPath(SpreadPath path)
    {
        paths.add(path);
    }

    void addPaths(Collection<SpreadPath> newPaths)
    {
        paths.addAll(newPaths);
    }

    public void sendBlockUpdate(BlockPos pos)
    {
        notifyQueue.add(pos);
    }

    public void resetPaths()
    {
        this.forceRebuild = true;
    }

    @Override
    public void remove()
    {
        super.remove();
        HearthPathManagement.HEARTH_POSITIONS.remove(this.pos);
    }

    public Set<BlockPos> getPaths()
    {
        return pathLookup;
    }
}