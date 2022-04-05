package dev.momostudios.coldsweat.util.world;

import dev.momostudios.coldsweat.util.world.SpreadPath;
import net.minecraft.block.*;
import net.minecraft.block.material.Material;
import net.minecraft.util.Direction;
import net.minecraft.util.math.*;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.vector.Vector3i;
import net.minecraft.util.palette.PalettedContainer;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.Heightmap;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class WorldHelper
{
    /**
     * Iterates through every other block until it reaches minecraft:air, then returns the Y value<br>
     * Ignores minecraft:cave_air<br>
     * This is different from {@code world.getHeight()} because it attempts to ignore blocks that are floating in the air
     */
    public static int getGroundLevel(BlockPos pos, World world)
    {
        // If Minecraft's height calculation is correct, use that
        int mcHeight = world.getHeight(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, pos.getX(), pos.getZ());
        if (pos.getY() >= mcHeight)
            return mcHeight;

        for (int c = 0; c < 255; c++)
        {
            BlockPos pos2 = new BlockPos(pos.getX(), c, pos.getZ());
            BlockState state = world.getBlockState(pos2);
            if (state.getMaterial() == Material.AIR && state.getBlock() != Blocks.CAVE_AIR)
            {
                return c;
            }
        }
        return 0;
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
        return canSeeSky(chunk, world, pos);
    }

    public static boolean canSeeSky(Chunk chunk, World world, BlockPos pos)
    {
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();

        if (chunk != null)
        {
            for (int i = 1; i < 319 - y; i++)
            {
                BlockState state = chunk.getBlockState(new BlockPos(x, y + i, z));

                if (state.isAir())
                {
                    continue;
                }

                if (isFullSide(state, Direction.DOWN, pos.up(i), world) || isFullSide(state, Direction.UP, pos.up(i), world))
                    return false;
            }
        }
        return true;
    }

    public static boolean canSpreadThrough(World world, @Nonnull SpreadPath path, @Nonnull Direction toDir, @Nullable Direction fromDir)
    {
        BlockPos pos = path.getPos();
        Chunk chunk = world.getChunkProvider().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
        if (chunk != null)
        {
            BlockState state = chunk.getBlockState(pos);

            if (state.isSolidSide(world, pos, toDir))
                return false;

            return !isFullSide(state, toDir, pos.offset(toDir), world) && !state.isSolidSide(world, pos, toDir.getOpposite());
        }
        return false;
    }

    public static boolean canSpreadThrough(World world, BlockState state, @Nonnull BlockPos pos, @Nonnull Direction toDir)
    {
        if (state.isAir())
            return true;

        return !isFullSide(state, toDir, pos.offset(toDir), world) && !state.isSolidSide(world, pos, toDir.getOpposite());
    }

    public static boolean canSpreadThrough(Chunk chunk, @Nonnull BlockPos pos, @Nonnull Direction toDir)
    {
        BlockState state = chunk.getSections()[pos.getY() >> 4].getBlockState(pos.getX() & 15, pos.getY() & 15, pos.getZ() & 15);
        return canSpreadThrough(chunk, state, pos, toDir);
    }

    public static boolean canSpreadThrough(@Nonnull Chunk chunk, BlockState state, @Nonnull BlockPos pos, @Nonnull Direction toDir)
    {
        World level = chunk.getWorld();

        if (state.isAir() || state.getCollisionShape(level, pos.offset(toDir)).isEmpty())
            return true;

        if (state.isSolidSide(level, pos, toDir))
            return false;

        return !isFullSide(state, toDir, pos.offset(toDir), level) && !state.isSolidSide(level, pos, toDir.getOpposite());
    }

    public static double distance(Vector3i pos1, Vector3i pos2)
    {
        return Math.sqrt(pos1.distanceSq(pos2));
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

    public static void schedule(Runnable runnable, int delayTicks)
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
}
