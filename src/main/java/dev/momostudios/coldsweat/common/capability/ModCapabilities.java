package dev.momostudios.coldsweat.common.capability;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;

public class ModCapabilities
{
    @CapabilityInject(ITemperatureCap.class)
    public static Capability<ITemperatureCap> PLAYER_TEMPERATURE = null;
}
