package dev.momostudios.coldsweat.util.entity;

import dev.momostudios.coldsweat.ColdSweat;
import dev.momostudios.coldsweat.api.temperature.modifier.TempModifier;
import dev.momostudios.coldsweat.api.registry.TempModifierRegistry;
import dev.momostudios.coldsweat.common.capability.ITemperatureCap;
import dev.momostudios.coldsweat.common.capability.ModCapabilities;
import dev.momostudios.coldsweat.common.capability.PlayerTempCapability;
import dev.momostudios.coldsweat.api.event.common.TempModifierEvent;
import dev.momostudios.coldsweat.core.network.ColdSweatPacketHandler;
import dev.momostudios.coldsweat.core.network.message.PlayerModifiersSyncMessage;
import dev.momostudios.coldsweat.core.network.message.PlayerTempSyncMessage;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.network.PacketDistributor;
import dev.momostudios.coldsweat.api.temperature.Temperature;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

public class TempHelper
{
    /**
     * Returns the player's temperature of the specified type.
     */
    public static Temperature getTemperature(PlayerEntity player, Temperature.Types type)
    {
        return new Temperature(player.getCapability(ModCapabilities.PLAYER_TEMPERATURE).orElse(new PlayerTempCapability()).get(type));
    }

    /**
     * Use {@link TempModifier}s for over-time effects.
     */
    public static void setTemperature(PlayerEntity player, Temperature value, Temperature.Types type)
    {
        setTemperature(player, value, type, true);
    }

    public static void setTemperature(PlayerEntity player, Temperature value, Temperature.Types type, boolean sync)
    {
        player.getCapability(ModCapabilities.PLAYER_TEMPERATURE).ifPresent(capability ->
        {
            if (sync && !player.world.isRemote)
            {
                updateTemperature(player,
                        type == Temperature.Types.CORE  ? value : getTemperature(player, Temperature.Types.CORE),
                        type == Temperature.Types.BASE  ? value : getTemperature(player, Temperature.Types.BASE),
                        type == Temperature.Types.WORLD ? value : getTemperature(player, Temperature.Types.WORLD),
                        type == Temperature.Types.MAX   ? value : getTemperature(player, Temperature.Types.MAX),
                        type == Temperature.Types.MIN   ? value : getTemperature(player, Temperature.Types.MIN));
            }
            capability.set(type, value.get());
        });
    }

    public static void addTemperature(PlayerEntity player, Temperature value, Temperature.Types type)
    {
        addTemperature(player, value, type, true);
    }

    public static void addTemperature(PlayerEntity player, Temperature value, Temperature.Types type, boolean sync)
    {
        player.getCapability(ModCapabilities.PLAYER_TEMPERATURE).ifPresent(capability ->
        {
            capability.set(type, value.get() + capability.get(type));
            if (sync && !player.world.isRemote)
            {
                updateTemperature(player,
                        type == Temperature.Types.CORE  ? value : getTemperature(player, Temperature.Types.CORE),
                        type == Temperature.Types.BASE  ? value : getTemperature(player, Temperature.Types.BASE),
                        type == Temperature.Types.WORLD ? value : getTemperature(player, Temperature.Types.WORLD),
                        type == Temperature.Types.CORE  ? value : getTemperature(player, Temperature.Types.MAX),
                        type == Temperature.Types.BASE  ? value : getTemperature(player, Temperature.Types.MIN));
            }
        });
    }

    /**
     * Applies the given modifier to the player.<br>
     *
     * @param duplicates allows or disallows duplicate TempModifiers to be applied
     * (You might use this for things that have stacking effects, for example)
     */
    public static void addModifier(PlayerEntity player, TempModifier modifier, Temperature.Types type, boolean duplicates)
    {
        addModifier(player, modifier, type, duplicates ? Integer.MAX_VALUE : 1, false);
    }

    /** Adds the given modifier to the player. <br>
     * If a TempModifier of this class already exists, it will be replaced with the given instance. <br>
     */
    public static void insertModifier(PlayerEntity player, TempModifier modifier, Temperature.Types type)
    {
        addModifier(player, modifier, type, 1, true);
    }

