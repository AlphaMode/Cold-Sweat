package dev.momostudios.coldsweat.api.temperature.block_effect;

import dev.momostudios.coldsweat.api.temperature.Temperature;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CampfireBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import dev.momostudios.coldsweat.util.math.CSMath;

public class SoulCampfireBlockEffect extends BlockEffect
{
    public SoulCampfireBlockEffect()
    {
        super(Blocks.SOUL_FIRE);
    }

    @Override
    public double getTemperature(PlayerEntity player, BlockState state, BlockPos pos, double distance)
    {
        if (this.hasBlock(state.getBlock()) && state.get(CampfireBlock.LIT))
        {
            return CSMath.blend(-0.2, 0, distance, 0.5, 7);
        }
        return 0;
    }

    @Override
    public double minEffect() {
        return CSMath.convertUnits(-20, Temperature.Units.F, Temperature.Units.MC, false);
    }

    @Override
    public double minTemperature() {
        return CSMath.convertUnits(-400, Temperature.Units.F, Temperature.Units.MC, true);
    }
}
