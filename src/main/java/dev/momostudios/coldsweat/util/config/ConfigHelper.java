package dev.momostudios.coldsweat.util.config;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ITag;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.*;

public class ConfigHelper
{
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

    public static Map<Item, Number> getItemsWithValues(List<? extends List<?>> source)
    {
        Map<Item, Number> map = new HashMap<>();
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
                        map.put(item, (Number) entry.get(1));
                    }
                }
            }
            else
            {
                Item newItem = ForgeRegistries.ITEMS.getValue(new ResourceLocation(itemID));

                if (newItem != null) map.put(newItem, (Number) entry.get(1));
            }
        }
        return map;
    }

    @Nullable
    public static Item[] getBiomes(String id)
    {
        if (id.startsWith("#"))
        {
            ITag<Item> tag = ItemTags.getCollection().get(new ResourceLocation(id.replace("#", "")));
            return tag != null ? tag.getAllElements().toArray(new Item[0]) : null;
        }
        return new Item[] { ForgeRegistries.ITEMS.getValue(new ResourceLocation(id)) };
    }

    public static Map<ResourceLocation, Number> getBiomesWithValues(List<? extends List<?>> source)
    {
        Map<ResourceLocation, Number> map = new HashMap<>();
        for (List<?> entry : source)
        {
            String biomeID = (String) entry.get(0);

            if (biomeID.startsWith("#"))
            {
                final String typeID = biomeID.replace("#", "");
                Optional<BiomeDictionary.Type> biomeType = BiomeDictionary.Type.getAll().stream().filter(type -> type.getName().equals(typeID)).findFirst();
                if (biomeType.isPresent())
                {
                    for (Biome biome : ForgeRegistries.BIOMES.getValues())
                    {
                        if (BiomeDictionary.getBiomes(biomeType.get()).contains(RegistryKey.getOrCreateKey(Registry.BIOME_KEY, biome.getRegistryName())))
                        {
                            map.put(biome.getRegistryName(), (Number) entry.get(1));
                        }
                    }
                }
            }
            else if (ForgeRegistries.BIOMES.getValue(new ResourceLocation(biomeID)) != null)
            {
                map.put(new ResourceLocation(biomeID), (Number) entry.get(1));
            }
        }
        return map;
    }
}
