package dev.momostudios.coldsweat.api.temperature.modifier;

import dev.momostudios.coldsweat.util.entity.TempHelper;
import dev.momostudios.coldsweat.util.math.CSMath;
import dev.momostudios.coldsweat.util.world.WorldHelper;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.LightType;
import dev.momostudios.coldsweat.api.temperature.Temperature;
import dev.momostudios.coldsweat.util.config.ConfigCache;
import net.minecraft.world.gen.Heightmap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class DepthTempModifier extends TempModifier
{
    @Override
    public Function<Temperature, Temperature> calculate(PlayerEntity player)
    {
        if (player.world.getDimensionType().getHasCeiling()) return temp -> temp;

        double midTemp = (ConfigCache.getInstance().maxTemp + ConfigCache.getInstance().minTemp) / 2;
        BlockPos playerPos = player.getPosition();

        List<Integer> lightLevels = new ArrayList<>();
        List<Double> depthLevels = new ArrayList<>();

        for (BlockPos pos : WorldHelper.getNearbyPositions(playerPos, 50, 5))
        {
            double depth = Math.max(0, player.world.getHeight(Heightmap.Type.WORLD_SURFACE, pos.getX(), pos.getZ()) - playerPos.getY());
            int light = player.world.getLightFor(LightType.SKY, pos);

            lightLevels.add(light);
            depthLevels.add(depth);
        }

        double light = CSMath.average(lightLevels.toArray(new Integer[0]));
        double depth = CSMath.average(depthLevels.toArray(new Double[0]));

        return temp ->
        {
            Map<Double, Double> valueMap = new HashMap<>();
            valueMap.put(temp.get(), 0.8);
            valueMap.put(CSMath.blend(midTemp, temp.get(), light, 0, 15), 2.0);
            valueMap.put(CSMath.blend(temp.get(), midTemp, depth, 2, 20), 4.0);

            return new Temperature(CSMath.weightedAverage(valueMap));
        };
    }

    public String getID()
    {
        return "cold_sweat:depth";
    }
}
