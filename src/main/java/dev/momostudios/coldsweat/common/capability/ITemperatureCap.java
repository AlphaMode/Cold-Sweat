package dev.momostudios.coldsweat.common.capability;

import dev.momostudios.coldsweat.api.temperature.Temperature;
import dev.momostudios.coldsweat.api.temperature.modifier.TempModifier;
import net.minecraft.entity.player.PlayerEntity;

import java.util.List;

public interface ITemperatureCap
{
    double get(Temperature.Types type);
    void set(Temperature.Types type, double value);
    List<TempModifier> getModifiers(Temperature.Types type);
    boolean hasModifier(Temperature.Types type, Class<? extends TempModifier> mod);
    void clearModifiers(Temperature.Types type);
    void copy(ITemperatureCap cap);
    void tickUpdate(PlayerEntity player);
    void tickClient(PlayerEntity player);
}
