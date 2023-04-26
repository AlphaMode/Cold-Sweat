package dev.momostudios.coldsweat.api.temperature.modifier;

import com.mojang.datafixers.util.Pair;
import dev.momostudios.coldsweat.util.config.ConfigSettings;
import dev.momostudios.coldsweat.util.math.CSMath;
import dev.momostudios.coldsweat.util.world.WorldHelper;
import net.minecraft.entity.player.PlayerEntity;
import dev.momostudios.coldsweat.api.temperature.Temperature;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.IChunk;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class BiomeTempModifier extends TempModifier
{
    static int SAMPLES = 25;

    @Override
    public Function<Temperature, Temperature> calculate(PlayerEntity player)
    {
        try
        {
            double worldTemp = 0;
            ResourceLocation dimensionID = player.world.getDimensionKey().getLocation();
            Number dimensionOverride = ConfigSettings.DIMENSION_TEMPS.get().get(dimensionID);

            if (dimensionOverride != null)
            {
                return temp -> temp.add(dimensionOverride.doubleValue());
            }
            else
            {
                Map<ChunkPos, IChunk> chunkMap = new HashMap<>();
                for (BlockPos blockPos : WorldHelper.getPositionGrid(player.getPosition(), SAMPLES, 16))
                {
                    IChunk chunk = chunkMap.computeIfAbsent(new ChunkPos(blockPos.getX() >> 4, blockPos.getZ() >> 4), chunkpos -> player.world.getChunkProvider().getChunk(chunkpos.x, chunkpos.z, ChunkStatus.BIOMES, false));
                    if (chunk == null) continue;

                    Biome biome = chunk.getBiomes().getNoiseBiome(blockPos.getX(), blockPos.getY(), blockPos.getZ());
                    ResourceLocation biomeID = biome.getRegistryName();

                    Pair<Double, Double> configTemp;
                    double biomeVariance = 1 / Math.max(1, 2 + biome.getDownfall() * 2);
                    double biomeTemp = biome.getTemperature();

                    // Get the biome's temperature, either overridden by config or calculated
                    // Start with biome override
                    configTemp = ConfigSettings.BIOME_TEMPS.get().getOrDefault(biomeID,
                                 // If no override, check for offset
                                 Pair.of((configTemp = ConfigSettings.BIOME_OFFSETS.get().getOrDefault(biomeID,
                                 // If no offset, do nothing
                                 Pair.of(0d, 0d)))
                                 // Add the biome's base temperature and calculate min/max based on biome's humidity
                                 .getFirst() + biomeTemp - biomeVariance, configTemp.getSecond() + biomeTemp + biomeVariance));

                    // Biome temp at midnight (bottom of the sine wave)
                    double min = configTemp.getFirst();
                    // Biome temp at noon (top of the sine wave)
                    double max = configTemp.getSecond();

                    // If time doesn't exist in the player's dimension, don't use it
                    if (!player.world.getDimensionType().getHasCeiling())
                    {   worldTemp += CSMath.blend(min, max, Math.sin(player.world.getDayTime() / (12000 / Math.PI)), -1, 1) / SAMPLES;
                    }
                    else worldTemp += CSMath.average(max, min) / SAMPLES;
                }

                worldTemp += ConfigSettings.DIMENSION_OFFSETS.get().getOrDefault(dimensionID, 0d);
            }
            double finalWorldTemp = worldTemp;
            return temp -> temp.add(finalWorldTemp);
        }
        catch (Exception e)
        {
            return (temp) -> temp;
        }
    }

    public String getID()
    {
        return "cold_sweat:biome_temperature";
    }
}