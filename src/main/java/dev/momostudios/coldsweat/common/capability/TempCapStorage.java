package dev.momostudios.coldsweat.common.capability;

import net.minecraft.nbt.INBT;
import net.minecraft.util.Direction;
import net.minecraftforge.common.capabilities.Capability;

import javax.annotation.Nullable;

public class TempCapStorage implements Capability.IStorage<ITemperatureCap>
{
    @Nullable
    @Override
    public INBT writeNBT(Capability<ITemperatureCap> capability, ITemperatureCap instance, Direction side)
    {
        return null;
    }

    @Override
    public void readNBT(Capability<ITemperatureCap> capability, ITemperatureCap instance, Direction side, INBT nbt)
    {

    }
}
