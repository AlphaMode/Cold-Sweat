package dev.momostudios.coldsweat.api.registry;

import dev.momostudios.coldsweat.ColdSweat;
import dev.momostudios.coldsweat.api.temperature.block_effect.BlockEffect;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class BlockEffectRegistry
{
    public static final HashSet<BlockEffect> BLOCK_EFFECTS = new HashSet<>();
    public static final HashMap<Block, BlockEffect> MAPPED_BLOCKS = new HashMap<>();
    public static final BlockEffect DEFAULT_BLOCK_EFFECT = new BlockEffect() {
        @Override
        public double getTemperature(PlayerEntity player, BlockState state, BlockPos pos, double distance)
        {
            return 0;
        }
    };

    public static void register(BlockEffect blockEffect)
    {
        blockEffect.validBlocks.forEach(block ->
        {
            if (MAPPED_BLOCKS.containsKey(block))
            {
                ColdSweat.LOGGER.error("Block \"{}\" already has a registered BlockEffect ({})! Skipping BlockEffect {}...",
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

    @Nullable
    public static BlockEffect getEntryFor(BlockState block)
    {
        if (MAPPED_BLOCKS.containsKey(block.getBlock()))
        {
            return MAPPED_BLOCKS.get(block.getBlock());
        }
        else
        {
            for (BlockEffect blockEffect : BlockEffectRegistry.BLOCK_EFFECTS)
            {
                if (blockEffect.hasBlock(block))
                {
                    BlockEffectRegistry.MAPPED_BLOCKS.put(block.getBlock(), blockEffect);
                    return blockEffect;
                }
            }

            BlockEffectRegistry.MAPPED_BLOCKS.put(block.getBlock(), BlockEffectRegistry.DEFAULT_BLOCK_EFFECT);
            return BlockEffectRegistry.DEFAULT_BLOCK_EFFECT;
        }
    }
}