    public static void addModifier(PlayerEntity player, TempModifier modifier, Temperature.Types type, int maxCount, boolean replace)
    {
        TempModifierEvent.Add event = new TempModifierEvent.Add(modifier, player, type, maxCount);
        MinecraftForge.EVENT_BUS.post(event);
        if (!event.isCanceled())
        {
            player.getCapability(ModCapabilities.PLAYER_TEMPERATURE).ifPresent(cap ->
            {
                TempModifier newModifier = event.getModifier();
                if (TempModifierRegistry.getEntries().containsKey(newModifier.getID()))
                {
                    List<TempModifier> modifiers = cap.getModifiers(type);
                    AtomicInteger duplicateCount = new AtomicInteger();

                    // If we're replacing, remove the old one first
                    if (replace)
                    {
                        // Test if there are more modifiers than maxCount allows
                        long modCount = modifiers.stream().filter(mod -> mod.getID().equals(newModifier.getID())).count();
                        int iterations = (int) modCount - maxCount;

                        // If there are more modifiers than maxCount allows, remove the excess
                        if (iterations >= 1)
                        {
                            cap.getModifiers(event.type).removeIf(mod ->
                            {
                                if (mod.getID().equals(newModifier.getID()))
                                {
                                    return duplicateCount.getAndIncrement() < iterations;
                                }
                                return false;
                            });
                        }
                    }
                    // If we're not replacing, test if there is room (# of modifiers of this type < maxCount)
                    else
                    {
                        for (TempModifier mod : cap.getModifiers(event.type))
                        {
                            if (mod.getID().equals(event.getModifier().getID()))
                            {
                                if (duplicateCount.getAndIncrement() >= event.maxCount)
                                {
                                    // Fail to add the modifier if there are already too many
                                    break;
                                }
                            }
                        }
                    }

                    // Add the modifier and update
                    if (duplicateCount.get() < event.maxCount)
                    {
                        cap.getModifiers(event.type).add(event.getModifier());
                        updateModifiers(player, cap);
                    }
                }
                else
                {
                    ColdSweat.LOGGER.error("Tried to reference invalid TempModifier with ID \"" + modifier.getID() + "\"! Is it not registered?");
                }
            });
        }
    }

    /**
     * Removes the specified number of TempModifiers of the specified type from the player
     * @param player The player being sampled
     * @param type Determines which TempModifier list to pull from
     * @param count The number of modifiers of the given type to be removed (can be higher than the number of modifiers on the player)
     * @param condition The predicate to determine which TempModifiers to remove
     */
    public static void removeModifiers(PlayerEntity player, Temperature.Types type, int count, Predicate<TempModifier> condition)
    {
        player.getCapability(ModCapabilities.PLAYER_TEMPERATURE).ifPresent(cap ->
        {
            AtomicInteger removed = new AtomicInteger(0);
            cap.getModifiers(type).removeIf(modifier ->
            {
                if (removed.get() < count)
                {
                    TempModifierEvent.Remove event = new TempModifierEvent.Remove(player, type, count, condition);
                    MinecraftForge.EVENT_BUS.post(event);
                    if (!event.isCanceled())
                    {
                        if (event.getCondition().test(modifier))
                        {
                            removed.incrementAndGet();
                            return true;
                        }
                    }
                }
                return false;
            });

            if (removed.get() > 0)
                updateModifiers(player, cap);
        });
    }

    /**
     * Gets all TempModifiers of the specified type on the player
     * @param player is the player being sampled
     * @param type determines which TempModifier list to pull from
     * @return a NEW list of all TempModifiers of the specified type
     */
    public static List<TempModifier> getModifiers(PlayerEntity player, Temperature.Types type)
    {
        List<TempModifier> mods =  player.getCapability(ModCapabilities.PLAYER_TEMPERATURE).orElse(new PlayerTempCapability()).getModifiers(type);
        mods.removeIf(mod -> mod == null || mod.getID() == null ||mod.getID().isEmpty());
        return mods;
    }

