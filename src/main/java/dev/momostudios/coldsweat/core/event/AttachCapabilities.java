package dev.momostudios.coldsweat.core.event;

import dev.momostudios.coldsweat.api.temperature.Temperature;
import dev.momostudios.coldsweat.common.capability.PlayerTempCapability;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import dev.momostudios.coldsweat.ColdSweat;
import dev.momostudios.coldsweat.common.te.HearthTileEntity;
import dev.momostudios.coldsweat.api.temperature.modifier.TempModifier;
import dev.momostudios.coldsweat.common.capability.HearthRadiusCapability;
import dev.momostudios.coldsweat.common.capability.IBlockStorageCap;
import dev.momostudios.coldsweat.util.entity.NBTHelper;
import dev.momostudios.coldsweat.util.entity.TempHelper;

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
        Capability<PlayerTempCapability> capability = PlayerTempCapability.TEMPERATURE;

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
                return backend.serializeNBT();
            }

            @Override
            public void deserializeNBT(CompoundNBT nbt)
            {
                backend.deserializeNBT(nbt);
            }
        };

        event.addCapability(new ResourceLocation(ColdSweat.MOD_ID, "temperature"), provider);
        event.addListener(optionalStorage::invalidate);
    }

    @SubscribeEvent
    public static void attachCapabilityToTileHandler(AttachCapabilitiesEvent<TileEntity> event)
    {
        if (!(event.getObject() instanceof HearthTileEntity)) return;

        HearthRadiusCapability backend = new HearthRadiusCapability();
        LazyOptional<IBlockStorageCap> optionalStorage = LazyOptional.of(() -> backend);

        ICapabilityProvider provider = new ICapabilityProvider()
        {
            @Override
            public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction direction)
            {
                if (cap == HearthRadiusCapability.HEARTH_BLOCKS)
                {
                    return optionalStorage.cast();
                }
                return LazyOptional.empty();
            }
        };

        event.addCapability(new ResourceLocation(ColdSweat.MOD_ID, "hearth_points"), provider);
        event.addListener(optionalStorage::invalidate);
    }
}