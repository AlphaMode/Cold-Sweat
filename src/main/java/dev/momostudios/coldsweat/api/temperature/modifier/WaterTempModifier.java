package dev.momostudios.coldsweat.api.temperature.modifier;

import dev.momostudios.coldsweat.util.entity.TempHelper;
import dev.momostudios.coldsweat.util.math.CSMath;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particles.ParticleTypes;
import dev.momostudios.coldsweat.api.temperature.Temperature;
import dev.momostudios.coldsweat.util.config.ConfigCache;
import net.minecraft.util.text.StringTextComponent;

import java.util.function.Function;

public class WaterTempModifier extends TempModifier
{
    public WaterTempModifier()
    {
        this(0);
    }

    public WaterTempModifier(double strength)
    {
        addArgument("strength", strength);
    }

    @Override
    public Function<Temperature, Temperature> calculate(PlayerEntity player)
    {
        double worldTemp = TempHelper.getTemperature(player, Temperature.Types.WORLD).get();
        double maxTemp = ConfigCache.getInstance().maxTemp;
        double minTemp = ConfigCache.getInstance().minTemp;

        double strength = this.<Double>getArgument("strength");
        double returnRate = Math.min(-0.0003, -0.0003 - (worldTemp / 800));
        double addAmount = player.isInWaterOrBubbleColumn() ? 0.01 : player.world.isRainingAt(player.getPosition()) ? 0.005 : returnRate;

        setArgument("strength", CSMath.clamp(strength + addAmount, 0d, Math.abs(CSMath.average(maxTemp, minTemp) - worldTemp) / 2));

        // If the strength is 0, this TempModifier expires~
        if (strength <= 0.0)
        {
            this.expires(this.getTicksExisted() - 1);
        }

        if (!player.isInWater())
        {
            if (Math.random() < strength)
            {
                double randX = player.getWidth() * (Math.random() - 0.5);
                double randY = player.getHeight() * Math.random();
                double randZ = player.getWidth() * (Math.random() - 0.5);
                player.world.addParticle(ParticleTypes.FALLING_WATER, player.getPosX() + randX, player.getPosY() + randY, player.getPosZ() + randZ, 0, 0, 0);
            }
        }

        return temp -> temp.add(-this.<Double>getArgument("strength"));
    }

    @Override
    public String getID()
    {
        return "cold_sweat:water";
    }
}
