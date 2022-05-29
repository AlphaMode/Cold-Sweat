package dev.momostudios.coldsweat.util.world;

import dev.momostudios.coldsweat.ColdSweat;
import dev.momostudios.coldsweat.core.network.ColdSweatPacketHandler;
import dev.momostudios.coldsweat.core.network.message.PlaySoundMessage;
import net.minecraft.block.*;
import net.minecraft.entity.Entity;
import net.minecraft.util.Direction;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.*;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.vector.Vector3i;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.gen.Heightmap;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.network.PacketDistributor;

import javax.annotation.Nonnull;
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

        Chunk chunk = world.getChunkProvider().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
        if (chunk == null) return mcHeight;

        for (int y = 0; y < world.getHeight(); y++)
        {
            BlockPos pos2 = new BlockPos(pos.getX(), y, pos.getZ());

            int chunkY = pos2.getY() >> 4;
            if (chunkY >= 0 && chunkY < chunk.getSections().length)
            {
                // Get the subchunk
                ChunkSection chunksection = chunk.getSections()[chunkY];

                // If this subchunk is only air, skip it
                if (chunksection.isEmpty())
                {
                    y += y % 16;
                    continue;
                }

                // Get the block state from this subchunk
                BlockState state = chunksection.getBlockState(pos2.getX() & 15, pos2.getY() & 15, pos2.getZ() & 15);
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

    public static boolean isSpreadBlocked(World world, @Nonnull BlockPos pos, @Nonnull Direction toDir)
    {
        Chunk chunk = world.getChunkProvider().getChunkNow(pos.getX() >> 4, pos.getZ() >>4);
        return chunk != null && isSpreadBlocked(chunk, pos, toDir);
    }

    public static boolean isSpreadBlocked(World world, BlockState state, @Nonnull BlockPos pos, @Nonnull Direction toDir)
    {
        Chunk chunk = world.getChunkProvider().getChunkNow(pos.getX() >> 4, pos.getZ() >>4);
        return chunk != null && isSpreadBlocked(chunk, state, pos, toDir);
    }

    public static boolean isSpreadBlocked(Chunk chunk, @Nonnull BlockPos pos, @Nonnull Direction toDir)
    {
        BlockState state = chunk.getSections()[pos.getY() >> 4].getBlockState(pos.getX() & 15, pos.getY() & 15, pos.getZ() & 15);
        return isSpreadBlocked(chunk, state, pos, toDir);
    }

    public static boolean isSpreadBlocked(@Nonnull Chunk chunk, BlockState state, @Nonnull BlockPos pos, @Nonnull Direction toDir)
    {
        World level = chunk.getWorld();

        if (state.isAir() || state.getCollisionShape(level, pos.offset(toDir)).isEmpty())
            return false;

        if (state.isSolidSide(level, pos, toDir))
            return true;

        return isFullSide(state, toDir, pos.offset(toDir), level) || state.isSolidSide(level, pos, toDir.getOpposite());
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

    /**
     * Plays a sound for all tracking clients that follows the source entity around.<br>
     * Why this isn't in Vanilla Minecraft is beyond me
     * @param sound The SoundEvent to play
     * @param entity The entity to attach the sound to (all tracking entities will hear the sound)
     * @param volume The volume of the sound
     * @param pitch The pitch of the sound
     */
    public static void playEntitySound(SoundEvent sound, Entity entity, float volume, float pitch)
    {
        ColdSweatPacketHandler.INSTANCE.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> entity),
                new PlaySoundMessage(sound.getRegistryName().toString(), volume, pitch, entity.getEntityId()));
    }
}
