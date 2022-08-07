package dev.momostudios.coldsweat.common.capability;

import dev.momostudios.coldsweat.api.temperature.modifier.*;
import dev.momostudios.coldsweat.core.network.ColdSweatPacketHandler;
import dev.momostudios.coldsweat.core.network.message.PlayerTempSyncMessage;
import dev.momostudios.coldsweat.util.entity.ModDamageSources;
import dev.momostudios.coldsweat.util.entity.NBTHelper;
import dev.momostudios.coldsweat.api.util.TempHelper;
import dev.momostudios.coldsweat.util.registries.ModEffects;
import dev.momostudios.coldsweat.api.temperature.Temperature;
import dev.momostudios.coldsweat.util.config.ConfigCache;
import dev.momostudios.coldsweat.util.math.CSMath;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.potion.Effects;
import dev.momostudios.coldsweat.api.temperature.Temperature.Type;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.fml.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

public class PlayerTempCap implements ITemperatureCap, INBTSerializable<CompoundNBT>
{
    static Type[] VALID_MODIFIER_TYPES = {Type.CORE, Type.BASE, Type.RATE,
                                          Type.MAX, Type.MIN, Type.WORLD};

    private double[] syncedValues = new double[5];
    private int ticksSinceSync = 0;

    double worldTemp;
    double coreTemp;
    double baseTemp;
    double maxOffset;
    double minOffset;

    List<TempModifier> worldModifiers = new ArrayList<>();
    List<TempModifier> coreModifiers  = new ArrayList<>();
    List<TempModifier> baseModifiers  = new ArrayList<>();
    List<TempModifier> rateModifiers  = new ArrayList<>();
    List<TempModifier> maxModifiers   = new ArrayList<>();
    List<TempModifier> minModifiers   = new ArrayList<>();

    public double get(Type type)
    {
        switch (type)
        {
            case WORLD: return worldTemp;
            case CORE:  return coreTemp;
            case BASE:  return baseTemp;
            case BODY:  return baseTemp + coreTemp;
            case MAX:   return maxOffset;
            case MIN:   return minOffset;
            default: throw new IllegalArgumentException("Illegal type for PlayerTempCapability.getValue(): " + type);
        }
    }

    public void set(Type type, double value)
    {
        switch (type)
        {
            case CORE:  { this.coreTemp  = value; break; }
            case BASE:  { this.baseTemp  = value; break; }
            case WORLD: { this.worldTemp = value; break; }
            case MAX:   { this.maxOffset = value; break; }
            case MIN:   { this.minOffset = value; break; }
            default : throw new IllegalArgumentException("Illegal type for PlayerTempCapability.setValue(): " + type);
        }
    }

    public List<TempModifier> getModifiers(Type type)
    {
        switch (type)
        {
            case CORE:  { return coreModifiers; }
            case BASE:  { return baseModifiers; }
            case RATE:  { return rateModifiers; }
            case WORLD: { return worldModifiers; }
            case MAX:   { return maxModifiers; }
            case MIN:   { return minModifiers; }
            default: throw new IllegalArgumentException("Illegal type for PlayerTempCapability.getModifiers(): " + type);
        }
    }

    public boolean hasModifier(Type type, Class<? extends TempModifier> mod)
    {
        return this.getModifiers(type).stream().anyMatch(mod::isInstance);
    }


    public void clearModifiers(Type type)
    {
        this.getModifiers(type).clear();
    }

    public void tickDummy(PlayerEntity player)
    {
        for (Type type : VALID_MODIFIER_TYPES)
        {
            new Temperature().with(getModifiers(type), player);
        }
    }

