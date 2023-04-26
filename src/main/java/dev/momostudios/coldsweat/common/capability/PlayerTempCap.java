package dev.momostudios.coldsweat.common.capability;

import dev.momostudios.coldsweat.ColdSweat;
import dev.momostudios.coldsweat.api.temperature.modifier.*;
import dev.momostudios.coldsweat.util.entity.ModDamageSources;
import dev.momostudios.coldsweat.util.entity.NBTHelper;
import dev.momostudios.coldsweat.api.util.TempHelper;
import dev.momostudios.coldsweat.util.registries.ModEffects;
import dev.momostudios.coldsweat.api.temperature.Temperature;
import dev.momostudios.coldsweat.util.config.ConfigSettings;
import dev.momostudios.coldsweat.util.math.CSMath;
import dev.momostudios.coldsweat.util.registries.ModItems;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.potion.Effects;
import dev.momostudios.coldsweat.api.temperature.Temperature.Type;
import net.minecraftforge.common.util.INBTSerializable;

import java.util.ArrayList;
import java.util.List;

public class PlayerTempCap implements ITemperatureCap, INBTSerializable<CompoundNBT>
{
    static Type[] VALID_MODIFIER_TYPES = {Type.CORE, Type.BASE, Type.RATE, Type.MAX, Type.MIN, Type.WORLD};
    static Type[] VALID_TEMPERATURE_TYPES = {Type.CORE, Type.BASE, Type.MAX, Type.MIN, Type.WORLD};

    private double[] syncedValues = {-Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE};
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

    public boolean showBodyTemp;
    public boolean showWorldTemp;

    public double getTemp(Type type)
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

