package dev.momostudios.coldsweat.api.temperature.modifier;

import dev.momostudios.coldsweat.api.registry.BlockEffectRegistry;
import net.minecraft.block.BlockState;
import dev.momostudios.coldsweat.api.temperature.Temperature;
import dev.momostudios.coldsweat.api.temperature.block_effect.BlockEffect;
import dev.momostudios.coldsweat.util.math.CSMath;
import dev.momostudios.coldsweat.util.world.WorldHelper;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

import java.util.*;

public class BlockTempModifier extends TempModifier
{
    public BlockTempModifier() {}

    @Override
    public Temperature getResult(Temperature temp, PlayerEntity player)
    {
        double totalTemp = 0;
        HashMap<ChunkPos, Chunk> chunkMap = new HashMap<>();
        World world = player.world;

        for (int x1 = -7; x1 < 14; x1++)
        {
            for (int z1 = -7; z1 < 14; z1++)
            {
                ChunkPos chunkPos = new ChunkPos((player.getPosition().getX() + x1) >> 4, (player.getPosition().getZ() + z1) >> 4);
                Chunk chunk = getChunk(world, chunkPos, chunkMap);

                for (int y1 = -7; y1 < 14; y1++)
                {
                    try
                    {
                        BlockPos blockpos = player.getPosition().add(x1, y1, z1);

                        BlockState state = chunk.getBlockState(blockpos);

                        if (state.getMaterial() == Material.AIR) continue;

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

                            // Lessen the effect with each block between the player and the block
                            Vector3d prevPos1 = playerPos1;
                            Vector3d prevPos2 = playerPos2;
                            int blocksBetween = 0;
                            for (int i = 0; i < distance * 1.25; i++)
                            {
                                // Get the next block (sub)position
                                Vector3d newPos1 = playerPos1.subtract(playerPos1.subtract(pos).scale(i / (distance * 1.25)));
                                Vector3d newPos2 = playerPos2.subtract(playerPos2.subtract(pos).scale(i / (distance * 1.25)));

                                // Check if the newPos1 is not a duplicate BlockPos or solid block
                                BlockPos bpos1 = new BlockPos(newPos1);
                                Vector3d facing1 = newPos1.subtract(prevPos1);
                                Direction dir1 = CSMath.getDirectionFromVector(facing1.x, facing1.y, facing1.z);

                                Chunk newChunk = getChunk(world, new ChunkPos(bpos1), chunkMap);

                                if (!bpos1.equals(new BlockPos(prevPos1)) && !bpos1.equals(blockpos)
                                && !WorldHelper.canSpreadThrough(newChunk, newChunk.getBlockState(bpos1), bpos1, dir1))
                                {
                                    // Divide the added temperature by 2 for each block between the player and the block
                                    blocksBetween++;
                                }

                                // Check if the newPos2 is not a duplicate BlockPos or solid block
                                BlockPos bpos2 = new BlockPos(newPos2);
                                Vector3d facing2 = newPos2.subtract(prevPos2);
                                Direction dir2 = CSMath.getDirectionFromVector(facing2.x, facing2.y, facing2.z);

                                if (!bpos2.equals(new BlockPos(prevPos2)) && !bpos2.equals(blockpos)
                                && !WorldHelper.canSpreadThrough(newChunk, newChunk.getBlockState(bpos2), bpos2, dir2))
                                {
                                    // Divide the added temperature by 2 for each block between the player and the block
                                    blocksBetween++;
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
        Chunk chunk;
        if (chunks.containsKey(chunkPos))
        {
            chunk = chunks.get(chunkPos);
        }
        else
        {
            chunk = world.getChunkProvider().getChunkNow(chunkPos.x, chunkPos.z);
            chunks.put(chunkPos, chunk);
        }
        return chunk;
    }
}