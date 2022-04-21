package dev.momostudios.coldsweat.api.temperature.modifier;

import dev.momostudios.coldsweat.util.math.CSMath;
import dev.momostudios.coldsweat.util.world.WorldHelper;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.LightType;
import dev.momostudios.coldsweat.api.temperature.Temperature;
import dev.momostudios.coldsweat.config.ConfigCache;
import net.minecraft.world.gen.Heightmap;

import java.util.HashMap;
import java.util.Map;

public class DepthTempModifier extends TempModifier
{
    @Override
    public Temperature getResult(Temperature temp, PlayerEntity player)
    {
        double midTemp = (ConfigCache.getInstance().maxTemp + ConfigCache.getInstance().minTemp) / 2;
        double depth = Math.max(0, WorldHelper.getGroundLevel(player.getPosition(), player.world) - player.getPosition().getY());

        Map<Double, Double> valueMap = new HashMap<>();
        valueMap.put(temp.get(), 0.5);
        valueMap.put(CSMath.blend(midTemp, temp.get(), player.world.getLightFor(LightType.SKY, player.getPosition()), 0, 15), 2.0);
        valueMap.put(CSMath.blend(temp.get(), midTemp, !WorldHelper.canSeeSky(player.world, player.getPosition()) ? depth : Math.max(0, depth - 10), 2, 20), 4.0);

        return new Temperature(CSMath.weightedAverage(valueMap));
    }

    public String getID()
    {
        return "cold_sweat:depth";
    }
}
