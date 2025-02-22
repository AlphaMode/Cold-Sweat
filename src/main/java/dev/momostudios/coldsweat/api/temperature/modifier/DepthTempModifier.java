package dev.momostudios.coldsweat.api.temperature.modifier;

import com.mojang.datafixers.util.Pair;
import dev.momostudios.coldsweat.api.util.Temperature;
import dev.momostudios.coldsweat.util.config.ConfigSettings;
import dev.momostudios.coldsweat.util.math.CSMath;
import dev.momostudios.coldsweat.util.world.WorldHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class DepthTempModifier extends TempModifier
{
    static int SAMPLES = 25;

    @Override
    public Function<Double, Double> calculate(LivingEntity entity, Temperature.Type type)
    {
        if (entity.level.dimensionType().hasCeiling()) return temp -> temp;

        double midTemp = (ConfigSettings.MAX_TEMP.get() + ConfigSettings.MIN_TEMP.get()) / 2;
        BlockPos playerPos = entity.blockPosition();

        List<Pair<Double, Double>> depthLevels = new ArrayList<>();

        for (BlockPos pos : WorldHelper.getPositionGrid(playerPos, SAMPLES, 5))
        {
            ChunkAccess chunk = entity.level.getChunk(pos.getX() >> 4, pos.getZ() >> 4, ChunkStatus.SURFACE, false);
            if (chunk == null) continue;
            double depth = Math.max(0, chunk.getHeight(Heightmap.Types.WORLD_SURFACE, pos.getX(), pos.getZ()) - playerPos.getY());

            depthLevels.add(Pair.of(depth, Math.max(0, 16 - Math.sqrt(pos.distSqr(playerPos)))));
        }

        double depth = CSMath.weightedAverage(depthLevels);

        return temp ->
        {
            List<Pair<Number, Number>> valueMap = new ArrayList<>();
            valueMap.add(Pair.of(temp, 0.8));
            valueMap.add(Pair.of(CSMath.blend(midTemp, temp, entity.level.getBrightness(LightLayer.SKY, entity.blockPosition()), 0, 15), 2.0));
            valueMap.add(Pair.of(CSMath.blend(temp, midTemp, depth, 4, 20), 4.0));

            return CSMath.weightedAverage(valueMap);
        };
    }

    public String getID()
    {
        return "cold_sweat:depth";
    }
}
