package dev.momostudios.coldsweat.util.entity;

import dev.momostudios.coldsweat.util.registries.ModItems;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.HandSide;

public class PlayerHelper
{
    public static ItemStack getItemInHand(LivingEntity player, HandSide hand)
    {
        return player.getHeldItem(hand == player.getPrimaryHand() ? Hand.MAIN_HAND : Hand.OFF_HAND);
    }

    public static HandSide getHandSide(Hand hand, PlayerEntity player)
    {
        return hand == Hand.MAIN_HAND ? player.getPrimaryHand() : player.getPrimaryHand() == HandSide.RIGHT ? HandSide.LEFT : HandSide.RIGHT;
    }

    public static boolean holdingLamp(LivingEntity player, HandSide hand)
    {
        return getItemInHand(player, hand).getItem() == ModItems.SOULSPRING_LAMP;
    }
}
