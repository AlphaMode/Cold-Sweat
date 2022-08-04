package dev.momostudios.coldsweat.api.util;

import dev.momostudios.coldsweat.ColdSweat;
import dev.momostudios.coldsweat.api.temperature.modifier.TempModifier;
import dev.momostudios.coldsweat.api.registry.TempModifierRegistry;
import dev.momostudios.coldsweat.common.capability.ITemperatureCap;
import dev.momostudios.coldsweat.common.capability.ModCapabilities;
import dev.momostudios.coldsweat.common.capability.PlayerTempCap;
import dev.momostudios.coldsweat.api.event.common.TempModifierEvent;
import dev.momostudios.coldsweat.core.network.ColdSweatPacketHandler;
import dev.momostudios.coldsweat.core.network.message.PlayerModifiersSyncMessage;
import dev.momostudios.coldsweat.core.network.message.PlayerTempSyncMessage;
import dev.momostudios.coldsweat.util.math.CSMath;
import dev.momostudios.coldsweat.util.math.InterruptableStreamer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.network.PacketDistributor;
import dev.momostudios.coldsweat.api.temperature.Temperature;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class TempHelper
{
    /**
     * Returns the player's temperature of the specified type.
     */
    public static Temperature getTemperature(PlayerEntity player, Temperature.Type type)
    {
        return new Temperature(player.getCapability(ModCapabilities.PLAYER_TEMPERATURE).orElse(new PlayerTempCap()).get(type));
    }

    public static void setTemperature(PlayerEntity player, Temperature value, Temperature.Type type)
    {
        player.getCapability(ModCapabilities.PLAYER_TEMPERATURE).ifPresent(cap ->
        {
            cap.set(type, value.get());
        });
    }

    public static void addTemperature(PlayerEntity player, Temperature value, Temperature.Type type)
    {
        player.getCapability(ModCapabilities.PLAYER_TEMPERATURE).ifPresent(cap ->
        {
            cap.set(type, value.add(cap.get(type)).get());
        });
    }

    /**
     * @param modClass The class of the TempModifier to check for
     * @param type The type of TempModifier to check for
     * @return true if the player has a TempModifier that extends the given class
     */
    public static boolean hasModifier(PlayerEntity player, Temperature.Type type, Class<? extends TempModifier> modClass)
    {
        return player.getCapability(ModCapabilities.PLAYER_TEMPERATURE).map(cap -> cap.hasModifier(type, modClass)).orElse(false);
    }

    /**
     * @return The first modifier of the given class that is applied to the player.
     */
    public static <T extends TempModifier> T getModifier(PlayerEntity player, Temperature.Type type, Class<T> modClass)
    {
        AtomicReference<T> mod = new AtomicReference<>();
        player.getCapability(ModCapabilities.PLAYER_TEMPERATURE).ifPresent(cap ->
        {
            for (TempModifier modifier : cap.getModifiers(type))
            {
                if (modifier.getClass() == modClass)
                {
                    mod.set((T) modifier);
                    break;
                }
            }
        });
        return mod.get();
    }

    /**
     * @return The first modifier applied to the player that fits the predicate.
     */
    public static TempModifier getModifier(PlayerEntity player, Temperature.Type type, Predicate<TempModifier> condition)
    {
        AtomicReference<TempModifier> mod = new AtomicReference<>();
        player.getCapability(ModCapabilities.PLAYER_TEMPERATURE).ifPresent(cap ->
        {
            for (TempModifier modifier : cap.getModifiers(type))
            {
                if (condition.test(modifier))
                {
                    mod.set(modifier);
                    break;
                }
            }
        });
        return mod.get();
    }

    /**
     * Applies the given modifier to the player.<br>
     *
     * @param allowDuplicates allows or disallows duplicate TempModifiers to be applied
     * (You might use this for things that have stacking effects, for example)
     */
    public static void addModifier(PlayerEntity player, TempModifier modifier, Temperature.Type type, boolean allowDuplicates)
    {
        addModifier(player, modifier, type, allowDuplicates ? Integer.MAX_VALUE : 1, false);
    }

    /** Adds the given modifier to the player. <br>
     * If a TempModifier of this class already exists, it will be replaced with the given instance. <br>
     */
    public static void replaceModifier(PlayerEntity player, TempModifier modifier, Temperature.Type type)
    {
        addModifier(player, modifier, type, 1, true);
    }

    public static void addModifier(PlayerEntity player, TempModifier modifier, Temperature.Type type, int maxCount, boolean replace)
    {
        TempModifierEvent.Add event = new TempModifierEvent.Add(modifier, player, type, maxCount);
        MinecraftForge.EVENT_BUS.post(event);
        if (!event.isCanceled())
        {
            TempModifier newModifier = event.getModifier();
            if (TempModifierRegistry.getEntries().containsKey(newModifier.getID()))
            {
                player.getCapability(ModCapabilities.PLAYER_TEMPERATURE).ifPresent(cap ->
                {
                    List<TempModifier> modifiers = cap.getModifiers(event.type);

                    // Find all the modifiers of this type
                    List<TempModifier> matchingMods = modifiers.stream().filter(mod -> mod.getID().equals(newModifier.getID())).collect(Collectors.toList());
                    int matchingCount = matchingMods.size();

                    // If there are more modifiers than allowed
                    if (matchingCount >= event.maxCount)
                    {
                        // If replacing, delete extra modifiers
                        if (replace)
                        {
                            modifiers.removeAll(matchingMods.stream().limit(matchingMods.size() - (event.maxCount - 1)).collect(Collectors.toList()));
                            matchingCount = 0;
                        }
                        // Otherwise the modifier can't be added
                        else return;
                    }

                    // Add the modifier and update
                    if (matchingCount < event.maxCount)
                    {
                        modifiers.add(event.getModifier());
                        updateModifiers(player, cap);
                    }
                });
            }
            else
            {
                ColdSweat.LOGGER.error("Tried to reference invalid TempModifier with ID \"" + modifier.getID() + "\"! Is it not registered?");
            }
        }
    }

    /**
     * Removes the specified number of TempModifiers of the specified type from the player
     * @param player The player being sampled
     * @param type Determines which TempModifier list to pull from
     * @param count The number of modifiers of the given type to be removed (can be higher than the number of modifiers on the player)
     * @param condition The predicate to determine which TempModifiers to remove
     */
    public static void removeModifiers(PlayerEntity player, Temperature.Type type, int count, Predicate<TempModifier> condition)
    {
        AtomicInteger removed = new AtomicInteger(0);

        player.getCapability(ModCapabilities.PLAYER_TEMPERATURE).ifPresent(cap ->
        {
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
                    return false;
                }
                return false;
            });

            // Update modifiers if anything actually changed
            if (removed.get() > 0)
                updateModifiers(player, cap);
        });
    }

    public static void removeModifiers(PlayerEntity player, Temperature.Type type, Predicate<TempModifier> condition)
    {
        removeModifiers(player, type, Integer.MAX_VALUE, condition);
    }

    /**
     * Gets all TempModifiers of the specified type on the player
     * @param player is the player being sampled
     * @param type determines which TempModifier list to pull from
     * @return a NEW list of all TempModifiers of the specified type
     */
    public static List<TempModifier> getModifiers(PlayerEntity player, Temperature.Type type)
    {
        return player.getCapability(ModCapabilities.PLAYER_TEMPERATURE).map(cap -> cap.getModifiers(type)).orElse(null);
    }

    /**
     * Iterates through all TempModifiers of the specified type on the player
     * @param type determines which TempModifier list to pull from
     * @param action the action(s) to perform on each TempModifier
     */
    public static void forEachModifier(PlayerEntity player, Temperature.Type type, Consumer<TempModifier> action)
    {
        player.getCapability(ModCapabilities.PLAYER_TEMPERATURE).ifPresent(cap ->
        {
            if (cap.getModifiers(type) != null)
            {
                cap.getModifiers(type).forEach(action);
            }
        });
    }


    public static void forEachModifier(PlayerEntity player, Temperature.Type type, BiConsumer<TempModifier, InterruptableStreamer<TempModifier>> action)
    {
        player.getCapability(ModCapabilities.PLAYER_TEMPERATURE).ifPresent(cap ->
        {
            if (cap.getModifiers(type) != null)
            {
                CSMath.breakableForEach(cap.getModifiers(type), action);
            }
        });
    }

    /**
     * Used for storing TempModifiers in the player's persistent data (NBT). <br>
     * <br>
     * @param type The type of TempModifier to be stored
     * @return The NBT tag name for the given type
     */
    public static String getModifierTag(Temperature.Type type)
    {
        switch (type)
        {
            case CORE  : return "bodyTempModifiers";
            case WORLD : return "worldTempModifiers";
            case BASE  : return "baseTempModifiers";
            case RATE  : return "rateTempModifiers";
            case MAX   : return "maxTempModifiers";
            case MIN   : return "minTempModifiers";

            default : throw new IllegalArgumentException("Received illegal argument type: " + type.name());
        }
    }

    /**
     * Used for storing Temperature values in the player's persistent data (NBT). <br>
     * <br>
     * @param type The type of Temperature to be stored. ({@link Temperature.Type#WORLD} should only be stored when needed to prevent lag)
     * @return The NBT tag name for the given type
     */
    public static String getTempTag(Temperature.Type type)
    {
        switch (type)
        {
            case CORE  : return "coreTemp";
            case WORLD : return "worldTemp";
            case BASE  : return "baseTemp";
            case MAX   : return "maxTWorldTemp";
            case MIN   : return "minWorldTemp";

            default : throw new IllegalArgumentException("Received illegal argument type: " + type.name());
        }
    }

    public static void updateTemperature(PlayerEntity player, ITemperatureCap cap, boolean instant)
    {
        if (!player.world.isRemote)
        {
            ColdSweatPacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> (ServerPlayerEntity) player),
            new PlayerTempSyncMessage(
                cap.get(Temperature.Type.WORLD),
                cap.get(Temperature.Type.CORE),
                cap.get(Temperature.Type.BASE),
                cap.get(Temperature.Type.MAX),
                cap.get(Temperature.Type.MIN), instant));
        }
    }

    public static void updateModifiers(PlayerEntity player, ITemperatureCap cap)
    {
        if (!player.world.isRemote)
        {
            ColdSweatPacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> (ServerPlayerEntity) player),
            new PlayerModifiersSyncMessage(
                cap.getModifiers(Temperature.Type.WORLD),
                cap.getModifiers(Temperature.Type.CORE),
                cap.getModifiers(Temperature.Type.BASE),
                cap.getModifiers(Temperature.Type.RATE),
                cap.getModifiers(Temperature.Type.MAX),
                cap.getModifiers(Temperature.Type.MIN)));
        }
    }
}
