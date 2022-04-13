package dev.momostudios.coldsweat.core.event;

import dev.momostudios.coldsweat.api.temperature.Temperature.Types;
import dev.momostudios.coldsweat.api.temperature.modifier.TempModifier;
import dev.momostudios.coldsweat.common.capability.ITemperatureCap;
import dev.momostudios.coldsweat.common.capability.ModCapabilities;
import dev.momostudios.coldsweat.common.capability.PlayerTempCapability;
import dev.momostudios.coldsweat.util.entity.NBTHelper;
import dev.momostudios.coldsweat.util.entity.TempHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import dev.momostudios.coldsweat.ColdSweat;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@Mod.EventBusSubscriber
public class AttachCapabilities
{
    @SubscribeEvent
    public static void attachCapabilityToEntityHandler(final AttachCapabilitiesEvent<Entity> event)
    {
        if (!(event.getObject() instanceof PlayerEntity)) return;

        PlayerTempCapability backend = new PlayerTempCapability();
        LazyOptional<PlayerTempCapability> optionalStorage = LazyOptional.of(() -> backend);
        Capability<ITemperatureCap> capability = ModCapabilities.PLAYER_TEMPERATURE;

        ICapabilityProvider provider = new ICapabilitySerializable<CompoundNBT>()
        {
            @Nonnull
            @Override
            public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction direction)
            {
                if (cap == capability)
                {
                    return optionalStorage.cast();
                }
                return LazyOptional.empty();
            }

            @Override
            public CompoundNBT serializeNBT()
            {
                return (CompoundNBT) capability.getStorage().writeNBT(capability, backend, null);
            }

            @Override
            public void deserializeNBT(CompoundNBT nbt)
            {
                capability.getStorage().readNBT(capability, backend, null, nbt);
            }
        };

        event.addCapability(new ResourceLocation(ColdSweat.MOD_ID, "temperature"), provider);
        event.addListener(optionalStorage::invalidate);
    }

    @SubscribeEvent
    public static void copyCaps(PlayerEvent.Clone event)
    {
        if (!event.isWasDeath())
        {
            PlayerEntity oldPlayer = event.getOriginal();
            oldPlayer.revive();
            oldPlayer.getCapability(ModCapabilities.PLAYER_TEMPERATURE).ifPresent(oldTempCap ->
            {
                event.getPlayer().getCapability(ModCapabilities.PLAYER_TEMPERATURE).ifPresent(newTempCap ->
                {
                    newTempCap.copy(oldTempCap);
                });
            });
            oldPlayer.remove();
        }
    }
}