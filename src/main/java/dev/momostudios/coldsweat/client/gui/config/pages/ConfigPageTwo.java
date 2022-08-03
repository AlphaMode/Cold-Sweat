package dev.momostudios.coldsweat.client.gui.config.pages;

import dev.momostudios.coldsweat.client.gui.config.ConfigPageBase;
import dev.momostudios.coldsweat.config.ClientSettingsConfig;
import dev.momostudios.coldsweat.util.config.ConfigCache;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;

import javax.annotation.Nullable;

public class ConfigPageTwo extends ConfigPageBase
{
    private final ConfigCache configCache;
    private final String ON;
    private final String OFF;

    public ConfigPageTwo(Screen parentScreen, ConfigCache configCache)
    {
        super(parentScreen, configCache);
        this.configCache = configCache;
        ON  = new TranslationTextComponent("options.on").getString();
        OFF = new TranslationTextComponent("options.off").getString();
    }

    @Override
    public int index()
    {
        return 1;
    }

    @Override
    public ITextComponent sectionOneTitle()
    {
        return new TranslationTextComponent("cold_sweat.config.section.other");
    }

    @Nullable
    @Override
    public ITextComponent sectionTwoTitle()
    {
        return new TranslationTextComponent("cold_sweat.config.section.hud_settings");
    }

    @Override
    protected void init()
    {
        super.init();

        ClientSettingsConfig clientConfig = ClientSettingsConfig.getInstance();

        // Enable Grace Period
        this.addButton("grace_toggle", Side.LEFT, () -> new TranslationTextComponent("cold_sweat.config.grace_period.name").getString() + ": " + (configCache.graceEnabled ? ON : OFF),
                button -> configCache.graceEnabled = !configCache.graceEnabled,
                true, true, new TranslationTextComponent("cold_sweat.config.grace_period.desc").getString());

        // Grace Period Length
        this.addDecimalInput("grace_length", Side.LEFT, new TranslationTextComponent("cold_sweat.config.grace_period_length.name"),
                value -> configCache.graceLength = value.intValue(),
                input -> input.setText(configCache.graceLength + ""),
                true, true, new TranslationTextComponent("cold_sweat.config.grace_period_length.desc_1").getString(),
                            new TranslationTextComponent("cold_sweat.config.grace_period_length.desc_2", "§7", "§r").getString());

        // Direction Buttons: Steve Head
        this.addDirectionPanel("icon_directions", Side.RIGHT, new TranslationTextComponent("cold_sweat.config.temp_icon_location.name"),
                amount -> clientConfig.setTempIconX(clientConfig.tempIconX() + amount),
                amount -> clientConfig.setTempIconY(clientConfig.tempIconY() + amount),
                () -> { clientConfig.setTempIconX(0); clientConfig.setTempIconY(0); },
                false, false, new TranslationTextComponent("cold_sweat.config.temp_icon_location.desc").getString());

        // Direction Buttons: Temp Readout
        this.addDirectionPanel("readout_directions", Side.RIGHT, new TranslationTextComponent("cold_sweat.config.temp_readout_location.name"),
                amount -> clientConfig.setTempReadoutX(clientConfig.tempReadoutX() + amount * (Screen.hasShiftDown() ? 10 : 1)),
                amount -> clientConfig.setTempReadoutY(clientConfig.tempReadoutY() + amount * (Screen.hasShiftDown() ? 10 : 1)),
                () -> { clientConfig.setTempReadoutX(0); clientConfig.setTempReadoutY(0); },
                false, false, new TranslationTextComponent("cold_sweat.config.temp_readout_location.desc").getString());

        this.addDirectionPanel("gauge_directions", Side.RIGHT, new TranslationTextComponent("cold_sweat.config.world_temp_location.name"),
                amount -> clientConfig.setTempGaugeX(clientConfig.tempGaugeX() + amount * (Screen.hasShiftDown() ? 10 : 1)),
                amount -> clientConfig.setTempGaugeY(clientConfig.tempGaugeY() + amount * (Screen.hasShiftDown() ? 10 : 1)),
                () -> { clientConfig.setTempGaugeX(0); clientConfig.setTempGaugeY(0); },
                false, false, new TranslationTextComponent("cold_sweat.config.world_temp_location.desc").getString());

        // Custom Hotbar
        this.addButton("custom_hotbar", Side.RIGHT, () -> new TranslationTextComponent("cold_sweat.config.custom_hotbar.name").getString() + ": " + (clientConfig.customHotbar() ? ON : OFF),
                button -> clientConfig.setCustomHotbar(!clientConfig.customHotbar()),
                false, false, new TranslationTextComponent("cold_sweat.config.custom_hotbar.desc").getString());

        // Icon Bobbing
        this.addButton("icon_bobbing", Side.RIGHT, () -> new TranslationTextComponent("cold_sweat.config.icon_bobbing.name").getString() + ": " + (clientConfig.iconBobbing() ? ON : OFF),
                button -> clientConfig.setIconBobbing(!clientConfig.iconBobbing()),
                false, false, new TranslationTextComponent("cold_sweat.config.icon_bobbing.desc").getString());

        this.addLabel("shift_label", Side.RIGHT, new TranslationTextComponent("cold_sweat.config.offset_shift.name").getString(), 11908533);
    }

    @Override
    public void onClose()
    {
        super.onClose();
        this.saveConfig(configCache);
    }
}
