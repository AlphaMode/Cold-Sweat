package dev.momostudios.coldsweat.config;

import dev.momostudios.coldsweat.api.util.Temperature;
import dev.momostudios.coldsweat.util.math.CSMath;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

public class ColdSweatConfig
{
    private static final ForgeConfigSpec SPEC;
    private static final ColdSweatConfig CONFIG_REFERENCE = new ColdSweatConfig();
    public  static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.IntValue difficulty;

    private static final ForgeConfigSpec.DoubleValue maxHabitable;
    private static final ForgeConfigSpec.DoubleValue minHabitable;
    private static final ForgeConfigSpec.DoubleValue rateMultiplier;

    private static final ForgeConfigSpec.BooleanValue fireResistanceEffect;
    private static final ForgeConfigSpec.BooleanValue iceResistanceEffect;

    private static final ForgeConfigSpec.BooleanValue damageScaling;
    private static final ForgeConfigSpec.BooleanValue requireThermometer;
    private static final ForgeConfigSpec.BooleanValue checkSleep;

    private static final ForgeConfigSpec.IntValue gracePeriodLength;
    private static final ForgeConfigSpec.BooleanValue gracePeriodEnabled;

    private static final ForgeConfigSpec.DoubleValue hearthEffect;

    private static final ForgeConfigSpec.BooleanValue coldSoulFire;

    private static final ForgeConfigSpec.BooleanValue showConfigButton;

    private static final ForgeConfigSpec.ConfigValue<List<? extends List<Object>>> blockTemps;

    private static final ForgeConfigSpec.BooleanValue cameraSway;
    private static final ForgeConfigSpec.BooleanValue heatstrokeFog;
    private static final ForgeConfigSpec.BooleanValue freezingHearts;
    private static final ForgeConfigSpec.BooleanValue coldKnockback;
    private static final ForgeConfigSpec.BooleanValue coldMining;
    private static final ForgeConfigSpec.BooleanValue coldMovement;

