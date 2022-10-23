package dev.momostudios.coldsweat.api.temperature.modifier;

import dev.momostudios.coldsweat.api.registry.BlockTempRegistry;
import dev.momostudios.coldsweat.api.temperature.Temperature;
import dev.momostudios.coldsweat.api.temperature.block_temp.BlockTemp;
import dev.momostudios.coldsweat.util.math.CSMath;
import dev.momostudios.coldsweat.util.world.WorldHelper;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.*;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.IChunk;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

public class BlockTempModifier extends TempModifier
{
    public BlockTempModifier() {}

    @Override
    public Function<Temperature, Temperature> calculate(PlayerEntity player)
    {
        Map<BlockTemp, Double> effectAmounts = new HashMap<>();

        World world = player.world;

        for (int x = -7; x < 7; x++)
        {
            for (int z = -7; z < 7; z++)
            {
                IChunk chunk = world.getChunkProvider().getChunk((player.getPosition().getX() + x) >> 4, (player.getPosition().getZ() + z) >> 4, ChunkStatus.FULL, false);
                if (chunk == null) continue;

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

                        // Is totalTemp within the bounds of the BlockTemp's min/max allowed temps?
                        if (CSMath.isBetween(effectAmount, be.minEffect(), be.maxEffect()))
                        {
                            // Get Vector positions of the centers of the source block and player
                            Vector3d pos = new Vector3d(blockpos.getX() + 0.5, blockpos.getY() + 0.5, blockpos.getZ() + 0.5);

                            // Cast a ray between the player and the block
                            // Lessen the effect with each block between the player and the block
                            AtomicInteger blocks = new AtomicInteger();
                            double playerRadius = player.getWidth() / 2;
                            Vector3d playerClosest = new Vector3d(CSMath.clamp(pos.x, player.getPosX() - playerRadius, player.getPosX() + playerRadius),
                                    CSMath.clamp(pos.y, player.getPosY(), player.getPosY() + player.getHeight()),
                                    CSMath.clamp(pos.z, player.getPosZ() - playerRadius, player.getPosZ() + playerRadius));

                            // Get the temperature of the block given the player's distance
                            double distance = CSMath.getDistance(playerClosest, pos);
                            double tempToAdd = be.getTemperature(player, state, blockpos, distance);

                            Vector3d ray = pos.subtract(playerClosest);
                            Direction facing = Direction.getFacingFromVector(ray.x, ray.y, ray.z);
                            WorldHelper.forBlocksInRay(playerClosest, pos, world,
                            (rayState, bpos) ->
                            {
                                if (WorldHelper.isSpreadBlocked(world, rayState, bpos, facing))
                                    blocks.getAndIncrement();
                            }, 3);

                            // Calculate the decrease in effectiveness due to blocks in the way
                            double blockDampening = blocks.get();

                            // Store this block type's total effect on the player
                            double blockTempTotal = effectAmount + tempToAdd / (blockDampening + 1);
                            effectAmounts.put(be, CSMath.clamp(blockTempTotal, be.minEffect(), be.maxEffect()));
                        }
                    }
                    catch (Exception ignored) {}
                }
            }
        }

        // Add the effects of all the blocks together and return the result
        return temp ->
        {
            for (Map.Entry<BlockTemp, Double> effect : effectAmounts.entrySet())
            {
                BlockTemp be = effect.getKey();
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
}