    public void tick(PlayerEntity player)
    {
        ConfigCache config = ConfigCache.getInstance();

        // Tick expiration time for world modifiers
        double newWorldTemp = new Temperature().with(getModifiers(Type.WORLD), player).get();
        double newCoreTemp  = new Temperature(this.coreTemp).with(getModifiers(Type.CORE), player).get();
        double newBaseTemp  = new Temperature().with(getModifiers(Type.BASE), player).get();
        double newMaxOffset = new Temperature().with(getModifiers(Type.MAX), player).get();
        double newMinOffset = new Temperature().with(getModifiers(Type.MIN), player).get();

        double maxTemp = config.maxTemp + newMaxOffset;
        double minTemp = config.minTemp + newMinOffset;

        double tempRate = 7.0d;

        // 1 if newWorldTemp is above max, -1 if below min, 0 if between the values
        int magnitude = CSMath.getSignForRange(newWorldTemp, minTemp, maxTemp);

        if (magnitude != 0)
        {
            double difference = Math.abs(newWorldTemp - CSMath.clamp(newWorldTemp, minTemp, maxTemp));
            Temperature changeBy = new Temperature(Math.max((difference / tempRate) * config.rate, Math.abs(config.rate / 50)) * magnitude);
            newCoreTemp = changeBy.with(getModifiers(Type.RATE), player).add(newCoreTemp).get();
        }
        // If the player's temperature and world temperature are not both hot or both cold
        if (magnitude != CSMath.getSign(newCoreTemp))
        {
            // Return the player's body temperature to 0
            newCoreTemp = newCoreTemp + getBodyReturnRate(newWorldTemp, newCoreTemp > 0 ? maxTemp : minTemp, config.rate, newCoreTemp);
        }

        // Sync the player's temperature to the client
        if (ticksSinceSync++ >= 5
        && ((int) syncedValues[0] != (int) newCoreTemp
        || (int) syncedValues[1] != (int) newBaseTemp
        || CSMath.crop(syncedValues[2], 2) != CSMath.crop(newWorldTemp, 2)
        || CSMath.crop(syncedValues[3], 2) != CSMath.crop(newMaxOffset, 2)
        || CSMath.crop(syncedValues[4], 2) != CSMath.crop(newMinOffset, 2)))
        {
            ticksSinceSync = 0;
            syncedValues = new double[] { newCoreTemp, newBaseTemp, newWorldTemp, newMaxOffset, newMinOffset };

            ColdSweatPacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> (ServerPlayerEntity) player),
                    new PlayerTempSyncMessage(newWorldTemp, newCoreTemp, newBaseTemp, newMaxOffset, newMinOffset, false));
        }

        // Write the new temperature values
        set(Type.BASE, newBaseTemp);
        set(Type.CORE, CSMath.clamp(newCoreTemp, -150d, 150d));
        set(Type.WORLD, newWorldTemp);
        set(Type.MAX, newMaxOffset);
        set(Type.MIN, newMinOffset);

        // Calculate body/base temperatures with modifiers
        double bodyTemp = get(Type.BODY);

        boolean hasFireResistance = player.isPotionActive(Effects.FIRE_RESISTANCE)   && config.fireRes;
        boolean hasIceResistance  = player.isPotionActive(ModEffects.ICE_RESISTANCE) && config.iceRes;

        //Deal damage to the player if temperature is critical
        if (player.ticksExisted % 40 == 0)
        {
            boolean damageScaling = config.damageScaling;

            if (bodyTemp >= 100 && !hasFireResistance && !player.isPotionActive(ModEffects.GRACE))
            {
                player.attackEntityFrom(damageScaling ? ModDamageSources.HOT.setDifficultyScaled() : ModDamageSources.HOT, 2f);
            }
            else if (bodyTemp <= -100 && !hasIceResistance && !player.isPotionActive(ModEffects.GRACE))
            {
                player.attackEntityFrom(damageScaling ? ModDamageSources.COLD.setDifficultyScaled() : ModDamageSources.COLD, 2f);
            }
        }
    }

    // Used for returning the player's temperature back to 0
    private static double getBodyReturnRate(double worldTemp, double tempLimit, double tempRate, double bodyTemp)
    {
        double staticRate = 3.0d;
        // Get the difference between the world temp and the threshold to determine the speed of return (closer to the threshold = slower)
        // Divide it by the staticRate (7) because it feels nice
        // Multiply it by the configured tempRate Rate Modifier
        // If it's too slow, default to tempRate / 30 instead
        // Multiply it by -CSMath.getSign(bodyTemp) to make it go toward 0
        double changeBy = Math.max((Math.abs(worldTemp - tempLimit) / staticRate) * tempRate, tempRate / 10) * -CSMath.getSign(bodyTemp);
        return CSMath.getLeastExtreme(changeBy, -bodyTemp);
    }

    @Override
    public void copy(ITemperatureCap cap)
    {
        // Copy temperature values
        for (Type type : Type.values())
        {
            if (type == Type.BODY || type == Type.RATE) continue;
            this.set(type, cap.get(type));
        }

        // Copy the modifiers
        for (Type type : Type.values())
        {
            if (type == Type.BODY) continue;
            this.getModifiers(type).clear();
            this.getModifiers(type).addAll(cap.getModifiers(type));
        }
    }

    @Override
    public CompoundNBT serializeNBT()
    {
        CompoundNBT nbt = new CompoundNBT();

        // Save the player's temperature data
        nbt.putDouble(TempHelper.getTempTag(Type.CORE), get(Type.CORE));
        nbt.putDouble(TempHelper.getTempTag(Type.BASE), get(Type.BASE));
        nbt.putDouble(TempHelper.getTempTag(Type.MAX),  get(Type.MAX));
        nbt.putDouble(TempHelper.getTempTag(Type.MIN),  get(Type.MIN));

        // Save the player's modifiers
        for (Type type : VALID_MODIFIER_TYPES)
        {
            ListNBT modifiers = new ListNBT();
            for (TempModifier modifier : getModifiers(type))
            {
                modifiers.add(NBTHelper.modifierToNBT(modifier));
            }

            // Write the list of modifiers to the player's persistent data
            nbt.put(TempHelper.getModifierTag(type), modifiers);
        }
        return nbt;
    }

    @Override
    public void deserializeNBT(CompoundNBT nbt)
    {
        set(Type.CORE, nbt.getDouble(TempHelper.getTempTag(Type.CORE)));
        set(Type.BASE, nbt.getDouble(TempHelper.getTempTag(Type.BASE)));
        set(Type.MAX,  nbt.getDouble(TempHelper.getTempTag(Type.MAX)));
        set(Type.MIN,  nbt.getDouble(TempHelper.getTempTag(Type.MIN)));

        // Load the player's modifiers
        Type[] validTypes =
        {
                Type.CORE, Type.BASE, Type.RATE,
                Type.MAX, Type.MIN, Type.WORLD
        };
        for (Type type : validTypes)
        {
            // Get the list of modifiers from the player's persistent data
            ListNBT modifiers = nbt.getList(TempHelper.getModifierTag(type), 10);

            // For each modifier in the list
            modifiers.forEach(modifier ->
            {
                CompoundNBT modifierNBT = (CompoundNBT) modifier;

                // Add the modifier to the player's temperature
                getModifiers(type).add(NBTHelper.NBTToModifier(modifierNBT));
            });
        }
    }
}
