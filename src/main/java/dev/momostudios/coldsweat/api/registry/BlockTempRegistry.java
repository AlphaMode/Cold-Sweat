package dev.momostudios.coldsweat.api.registry;

import dev.momostudios.coldsweat.ColdSweat;
import dev.momostudios.coldsweat.api.temperature.block_temp.BlockTemp;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;

public class BlockTempRegistry
{
    public static final LinkedHashSet<BlockTemp> BLOCK_EFFECTS = new LinkedHashSet<>();
    public static final HashMap<Block, BlockTemp> MAPPED_BLOCKS = new HashMap<>();
    public static final BlockTemp DEFAULT_BLOCK_EFFECT = new BlockTemp()
    {
        @Override
        public double getTemperature(PlayerEntity player, BlockState state, BlockPos pos, double distance)
        {
            return 0;
        }
    };

    public static void register(BlockTemp blockEffect)
    {
        blockEffect.validBlocks.forEach(block ->
        {
            if (MAPPED_BLOCKS.containsKey(block))
            {
                ColdSweat.LOGGER.error("Block \"{}\" already has a registered BlockTemp ({})! Skipping BlockTemp {}...",
                        block.getRegistryName().toString(), MAPPED_BLOCKS.get(block).getClass().getSimpleName(), blockEffect.getClass().getSimpleName());
            }
            else
            {
                MAPPED_BLOCKS.put(block, blockEffect);
            }
        });
        BLOCK_EFFECTS.add(blockEffect);
    }

    public static void flush()
    {
        MAPPED_BLOCKS.clear();
    }

    public static BlockTemp getEntryFor(BlockState blockstate)
    {
        if (blockstate.isAir()) return DEFAULT_BLOCK_EFFECT;

        return MAPPED_BLOCKS.computeIfAbsent(blockstate.getBlock(), (block) -> DEFAULT_BLOCK_EFFECT);
    }
}
