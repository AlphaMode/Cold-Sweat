package dev.momostudios.coldsweat.util.entity;

import dev.momostudios.coldsweat.ColdSweat;
import dev.momostudios.coldsweat.api.temperature.modifier.TempModifier;
import dev.momostudios.coldsweat.api.registry.TempModifierRegistry;
import dev.momostudios.coldsweat.common.capability.PlayerTempCapability;
import dev.momostudios.coldsweat.api.event.common.TempModifierEvent;
import dev.momostudios.coldsweat.core.network.ColdSweatPacketHandler;
import dev.momostudios.coldsweat.core.network.message.PlayerModifiersSyncMessage;
import dev.momostudios.coldsweat.core.network.message.PlayerTempSyncMessage;
import dev.momostudios.coldsweat.util.math.CSMath;
import dev.momostudios.coldsweat.util.registries.ModItems;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.HandSide;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.network.PacketDistributor;
import dev.momostudios.coldsweat.api.temperature.Temperature;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class TempHelper
{
    /**
     * Returns the player's temperature of the specified type.
     */
    public static Temperature getTemperature(PlayerEntity player, Temperature.Types type)
    {
        return new Temperature(player.getCapability(PlayerTempCapability.TEMPERATURE).orElse(new PlayerTempCapability()).get(type));
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
        player.getCapability(PlayerTempCapability.TEMPERATURE).ifPresent(capability ->
        {
            if (sync && !player.world.isRemote)
            {
                updateTemperature(player,
                        type == Temperature.Types.CORE ? value : getTemperature(player, Temperature.Types.CORE),
                        type == Temperature.Types.BASE ? value : getTemperature(player, Temperature.Types.BASE),
                        type == Temperature.Types.WORLD ? value : getTemperature(player, Temperature.Types.WORLD),
                        type == Temperature.Types.HOTTEST ? value : getTemperature(player, Temperature.Types.HOTTEST),
                        type == Temperature.Types.COLDEST ? value : getTemperature(player, Temperature.Types.COLDEST));
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
        player.getCapability(PlayerTempCapability.TEMPERATURE).ifPresent(capability ->
        {
            capability.set(type, value.get() + capability.get(type));
            if (sync && !player.world.isRemote)
            {
                updateTemperature(player,
                        type == Temperature.Types.CORE  ? value : getTemperature(player, Temperature.Types.CORE),
                        type == Temperature.Types.BASE  ? value : getTemperature(player, Temperature.Types.BASE),
                        type == Temperature.Types.WORLD ? value : getTemperature(player, Temperature.Types.WORLD),
                        type == Temperature.Types.CORE  ? value : getTemperature(player, Temperature.Types.HOTTEST),
                        type == Temperature.Types.BASE  ? value : getTemperature(player, Temperature.Types.COLDEST));
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
        addModifier(player, modifier, type, duplicates ? Integer.MAX_VALUE : 1);
    }

    public static void addModifier(PlayerEntity player, TempModifier modifier, Temperature.Types type, int maxCount)
    {
        TempModifierEvent.Add event = new TempModifierEvent.Add(modifier, player, type, maxCount);
        MinecraftForge.EVENT_BUS.post(event);
        if (!event.isCanceled())
        {
            player.getCapability(PlayerTempCapability.TEMPERATURE).ifPresent(cap ->
            {
                AtomicInteger duplicateCount = new AtomicInteger(0);
                if (TempModifierRegistry.getEntries().containsKey(event.getModifier().getID()))
                {
                    CSMath.breakableForEach(cap.getModifiers(event.type), (mod, looper) ->
                    {
                        if (mod.getID().equals(event.getModifier().getID()))
                        {
                            if (duplicateCount.getAndIncrement() > event.maxCount)
                            {
                                looper.stop();
                            }
                        }
                    });
                    if (duplicateCount.get() < event.maxCount)
                    {
                        cap.getModifiers(event.type).add(event.getModifier());
                    }
                }
                else
                {
                    ColdSweat.LOGGER.error("TempModifierEvent.Add: No TempModifier with ID " + modifier.getID() + " found! Is it not registered?");
                }

                if (!player.world.isRemote)
                    updateModifiers(player,
                            cap.getModifiers(Temperature.Types.CORE),
                            cap.getModifiers(Temperature.Types.BASE),
                            cap.getModifiers(Temperature.Types.WORLD),
                            cap.getModifiers(Temperature.Types.RATE),
                            cap.getModifiers(Temperature.Types.HOTTEST),
                            cap.getModifiers(Temperature.Types.COLDEST));
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
        player.getCapability(PlayerTempCapability.TEMPERATURE).ifPresent(cap ->
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

            if (!player.world.isRemote)
                updateModifiers(player,
                        cap.getModifiers(Temperature.Types.CORE),
                        cap.getModifiers(Temperature.Types.BASE),
                        cap.getModifiers(Temperature.Types.WORLD),
                        cap.getModifiers(Temperature.Types.RATE),
                        cap.getModifiers(Temperature.Types.HOTTEST),
                        cap.getModifiers(Temperature.Types.COLDEST));
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
        List<TempModifier> mods =  player.getCapability(PlayerTempCapability.TEMPERATURE).orElse(new PlayerTempCapability()).getModifiers(type);
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
        return player.getCapability(PlayerTempCapability.TEMPERATURE).map(cap -> cap.hasModifier(type, modClass)).orElse(false);
    }

    /**
     * Iterates through all TempModifiers of the specified type on the player
     * @param type determines which TempModifier list to pull from
     * @param action the action(s) to perform on each TempModifier
     */
    public static void forEachModifier(PlayerEntity player, Temperature.Types type, Consumer<TempModifier> action)
    {
        player.getCapability(PlayerTempCapability.TEMPERATURE).ifPresent(cap ->
        {
            if (cap.getModifiers(type) != null)
            {
                cap.getModifiers(type).forEach(action);
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
            case HOTTEST :  return "hottest_temp_modifiers";
            case COLDEST :  return "coldest_temp_modifiers";
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
            case HOTTEST:   return "hottest_temperature";
            case COLDEST:   return "coldest_temperature";
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

    public static void updateModifiers(PlayerEntity player, PlayerTempCapability cap)
    {
        if (!player.world.isRemote)
        {
            ColdSweatPacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> (ServerPlayerEntity) player),
                    new PlayerModifiersSyncMessage(
                            cap.getModifiers(Temperature.Types.CORE),
                            cap.getModifiers(Temperature.Types.WORLD),
                            cap.getModifiers(Temperature.Types.BASE),
                            cap.getModifiers(Temperature.Types.RATE),
                            cap.getModifiers(Temperature.Types.HOTTEST),
                            cap.getModifiers(Temperature.Types.COLDEST)));
        }
    }
}
