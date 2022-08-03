package dev.momostudios.coldsweat.client.gui.config.pages;

import com.mojang.blaze3d.matrix.MatrixStack;
import dev.momostudios.coldsweat.client.gui.config.ConfigScreen;
import dev.momostudios.coldsweat.client.gui.config.DifficultyDescriptions;
import dev.momostudios.coldsweat.api.temperature.Temperature;
import dev.momostudios.coldsweat.config.ColdSweatConfig;
import dev.momostudios.coldsweat.util.config.ConfigCache;
import dev.momostudios.coldsweat.core.network.ColdSweatPacketHandler;
import dev.momostudios.coldsweat.core.network.message.ClientConfigSendMessage;
import dev.momostudios.coldsweat.util.math.CSMath;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SimpleSound;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.client.gui.GuiUtils;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(Dist.CLIENT)
public class ConfigPageDifficulty extends Screen
{
    public static boolean IS_MOUSE_DOWN = false;
    
    @SubscribeEvent
    public static void onClicked(GuiScreenEvent.MouseClickedEvent event)
    {
        if (event.getButton() == 0 && Minecraft.getInstance().currentScreen instanceof ConfigPageDifficulty)
            IS_MOUSE_DOWN = true;
    }

    @SubscribeEvent
    public static void onReleased(GuiScreenEvent.MouseReleasedEvent event)
    {
        if (event.getButton() == 0 && Minecraft.getInstance().currentScreen instanceof ConfigPageDifficulty)
            IS_MOUSE_DOWN = false;
    }
    
    private final Screen parentScreen;
    private final ConfigCache configCache;

    private static final int TITLE_HEIGHT = ConfigScreen.TITLE_HEIGHT;
    private static final int BOTTOM_BUTTON_HEIGHT_OFFSET = ConfigScreen.BOTTOM_BUTTON_HEIGHT_OFFSET;
    private static final int BOTTOM_BUTTON_WIDTH = ConfigScreen.BOTTOM_BUTTON_WIDTH;

    ResourceLocation configButtons = new ResourceLocation("cold_sweat:textures/gui/screen/configs/config_buttons.png");

    public ConfigPageDifficulty(Screen parentScreen, ConfigCache configCache)
    {
        super(new TranslationTextComponent("cold_sweat.config.section.difficulty.name"));
        this.parentScreen = parentScreen;
        this.configCache = configCache;
    }

    public int index()
    {
        return -1;
    }


    @Override
    protected void init()
    {
        this.addButton(new Button(
                this.width / 2 - BOTTOM_BUTTON_WIDTH / 2,
                this.height - BOTTOM_BUTTON_HEIGHT_OFFSET,
                BOTTOM_BUTTON_WIDTH, 20,
                new TranslationTextComponent("gui.done"),
                button -> this.close()));
    }

    @Override
    public void render(@Nonnull MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks)
    {
        // Render Background
        if (this.minecraft.world != null) {
            this.fillGradient(matrixStack, 0, 0, this.width, this.height, -1072689136, -804253680);
        }
        else {
            this.renderDirtBackground(0);
        }

        // Get a list of TextComponents to render
        List<TextComponent> descLines = new ArrayList<>();
        descLines.add(new StringTextComponent(""));

        // Get max text length (used to extend the text box if it's too wide)
        int longestLine = 0;
        for (String text : DifficultyDescriptions.getListFor(configCache.difficulty))
        {
            String ttLine = "  " + text + "  ";
            // Add the text and a new line to the list
            descLines.add(new StringTextComponent(ttLine));
            descLines.add(new StringTextComponent(""));

            int lineWidth = font.getStringWidth(ttLine);
            if (lineWidth > longestLine)
                longestLine = lineWidth;
        }

        // Draw Text Box
        int middleX = this.width / 2;
        int middleY = this.height / 2;
        GuiUtils.drawHoveringText(matrixStack, descLines, middleX - longestLine / 2 - 10, middleY - 16, this.width, this.height, longestLine, this.font);

        // Set the mouse's position for ConfigScreen (used for click events)
        ConfigScreen.MOUSE_X = mouseX;
        ConfigScreen.MOUSE_Y = mouseY;

        // Draw Title
        drawCenteredString(matrixStack, this.font, this.title.getString(), this.width / 2, TITLE_HEIGHT, 0xFFFFFF);


        this.minecraft.textureManager.bindTexture(configButtons);

        // Draw Slider
        this.blit(matrixStack, this.width / 2 - 76, this.height / 2 - 53, 12,
                isMouseOverSlider(mouseX, mouseY) ? 174 : 168, 152, 6);

        // Draw Slider Head
        this.blit(matrixStack, this.width / 2 - 78 + (configCache.difficulty * 37), this.height / 2 - 58,
                isMouseOverSlider(mouseX, mouseY) ? 0 : 6, 168, 6, 16);

        // Draw Difficulty Title
        String difficultyName = ConfigScreen.difficultyName(configCache.difficulty);
        this.font.drawStringWithShadow(matrixStack, difficultyName, this.width / 2.0f - (font.getStringWidth(difficultyName) / 2f),
                this.height / 2.0f - 84, ConfigScreen.difficultyColor(configCache.difficulty));

        // Render Button(s)
        super.render(matrixStack, mouseX, mouseY, partialTicks);
    }

