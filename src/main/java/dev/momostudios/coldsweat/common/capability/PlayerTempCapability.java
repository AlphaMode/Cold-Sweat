package dev.momostudios.coldsweat.common.capability;

import dev.momostudios.coldsweat.util.entity.ModDamageSources;
import dev.momostudios.coldsweat.util.entity.TempHelper;
import dev.momostudios.coldsweat.util.registries.ModEffects;
import dev.momostudios.coldsweat.api.temperature.Temperature;
import dev.momostudios.coldsweat.api.temperature.modifier.TempModifier;
import dev.momostudios.coldsweat.config.ConfigCache;
import dev.momostudios.coldsweat.util.math.CSMath;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.potion.Effects;
import dev.momostudios.coldsweat.api.temperature.Temperature.Types;

import java.util.ArrayList;
import java.util.List;

public class PlayerTempCapability implements ITemperatureCap
{
    int updateCooldown = 0;
    boolean pendingUpdate = false;
    double syncedWorldTemp = 0;
    double syncedCoreTemp = 0;
    double syncedBaseTemp = 0;
    double syncedMaxTemp = 0;
    double syncedMinTemp = 0;

    double worldTemp;
    double coreTemp;
    double baseTemp;
    double maxWorldTemp;
    double minWorldTemp;

    List<TempModifier> worldModifiers    = new ArrayList<>();
    List<TempModifier> bodyModifiers     = new ArrayList<>();
    List<TempModifier> baseModifiers     = new ArrayList<>();
    List<TempModifier> rateModifiers     = new ArrayList<>();
    List<TempModifier> maxWorldModifiers = new ArrayList<>();
    List<TempModifier> minWorldModifiers = new ArrayList<>();

    public double get(Types type)
    {
        switch (type)
        {
            case WORLD:    return worldTemp;
            case CORE:     return coreTemp;
            case BASE:     return baseTemp;
            case BODY:     return baseTemp + coreTemp;
            case MAX:      return maxWorldTemp;
            case MIN:      return minWorldTemp;
            default: throw new IllegalArgumentException("Illegal type for PlayerTempCapability.getValue(): " + type);
        }
    }

    public void set(Types type, double value)
    {
        switch (type)
        {
            case CORE:     { this.coreTemp     = value; break; }
            case BASE:     { this.baseTemp     = value; break; }
            case WORLD:    { this.worldTemp    = value; break; }
            case MAX:      { this.maxWorldTemp = value; break; }
            case MIN:      { this.minWorldTemp = value; break; }
            default : throw new IllegalArgumentException("Illegal type for PlayerTempCapability.setValue(): " + type);
        }
    }

    public List<TempModifier> getModifiers(Types type)
    {
        switch (type)
        {
            case CORE:     { return bodyModifiers; }
            case BASE:     { return baseModifiers; }
            case RATE:     { return rateModifiers; }
            case WORLD:    { return worldModifiers; }
            case MAX:      { return maxWorldModifiers; }
            case MIN:      { return minWorldModifiers; }
            default: throw new IllegalArgumentException("Illegal type for PlayerTempCapability.getModifiers(): " + type);
        }
    }

    public boolean hasModifier(Types type, Class<? extends TempModifier> mod)
    {
        switch (type)
        {
            case CORE:     { return this.bodyModifiers.stream().anyMatch(mod::isInstance); }
            case BASE:     { return this.baseModifiers.stream().anyMatch(mod::isInstance); }
            case RATE:     { return this.rateModifiers.stream().anyMatch(mod::isInstance); }
            case WORLD:    { return this.worldModifiers.stream().anyMatch(mod::isInstance); }
            case MAX:      { return this.maxWorldModifiers.stream().anyMatch(mod::isInstance); }
            case MIN:      { return this.minWorldModifiers.stream().anyMatch(mod::isInstance); }
            default: throw new IllegalArgumentException("Illegal type for PlayerTempCapability.hasModifier(): " + type);
        }
    }


    public void clearModifiers(Types type)
    {
        switch (type)
        {
            case WORLD:   { this.worldModifiers.clear(); break; }
            case CORE:    { this.bodyModifiers.clear(); break; }
            case BASE:    { this.baseModifiers.clear(); break; }
            case RATE:    { this.rateModifiers.clear(); break; }
            case MAX:     { this.maxWorldModifiers.clear(); break; }
            case MIN:     { this.minWorldModifiers.clear(); break; }
            default: throw new IllegalArgumentException("Illegal type for PlayerTempCapability.clearModifiers(): " + type);
        }
    }

    @Override
    public void copy(ITemperatureCap cap)
    {
        // Copy temperature values
        for (Types type : Types.values())
        {
            if (type == Types.BODY || type == Types.RATE) continue;
            this.set(type, cap.get(type));
        }

        // Copy the modifiers
        for (Types type : Types.values())
        {
            if (type == Types.BODY) continue;
            this.getModifiers(type).clear();
            this.getModifiers(type).addAll(cap.getModifiers(type));
        }
    }

    public void tickClient(PlayerEntity player)
    {
        if (player.world.isRemote)
        for (Types type : Types.values())
        {
            if (type == Types.BODY) continue;
            tickModifiers(new Temperature(), player, getModifiers(type));
        }
    }

