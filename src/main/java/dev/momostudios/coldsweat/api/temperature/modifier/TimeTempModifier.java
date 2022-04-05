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

public class TimeTempModifier extends TempModifier
{
    Map<Biome, RegistryKey<Biome>> biomeKeys = new HashMap<>();

    @Override
    public Temperature getResult(Temperature temp, PlayerEntity player)
    {
        if (!player.world.getDimensionType().doesFixedTimeExist())
        {
            try
            {
                float timeTemp = 0;
                World world = player.world;
                for (BlockPos blockPos : WorldHelper.getNearbyPositions(player.getPosition(), 200, 6))
                {
                    BiomeManager biomeManager = player.world.getBiomeManager();
                    Biome biome = biomeManager.getBiome(blockPos);

                    RegistryKey<Biome> key = biomeKeys.get(biome);

                    if (key == null)
                        key = RegistryKey.getOrCreateKey(Registry.BIOME_KEY, world.func_241828_r().getRegistry(Registry.BIOME_KEY).getKey(world.getBiome(blockPos)));

                    if (BiomeDictionary.hasType(key, BiomeDictionary.Type.HOT) &&
                            BiomeDictionary.hasType(key, BiomeDictionary.Type.SANDY))
                    {
                        timeTemp += Math.sin(world.getDayTime() / 3819.7186342) - 0.5;
                    }
                    else
                    {
                        timeTemp += (Math.sin(world.getDayTime() / 3819.7186342) / 4d) - 0.125;
                    }
                }

                return temp.add(timeTemp / 200);
            }
            catch (Exception e)
            {
                return temp;
            }
        }
        else return temp;
    }

    public String getID()
    {
        return "cold_sweat:time";
    }
}