    private void close()
    {
        // Super Easy
        if (configCache.difficulty == 0)
        {
            configCache.minTemp = CSMath.convertUnits(40, Temperature.Units.F, Temperature.Units.MC, true);
            configCache.maxTemp = CSMath.convertUnits(120, Temperature.Units.F, Temperature.Units.MC, true);
            configCache.rate = 0.5;
            configCache.requireThermometer = false;
            configCache.damageScaling = false;
            configCache.fireRes = true;
            configCache.iceRes = true;
        }
        // Easy
        else if (configCache.difficulty == 1)
        {
            configCache.minTemp = CSMath.convertUnits(45, Temperature.Units.F, Temperature.Units.MC, true);
            configCache.maxTemp = CSMath.convertUnits(110, Temperature.Units.F, Temperature.Units.MC, true);
            configCache.rate = 0.75;
            configCache.requireThermometer = false;
            configCache.damageScaling = false;
            configCache.fireRes = true;
            configCache.iceRes = true;
        }
        // Normal
        else if (configCache.difficulty == 2)
        {
            configCache.minTemp = CSMath.convertUnits(50, Temperature.Units.F, Temperature.Units.MC, true);
            configCache.maxTemp = CSMath.convertUnits(100, Temperature.Units.F, Temperature.Units.MC, true);
            configCache.rate = 1.0;
            configCache.requireThermometer = true;
            configCache.damageScaling = true;
            configCache.fireRes = false;
            configCache.iceRes = false;
        }
        // Hard
        else if (configCache.difficulty == 3)
        {
            configCache.minTemp = CSMath.convertUnits(60, Temperature.Units.F, Temperature.Units.MC, true);
            configCache.maxTemp = CSMath.convertUnits(90, Temperature.Units.F, Temperature.Units.MC, true);
            configCache.rate = 1.5;
            configCache.requireThermometer = true;
            configCache.damageScaling = true;
            configCache.fireRes = false;
            configCache.iceRes = false;
        }
        saveConfig(configCache);
        ConfigScreen.MC.displayGuiScreen(parentScreen);
    }

    boolean isMouseOverSlider(double mouseX, double mouseY)
    {
        return (mouseX >= this.width / 2.0 - 80 && mouseX <= this.width / 2.0 + 80 &&
                mouseY >= this.height / 2.0 - 67 && mouseY <= this.height / 2.0 - 35);
    }

    @Override
    public void tick()
    {
        double x = ConfigScreen.MOUSE_X;
        double y = ConfigScreen.MOUSE_Y;
        if (ConfigScreen.IS_MOUSE_DOWN && isMouseOverSlider(x, y))
        {
            int newDifficulty = 0;
            if (x < this.width / 2.0 - 76 + (19))
            {
                newDifficulty = 0;
            } else if (x < this.width / 2.0 - 76 + (19 * 3))
            {
                newDifficulty = 1;
            } else if (x < this.width / 2.0 - 76 + (19 * 5))
            {
                newDifficulty = 2;
            } else if (x < this.width / 2.0 - 76 + (19 * 7))
            {
                newDifficulty = 3;
            } else if (x < this.width / 2.0 - 76 + (19 * 9))
            {
                newDifficulty = 4;
            }

            if (newDifficulty != configCache.difficulty)
            {
                this.minecraft.getSoundHandler().play(SimpleSound.master(SoundEvents.BLOCK_NOTE_BLOCK_HAT, 1.8f, 0.5f));
            }
            configCache.difficulty = newDifficulty;
        }
    }

    public void saveConfig(ConfigCache configCache)
    {
        if (Minecraft.getInstance().player != null)
        {
            if (Minecraft.getInstance().player.hasPermissionLevel(2))
            {
                if (!Minecraft.getInstance().isIntegratedServerRunning())
                {
                    ColdSweatPacketHandler.INSTANCE.sendToServer(new ClientConfigSendMessage(configCache));
                }
                else
                {
                    ColdSweatConfig.getInstance().writeValues(configCache);
                }
            }
        }
        else
        {
            ColdSweatConfig.getInstance().writeValues(configCache);
        }
        ConfigCache.setInstance(configCache);
    }
}
