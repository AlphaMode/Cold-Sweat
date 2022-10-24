package dev.momostudios.coldsweat.client.gui.config.pages;

import dev.momostudios.coldsweat.client.event.WorldTempGaugeDisplay;
import dev.momostudios.coldsweat.client.gui.config.AbstractConfigPage;
import dev.momostudios.coldsweat.client.gui.config.ConfigScreen;
import dev.momostudios.coldsweat.api.temperature.Temperature;
import dev.momostudios.coldsweat.config.ClientSettingsConfig;
import dev.momostudios.coldsweat.util.config.ConfigSettings;
import dev.momostudios.coldsweat.util.math.CSMath;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;

import java.util.function.Supplier;

public class ConfigPageOne extends AbstractConfigPage
{
    Screen parentScreen;
    ConfigSettings configSettings;
    private final String ON;
    private final String OFF;

    public ConfigPageOne(Screen parentScreen, ConfigSettings configSettings)
    {
        super(parentScreen, configSettings);
        if (parentScreen == null)
        {
            parentScreen = Minecraft.getInstance().currentScreen;
        }
        this.parentScreen = parentScreen;
        this.configSettings = configSettings;
        ON = new TranslationTextComponent("options.on").getString();
        OFF = new TranslationTextComponent("options.off").getString();
    }

    @Override
    public int index()
    {
        return 0;
    }

    @Override
    public ITextComponent sectionOneTitle()
    {
        return new TranslationTextComponent("cold_sweat.config.section.temperature_details");
    }

    @Override
    public ITextComponent sectionTwoTitle()
    {
        return new TranslationTextComponent("cold_sweat.config.section.difficulty.name");
    }