    /**
     * @param modClass The class of the TempModifier to check for
     * @param type The type of TempModifier to check for
     * @return true if the player has a TempModifier that extends the given class
     */
    public static boolean hasModifier(PlayerEntity player, Class<? extends TempModifier> modClass, Temperature.Types type)
    {
        return player.getCapability(ModCapabilities.PLAYER_TEMPERATURE).map(cap -> cap.hasModifier(type, modClass)).orElse(false);
    }

    /**
     * Iterates through all TempModifiers of the specified type on the player
     * @param type determines which TempModifier list to pull from
     * @param action the action(s) to perform on each TempModifier
     */
    public static void forEachModifier(PlayerEntity player, Temperature.Types type, BiConsumer<TempModifier, Iterator<TempModifier>> action)
    {
        player.getCapability(ModCapabilities.PLAYER_TEMPERATURE).ifPresent(cap ->
        {
            List<TempModifier> modList = cap.getModifiers(type);
            if (modList != null)
            {
                Iterator<TempModifier> iterator = modList.iterator();
                while (iterator.hasNext())
                {
                    TempModifier modifier = iterator.next();
                    action.accept(modifier, iterator);
                }
            }
        });
    }

    /**
     * Used for storing TempModifiers in the player's persistent data (NBT). <br>
     * <br>
     * @param type The type of TempModifier to be stored
     * @return The NBT tag name for the given type
     */
    public static String getModifierTag(Temperature.Types type)
    {
        switch (type)
        {
            case CORE:     return "body_temp_modifiers";
            case WORLD:     return "world_temp_modifiers";
            case BASE :     return "base_temp_modifiers";
            case RATE :     return "rate_temp_modifiers";
            case MAX:  return "hottest_temp_modifiers";
            case MIN:  return "coldest_temp_modifiers";
            default : throw new IllegalArgumentException("Received illegal argument type: " + type.name());
        }
    }

    /**
     * Used for storing Temperature values in the player's persistent data (NBT). <br>
     * <br>
     * @param type The type of Temperature to be stored. ({@link Temperature.Types#WORLD} should only be stored when needed to prevent lag)
     * @return The NBT tag name for the given type
     */
    public static String getTempTag(Temperature.Types type)
    {
        switch (type)
        {
            case CORE:      return "body_temperature";
            case WORLD:     return "world_temperature";
            case BASE :     return "base_temperature";
            case BODY:      return "total_temperature";
            case MAX:   return "hottest_temperature";
            case MIN:   return "coldest_temperature";
            default : throw new IllegalArgumentException("Received illegal argument type: " + type.name());
        }
    }

    public static void updateTemperature(PlayerEntity player, Temperature bodyTemp, Temperature baseTemp, Temperature worldTemp, Temperature max, Temperature min)
    {
        if (!player.world.isRemote)
        {
            ColdSweatPacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> (ServerPlayerEntity) player),
                    new PlayerTempSyncMessage(bodyTemp.get(), baseTemp.get(), worldTemp.get(), max.get(), min.get()));
        }
    }

    public static void updateModifiers(PlayerEntity player, List<TempModifier> body, List<TempModifier> ambient, List<TempModifier> base, List<TempModifier> rate, List<TempModifier> max, List<TempModifier> min)
    {
        if (!player.world.isRemote)
        {
            ColdSweatPacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> (ServerPlayerEntity) player),
                    new PlayerModifiersSyncMessage(body, ambient, base, rate, max, min));
        }
    }

    public static void updateModifiers(PlayerEntity player, ITemperatureCap cap)
    {
        if (!player.world.isRemote)
        {
            ColdSweatPacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> (ServerPlayerEntity) player),
                    new PlayerModifiersSyncMessage(
                            cap.getModifiers(Temperature.Types.CORE),
                            cap.getModifiers(Temperature.Types.WORLD),
                            cap.getModifiers(Temperature.Types.BASE),
                            cap.getModifiers(Temperature.Types.RATE),
                            cap.getModifiers(Temperature.Types.MAX),
                            cap.getModifiers(Temperature.Types.MIN)));
        }
    }
}
