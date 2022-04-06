package dev.momostudios.coldsweat.client.gui.config;

import dev.momostudios.coldsweat.client.gui.config.pages.ConfigPageOne;
import dev.momostudios.coldsweat.config.ConfigCache;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;

public class ConfigButton extends Button
{
    ConfigCache configCache = ConfigCache.getInstance();

    public ConfigButton(int x, int y, int width, int height, ITextComponent title, Button.IPressable pressedAction)
    {
        super(x, y, width, height, title, pressedAction);
    }

    public boolean setsCustomDifficulty() {
        return true;
    }

    @Override
    public void onPress()
    {
        if (setsCustomDifficulty())
        {
            configCache.difficulty = 4;

            if (Minecraft.getInstance().currentScreen instanceof ConfigPageOne)
            {
                ((ConfigPageOne) Minecraft.getInstance().currentScreen).getElementBatch("difficulty").get(0).setMessage(
                        new StringTextComponent(new TranslationTextComponent("cold_sweat.config.difficulty.name").getString() +
                        " (" + ConfigScreen.difficultyName(configCache.difficulty) + ")..."));
            }
        }

        super.onPress();
    }
}