    @Override
    protected void init()
    {
        super.init();

        ClientSettingsConfig clientConfig = ClientSettingsConfig.getInstance();

        Supplier<Temperature.Units> properUnits = () -> clientConfig.celsius() ? Temperature.Units.C : Temperature.Units.F;

        // The options

        // Celsius
        this.addButton("units", Side.LEFT, () -> new TranslationTextComponent("cold_sweat.config.units.name").getString() + ": " +
                (clientConfig.celsius() ? new TranslationTextComponent("cold_sweat.config.celsius.name").getString() :
                new TranslationTextComponent("cold_sweat.config.fahrenheit.name").getString()), button ->
        {
            clientConfig.setCelsius(!clientConfig.celsius());
            // Update the world temp. gauge when the button is pressed
            WorldTempGaugeDisplay.WORLD_TEMP = CSMath.convertUnits(WorldTempGaugeDisplay.PLAYER_CAP.getTemp(Temperature.Type.WORLD), Temperature.Units.MC, properUnits.get(), true);

            button.setMessage(new StringTextComponent(new TranslationTextComponent("cold_sweat.config.units.name").getString() + ": " +
                    (clientConfig.celsius() ? new TranslationTextComponent("cold_sweat.config.celsius.name").getString() :
                            new TranslationTextComponent("cold_sweat.config.fahrenheit.name").getString())));

            ((TextFieldWidget) this.elementBatches.get("max_temp").get(0)).setText(String.valueOf(ConfigScreen.TWO_PLACES.format(
                    CSMath.convertUnits(configSettings.maxTemp, Temperature.Units.MC, properUnits.get(), true))));

            ((TextFieldWidget) this.elementBatches.get("min_temp").get(0)).setText(String.valueOf(ConfigScreen.TWO_PLACES.format(
                    CSMath.convertUnits(configSettings.minTemp, Temperature.Units.MC, properUnits.get(), true))));
        }, false, false, new TranslationTextComponent("cold_sweat.config.units.desc").getString());


        // Temp Offset
        this.addDecimalInput("temp_offset", Side.LEFT, new TranslationTextComponent("cold_sweat.config.temp_offset.name"),
                value -> clientConfig.setTempOffset(value.intValue()),
                input -> input.setText(String.valueOf(clientConfig.tempOffset())),
                false, false, new TranslationTextComponent("cold_sweat.config.temp_offset.desc_1").getString(),
                              "§7"+new TranslationTextComponent("cold_sweat.config.temp_offset.desc_2").getString()+"§r");

        // Max Temperature
        this.addDecimalInput("max_temp", Side.LEFT, new TranslationTextComponent("cold_sweat.config.max_temperature.name"),
                value -> configSettings.maxTemp = CSMath.convertUnits(value, properUnits.get(), Temperature.Units.MC, true),
                input -> input.setText(String.valueOf(CSMath.convertUnits(configSettings.maxTemp, Temperature.Units.MC, properUnits.get(), true))),
                false, false, new TranslationTextComponent("cold_sweat.config.max_temperature.desc").getString());

        // Min Temperature
        this.addDecimalInput("min_temp", Side.LEFT, new TranslationTextComponent("cold_sweat.config.min_temperature.name"),
                value -> configSettings.minTemp = CSMath.convertUnits(value, properUnits.get(), Temperature.Units.MC, true),
                input -> input.setText(String.valueOf(CSMath.convertUnits(configSettings.minTemp, Temperature.Units.MC, properUnits.get(), true))),
                false, false, new TranslationTextComponent("cold_sweat.config.min_temperature.desc").getString());

        // Rate Multiplier
        this.addDecimalInput("rate", Side.LEFT, new TranslationTextComponent("cold_sweat.config.temperature_rate.name"),
                value -> configSettings.rate = value,
                input -> input.setText(String.valueOf(configSettings.rate)),
                false, false, new TranslationTextComponent("cold_sweat.config.temperature_rate.desc").getString());

        // Difficulty button
        this.addButton("difficulty", Side.RIGHT, () -> new TranslationTextComponent("cold_sweat.config.difficulty.name").getString()
                + " (" + ConfigScreen.difficultyName(configSettings.difficulty) + ")...",
                button -> mc.displayGuiScreen(new ConfigPageDifficulty(this, configSettings)),
                true, false, new TranslationTextComponent("cold_sweat.config.difficulty.desc").getString());

        this.addEmptySpace(Side.RIGHT, 1);


        // Misc. Temp Effects
        this.addButton("ice_resistance", Side.RIGHT,
                () -> new TranslationTextComponent("cold_sweat.config.ice_resistance.name").getString() + ": " + (configSettings.iceRes ? ON : OFF),
                button -> configSettings.iceRes = !configSettings.iceRes,
                true, true, new TranslationTextComponent("cold_sweat.config.ice_resistance.desc").getString());

        this.addButton("fire_resistance", Side.RIGHT,
                () -> new TranslationTextComponent("cold_sweat.config.fire_resistance.name").getString() + ": " + (configSettings.fireRes ? ON : OFF),
                button -> configSettings.fireRes = !configSettings.fireRes,
                true, true, new TranslationTextComponent("cold_sweat.config.fire_resistance.desc").getString());

        this.addButton("show_ambient", Side.RIGHT,
                () -> new TranslationTextComponent("cold_sweat.config.require_thermometer.name").getString() + ": " + (configSettings.requireThermometer ? ON : OFF),
                button -> configSettings.requireThermometer = !configSettings.requireThermometer,
                true, true, new TranslationTextComponent("cold_sweat.config.require_thermometer.desc").getString());

        this.addButton("damage_scaling", Side.RIGHT,
                () -> new TranslationTextComponent("cold_sweat.config.damage_scaling.name").getString() + ": " + (configSettings.damageScaling ? ON : OFF),
                button -> configSettings.damageScaling = !configSettings.damageScaling,
                true, true, new TranslationTextComponent("cold_sweat.config.damage_scaling.desc").getString());
    }

    @Override
    public void onClose()
    {
        ConfigScreen.saveConfig(configSettings);
        super.onClose();
    }
}
