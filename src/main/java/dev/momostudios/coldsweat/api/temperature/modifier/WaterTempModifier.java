package dev.momostudios.coldsweat.api.temperature.modifier;

import dev.momostudios.coldsweat.util.math.CSMath;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particles.ParticleTypes;
import dev.momostudios.coldsweat.api.temperature.Temperature;
import dev.momostudios.coldsweat.config.ConfigCache;

public class WaterTempModifier extends TempModifier
{
    public WaterTempModifier()
    {
        addArgument("strength", 0.01);
    }

    public WaterTempModifier(double strength)
    {
        addArgument("strength", strength);
    }

    @Override
    public Temperature getResult(Temperature temp, PlayerEntity player)
    {
        double maxTemp = ConfigCache.getInstance().maxTemp;
        double minTemp = ConfigCache.getInstance().minTemp;

        try
        {
            double strength = this.<Double>getArgument("strength");
            double returnRate = Math.min(-0.0003, -0.0003 - (temp.get() / 800));
            double addAmount = player.isInWaterOrBubbleColumn() ? 0.01 : player.world.isRainingAt(player.getPosition()) ? 0.005 : returnRate;

            setArgument("strength", CSMath.clamp(strength + addAmount, 0d, Math.abs(CSMath.average(maxTemp, minTemp) - temp.get()) / 2));

            if (!player.isInWater() && strength > 0.0)
            {
                if (Math.random() < strength)
                {
                    double randX = player.getWidth() * (Math.random() - 0.5);
                    double randY = player.getHeight() * Math.random();
                    double randZ = player.getWidth() * (Math.random() - 0.5);
                    player.world.addParticle(ParticleTypes.FALLING_WATER, player.getPosX() + randX, player.getPosY() + randY, player.getPosZ() + randZ, 0, 0, 0);
                }
            }

            return temp.add(-this.<Double>getArgument("strength"));
        }
        // Remove the modifier if an exception is thrown
        catch (Exception e)
        {
            e.printStackTrace();
            clearArgument("strength");
            setArgument("strength", 1d);
            return temp;
        }
    }

    @Override
    public String getID()
    {
        return "cold_sweat:water";
    }
}
