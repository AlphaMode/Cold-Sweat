package dev.momostudios.coldsweat.util.world;

import dev.momostudios.coldsweat.ColdSweat;
import dev.momostudios.coldsweat.core.network.ColdSweatPacketHandler;
import dev.momostudios.coldsweat.core.network.message.PlaySoundMessage;
import net.minecraft.block.*;
import net.minecraft.entity.Entity;
import net.minecraft.util.Direction;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.*;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.gen.Heightmap;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

public class WorldHelper
{
    /**
     * Iterates through every other block until it reaches minecraft:air, then returns the Y value<br>
     * Ignores minecraft:cave_air<br>
     * This is different from {@code world.getHeight()} because it attempts to ignore blocks that are floating in the air
     */
    public static int getGroundLevel(BlockPos pos, World world)
    {
        // If Minecraft's height calculation is good enough, use that
        int mcHeight = world.getHeight(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, pos.getX(), pos.getZ());
        if (pos.getY() >= mcHeight)
            return mcHeight;

        Chunk chunk = world.getChunkProvider().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
        if (chunk == null) return mcHeight;

        for (int y = 0; y < world.getHeight(); y++)
        {
            BlockPos pos2 = new BlockPos(pos.getX(), y, pos.getZ());

            int chunkY = pos2.getY() >> 4;
            if (chunkY >= 0 && chunkY < chunk.getSections().length)
            {
                // Get the subchunk
                ChunkSection subchunk = chunk.getSections()[chunkY];
                if (subchunk == null) return mcHeight;

                // If this subchunk is only air, skip it
                if (subchunk.isEmpty())
                {
                    y += y % 16;
                    continue;
                }

                // Get the block state from this subchunk
                BlockState state = subchunk.getBlockState(pos2.getX() & 15, pos2.getY() & 15, pos2.getZ() & 15);
                // If this block is a surface block, return the Y
                if (state.isAir() && state.getBlock() != Blocks.CAVE_AIR)
                {
                    return y;
                }
            }
        }
        return mcHeight;
    }

    /**
     * Gets the average biome temperature in a grid of BlockPos 3 blocks apart<br>
     * Search area scales with the number of samples
     * @param pos is the center of the search box
     * @param samples is the number of checks performed. Higher samples = more accurate but more resource-intensive too
     * @param interval is how far apart each check is. Higher values means less dense and larger search area
     */
    public static List<BlockPos> getNearbyPositions(BlockPos pos, int samples, int interval)
    {
        List<BlockPos> posList = new ArrayList<>();
        int sampleRoot = (int) Math.sqrt(samples);

        for (int sx = 0; sx < sampleRoot; sx++)
        {
            for (int sz = 0; sz < sampleRoot; sz++)
            {
                int length = interval * sampleRoot;
                posList.add(pos.add(sx * interval - (length / 2), 0, sz * interval - (length / 2)));
            }
        }

        return posList;
    }

    public static boolean canSeeSky(World world, BlockPos pos)
    {
        Chunk chunk = world.getChunkProvider().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
        return chunk == null || canSeeSky(chunk, world, pos);
    }

    public static boolean canSeeSky(Chunk chunk, World world, BlockPos pos)
    {
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();

        for (int i = 1; i < world.getHeight() - y; i++)
        {
            BlockState state = getBlockState(chunk, new BlockPos(x, y + i, z));

            if (state.isAir())
            {
                continue;
            }

            if (isFullSide(state, Direction.DOWN, pos.up(i), world) || isFullSide(state, Direction.UP, pos.up(i), world))
                return false;
        }
        return true;
    }

    public static boolean isSpreadBlocked(World world, BlockPos pos, Direction toDir)
    {
        Chunk chunk = world.getChunkProvider().getChunkNow(pos.getX() >> 4, pos.getZ() >>4);
        return chunk == null || isSpreadBlocked(chunk, pos, toDir);
    }

    public static boolean isSpreadBlocked(Chunk chunk, BlockPos pos, Direction toDir)
    {
        ChunkSection[] sections = chunk.getSections();
        int subChunkY = pos.getY() >> 4;

        if (sections.length < subChunkY) return true;

        ChunkSection section = sections[subChunkY];
        BlockState state = section.getBlockState(pos.getX() & 15, pos.getY() & 15, pos.getZ() & 15);

        return isSpreadBlocked(chunk, state, pos, toDir);
    }

    public static boolean isSpreadBlocked(Chunk chunk, BlockState state, BlockPos pos, Direction toDir)
    {
        World world = chunk.getWorld();
        BlockPos offsetPos = pos.offset(toDir);

        if (state.isAir() || state.getCollisionShape(world, offsetPos).isEmpty())
            return false;

        if (state.isSolidSide(world, pos, toDir))
            return true;

        return isFullSide(state, toDir, offsetPos, world) || state.isSolidSide(world, pos, toDir.getOpposite());
    }

