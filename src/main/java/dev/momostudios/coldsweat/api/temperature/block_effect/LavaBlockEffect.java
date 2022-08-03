package dev.momostudios.coldsweat.api.temperature.block_effect;

import dev.momostudios.coldsweat.api.temperature.Temperature;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FlowingFluidBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import dev.momostudios.coldsweat.util.math.CSMath;

public class LavaBlockEffect extends BlockEffect
{
    public LavaBlockEffect()
    {
        super(Blocks.LAVA);
    }

    @Override
    public double getTemperature(PlayerEntity player, BlockState state, BlockPos pos, double distance)
    {
        if (hasBlock(state.getBlock()))
        {
            double temp = 0.05 + (15 - state.get(FlowingFluidBlock.LEVEL)) / 80d;
            return CSMath.blend(temp, 0, distance, 0.5, 7);
        }
        return 0;
    }

    @Override
    public double maxEffect() {
        return CSMath.convertUnits(300, Temperature.Units.F, Temperature.Units.MC, false);
    }

    @Override
    public double maxTemperature() {
        return CSMath.convertUnits(1000, Temperature.Units.F, Temperature.Units.MC, true);
    }
}
