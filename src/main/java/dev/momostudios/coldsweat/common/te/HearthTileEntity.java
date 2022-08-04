package dev.momostudios.coldsweat.common.te;

import com.mojang.datafixers.util.Pair;
import dev.momostudios.coldsweat.ColdSweat;
import dev.momostudios.coldsweat.api.temperature.modifier.HearthTempModifier;
import dev.momostudios.coldsweat.common.container.HearthContainer;
import dev.momostudios.coldsweat.api.temperature.Temperature;
import dev.momostudios.coldsweat.core.init.ParticleTypesInit;
import dev.momostudios.coldsweat.core.init.TileEntityInit;
import dev.momostudios.coldsweat.core.network.ColdSweatPacketHandler;
import dev.momostudios.coldsweat.core.network.message.HearthResetMessage;
import dev.momostudios.coldsweat.util.config.ConfigCache;
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
import net.minecraftforge.registries.ForgeRegistries;
import dev.momostudios.coldsweat.common.block.HearthBottomBlock;
import dev.momostudios.coldsweat.config.ItemSettingsConfig;

import java.util.*;

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
    BlockPos pos;

    int coldFuel = 0;
    int hotFuel = 0;
    boolean shouldUseHotFuel = false;
    boolean shouldUseColdFuel = false;
    boolean hasHotFuel = false;
    boolean hasColdFuel = false;

    boolean isPlayerNearby = false;
    boolean shouldRebuild = false;
    int rebuildCooldown = 0;
    int insulationLevel = 0;

    public int ticksExisted;

    private Chunk workingChunk = null;
    private Pair<Integer, Integer> workingCoords = new Pair<>(this.getPos().getX() >> 4, this.getPos().getZ() >> 4);

    public static final int MAX_FUEL = 1000;

    public HearthTileEntity(TileEntityType<?> tileEntityTypeIn)
    {
        super(tileEntityTypeIn);
        pos = this.getPos();
        this.addPath(new SpreadPath(this.getPos()));
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
        paths.put(this.getPos(), new SpreadPath(this.getPos()));

        this.ticksExisted++;

        int hotFuel = this.getHotFuel();
        int coldFuel = this.getColdFuel();

        if (rebuildCooldown > 0) rebuildCooldown--;

        // Gradually increases insulation amount
        if (insulationLevel < INSULATION_TIME)
        {
            insulationLevel++;
            if (this.ticksExisted % 20 == 0)
            {
                this.getTileData().putInt("insulationLevel", insulationLevel);
            }
        }

        if (this.world != null && this.ticksExisted % 20 == 0)
        {
            this.isPlayerNearby = false;
            for (PlayerEntity player : this.world.getPlayers())
            {
                if (player.getDistanceSq(this.getPos().getX() + 0.5, this.getPos().getY() + 0.5, this.getPos().getZ() + 0.5) < 400)
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
            this.rebuildCooldown = 100;
            this.resetPaths();
            if (!world.isRemote)
            {
                ColdSweatPacketHandler.INSTANCE.send(PacketDistributor.TRACKING_CHUNK.with(() -> world.getChunkProvider().getChunkNow(this.getPos().getX() >> 4, this.getPos().getZ() >> 4)), new HearthResetMessage(this.getPos()));
            }
        }

        if (world != null && (hotFuel > 0 || coldFuel > 0) && this.isPlayerNearby)
        {
            boolean showParticles = world.isRemote && !this.getTileData().getBoolean("hideParticles") && Minecraft.getInstance().gameSettings.particles == ParticleStatus.ALL;
            int pathCount = paths.size();

            Map<PlayerEntity, Pair<EffectInstance, HearthTempModifier>> playerInsulation = new HashMap<>();
            for (PlayerEntity player : world.getPlayers())
            {
                if (player.getDistanceSq(this.getPos().getX() + 0.5, this.getPos().getY() + 0.5, this.getPos().getZ() + 0.5) < MAX_DISTANCE * MAX_DISTANCE)
                {
                    playerInsulation.put(player, Pair.of(player.getActivePotionEffect(ModEffects.INSULATION), TempHelper.getModifier(player, Temperature.Type.WORLD, HearthTempModifier.class)));
                }
            }

            // Create temporary list to add back to the master path list
            LinkedHashMap<BlockPos, SpreadPath> newPaths = new LinkedHashMap<>();

            /*
             Partition the points into logical "sub-maps" to be iterated over separately each tick
            */
            // Starting index (-1 because it is incremented before it is gotten)
            int index = -1;
            // Size of each partition (scales dynamically with the number of paths)
            int partSize = CSMath.clamp(pathCount / 30, 10, 200);
            // Number of partitions
            int partCount = (int) Math.ceil(pathCount / (double) partSize);
            // Index of the last point being worked on this tick
            int lastIndex = Math.min(pathCount, partSize * (this.ticksExisted % partCount + 1) + 1);
            // Index of the first point being worked on this tick
            int firstIndex = Math.max(0, lastIndex - partSize);

            // Iterate over the specified partition of paths
            for (SpreadPath spreadPath : paths.values())
            {
                index++;
                if (index < firstIndex) continue;
                if (index >= lastIndex) break;

                int x = spreadPath.x;
                int y = spreadPath.y;
                int z = spreadPath.z;

                // Air Particles
                if (showParticles)
                {
                    // Spawn flame particles if F3 is up
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
                        if (CSMath.getDistance(player, x + 0.5, y + 0.5, z + 0.5) > 0.6) continue;

                        Pair<EffectInstance, HearthTempModifier> playerData = playerInsulation.get(player);
                        EffectInstance effect = playerData.getFirst();

                        if (effect == null || effect.getDuration() < 60)
                        {
                            HearthTempModifier mod = playerData.getSecond();
                            // Get the player's temperature
                            double temp = (mod != null) ? mod.getLastInput().get() : TempHelper.getTemperature(player, Temperature.Type.WORLD).get();

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

                if (spreadPath.frozen)
                {
                    continue;
                }

                // Try to spread to new blocks
                if (pathCount < MAX_PATHS && spreadPath.withinDistance(this.getPos(), MAX_DISTANCE))
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
                                if (!WorldHelper.isSpreadBlocked(chunk, spreadPath.getPos(), tryPath.origin))
                                    newPaths.put(tryPos, tryPath);
                            }
                        }
                    }
                }
                spreadPath.frozen = true;
            }

            // Add new positions
            paths.putAll(newPaths);

            // Drain fuel
            if (this.ticksExisted % 80 == 0)
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
                    if (fuelStack.hasContainerItem() && fuelStack.getCount() == 1)
                    {
                        this.setItemInSlot(0, fuelStack.getContainerItem());
                        addFuel(itemFuel, hotFuel, coldFuel);
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
        if (Math.random() < coldFuel / 3000d)
        {
            double d0 = this.getPos().getX() + 0.5d;
            double d1 = this.getPos().getY() + 1.8d;
            double d2 = this.getPos().getZ() + 0.5d;
            double d3 = (Math.random() - 0.5) / 4;
            double d4 = (Math.random() - 0.5) / 4;
            double d5 = (Math.random() - 0.5) / 4;
            world.addParticle(ParticleTypesInit.STEAM.get(), d0 + d3, d1 + d4, d2 + d5, 0.0D, 0.04D, 0.0D);
        }
        if (Math.random() < hotFuel / 3000d)
        {
            double d0 =  this.getPos().getX() + 0.5d;
            double d1 =  this.getPos().getY() + 1.8d;
            double d2 =  this.getPos().getZ() + 0.5d;
            double d3 = (Math.random() - 0.5) / 2;
            double d4 = (Math.random() - 0.5) / 2;
            double d5 = (Math.random() - 0.5) / 2;
            BasicParticleType particle = Math.random() < 0.5 ? ParticleTypes.LARGE_SMOKE : ParticleTypes.SMOKE;
            world.addParticle(particle, d0 + d3, d1 + d4, d2 + d5, 0.0D, 0.0D, 0.0D);
        }
    }

    public void resetPaths()
    {
        Map<BlockPos, SpreadPath> newMap = new HashMap<>();
        SpreadPath path = new SpreadPath(this.getPos());
        newMap.put(this.getPos(), path);
        this.replacePaths(newMap);
    }

    public static int getItemFuel(ItemStack item)
    {
        for (List<?> iterator : new ItemSettingsConfig().hearthItems())
        {
            String testItem = (String) iterator.get(0);

            ResourceLocation testRegistry = ForgeRegistries.ITEMS.getKey(item.getItem());
            if (testRegistry != null && testItem.equals(testRegistry.toString()))
            {
                return ((Number) iterator.get(1)).intValue();
            }
        }
        return 0;
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

        if (amount == 0 && hasHotFuel)
        {
            hasHotFuel = false;
            world.playSound(null, pos, ModSounds.HEARTH_FUEL, SoundCategory.BLOCKS, 1, (float) Math.random() * 0.2f + 0.9f);
        }
        else hasHotFuel = true;
    }

    public void setColdFuel(int amount)
    {
        this.coldFuel = amount;
        this.markDirty();
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
        if (world != null)
        {
            int hotFuel = this.getHotFuel();
            int coldFuel = this.getColdFuel();

            BlockState state = world.getBlockState(this.getPos());
            int waterLevel = coldFuel == 0 ? 0 : (coldFuel < MAX_FUEL / 2 ? 1 : 2);
            int lavaLevel = hotFuel == 0 ? 0 : (hotFuel < MAX_FUEL / 2 ? 1 : 2);

            BlockState desiredState = state.with(HearthBottomBlock.WATER, waterLevel).with(HearthBottomBlock.LAVA, lavaLevel);
            if (state.get(HearthBottomBlock.WATER) != waterLevel || state.get(HearthBottomBlock.LAVA) != lavaLevel)
            {
                world.setBlockState(this.getPos(), desiredState, 3);
            }
            this.markDirty();
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
        ItemStackHelper.loadAllItems(nbt, items);
    }

    @Override
    public CompoundNBT write(CompoundNBT nbt)
    {
        super.write(nbt);
        nbt.putInt("coldFuel", this.getColdFuel());
        nbt.putInt("hotFuel", this.getHotFuel());
        ItemStackHelper.saveAllItems(nbt, this.items);
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

    public void attemptReset(boolean shouldRebuild)
    {
        this.shouldRebuild = shouldRebuild;
    }
}