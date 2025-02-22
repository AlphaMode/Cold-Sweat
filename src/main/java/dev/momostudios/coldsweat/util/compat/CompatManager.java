package dev.momostudios.coldsweat.util.compat;

import de.teamlapen.werewolves.entities.player.werewolf.WerewolfPlayer;
import dev.momostudios.coldsweat.api.event.common.TemperatureDamageEvent;
import dev.momostudios.coldsweat.util.entity.ModDamageSources;
import dev.momostudios.coldsweat.util.math.CSMath;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

import java.util.Collection;

@Mod.EventBusSubscriber
public class CompatManager
{
    private static final boolean BOP_LOADED = ModList.get().isLoaded("biomesoplenty");
    private static final boolean SEASONS_LOADED = ModList.get().isLoaded("sereneseasons");
    private static final boolean CURIOS_LOADED = ModList.get().isLoaded("curios");
    private static final boolean WEREWOLVES_LOADED = ModList.get().isLoaded("werewolves");
    private static final boolean SPIRIT_LOADED = ModList.get().isLoaded("spirit");
    private static final boolean ARMOR_UNDERWEAR_LOADED = ModList.get().isLoaded("armorunder");
    private static final boolean BYG_LOADED = ModList.get().isLoaded("byg");
    private static final boolean CREATE_LOADED = ModList.get().isLoaded("create");
    private static final boolean ATMOSPHERIC_LOADED = ModList.get().isLoaded("atmospheric");
    private static final boolean ENVIRONMENTAL_LOADED = ModList.get().isLoaded("environmental");
    private static final boolean TERRALITH_LOADED = ModList.get().isLoaded("terralith");

    public static boolean isBiomesOPlentyLoaded()
    {
        return BOP_LOADED;
    }
    public static boolean isSereneSeasonsLoaded()
    {
        return SEASONS_LOADED;
    }
    public static boolean isCuriosLoaded()
    {
        return CURIOS_LOADED;
    }
    public static boolean isWerewolvesLoaded()
    {
        return WEREWOLVES_LOADED;
    }
    public static boolean isSpiritLoaded()
    {
        return SPIRIT_LOADED;
    }
    public static boolean isArmorUnderwearLoaded()
    {
        return ARMOR_UNDERWEAR_LOADED;
    }
    public static boolean isBiomesYoullGoLoaded()
    {
        return BYG_LOADED;
    }
    public static boolean isCreateLoaded()
    {
        return CREATE_LOADED;
    }
    public static boolean isAtmosphericLoaded()
    {
        return ATMOSPHERIC_LOADED;
    }
    public static boolean isEnvironmentalLoaded()
    {
        return ENVIRONMENTAL_LOADED;
    }
    public static boolean isTerralithLoaded()
    {
        return TERRALITH_LOADED;
    }

    public static boolean hasOzzyLiner(ItemStack stack)
    {
        return false;
    }
    public static boolean hasOttoLiner(ItemStack stack)
    {
        return false;
    }
    public static boolean hasOllieLiner(ItemStack stack)
    {
        return false;
    }

    public static boolean isWerewolf(Player player)
    {
        return WEREWOLVES_LOADED && WerewolfPlayer.getOpt(player).filter(w -> w.getLevel() > 0).map(w -> w.getForm().isTransformed()).orElse(false);
    }

    @SubscribeEvent
    public static void onLivingTempDamage(TemperatureDamageEvent event)
    {
        // Armor Underwear compat
        boolean isDamageCold;
        if (ARMOR_UNDERWEAR_LOADED
        && ((isDamageCold = event.getSource() == ModDamageSources.COLD) || event.getSource() == ModDamageSources.HOT))
        {
            int liners = (int) ((Collection<ItemStack>) event.getEntity().getArmorSlots()).stream()
                    .filter(stack -> isDamageCold ? hasOttoLiner(stack) : hasOllieLiner(stack))
                    .count();

            float newAmount = CSMath.blend(event.getAmount(), 0, liners, 0, 4);
            if (liners >= 4)
            {   event.negate();
                return;
            }
            event.setAmount(newAmount);
        }
    }
}
