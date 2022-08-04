package dev.momostudios.coldsweat.api.temperature.modifier;

import dev.momostudios.coldsweat.util.world.WorldHelper;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeManager;
import net.minecraftforge.common.BiomeDictionary;
import dev.momostudios.coldsweat.api.temperature.Temperature;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class TimeTempModifier extends TempModifier
{
    static Map<Biome, RegistryKey<Biome>> BIOME_KEYS = new HashMap<>();

    @Override
    public Function<Temperature, Temperature> calculate(PlayerEntity player)
    {
        if (!player.world.getDimensionType().doesFixedTimeExist())
        {
            float timeTemp = 0;
            World world = player.world;
            for (BlockPos blockPos : WorldHelper.getNearbyPositions(player.getPosition(), 50, 10))
            {
                BiomeManager biomeManager = player.world.getBiomeManager();
                Biome biome = biomeManager.getBiome(blockPos);

                RegistryKey<Biome> key = BIOME_KEYS.get(biome);

                if (key == null)
                    key = RegistryKey.getOrCreateKey(Registry.BIOME_KEY, biome.getRegistryName());

                if (BiomeDictionary.hasType(key, BiomeDictionary.Type.HOT)
                &&  BiomeDictionary.hasType(key, BiomeDictionary.Type.SANDY))
                {
                    timeTemp += Math.sin(world.getDayTime() / 3819.7186342) - 0.5;
                }
                else
                {
                    timeTemp += (Math.sin(world.getDayTime() / 3819.7186342) / 4d) - 0.125;
                }
            }

            float finalTimeTemp = timeTemp;
            return temp -> temp.add(finalTimeTemp / 200);
        }
        else return temp -> temp;
    }

    public String getID()
    {
        return "cold_sweat:time";
    }
}