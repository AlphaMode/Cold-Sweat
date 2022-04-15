package dev.momostudios.coldsweat.util.entity;

import dev.momostudios.coldsweat.api.registry.TempModifierRegistry;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.*;
import dev.momostudios.coldsweat.api.temperature.modifier.TempModifier;
import net.minecraft.tileentity.TileEntity;

import java.util.function.Predicate;

public class NBTHelper
{
    public static Object getObjectFromINBT(INBT inbt)
    {
        if (inbt instanceof StringNBT)
        {
            return ((StringNBT) inbt).getString();
        }
        else if (inbt instanceof IntNBT)
        {
            return ((IntNBT) inbt).getInt();
        }
        else if (inbt instanceof FloatNBT)
        {
            return ((FloatNBT) inbt).getFloat();
        }
        else if (inbt instanceof DoubleNBT)
        {
            return ((DoubleNBT) inbt).getDouble();
        }
        else if (inbt instanceof ShortNBT)
        {
            return ((ShortNBT) inbt).getShort();
        }
        else if (inbt instanceof LongNBT)
        {
            return ((LongNBT) inbt).getLong();
        }
        else if (inbt instanceof IntArrayNBT)
        {
            return ((IntArrayNBT) inbt).getIntArray();
        }
        else if (inbt instanceof LongArrayNBT)
        {
            return ((LongArrayNBT) inbt).getAsLongArray();
        }
        else if (inbt instanceof ByteArrayNBT)
        {
            return ((ByteArrayNBT) inbt).getByteArray();
        }
        else if (inbt instanceof ByteNBT)
        {
            return ((ByteNBT) inbt).getByte() != 0;
        }
        else throw new UnsupportedOperationException("Unsupported NBT type: " + inbt.getClass().getName());
    }

    public static INBT getINBTFromObject(Object object)
    {
        if (object instanceof String)
        {
            return StringNBT.valueOf((String) object);
        }
        else if (object instanceof Integer)
        {
            return IntNBT.valueOf((Integer) object);
        }
        else if (object instanceof Float)
        {
            return FloatNBT.valueOf((Float) object);
        }
        else if (object instanceof Double)
        {
            return DoubleNBT.valueOf((Double) object);
        }
        else if (object instanceof Byte)
        {
            return ByteNBT.valueOf((Byte) object);
        }
        else if (object instanceof Short)
        {
            return ShortNBT.valueOf((Short) object);
        }
        else if (object instanceof Long)
        {
            return LongNBT.valueOf((Long) object);
        }
        else if (object instanceof int[])
        {
            return new IntArrayNBT((int[]) object);
        }
        else if (object instanceof long[])
        {
            return new LongArrayNBT((long[]) object);
        }
        else if (object instanceof byte[])
        {
            return new ByteArrayNBT((byte[]) object);
        }
        else if (object instanceof Boolean)
        {
            return ByteNBT.valueOf((Boolean) object ? (byte) 1 : (byte) 0);
        }
        else
        {
            throw new UnsupportedOperationException("Unsupported object type: " + object.getClass().getName());
        }
    }

    public static CompoundNBT modifierToNBT(TempModifier modifier)
    {
        // Write the modifier's data to a CompoundNBT
        CompoundNBT modifierNBT = new CompoundNBT();
        modifierNBT.putString("id", modifier.getID());

        // Add the modifier's arguments
        modifier.getArguments().forEach((name, value) ->
        {
            modifierNBT.put(name, NBTHelper.getINBTFromObject(value));
        });

        // Read the modifier's expiration time
        if (modifier.getExpireTime() != -1)
            modifierNBT.putInt("expireTicks", modifier.getExpireTime());

        // Read the modifier's tick rate
        if (modifier.getTickRate() > 1)
            modifierNBT.putInt("tickRate", modifier.getTickRate());

        // Read the modifier's ticks existed
        modifierNBT.putInt("ticksExisted", modifier.getTicksExisted());

        return modifierNBT;
    }

    public static TempModifier NBTToModifier(CompoundNBT modifierNBT)
    {
        // Create a new modifier from the CompoundNBT
        TempModifier newModifier = TempModifierRegistry.getEntryFor(modifierNBT.getString("id"));

        if (newModifier == null) return null;
        modifierNBT.keySet().forEach(key ->
        {
            // Add the modifier's arguments
            if (key != null && !modifierNBT.get(key).getString().equals(modifierNBT.getString("id")))
                newModifier.addArgument(key, NBTHelper.getObjectFromINBT(modifierNBT.get(key)));
        });

        // Set the modifier's expiration time
        if (modifierNBT.contains("expireTicks"))
            newModifier.expires(modifierNBT.getInt("expireTicks"));

        // Set the modifier's tick rate
        if (modifierNBT.contains("tickRate"))
            newModifier.tickRate(modifierNBT.getInt("tickRate"));

        // Set the modifier's ticks existed
        newModifier.setTicksExisted(modifierNBT.getInt("ticksExisted"));

        return newModifier;
    }

    public static int incrementNBT(Object owner, String key, int amount, Predicate<Integer> predicate)
    {
        CompoundNBT nbt;
        if (owner instanceof LivingEntity)
        {
            nbt = ((LivingEntity) owner).getPersistentData();
        }
        else if (owner instanceof ItemStack)
        {
            nbt = ((ItemStack) owner).getOrCreateTag();
        }
        else if (owner instanceof TileEntity)
        {
            nbt = ((TileEntity) owner).getTileData();
        }
        else return 0;

        int value = nbt.getInt(key);
        if (predicate.test(value))
        {
            nbt.putInt(key, value + amount);
        }
        return value + amount;
    }
}
