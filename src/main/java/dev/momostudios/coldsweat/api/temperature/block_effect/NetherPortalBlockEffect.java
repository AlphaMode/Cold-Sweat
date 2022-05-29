package dev.momostudios.coldsweat.api.temperature.block_effect;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.DimensionType;
import dev.momostudios.coldsweat.util.math.CSMath;

public class NetherPortalBlockEffect extends BlockEffect
{
    public NetherPortalBlockEffect()
    {
        super(Blocks.NETHER_PORTAL);
    }

    @Override
    public double getTemperature(PlayerEntity player, BlockState state, BlockPos pos, double distance)
    {
        boolean isInOverworld = player.world.getDimensionKey().getLocation().toString().equals(DimensionType.OVERWORLD.getLocation().toString());
        return CSMath.blend(isInOverworld ? 0.3 : -0.2, 0, distance, 0, 3);
    }

    @Override
    public boolean hasBlock(Block block)
    {
        return block == net.minecraft.block.Blocks.NETHER_PORTAL;
    }

    @Override
    public double maxEffect()
    {
        return 1;
    }

    @Override
    public double minEffect()
    {
        return -1;
    }
}
