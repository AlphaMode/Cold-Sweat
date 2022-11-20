package dev.momostudios.coldsweat.common.event;

import com.mojang.datafixers.util.Pair;
import dev.momostudios.coldsweat.api.event.common.BlockChangedEvent;
import dev.momostudios.coldsweat.common.te.HearthTileEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.*;

@Mod.EventBusSubscriber
public class HearthPathManagement
{
    public static LinkedHashMap<BlockPos, Integer> HEARTH_POSITIONS = new LinkedHashMap<>();

    public static final Set<Pair<BlockPos, String>> DISABLED_HEARTHS = new HashSet<>();

    @SubscribeEvent
    public static void onBlockUpdated(BlockChangedEvent event)
    {
        BlockPos pos = event.getPos();
        World world = event.getWorld();
        if (event.getPrevState().getShape(world, pos) != event.getNewState().getShape(world, pos))
        {
            for (Map.Entry<BlockPos, Integer> entry : HEARTH_POSITIONS.entrySet())
            {
                BlockPos hearthPos = entry.getKey();
                int range = entry.getValue();
                TileEntity te = event.getWorld().getTileEntity(hearthPos);
                if (pos.withinDistance(hearthPos, range) && te instanceof HearthTileEntity)
                {
                    ((HearthTileEntity) te).sendBlockUpdate(pos);
                }
            }
        }
    }

    /**
     * Save the player's disabled hearths on logout
     */
    @SubscribeEvent
    public static void saveDisabledHearths(PlayerEvent.PlayerLoggedOutEvent event)
    {
        event.getPlayer().getPersistentData().put("disabledHearths", serializeDisabledHearths());
        HEARTH_POSITIONS.clear();
    }

    public static CompoundNBT serializeDisabledHearths()
    {
        CompoundNBT disabledHearths = new CompoundNBT();

        int i = 0;
        for (Pair<BlockPos, String> pair : DISABLED_HEARTHS)
        {
            CompoundNBT hearthData = new CompoundNBT();
            hearthData.putLong("pos", pair.getFirst().toLong());
            hearthData.putString("world", pair.getSecond());
            disabledHearths.put(String.valueOf(i), hearthData);
            i++;
        }
        return disabledHearths;
    }

    /**
     * Load the player's disabled Hearths on login
     */
    @SubscribeEvent
    public static void loadDisabledHearths(PlayerEvent.PlayerLoggedInEvent event)
    {
        deserializeDisabledHearths(event.getPlayer().getPersistentData().getCompound("disabledHearths"));
    }

    public static void deserializeDisabledHearths(CompoundNBT disabledHearths)
    {
        DISABLED_HEARTHS.clear();
        for (String key : disabledHearths.keySet())
        {
            CompoundNBT hearthData = disabledHearths.getCompound(key);
            DISABLED_HEARTHS.add(Pair.of(BlockPos.fromLong(hearthData.getLong("pos")), hearthData.getString("world")));
        }
    }
}