    public static boolean isFullSide(BlockState state, Direction dir, BlockPos pos, World world)
    {
        if (state.isSolidSide(world, pos, dir))
            return true;
        if (state.isAir())
            return false;

        VoxelShape shape = state.getRenderShape(world, pos);
        final double[] area = {0};
        if (!shape.isEmpty())
        {
            shape.forEachBox((minX, minY, minZ, maxX, maxY, maxZ) ->
            {
                if (area[0] < 1)
                    switch (dir.getAxis())
                    {
                        case X:
                            area[0] += (maxY - minY) * (maxZ - minZ);
                            break;
                        case Y:
                            area[0] += (maxX - minX) * (maxZ - minZ);
                            break;
                        case Z:
                            area[0] += (maxX - minX) * (maxY - minY);
                            break;
                    }
            });
            return area[0] >= 1;
        }
        return false;
    }

    /**
     * Executes the given Runnable on the serverside after a specified delay
     * @param runnable The code to execute
     * @param delayTicks The delay in ticks
     */
    public static void schedule(Runnable runnable, int delayTicks)
    {
        try
        {
            new Object()
            {
                private int ticks = 0;

                public void start()
                {
                    MinecraftForge.EVENT_BUS.register(this);
                }

                @SubscribeEvent
                public void tick(TickEvent.ServerTickEvent event)
                {
                    if (event.phase == TickEvent.Phase.END)
                    {
                        ticks++;
                        if (ticks >= delayTicks)
                            run();
                    }
                }

                private void run()
                {
                    runnable.run();
                    MinecraftForge.EVENT_BUS.unregister(this);
                }
            }.start();
        }
        catch (Exception e)
        {
            ColdSweat.LOGGER.error("Failed to schedule action!");
            e.printStackTrace();
        }
    }

    public static BlockState getBlockState(Chunk chunk, BlockPos blockpos)
    {
        int x = blockpos.getX();
        int y = blockpos.getY();
        int z = blockpos.getZ();

        ChunkSection[] sections = chunk.getSections();
        try
        {
            return sections[blockpos.getY() >> 4].getBlockState(x & 15, y & 15, z & 15);
        }
        catch (Exception e)
        {
            return chunk.getBlockState(blockpos);
        }
    }

    /**
     * Plays a sound for all tracking clients that follows the source entity around.<br>
     * Why this isn't in Vanilla Minecraft is beyond me
     * @param sound The SoundEvent to play
     * @param entity The entity to attach the sound to (all tracking entities will hear the sound)
     * @param volume The volume of the sound
     * @param pitch The pitch of the sound
     */
    public static void playEntitySound(SoundEvent sound, SoundCategory category, Entity entity, float volume, float pitch)
    {
        ColdSweatPacketHandler.INSTANCE.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> entity),
                new PlaySoundMessage(sound.getRegistryName().toString(), category, volume, pitch, entity.getEntityId()));
    }

    public static <T> List<T> gatherRTResults(RayTraceContext context, BiFunction<RayTraceContext, BlockPos, T> rayTracer)
    {
        List<T> rayTraces = new ArrayList<>();
        Vector3d vector3d = context.getStartVec();
        Vector3d vector3d1 = context.getEndVec();

        if (!vector3d.equals(vector3d1))
        {
            double d0 = MathHelper.lerp(-1.0E-7D, vector3d1.x, vector3d.x);
            double d1 = MathHelper.lerp(-1.0E-7D, vector3d1.y, vector3d.y);
            double d2 = MathHelper.lerp(-1.0E-7D, vector3d1.z, vector3d.z);
            double d3 = MathHelper.lerp(-1.0E-7D, vector3d.x, vector3d1.x);
            double d4 = MathHelper.lerp(-1.0E-7D, vector3d.y, vector3d1.y);
            double d5 = MathHelper.lerp(-1.0E-7D, vector3d.z, vector3d1.z);
            int i = MathHelper.floor(d3);
            int j = MathHelper.floor(d4);
            int k = MathHelper.floor(d5);
            BlockPos.Mutable blockpos$mutable = new BlockPos.Mutable(i, j, k);

            double d6 = d0 - d3;
            double d7 = d1 - d4;
            double d8 = d2 - d5;
            int l = MathHelper.signum(d6);
            int i1 = MathHelper.signum(d7);
            int j1 = MathHelper.signum(d8);
            double d9 = l == 0 ? Double.MAX_VALUE : (double) l / d6;
            double d10 = i1 == 0 ? Double.MAX_VALUE : (double) i1 / d7;
            double d11 = j1 == 0 ? Double.MAX_VALUE : (double) j1 / d8;
            double d12 = d9 * (l > 0 ? 1.0D - MathHelper.frac(d3) : MathHelper.frac(d3));
            double d13 = d10 * (i1 > 0 ? 1.0D - MathHelper.frac(d4) : MathHelper.frac(d4));
            double d14 = d11 * (j1 > 0 ? 1.0D - MathHelper.frac(d5) : MathHelper.frac(d5));

            while (d12 <= 1.0D || d13 <= 1.0D || d14 <= 1.0D)
            {
                if (d12 < d13)
                {
                    if (d12 < d14)
                    {
                        i += l;
                        d12 += d9;
                    }
                    else
                    {
                        k += j1;
                        d14 += d11;
                    }
                }
                else if (d13 < d14)
                {
                    j += i1;
                    d13 += d10;
                }
                else
                {
                    k += j1;
                    d14 += d11;
                }

                T result1 = rayTracer.apply(context, blockpos$mutable.setPos(i, j, k));
                if (result1 != null)
                {
                    rayTraces.add(result1);
                }
            }

        }
        return rayTraces;
    }
}
