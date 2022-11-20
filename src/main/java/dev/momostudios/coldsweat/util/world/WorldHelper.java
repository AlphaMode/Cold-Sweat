package dev.momostudios.coldsweat.util.world;

import dev.momostudios.coldsweat.core.network.ColdSweatPacketHandler;
import dev.momostudios.coldsweat.core.network.message.PlaySoundMessage;
import dev.momostudios.coldsweat.util.math.CSMath;
import net.minecraft.block.*;
import net.minecraft.entity.Entity;
import net.minecraft.util.Direction;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.*;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.IChunk;
import net.minecraft.world.gen.Heightmap;
import net.minecraftforge.fml.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

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
     * Gets all block positions in a grid centered around the given BlockPos<br>
     * @param pos The center of the search box.
     * @param samples The number of checks performed. Higher samples = more accurate & larger area.
     * @param interval How far apart each position in the grid is. Higher interval = less dense & larger area
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

    public static boolean canSeeSky(Chunk chunk, World level, BlockPos pos, int maxDistance)
    {
        for (int i = 0; i < Math.min(maxDistance, level.getHeight() - pos.getY()); i++)
        {
            BlockPos pos2 = pos.up(i);
            // Get the subchunk
            ChunkSection subchunk = getChunkSection(chunk, pos2.getY());

            // If this subchunk is only air, skip it
            if (subchunk == null || subchunk.isEmpty())
            {
                i += 16 - (i % 16);
                continue;
            }

            BlockState state = subchunk.getBlockState(pos2.getX() & 15, pos2.getY() & 15, pos2.getZ() & 15);
            if (isSpreadBlocked(level, state, pos2, Direction.UP, Direction.UP))
            {
                return false;
            }
        }
        return true;
    }

    public static boolean canSeeSky(World world, BlockPos pos, int maxDistance)
    {
        Chunk chunk = (Chunk) world.getChunkProvider().getChunk(pos.getX() >> 4, pos.getZ() >> 4, ChunkStatus.FULL, false);
        return chunk == null || canSeeSky(chunk, world, pos, maxDistance);
    }

    public static boolean isSpreadBlocked(World world, BlockState state, BlockPos pos, Direction toDir, Direction fromDir)
    {
        if (state.isAir()) return false;
        VoxelShape shape = state.getBlock().getShape(state, world, pos, ISelectionContext.dummy());

        return fromDir == toDir ? isFullSide(CSMath.flattenShape(toDir.getAxis(), shape), toDir)
                : (isFullSide(shape.project(toDir), toDir) || isFullSide(shape.project(fromDir.getOpposite()), fromDir.getOpposite()));
    }

    public static boolean isFullSide(VoxelShape shape, Direction dir)
    {
        if (shape.isEmpty()) return false;
        if (shape.equals(VoxelShapes.fullCube())) return true;

        // Return true if the 2D x/y area of the shape is >= 1
        double[] area = new double[1];
        switch (dir.getAxis())
        {
            case X:
            shape.forEachBox((x1, y1, z1, x2, y2, z2) -> area[0] += (y2 - y1) * (z2 - z1)); break;
            case Y:
            shape.forEachBox((x1, y1, z1, x2, y2, z2) -> area[0] += (x2 - x1) * (z2 - z1)); break;
            case Z:
            shape.forEachBox((x1, y1, z1, x2, y2, z2) -> area[0] += (x2 - x1) * (y2 - y1)); break;
        }
        return area[0] >= 1;
    }


    public static BlockState getBlockState(IChunk chunk, BlockPos blockpos)
    {
        int x = blockpos.getX();
        int y = blockpos.getY();
        int z = blockpos.getZ();

        try
        {
            return getChunkSection(chunk, y).getBlockState(x & 15, y & 15, z & 15);
        }
        catch (Exception e)
        {
            return chunk.getBlockState(blockpos);
        }
    }

    public static ChunkSection getChunkSection(IChunk chunk, int y)
    {
        ChunkSection[] sections = chunk.getSections();
        return sections[Math.min(sections.length - 1, y >> 4)];
    }

    /**
     * Plays a sound for all tracking clients that follows the source entity around.<br>
     * Why this isn't in Vanilla Minecraft is beyond me
     * @param sound The SoundEvent to play
     * @param entity The entity to attach the sound to (all tracking entities will hear the sound)
     * @param volume The volume of the sound
     * @param pitch The pitch of the sound
     */
    public static void playEntitySound(SoundEvent sound, Entity entity, SoundCategory category, float volume, float pitch)
    {
        ColdSweatPacketHandler.INSTANCE.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> entity),
                new PlaySoundMessage(sound.getRegistryName().toString(), category, volume, pitch, entity.getEntityId()));
    }

    /**
     * Iterates through every block along the given vector
     * @param from The starting position
     * @param to The ending position
     * @param rayTracer function to run on each found block
     * @param maxHits the maximum number of blocks to act upon before the ray expires
     */
    public static void forBlocksInRay(Vector3d from, Vector3d to, World world, BiConsumer<BlockState, BlockPos> rayTracer, int maxHits)
    {
        // Don't bother if the ray has no length
        if (!from.equals(to))
        {
            Vector3d ray = to.subtract(from);
            Vector3d normalRay = ray.normalize();
            BlockPos.Mutable pos = new BlockPos.Mutable();
            IChunk workingChunk = world.getChunkProvider().getChunk((int) from.x >> 4, (int) from.z >> 4, ChunkStatus.FULL, false);

            // Iterate over every block-long segment of the ray
            for (int i = 0; i < ray.length(); i++)
            {
                // Get the position of the current segment
                Vector3d vec = from.add(normalRay.scale(i));

                // Skip if the position is the same as the last one
                if (new BlockPos(vec).equals(pos)) continue;
                pos.setPos(vec.x, vec.y, vec.z);

                // Set new workingChunk if the ray travels outside the current one
                if (workingChunk == null || !workingChunk.getPos().equals(new ChunkPos(pos)))
                    workingChunk = world.getChunkProvider().getChunk(pos.getX() >> 4, pos.getZ() >> 4, ChunkStatus.FULL, false);

                if (workingChunk == null) continue;

                // Get the blockstate at the current position
                BlockState state = getChunkSection(workingChunk, pos.getY()).getBlockState(pos.getX() & 15, pos.getY() & 15, pos.getZ() & 15);

                // If the block isn't air, then we hit something
                if (!state.isAir())
                {
                    maxHits--;
                    if (maxHits <= 0) break;
                }
                rayTracer.accept(state, pos);
            }
        }
    }
}
