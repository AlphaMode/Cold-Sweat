package dev.momostudios.coldsweat.common.capability;

import dev.momostudios.coldsweat.api.temperature.modifier.*;
import dev.momostudios.coldsweat.core.network.ColdSweatPacketHandler;
import dev.momostudios.coldsweat.core.network.message.PlayerTempSyncMessage;
import dev.momostudios.coldsweat.util.entity.ModDamageSources;
import dev.momostudios.coldsweat.util.entity.NBTHelper;
import dev.momostudios.coldsweat.util.entity.TempHelper;
import dev.momostudios.coldsweat.util.registries.ModEffects;
import dev.momostudios.coldsweat.api.temperature.Temperature;
import dev.momostudios.coldsweat.util.config.ConfigCache;
import dev.momostudios.coldsweat.util.math.CSMath;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.potion.Effects;
import dev.momostudios.coldsweat.api.temperature.Temperature.Types;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.fml.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

public class PlayerTempCap implements ITemperatureCap, INBTSerializable<CompoundNBT>
{
    static Temperature.Types[] VALID_MODIFIER_TYPES = {Temperature.Types.CORE, Temperature.Types.BASE, Temperature.Types.RATE,
                                                       Temperature.Types.MAX,  Temperature.Types.MIN,  Temperature.Types.WORLD};

    private double[] syncedValues = new double[5];
    private int ticksSinceSync = 0;

    double worldTemp;
    double coreTemp;
    double baseTemp;
    double maxTemp;
    double minTemp;

    List<TempModifier> worldModifiers = new ArrayList<>();
    List<TempModifier> coreModifiers  = new ArrayList<>();
    List<TempModifier> baseModifiers  = new ArrayList<>();
    List<TempModifier> rateModifiers  = new ArrayList<>();
    List<TempModifier> maxModifiers   = new ArrayList<>();
    List<TempModifier> minModifiers   = new ArrayList<>();

    public double get(Types type)
    {
        switch (type)
        {
            case WORLD: return worldTemp;
            case CORE:  return coreTemp;
            case BASE:  return baseTemp;
            case BODY:  return baseTemp + coreTemp;
            case MAX:   return maxTemp;
            case MIN:   return minTemp;
            default: throw new IllegalArgumentException("Illegal type for PlayerTempCapability.getValue(): " + type);
        }
    }

    public void set(Types type, double value)
    {
        switch (type)
        {
            case CORE:  { this.coreTemp  = value; break; }
            case BASE:  { this.baseTemp  = value; break; }
            case WORLD: { this.worldTemp = value; break; }
            case MAX:   { this.maxTemp   = value; break; }
            case MIN:   { this.minTemp   = value; break; }
            default : throw new IllegalArgumentException("Illegal type for PlayerTempCapability.setValue(): " + type);
        }
    }

    public List<TempModifier> getModifiers(Types type)
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

    public boolean hasModifier(Types type, Class<? extends TempModifier> mod)
    {
        return this.getModifiers(type).stream().anyMatch(mod::isInstance);
    }


    public void clearModifiers(Types type)
    {
        this.getModifiers(type).clear();
    }

    public void tickDummy(PlayerEntity player)
    {
        for (Temperature.Types type : VALID_MODIFIER_TYPES)
        {
            new Temperature().with(getModifiers(type), player);
        }
    }

