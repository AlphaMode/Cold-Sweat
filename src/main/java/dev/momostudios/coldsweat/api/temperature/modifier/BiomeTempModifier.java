package dev.momostudios.coldsweat.api.temperature.modifier;

import dev.momostudios.coldsweat.config.WorldSettingsConfig;
import dev.momostudios.coldsweat.util.config.ConfigHelper;
import dev.momostudios.coldsweat.util.config.LoadedValue;
import dev.momostudios.coldsweat.util.world.WorldHelper;
import net.minecraft.entity.player.PlayerEntity;
import dev.momostudios.coldsweat.api.temperature.Temperature;
import dev.momostudios.coldsweat.util.config.ConfigCache;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class BiomeTempModifier extends TempModifier
{
    static LoadedValue<Map<ResourceLocation, Number>> BIOME_TEMPS       = LoadedValue.of(() ->
            ConfigHelper.getBiomesWithValues(WorldSettingsConfig.getInstance().biomeTemperatures()));

    static LoadedValue<Map<ResourceLocation, Number>> BIOME_OFFSETS     = LoadedValue.of(() ->
            ConfigHelper.getBiomesWithValues(WorldSettingsConfig.getInstance().biomeOffsets()));

    static LoadedValue<Map<ResourceLocation, Number>> DIMENSION_TEMPS   = LoadedValue.of(() ->
    {
        Map<ResourceLocation, Number> map = new HashMap<>();
        for (List<?> entry : WorldSettingsConfig.getInstance().dimensionTemperatures())
        {
            map.put(new ResourceLocation((String) entry.get(0)), (Number) entry.get(1));
        }
        return map;
    });

    static LoadedValue<Map<ResourceLocation, Number>> DIMENSION_OFFSETS = LoadedValue.of(() ->
    {
        Map<ResourceLocation, Number> map = new HashMap<>();
        for (List<?> entry : WorldSettingsConfig.getInstance().dimensionOffsets())
        {
            map.put(new ResourceLocation((String) entry.get(0)), (Number) entry.get(1));
        }
        return map;
    });

    @Override
    public Function<Temperature, Temperature> calculate(PlayerEntity player)
    {
        BiomeManager biomeManager = player.world.getBiomeManager();

        double worldTemp = 0;
        for (BlockPos blockPos : WorldHelper.getNearbyPositions(player.getPosition(), 50, 10))
        {
            Biome biome = biomeManager.getBiome(blockPos);

            ResourceLocation biomeID = biome.getRegistryName();
            ResourceLocation dimensionID = player.world.getDimensionKey().getLocation();

            // Should temperature be overridden by config
            Number biomeOverride = BIOME_TEMPS.get().get(biomeID);
            Number dimensionOverride = DIMENSION_TEMPS.get().get(dimensionID);

            if (dimensionOverride != null)
            {
                worldTemp += dimensionOverride.doubleValue();
                continue;
            }
            if (biomeOverride != null)
            {
                worldTemp += biomeOverride.doubleValue();
                continue;
            }

            Number biomeOffset = BIOME_OFFSETS.get().get(biomeID);
            Number dimensionOffset = DIMENSION_OFFSETS.get().get(dimensionID);

            // If temperature is not overridden, apply the offsets
            worldTemp += biome.getTemperature(blockPos);
            if (biomeOffset != null) worldTemp += biomeOffset.doubleValue();
            if (dimensionOffset != null) worldTemp += dimensionOffset.doubleValue();

        }
        double finalWorldTemp = worldTemp;
        return temp -> temp.add(finalWorldTemp / 50);
    }

    public String getID()
    {
        return "cold_sweat:biome_temperature";
    }
}