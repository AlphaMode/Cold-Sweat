package dev.momostudios.coldsweat.util.entity;

import net.minecraft.util.DamageSource;

public class ModDamageSources
{
    public static final DamageSource COLD = (new DamageSource("cold"))
        .setDamageBypassesArmor()
        .setDamageIsAbsolute();

    public static final DamageSource HOT  = (new DamageSource("hot"))
        .setDamageBypassesArmor()
        .setFireDamage()
        .setDamageIsAbsolute();
}
