package dev.momostudios.coldsweat.common.capability;

import dev.momostudios.coldsweat.api.temperature.Temperature;
import dev.momostudios.coldsweat.api.temperature.modifier.TempModifier;
import dev.momostudios.coldsweat.util.entity.NBTHelper;
import dev.momostudios.coldsweat.util.entity.TempHelper;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.util.Direction;
import net.minecraftforge.common.capabilities.Capability;

import javax.annotation.Nullable;

public class TempCapStorage implements Capability.IStorage<ITemperatureCap>
{
    @Nullable
    @Override
    public CompoundNBT writeNBT(Capability<ITemperatureCap> capability, ITemperatureCap instance, Direction side)
    {
        CompoundNBT nbt = new CompoundNBT();

        // Save the player's temperature data
        nbt.putDouble(TempHelper.getTempTag(Temperature.Types.CORE),    instance.get(Temperature.Types.CORE));
        nbt.putDouble(TempHelper.getTempTag(Temperature.Types.BASE),    instance.get(Temperature.Types.BASE));
        nbt.putDouble(TempHelper.getTempTag(Temperature.Types.HOTTEST), instance.get(Temperature.Types.HOTTEST));
        nbt.putDouble(TempHelper.getTempTag(Temperature.Types.COLDEST), instance.get(Temperature.Types.COLDEST));

        // Save the player's modifiers
        Temperature.Types[] validTypes =
        {
            Temperature.Types.CORE, Temperature.Types.BASE, Temperature.Types.RATE, Temperature.Types.HOTTEST, Temperature.Types.COLDEST, Temperature.Types.WORLD
        };
        for (Temperature.Types type : validTypes)
        {
            ListNBT modifiers = new ListNBT();
            for (TempModifier modifier : instance.getModifiers(type))
            {
                modifiers.add(NBTHelper.modifierToNBT(modifier));
            }

            // Write the list of modifiers to the player's persistent data
            nbt.put(TempHelper.getModifierTag(type), modifiers);
        }
        return nbt;
    }

    @Override
    public void readNBT(Capability<ITemperatureCap> capability, ITemperatureCap instance, Direction side, INBT nbt)
    {
        if (nbt instanceof CompoundNBT)
        {
            CompoundNBT compound = (CompoundNBT) nbt;
            instance.set(Temperature.Types.CORE,    compound.getDouble(TempHelper.getTempTag(Temperature.Types.CORE)));
            instance.set(Temperature.Types.BASE,    compound.getDouble(TempHelper.getTempTag(Temperature.Types.BASE)));
            instance.set(Temperature.Types.HOTTEST, compound.getDouble(TempHelper.getTempTag(Temperature.Types.HOTTEST)));
            instance.set(Temperature.Types.COLDEST, compound.getDouble(TempHelper.getTempTag(Temperature.Types.COLDEST)));

            // Load the player's modifiers
            Temperature.Types[] validTypes =
            {
                Temperature.Types.CORE, Temperature.Types.BASE, Temperature.Types.RATE, Temperature.Types.HOTTEST, Temperature.Types.COLDEST, Temperature.Types.WORLD
            };
            for (Temperature.Types type : validTypes)
            {
                // Get the list of modifiers from the player's persistent data
                ListNBT modifiers = compound.getList(TempHelper.getModifierTag(type), 10);

                // For each modifier in the list
                modifiers.forEach(modifier ->
                {
                    CompoundNBT modifierNBT = (CompoundNBT) modifier;

                    // Add the modifier to the player's temperature
                    instance.getModifiers(type).add(NBTHelper.NBTToModifier(modifierNBT));
                });
            }
        }
    }
}