    public void tickUpdate(PlayerEntity player)
    {
        ConfigCache config = ConfigCache.getInstance();

        if (updateCooldown > 0)
            updateCooldown--;

        // Tick expiration time for world modifiers
        double worldTemp = tickModifiers(new Temperature(), player, getModifiers(Types.WORLD)).get();

        Temperature coreTemp = tickModifiers(new Temperature(get(Types.CORE)), player, getModifiers(Types.CORE));

        Temperature baseTemp = tickModifiers(new Temperature(), player, getModifiers(Types.BASE));

        double maxOffset = tickModifiers(new Temperature(), player, getModifiers(Types.MAX)).get();
        double minOffset = tickModifiers(new Temperature(), player, getModifiers(Types.MIN)).get();

        double maxTemp = config.maxTemp + maxOffset;
        double minTemp = config.minTemp + minOffset;

        double tempRate = 7.0d;

        if ((worldTemp > maxTemp && coreTemp.get() >= 0)
        ||  (worldTemp < minTemp && coreTemp.get() <= 0))
        {
            boolean isOver = worldTemp > maxTemp;
            double difference = Math.abs(worldTemp - (isOver ? maxTemp : minTemp));
            Temperature changeBy = new Temperature(Math.max((difference / tempRate) * config.rate, Math.abs(config.rate / 50)) * (isOver ? 1 : -1));
            coreTemp = coreTemp.add(tickModifiers(changeBy, player, getModifiers(Types.RATE)));
        }
        else
        {
            // Return the player's body temperature to 0
            Temperature returnRate = new Temperature(getBodyReturnRate(worldTemp, coreTemp.get() > 0 ? maxTemp : minTemp, config.rate, coreTemp.get()));
            coreTemp = coreTemp.add(returnRate);
        }

        if ((int) syncedCoreTemp != (int) coreTemp.get()
        ||  (int) syncedBaseTemp != (int) baseTemp.get()
        || CSMath.crop(syncedWorldTemp, 2) != CSMath.crop(worldTemp, 2)
        || CSMath.crop(syncedMaxTemp,   2) != CSMath.crop(maxOffset, 2)
        || CSMath.crop(syncedMinTemp,   2) != CSMath.crop(minOffset, 2))
        {
            pendingUpdate = true;
        }

        if (updateCooldown-- <= 0 && pendingUpdate)
        {
            TempHelper.updateTemperature(player,
                                         new Temperature(get(Temperature.Types.CORE)),
                                         new Temperature(get(Temperature.Types.BASE)),
                                         new Temperature(get(Temperature.Types.WORLD)),
                                         new Temperature(get(Temperature.Types.MAX)),
                                         new Temperature(get(Temperature.Types.MIN)));
            pendingUpdate = false;
            updateCooldown = 5;

            syncedBaseTemp = baseTemp.get();
            syncedCoreTemp = coreTemp.get();
            syncedWorldTemp = worldTemp;
            syncedMaxTemp = maxOffset;
            syncedMinTemp = minOffset;
        }

        // Sets the player's body temperature to BASE + CORE
        set(Temperature.Types.BASE, baseTemp.get());
        set(Temperature.Types.CORE, CSMath.clamp(coreTemp.get(), -150d, 150d));
        set(Temperature.Types.WORLD, worldTemp);
        set(Temperature.Types.MAX, maxOffset);
        set(Temperature.Types.MIN, minOffset);

        // Calculate body/base temperatures with modifiers
        Temperature bodyTemp = baseTemp.add(coreTemp);

        //Deal damage to the player if temperature is critical
        boolean hasFireResistance = player.isPotionActive(Effects.FIRE_RESISTANCE)   && config.fireRes;
        boolean hasIceResistance  = player.isPotionActive(ModEffects.ICE_RESISTANCE) && config.iceRes;
        if (player.ticksExisted % 40 == 0)
        {
            boolean damageScaling = config.damageScaling;

            if (bodyTemp.get() >= 100 && !hasFireResistance && !player.isPotionActive(ModEffects.GRACE))
            {
                player.attackEntityFrom(damageScaling ? ModDamageSources.HOT.setDifficultyScaled() : ModDamageSources.HOT, 2f);
            }
            if (bodyTemp.get() <= -100 && !hasIceResistance && !player.isPotionActive(ModEffects.GRACE))
            {
                player.attackEntityFrom(damageScaling ? ModDamageSources.COLD.setDifficultyScaled() : ModDamageSources.COLD, 2f);
            }
        }
    }

    // Used for returning the player's temperature back to 0
    private static double getBodyReturnRate(double world, double cap, double rate, double bodyTemp)
    {
        double tempRate = 7.0d;
        double changeBy = Math.max((Math.abs(world - cap) / tempRate) * rate, Math.abs(rate / 30));
        return Math.min(Math.abs(bodyTemp), changeBy) * (bodyTemp > 0 ? -1 : 1);
    }

    private static Temperature tickModifiers(Temperature temp, PlayerEntity player, List<TempModifier> modifiers)
    {
        Temperature result = temp.with(modifiers, player);

        modifiers.removeIf(modifier ->
        {
            modifier.setTicksExisted(modifier.getTicksExisted() + 1);
            if (modifier.getExpireTime() != -1)
            {
                return modifier.getTicksExisted() >= modifier.getExpireTime();
            }
            return false;
        });

        return result;
    }
}
