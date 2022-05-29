package dev.momostudios.coldsweat.api.temperature.modifier;

import dev.momostudios.coldsweat.api.registry.BlockEffectRegistry;
import dev.momostudios.coldsweat.api.temperature.Temperature;
import dev.momostudios.coldsweat.api.temperature.block_effect.BlockEffect;
import dev.momostudios.coldsweat.util.math.CSMath;
import dev.momostudios.coldsweat.util.world.WorldHelper;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;

import java.util.HashMap;
import java.util.Map;

public class BlockTempModifier extends TempModifier
{
    Map<ChunkPos, Chunk> chunkMap = new HashMap<>();

    public BlockTempModifier() {}

    @Override
    public Temperature getResult(Temperature temp, PlayerEntity player)
    {
        if (player.ticksExisted % 20 == 0)
        {
            chunkMap.clear();
        }

        double totalTemp = 0;
        World level = player.world;

        for (int x = -7; x < 14; x++)
        {
            for (int z = -7; z < 14; z++)
            {
                ChunkPos chunkPos = new ChunkPos((player.getPosition().getX() + x) >> 4, (player.getPosition().getZ() + z) >> 4);
                Chunk chunk = getChunk(level, chunkPos, chunkMap);

                for (int y = -7; y < 14; y++)
                {
                    try
                    {
                        BlockPos blockpos = player.getPosition().add(x, y, z);

                        ChunkSection section = chunk.getSections()[blockpos.getY() >> 4];
                        BlockState state = section.getBlockState(blockpos.getX() & 15, blockpos.getY() & 15, blockpos.getZ() & 15);

                        if (state.isAir()) continue;

                        // Get the BlockEffect associated with the block
                        BlockEffect be = BlockEffectRegistry.getEntryFor(state);

                        if (be == null || be.equals(BlockEffectRegistry.DEFAULT_BLOCK_EFFECT)) continue;

                        // Is totalTemp within the bounds of the BlockEffect's min/max allowed temps?
                        if (CSMath.isBetween(totalTemp, be.minEffect(), be.maxEffect())
                                && CSMath.isBetween(temp.get() + totalTemp, be.minTemperature(), be.maxTemperature()))
                        {
                            // Get Vector positions of the centers of the source block and player
                            Vector3d pos = new Vector3d(blockpos.getX() + 0.5, blockpos.getY() + 0.5, blockpos.getZ() + 0.5);
                            Vector3d playerPos1 = new Vector3d(player.getPosX(), player.getPosY() + player.getEyeHeight() * 0.25, player.getPosZ());
                            Vector3d playerPos2 = new Vector3d(player.getPosX(), player.getPosY() + player.getEyeHeight(), player.getPosZ());

                            // Get the temperature of the block given the player's distance
                            double distance = CSMath.getDistance(player, new Vector3d(pos.x, pos.y, pos.z));
                            double tempToAdd = be.getTemperature(player, state, blockpos, distance);

                            // Cast a ray between the player and the block
                            // Lessen the effect with each block between the player and the block
                            Vector3d prevPos1 = playerPos1;
                            Vector3d prevPos2 = playerPos2;
                            int blocksBetween = 0;
                            for (int i = 0; i < distance * 1.25; i++)
                            {
                                // Get the position on the line of this current iteration
                                double factor = (i / (distance * 1.25));

                                // Get the next block (sub)position
                                double x1 = playerPos1.x - (playerPos1.x - pos.x) * factor;
                                double y1 = playerPos1.y - (playerPos1.y - pos.y) * factor;
                                double z1 = playerPos1.z - (playerPos1.z - pos.z) * factor;
                                Vector3d newPos1 = new Vector3d(x1, y1, z1);

                                double x2 = playerPos2.x - (playerPos2.x - pos.x) * factor;
                                double y2 = playerPos2.y - (playerPos2.y - pos.y) * factor;
                                double z2 = playerPos2.z - (playerPos2.z - pos.z) * factor;
                                Vector3d newPos2 = new Vector3d(x2, y2, z2);

                                /*
                                 Check if the newPos1 is not a duplicate BlockPos or solid block
                                 */
                                BlockPos bpos1 = new BlockPos(newPos1);
                                Vector3d facing1 = newPos1.subtract(prevPos1);
                                Direction dir1 = CSMath.getDirectionFromVector(facing1.x, facing1.y, facing1.z);

                                Chunk newChunk = null;

                                //Skip this iteration if this is a duplicate BlockPos
                                if (!bpos1.equals(new BlockPos(prevPos1)) && !bpos1.equals(blockpos))
                                {
                                    // Only get the blockstate if we're actually looking at this position
                                    BlockState state1 = section.getBlockState((int) x1 & 15, (int) y1 & 15, (int) z1 & 15);
                                    // Only get the chunk if we're actually looking at this position
                                    newChunk = getChunk(level, new ChunkPos(bpos1), chunkMap);

                                    if (WorldHelper.isSpreadBlocked(newChunk, state1, bpos1, dir1))
                                    {
                                        // Divide the added temperature by 2 for each block between the player and the block
                                        blocksBetween++;
                                    }
                                }

                                /*
                                 Check if the newPos2 is not a duplicate BlockPos or solid block
                                 */
                                BlockPos bpos2 = new BlockPos(newPos2);
                                // Skip this iteration if the head/feet rays intersect (it will be the same blockstate)
                                if (bpos2 != bpos1)
                                {
                                    Vector3d facing2 = newPos2.subtract(prevPos2);
                                    Direction dir2 = CSMath.getDirectionFromVector(facing2.x, facing2.y, facing2.z);

                                    // Skip this iteration if this is a duplicate BlockPos
                                    if (!bpos2.equals(new BlockPos(prevPos2)) && !bpos2.equals(blockpos))
                                    {
                                        // Only get the blockstate if we're actually looking at this position
                                        BlockState state2 = section.getBlockState((int) x2 & 15, (int) y2 & 15, (int) z2 & 15);
                                        // Only get the chunk if we're actually looking at this position
                                        if (newChunk == null) newChunk = getChunk(level, new ChunkPos(bpos1), chunkMap);

                                        if (WorldHelper.isSpreadBlocked(newChunk, state2, bpos2, dir2))
                                        {
                                            // Divide the added temperature by 2 for each block between the player and the block
                                            blocksBetween++;
                                        }
                                    }
                                }

                                prevPos1 = newPos1;
                                prevPos2 = newPos2;
                            }
                            double blockDampening = Math.pow(1.5, blocksBetween);

                            totalTemp += tempToAdd / blockDampening;
                        }
                    }
                    catch (Exception e) {}
                }
            }
        }

        return temp.add(totalTemp);
    }

    public String getID()
    {
        return "cold_sweat:nearby_blocks";
    }

    Chunk getChunk(World world, ChunkPos pos, Map<ChunkPos, Chunk> chunks)
    {
        ChunkPos chunkPos = new ChunkPos(pos.x, pos.z);
        Chunk chunk = chunks.get(chunkPos);
        if (chunk == null)
        {
            chunk = world.getChunkProvider().getChunkNow(chunkPos.x, chunkPos.z);
            chunks.put(chunkPos, chunk);
        }
        return chunk;
    }
}