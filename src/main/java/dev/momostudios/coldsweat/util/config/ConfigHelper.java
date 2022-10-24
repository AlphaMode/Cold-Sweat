package dev.momostudios.coldsweat.util.config;

import com.mojang.datafixers.util.Pair;
import dev.momostudios.coldsweat.ColdSweat;
import dev.momostudios.coldsweat.api.temperature.Temperature;
import dev.momostudios.coldsweat.util.math.CSMath;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ITag;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;

public class ConfigHelper
{
    public static CompoundNBT writeConfigSettingsToNBT(ConfigSettings config)
    {
        CompoundNBT nbt = new CompoundNBT();
        nbt.putInt("difficulty", config.difficulty);
        nbt.putDouble("minTemp", config.minTemp);
        nbt.putDouble("maxTemp", config.maxTemp);
        nbt.putDouble("rate", config.rate);
        nbt.putBoolean("fireResistance", config.fireRes);
        nbt.putBoolean("iceResistance", config.iceRes);
        nbt.putBoolean("damageScaling", config.damageScaling);
        nbt.putBoolean("requireThermometer", config.requireThermometer);
        nbt.putInt("graceLength", config.graceLength);
        nbt.putBoolean("graceEnabled", config.graceEnabled);
        return nbt;
    }

    public static ConfigSettings readConfigSettingsFromNBT(CompoundNBT nbt)
    {
        ConfigSettings config = new ConfigSettings();
        if (nbt == null)
        {
            ColdSweat.LOGGER.error("Failed to read config settings!");
            return config;
        }

        config.difficulty = nbt.getInt("difficulty");
        config.minTemp = nbt.getDouble("minTemp");
        config.maxTemp = nbt.getDouble("maxTemp");
        config.rate = nbt.getDouble("rate");
        config.fireRes = nbt.getBoolean("fireResistance");
        config.iceRes = nbt.getBoolean("iceResistance");
        config.damageScaling = nbt.getBoolean("damageScaling");
        config.requireThermometer = nbt.getBoolean("requireThermometer");
        config.graceLength = nbt.getInt("graceLength");
        config.graceEnabled = nbt.getBoolean("graceEnabled");
        return config;
    }

    public static List<Block> getBlocks(String... ids)
    {
        List<Block> blocks = new ArrayList<>();
        for (String id : ids)
        {
            if (id.startsWith("#"))
            {
                final String tagID = id.replace("#", "");
                ITag<Block> blockTag = BlockTags.getCollection().get(new ResourceLocation(tagID));
                if (blockTag != null)
                {
                    blocks.addAll(blockTag.getAllElements());
                }
            }
            blocks.add(ForgeRegistries.BLOCKS.getValue(new ResourceLocation(id)));
        }
        return blocks;
    }

    public static Map<Block, Number> getBlocksWithValues(List<? extends List<?>> source)
    {
        Map<Block, Number> map = new HashMap<>();
        for (List<?> entry : source)
        {
            String blockID = (String) entry.get(0);

            if (blockID.startsWith("#"))
            {
                final String tagID = blockID.replace("#", "");
                ITag<Block> tag = BlockTags.getCollection().get(new ResourceLocation(tagID));
                if (tag != null)
                {
                    for (Block block : tag.getAllElements())
                    {
                        map.put(block, (Number) entry.get(1));
                    }
                }
            }
            else
            {
                Block newItem = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(blockID));

                if (newItem != null) map.put(newItem, (Number) entry.get(1));
            }
        }
        return map;
    }

    public static List<Item> getItems(String... ids)
    {
        List<Item> items = new ArrayList<>();
        for (String id : ids)
        {
            if (id.startsWith("#"))
            {
                final String tagID = id.replace("#", "");
                ITag<Item> itemTag = ItemTags.getCollection().get(new ResourceLocation(tagID));
                if (itemTag != null)
                {
                    items.addAll(itemTag.getAllElements());
                }
            }
            items.add(ForgeRegistries.ITEMS.getValue(new ResourceLocation(id)));
        }
        return items;
    }

    public static Map<Item, Double> getItemsWithValues(List<? extends List<?>> source)
    {
        Map<Item, Double> map = new HashMap<>();
        for (List<?> entry : source)
        {
            String itemID = (String) entry.get(0);

            if (itemID.startsWith("#"))
            {
                final String tagID = itemID.replace("#", "");
                ITag<Item> tag = ItemTags.getCollection().get(new ResourceLocation(tagID));
                if (tag != null)
                {
                    for (Item item : tag.getAllElements())
                    {
                        map.put(item, ((Number) entry.get(1)).doubleValue());
                    }
                }
            }
            else
            {
                Item newItem = ForgeRegistries.ITEMS.getValue(new ResourceLocation(itemID));

                if (newItem != null) map.put(newItem, ((Number) entry.get(1)).doubleValue());
            }
        }
        return map;
    }

    public static Map<ResourceLocation, Pair<Double, Double>> getBiomeTemps(List<? extends List<?>> source, boolean absolute)
    {
        Map<ResourceLocation, Pair<Double, Double>> map = new HashMap<>();
        for (List<?> entry : source)
        {
            String biomeID = (String) entry.get(0);

            double min = ((Number) entry.get(1)).doubleValue();
            double max = ((Number) entry.get(2)).doubleValue();

            // Convert the temperature to MC units if needed
            if (entry.size() == 4)
            {
                try
                {
                    Temperature.Units units = Temperature.Units.valueOf(((String) entry.get(3)).toUpperCase());
                    min = CSMath.convertUnits(min, units, Temperature.Units.MC, absolute);
                    max = CSMath.convertUnits(max, units, Temperature.Units.MC, absolute);
                } catch (Exception ignored) {}
            }

            if (ForgeRegistries.BIOMES.getValue(new ResourceLocation(biomeID)) != null)
            {
                // Maps the biome ID to the temperature (and variance if present)
                map.put(new ResourceLocation(biomeID), Pair.of(min, max));
            }
        }
        return map;
    }
}