    static 
    {
        showConfigButton = BUILDER
                .comment("Show the config menu button in the Options menu")
                .define("Enable In-Game Config", true);

        /*
         Difficulty
         */
        difficulty = BUILDER
                .comment("Overrides all other config options for easy difficulty management",
                        "This value is changed by the in-game config. It does nothing otherwise.")
                .defineInRange("Difficulty", 3, 1, 5);

        /*
         Potion effects affecting the player's temperature
         */
        BUILDER.push("Item settings");
        fireResistanceEffect = BUILDER
                .comment("Fire Resistance blocks all hot temperatures")
                .define("Fire Resistance Immunity", true);
        iceResistanceEffect = BUILDER
                .comment("Ice Resistance blocks all cold temperatures")
                .define("Ice Resistance Immunity", true);
        requireThermometer = BUILDER
            .comment("Thermometer item is required to see world temperature")
            .define("Require Thermometer", true);
        BUILDER.pop();

        /*
         Misc. things that are affected by temperature
         */
        BUILDER.push("Misc temperature-related things");
        damageScaling = BUILDER
            .comment("Sets whether damage scales with difficulty")
            .define("Damage Scaling", true);
        checkSleep = BUILDER
            .comment("When set to true, players cannot sleep if they are cold or hot enough to die")
            .define("Prevent Sleep When in Danger", true);
        BUILDER.pop();

        /*
         Details about how the player is affected by temperature
         */
        BUILDER.push("Details about how the player is affected by temperature");
        minHabitable = BUILDER
                .comment("Minimum habitable temperature (default: " + CSMath.convertTemp(50, Temperature.Units.F, Temperature.Units.MC, true) + ")")
                .defineInRange("Minimum Habitable Temperature", CSMath.convertTemp(50, Temperature.Units.F, Temperature.Units.MC, true), Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
        maxHabitable = BUILDER
                .comment("Maximum habitable temperature (default: " + CSMath.convertTemp(100, Temperature.Units.F, Temperature.Units.MC, true) + ")")
                .defineInRange("Maximum Habitable Temperature", CSMath.convertTemp(100, Temperature.Units.F, Temperature.Units.MC, true), Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
        rateMultiplier = BUILDER
                .comment("Rate at which the player's body temperature changes (default: 1.0 (100%))")
                .defineInRange("Rate Multiplier", 1.0, 0, Double.POSITIVE_INFINITY);
        BUILDER.pop();


        BUILDER.push("Temperature Effects");
            BUILDER.push("Hot");
            cameraSway = BUILDER
                .comment("When set to true, the camera will sway randomly when the player is too hot")
                .define("Camera Sway", true);
            heatstrokeFog = BUILDER
                .comment("When set to true, the player's view distance will decrease when they are too hot")
                .define("Heatstroke Fog", true);
            BUILDER.pop();

            BUILDER.push("Cold");
            freezingHearts = BUILDER
                .comment("When set to true, some of the player's hearts will freeze when they are too cold, preventing regeneration")
                .define("Freezing Hearts", true);
            coldKnockback = BUILDER
                .comment("When set to true, the player's attack knockback will be reduced when they are too cold")
                .define("Cold Knockback Reduction", true);
            coldMovement = BUILDER
                .comment("When set to true, the player's movement speed will be reduced when they are too cold")
                .define("Cold Slowness", true);
            coldMining = BUILDER
                .comment("When set to true, the player's mining speed will be reduced when they are too cold")
                .define("Cold Mining Fatigue", true);
            BUILDER.pop();
        BUILDER.pop();


        BUILDER.push("Grace Period Details");
                gracePeriodLength = BUILDER
                .comment("Grace period length in ticks (default: 6000)")
                .defineInRange("Grace Period Length", 6000, 0, Integer.MAX_VALUE);
                gracePeriodEnabled = BUILDER
                .comment("Enables the grace period (default: true)")
                .define("Grace Period Enabled", true);
        BUILDER.pop();

        BUILDER.push("Block Temperatures");
        blockTemps = BUILDER
                .comment("Allows for adding simple BlockTemps without the use of Java mods",
                         "Format (All temperatures are in Minecraft units):",
                         "[[\"block-ids\", <temperature>, <range (max 7)>, <*true/false: falloff>, <*max effect>, *<predicates>], [etc...], [etc...]]",
                         "(* = optional) (1 °MC = 42 °F/ 23.33 °C)",
                         "",
                         "Arguments:",
                         "block-ids: multiple IDs can be used by separating them with commas (i.e: \"minecraft:torch,minecraft:wall_torch\")",
                         "temperature: the temperature of the block, in Minecraft units",
                         "falloff: the block is less effective as distance increases",
                         "max effect: the maximum temperature change this block can cause to a player (even with multiple blocks)",
                         "predicates: the state that the block has to be in for the temperature to be applied (lit=true for a campfire, for example).",
                         "Multiple predicates can be used by separating them with commas (i.e: \"lit=true,waterlogged=false\")")
                .defineList("BlockTemps", Arrays.asList
                                (
                                        Arrays.asList("minecraft:soul_fire",     -0.2, 7, true, 0.8),
                                        Arrays.asList("minecraft:fire",           0.2, 7, true, 0.8),
                                        Arrays.asList("minecraft:magma_block",   0.15, 3, true, 0.6),
                                        Arrays.asList("minecraft:soul_campfire", -0.2, 3, true, 0.6, "lit=true"),
                                        Arrays.asList("minecraft:ice",           -0.1, 4, true, 0.5),
                                        Arrays.asList("minecraft:packed_ice",    -0.2, 4, true, 1.0),
                                        Arrays.asList("minecraft:blue_ice",      -0.3, 4, true, 1.0)
                                ),
                        it -> it instanceof List<?> list && list.size() >= 3 && list.get(0) instanceof String && list.get(1) instanceof Number && list.get(2) instanceof Number);
        BUILDER.pop();

        BUILDER.push("Hearth");
            hearthEffect = BUILDER
                    .comment("How strong the hearth is (default: 0.5)")
                    .defineInRange("Hearth Strength", 0.5, 0, 1.0);
        BUILDER.pop();

        BUILDER.push("Cold Soul Fire");
        coldSoulFire = BUILDER
                .comment("Converts damage dealt by Soul Fire to cold damage (default: true)",
                         "Does not affect the block's temperature")
                .define("Cold Soul Fire", true);
        BUILDER.pop();

        SPEC = BUILDER.build();
    }

    public static void setup()
    {
        Path configPath = FMLPaths.CONFIGDIR.get();
        Path csConfigPath = Paths.get(configPath.toAbsolutePath().toString(), "coldsweat");

        // Create the config folder
        try
        {
            Files.createDirectory(csConfigPath);
        }
        catch (Exception e)
        {
            // Do nothing
        }

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, SPEC, "coldsweat/main.toml");
    }

    public static ColdSweatConfig getInstance()
    {
        return CONFIG_REFERENCE;
    }

    /*
     * Non-private values for use elsewhere
     */
    public boolean isButtonShowing()
    {
        return showConfigButton.get();
    }

    public int getDifficulty() {
        return difficulty.get();
    }

    public boolean isFireResistanceEnabled() {
        return fireResistanceEffect.get();
    }

    public boolean isIceResistanceEnabled() {
        return iceResistanceEffect.get();
    }

    public boolean thermometerRequired() {
        return requireThermometer.get();
    }

    public boolean doDamageScaling() {
        return damageScaling.get();
    }

    public double getMinTempHabitable() {
        return minHabitable.get();
    }

    public double getMaxTempHabitable() {
        return maxHabitable.get();
    }

    public double getRateMultiplier() {
        return rateMultiplier.get();
    }

    public int getGracePeriodLength()
    {
        return gracePeriodLength.get();
    }

    public boolean isGracePeriodEnabled()
    {
        return gracePeriodEnabled.get();
    }

    public boolean isSoulFireCold()
    {
        return coldSoulFire.get();
    }

    public List<? extends List<Object>> getBlockTemps()
    {
        return blockTemps.get();
    }

    public double getHearthEffect()
    {
        return hearthEffect.get();
    }

    public boolean isSleepChecked()
    {
        return checkSleep.get();
    }

    public boolean isCameraSwayEnabled()
    {
        return cameraSway.get();
    }

    public boolean heatstrokeFog()
    {
        return heatstrokeFog.get();
    }

    public boolean freezingHearts()
    {
        return freezingHearts.get();
    }

    public boolean coldKnockback()
    {
        return coldKnockback.get();
    }

    public boolean coldMining()
    {
        return coldMining.get();
    }

    public boolean coldMovement()
    {
        return coldMovement.get();
    }

    /*
     * Safe set methods for config values
     */
    public void setDifficulty(int value) {
        difficulty.set(value);
    }

    public void setMaxHabitable(double temp) {
        maxHabitable.set(temp);
    }

    public void setMinHabitable(double temp) {
        minHabitable.set(temp);
    }

    public void setRateMultiplier(double rate) {
        rateMultiplier.set(rate);
    }

    public void setFireResistanceEnabled(boolean isEffective) {
        fireResistanceEffect.set(isEffective);
    }

    public void setIceResistanceEnabled(boolean isEffective) {
        iceResistanceEffect.set(isEffective);
    }

    public void setRequireThermometer(boolean required) {
        requireThermometer.set(required);
    }

    public void setDamageScaling(boolean enabled) {
        damageScaling.set(enabled);
    }

    public void setGracePeriodLength(int ticks)
    {
        gracePeriodLength.set(ticks);
    }

    public void setGracePeriodEnabled(boolean enabled)
    {
        gracePeriodEnabled.set(enabled);
    }

    public void setCameraSway(boolean sway)
    {
        cameraSway.set(sway);
    }

    public void setHeatstrokeFog(boolean fog)
    {
        heatstrokeFog.set(fog);
    }

    public void setFreezingHearts(boolean hearts)
    {
        freezingHearts.set(hearts);
    }

    public void setColdKnockback(boolean knockback)
    {
        coldKnockback.set(knockback);
    }

    public void setColdMining(boolean mining)
    {
        coldMining.set(mining);
    }

    public void setColdMovement(boolean movement)
    {
        coldMovement.set(movement);
    }

    public void save() {
        SPEC.save();
    }
}
