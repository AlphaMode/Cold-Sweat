package dev.momostudios.coldsweat.api.temperature.modifier;

import dev.momostudios.coldsweat.api.registry.BlockEffectRegistry;
import dev.momostudios.coldsweat.api.temperature.Temperature;
import dev.momostudios.coldsweat.api.temperature.block_effect.BlockEffect;
import dev.momostudios.coldsweat.util.math.CSMath;
import dev.momostudios.coldsweat.util.world.WorldHelper;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.*;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

public class BlockTempModifier extends TempModifier
{
    Map<ChunkPos, Chunk> chunkMap = new HashMap<>();

    public BlockTempModifier() {}

    @Override
    public Function<Temperature, Temperature> calculate(PlayerEntity player)
    {
        Map<BlockEffect, Double> effectAmounts = new HashMap<>();
        ChunkPos playerChunkPos = new ChunkPos((player.getPosition().getX()) >> 4, (player.getPosition().getZ()) >> 4);

        if (player.ticksExisted % 200 == 0)
        {
            chunkMap.keySet().removeIf(chunkPos -> chunkPos.getChessboardDistance(playerChunkPos) > 1);
        }

        World world = player.world;

        for (int x = -7; x < 7; x++)
        {
            for (int z = -7; z < 7; z++)
            {
                ChunkPos chunkPos = new ChunkPos((player.getPosition().getX() + x) >> 4, (player.getPosition().getZ() + z) >> 4);
                Chunk chunk = getChunk(world, chunkPos);

                for (int y = -7; y < 7; y++)
                {
                    try
                    {
                        BlockPos blockpos = player.getPosition().add(x, y, z);

                        ChunkSection section = chunk.getSections()[blockpos.getY() >> 4];
                        BlockState state = section.getBlockState(blockpos.getX() & 15, blockpos.getY() & 15, blockpos.getZ() & 15);

                        if (state.isAir()) continue;

                        // Get the BlockTemp associated with the block
                        BlockTemp be = BlockTempRegistry.getEntryFor(state);

                        if (be == null || be.equals(BlockTempRegistry.DEFAULT_BLOCK_EFFECT)) continue;

                        // Get the amount that this block has affected the player so far
                        double effectAmount = effectAmounts.getOrDefault(be, 0.0);

                        // Is totalTemp within the bounds of the BlockEffect's min/max allowed temps?
                        if (CSMath.isBetween(effectAmount, be.minEffect(), be.maxEffect()))
                        {
                            // Get Vector positions of the centers of the source block and player
                            Vector3d pos = new Vector3d(blockpos.getX() + 0.5, blockpos.getY() + 0.5, blockpos.getZ() + 0.5);

                            // Get the temperature of the block given the player's distance
                            double distance = CSMath.getDistance(player, new Vector3d(pos.x, pos.y, pos.z));
                            double tempToAdd = be.getTemperature(player, state, blockpos, distance);

                            // Skip this block if the effect is too weak to notice anyway
                            if (Math.abs(tempToAdd) < 0.01) continue;

                            // Cast a ray between the player and the block
                            // Lessen the effect with each block between the player and the block
                            AtomicInteger blocks = new AtomicInteger();
                            WorldHelper.gatherRTResults(new RayTraceContext(player.getPositionVec().add(0, player.getHeight() / 2, 0), pos,
                                    RayTraceContext.BlockMode.VISUAL, RayTraceContext.FluidMode.NONE, player),
                                    (ctx, bpos) ->
                                    {
                                        BlockState rayState = world.getChunkProvider().getChunkNow(bpos.getX() >> 4, bpos.getZ() >> 4).getBlockState(bpos);
                                        if (rayState.isSolid())
                                        {
                                            blocks.getAndIncrement();
                                        }
                                        return rayState;
                                    });

                            // Calculate the decrease in effectiveness due to blocks in the way
                            double blockDampening = blocks.get() + 1;

                            // Store this block type's total effect on the player
                            double blockEffectTotal = effectAmount + tempToAdd / blockDampening * 2;
                            effectAmounts.put(be, CSMath.clamp(blockEffectTotal, be.minEffect(), be.maxEffect()));
                        }
                    }
                    catch (Exception ignored) {}
                }
            }
        }

        // Add the effects of all the blocks together and return the result
        return temp ->
        {
            for (Map.Entry<BlockEffect, Double> effect : effectAmounts.entrySet())
            {
                BlockEffect be = effect.getKey();
                double min = be.minTemperature();
                double max = be.maxTemperature();
                if (!CSMath.isInRange(temp.get(), min, max)) continue;
                temp.set(CSMath.clamp(temp.get() + effect.getValue(), min, max));
            }
            return temp;
        };
    }

    public String getID()
    {
        return "cold_sweat:nearby_blocks";
    }

    Chunk getChunk(World world, ChunkPos pos)
    {
        ChunkPos chunkPos = new ChunkPos(pos.x, pos.z);
        Chunk chunk = chunkMap.get(chunkPos);
        if (chunk == null)
        {
            chunkMap.put(chunkPos, chunk = world.getChunkProvider().getChunkNow(chunkPos.x, chunkPos.z));
        }
        return chunk;
    }
}