    public void tick(PlayerEntity player)
    {
        ConfigCache config = ConfigCache.getInstance();

        // Tick expiration time for world modifiers
        double worldTemp = new Temperature().with(getModifiers(Types.WORLD), player).get();
        Temperature coreTemp = new Temperature(this.coreTemp).with(getModifiers(Types.CORE), player);
        Temperature baseTemp = new Temperature().with(getModifiers(Types.BASE), player);
        double maxOffset = new Temperature().with(getModifiers(Types.MAX), player).get();
        double minOffset = new Temperature().with(getModifiers(Types.MIN), player).get();

        double maxTemp = config.maxTemp + maxOffset;
        double minTemp = config.minTemp + minOffset;

        double tempRate = 7.0d;

        int magnitude = CSMath.getSignForRange(worldTemp, minTemp, maxTemp);
        if (magnitude != 0)
        {
            double difference = Math.abs(worldTemp - CSMath.clamp(worldTemp, minTemp, maxTemp));
            Temperature changeBy = new Temperature(Math.max((difference / tempRate) * config.rate, Math.abs(config.rate / 50)) * magnitude);
            coreTemp = coreTemp.add(changeBy.with(getModifiers(Types.RATE), player));
        }
        if (magnitude != CSMath.getSign(coreTemp.get()))
        {
            // Return the player's body temperature to 0
            coreTemp = coreTemp.add(getBodyReturnRate(worldTemp, coreTemp.get() > 0 ? maxTemp : minTemp, config.rate, coreTemp.get()));
        }

        if (ticksSinceSync++ >= 5
        && ((int) syncedValues[0] != (int) coreTemp.get()
        || (int) syncedValues[1] != (int) baseTemp.get()
        || CSMath.crop(syncedValues[2], 2) != CSMath.crop(worldTemp, 2)
        || CSMath.crop(syncedValues[3], 2) != CSMath.crop(maxOffset, 2)
        || CSMath.crop(syncedValues[4], 2) != CSMath.crop(minOffset, 2)))
        {
            ticksSinceSync = 0;
            syncedValues = new double[] { coreTemp.get(), baseTemp.get(), worldTemp, maxOffset, minOffset };

            ColdSweatPacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> (ServerPlayerEntity) player),
                    new PlayerTempSyncMessage(
                            worldTemp,
                            coreTemp.get(),
                            baseTemp.get(),
                            maxOffset,
                            minOffset, false));
        }

        // Sets the player's body temperature to BASE + CORE
        set(Temperature.Types.BASE, baseTemp.get());
        set(Temperature.Types.CORE, CSMath.clamp(coreTemp.get(), -150d, 150d));
        set(Temperature.Types.WORLD, worldTemp);
        set(Temperature.Types.MAX, maxOffset);
        set(Temperature.Types.MIN, minOffset);

        // Calculate body/base temperatures with modifiers
        Temperature bodyTemp = baseTemp.add(coreTemp);

        boolean hasFireResistance = player.isPotionActive(Effects.FIRE_RESISTANCE)   && config.fireRes;
        boolean hasIceResistance  = player.isPotionActive(ModEffects.ICE_RESISTANCE) && config.iceRes;

        //Deal damage to the player if temperature is critical
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

    @Override
    public CompoundNBT serializeNBT()
    {
        CompoundNBT nbt = new CompoundNBT();

        // Save the player's temperature data
        nbt.putDouble(TempHelper.getTempTag(Temperature.Types.CORE), get(Temperature.Types.CORE));
        nbt.putDouble(TempHelper.getTempTag(Temperature.Types.BASE), get(Temperature.Types.BASE));
        nbt.putDouble(TempHelper.getTempTag(Temperature.Types.MAX),  get(Temperature.Types.MAX));
        nbt.putDouble(TempHelper.getTempTag(Temperature.Types.MIN),  get(Temperature.Types.MIN));

        // Save the player's modifiers
        for (Temperature.Types type : VALID_MODIFIER_TYPES)
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
        set(Temperature.Types.CORE, nbt.getDouble(TempHelper.getTempTag(Temperature.Types.CORE)));
        set(Temperature.Types.BASE, nbt.getDouble(TempHelper.getTempTag(Temperature.Types.BASE)));
        set(Temperature.Types.MAX,  nbt.getDouble(TempHelper.getTempTag(Temperature.Types.MAX)));
        set(Temperature.Types.MIN,  nbt.getDouble(TempHelper.getTempTag(Temperature.Types.MIN)));

        // Load the player's modifiers
        Temperature.Types[] validTypes =
        {
            Temperature.Types.CORE, Temperature.Types.BASE, Temperature.Types.RATE,
            Temperature.Types.MAX, Temperature.Types.MIN, Temperature.Types.WORLD
        };
        for (Temperature.Types type : validTypes)
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