    public void setTemp(Type type, double value)
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
            new Temperature().with(player, getModifiers(type));
        }
    }

    public void tick(PlayerEntity player)
    {
        ConfigSettings config = ConfigSettings.getInstance();

        // Tick expiration time for world modifiers
        double newWorldTemp = new Temperature().with(player, getModifiers(Type.WORLD)).get();
        double newCoreTemp  = new Temperature(this.coreTemp).with(player, getModifiers(Type.CORE)).get();
        double newBaseTemp  = new Temperature().with(player, getModifiers(Type.BASE)).get();
        double newMaxOffset = new Temperature().with(player, getModifiers(Type.MAX)).get();
        double newMinOffset = new Temperature().with(player, getModifiers(Type.MIN)).get();

        double maxTemp = config.maxTemp + newMaxOffset;
        double minTemp = config.minTemp + newMinOffset;

        double tempRate = 7.0d;

        // 1 if newWorldTemp is above max, -1 if below min, 0 if between the values (safe)
        int magnitude = CSMath.getSignForRange(newWorldTemp, minTemp, maxTemp);

        // Don't make the player's temperature worse if they're in creative/spectator mode
        if (magnitude != 0 && !(player.isCreative() || player.isSpectator()))
        {
            double difference = Math.abs(newWorldTemp - CSMath.clamp(newWorldTemp, minTemp, maxTemp));
            Temperature changeBy = new Temperature(Math.max((difference / tempRate) * config.rate, Math.abs(config.rate / 50)) * magnitude);
            newCoreTemp = changeBy.with(player, getModifiers(Type.RATE)).add(newCoreTemp).get();
        }
        // If the player's temperature and world temperature are not both hot or both cold
        if (magnitude != CSMath.getSign(newCoreTemp))
        {
            // Return the player's body temperature to 0
            newCoreTemp += getBodyReturnRate(newWorldTemp, newCoreTemp > 0 ? maxTemp : minTemp, config.rate, newCoreTemp);
        }

        // Update whether certain UI elements are being displayed (temp isn't synced if the UI element isn't showing)
        if (player.ticksExisted % 20 == 0)
        {
            showWorldTemp = !ConfigSettings.getInstance().requireThermometer
                    || player.inventory.mainInventory.stream().limit(9).anyMatch(stack -> stack.getItem() == ModItems.THERMOMETER)
                    || player.getHeldItemOffhand().getItem() == ModItems.THERMOMETER;
            showBodyTemp = !player.isCreative() && !player.isSpectator();
        }

        // Sync the player's temperature to the client
        // Write the new temperature values
        setTemp(Temperature.Type.BASE, newBaseTemp);
        setTemp(Temperature.Type.CORE, CSMath.clamp(newCoreTemp, -150d, 150d));
        setTemp(Temperature.Type.WORLD, newWorldTemp);
        setTemp(Temperature.Type.MAX, newMaxOffset);
        setTemp(Temperature.Type.MIN, newMinOffset);

        // Sync the temperature values to the client
        if (ticksSinceSync++ >= 5
        && ((Math.abs(syncedValues[0] - newCoreTemp) >= 1 && showBodyTemp)
        ||  (Math.abs(syncedValues[1] - newBaseTemp) >= 1 && showBodyTemp)
        ||  (Math.abs(syncedValues[2] - newWorldTemp) >= 0.02 && showWorldTemp)
        ||  (Math.abs(syncedValues[3] - newMaxOffset) >= 0.02 && showWorldTemp)
        ||  (Math.abs(syncedValues[4] - newMinOffset) >= 0.02 && showWorldTemp)))
        {
            TempHelper.updateTemperature(player, this, false);
            syncedValues = new double[] { newCoreTemp, newBaseTemp, newWorldTemp, newMaxOffset, newMinOffset };
            ticksSinceSync = 0;
        }

        // Calculate body/base temperatures with modifiers
        double bodyTemp = getTemp(Type.BODY);

        //Deal damage to the player if temperature is critical
        if (player.ticksExisted % 40 == 0)
        {
            boolean damageScaling = config.damageScaling;

            if (bodyTemp >= 100 && !(player.isPotionActive(Effects.FIRE_RESISTANCE) && config.fireRes)
            && !player.isPotionActive(ModEffects.GRACE))
            {
                player.attackEntityFrom(damageScaling ? ModDamageSources.HOT.setDifficultyScaled() : ModDamageSources.HOT, 2f);
            }
            else if (bodyTemp <= -100 && !(player.isPotionActive(ModEffects.ICE_RESISTANCE) && config.iceRes)
            && !player.isPotionActive(ModEffects.GRACE))
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
        for (Type type : VALID_TEMPERATURE_TYPES)
        {
            if (type == Type.BODY || type == Type.RATE) continue;
            this.setTemp(type, cap.getTemp(type));
        }

        // Copy the modifiers
        for (Type type : VALID_MODIFIER_TYPES)
        {
            this.getModifiers(type).clear();
            this.getModifiers(type).addAll(cap.getModifiers(type));
        }
    }

    @Override
    public CompoundNBT serializeNBT()
    {
        // Save the player's temperatures
        CompoundNBT nbt = this.serializeTemps();

        // Save the player's modifiers
        nbt.merge(this.serializeModifiers());
        return nbt;
    }

    public CompoundNBT serializeTemps()
    {
        CompoundNBT nbt = new CompoundNBT();

        // Save the player's temperature data
        for (Type type : VALID_TEMPERATURE_TYPES)
        {
            nbt.putDouble(TempHelper.getTempTag(type), this.getTemp(type));
        }
        return nbt;
    }

    public CompoundNBT serializeModifiers()
    {
        CompoundNBT nbt = new CompoundNBT();

        // Save the player's modifiers
        for (Type type : VALID_MODIFIER_TYPES)
        {
            ListNBT modifiers = new ListNBT();
            for (TempModifier modifier : this.getModifiers(type))
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
        // Load the player's temperatures
        deserializeTemps(nbt);

        // Load the player's modifiers
        deserializeModifiers(nbt);
    }

    public void deserializeTemps(CompoundNBT nbt)
    {
        for (Type type : VALID_TEMPERATURE_TYPES)
        {
            setTemp(type, nbt.getDouble(TempHelper.getTempTag(type)));
        }
    }

    public void deserializeModifiers(CompoundNBT nbt)
    {
        for (Type type : VALID_MODIFIER_TYPES)
        {
            getModifiers(type).clear();

            // Get the list of modifiers from the player's persistent data
            ListNBT modifiers = nbt.getList(TempHelper.getModifierTag(type), 10);

            // For each modifier in the list
            modifiers.forEach(modNBT ->
            {
                TempModifier modifier = NBTHelper.NBTToModifier((CompoundNBT) modNBT);

                // Add the modifier to the player's temperature
                if (modifier != null) getModifiers(type).add(modifier);
                else ColdSweat.LOGGER.error("Failed to load modifier \"{}\" of type {}", ((CompoundNBT) modNBT).getString("id"), type);
            });
        }
    }
}
