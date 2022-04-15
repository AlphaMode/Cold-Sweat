package dev.momostudios.coldsweat.client.gui.config.pages;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.momostudios.coldsweat.client.gui.config.ConfigScreen;
import dev.momostudios.coldsweat.client.gui.config.DifficultyDescriptions;
import dev.momostudios.coldsweat.api.temperature.Temperature;
import dev.momostudios.coldsweat.config.ConfigCache;
import dev.momostudios.coldsweat.util.math.CSMath;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SimpleSound;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldVertexBufferUploader;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.client.gui.GuiUtils;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nonnull;

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
        this.addListener(new Button(
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

        // Get max text length (used to extend the text box if it's too wide)
        int extra = 0;
        for (String text : DifficultyDescriptions.getListFor(configCache.difficulty))
        {
            int lineWidth = font.getStringWidth(text);
            if (lineWidth > extra && lineWidth > 300)
                extra = Math.abs(lineWidth - 300) / 2;
        }

        // Draw Text Box
        Matrix4f ms = matrixStack.getLast().getMatrix();
        int bgColor = GuiUtils.DEFAULT_BACKGROUND_COLOR;
        int borderColor = GuiUtils.DEFAULT_BORDER_COLOR_START;
        int borderColor2 = GuiUtils.DEFAULT_BORDER_COLOR_END;
        int middleX = this.width / 2;
        int middleY = this.height / 2;
        drawGradientRect(ms, 0, middleX - 169 - extra, middleY - 29, middleX + 169 + extra, middleY + 98, bgColor, bgColor); // BG

        drawGradientRect(ms, 0, middleX - 169 - extra, middleY + 98, middleX + 169 + extra, middleY + 99, bgColor, bgColor); // bottom
        drawGradientRect(ms, 0, middleX - 169 - extra, middleY - 30, middleX + 169 + extra, middleY - 29, bgColor, bgColor); // top
        drawGradientRect(ms, 0, middleX - 170 - extra, middleY - 29, middleX - 169 - extra, middleY + 98, bgColor, bgColor); // left
        drawGradientRect(ms, 0, middleX + 169 + extra, middleY - 29, middleX + 170 + extra, middleY + 98, bgColor, bgColor); // right

        drawGradientRect(ms, 0, middleX - 169 - extra, middleY + 97, middleX + 169 + extra, middleY + 98, borderColor2, borderColor2); // bottom border
        drawGradientRect(ms, 0, middleX - 169 - extra, middleY - 29, middleX + 169 + extra, middleY - 28, borderColor, borderColor); // top border
        drawGradientRect(ms, 0, middleX - 169 - extra, middleY - 28, middleX - 168 - extra, middleY + 97, borderColor, borderColor2); // left border
        drawGradientRect(ms, 0, middleX + 168 + extra, middleY - 28, middleX + 169 + extra, middleY + 97, borderColor, borderColor2); // right border

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

        // Draw Difficulty Description
        int line = 0;
        for (String text : DifficultyDescriptions.getListFor(configCache.difficulty))
        {
            this.font.drawString(matrixStack, text, this.width / 2f - 162 - extra, this.height / 2f - 22 + (line * 20f), 15393256);
            line++;
        }

        // Render Button(s)
        super.render(matrixStack, mouseX, mouseY, partialTicks);
    }

    @SuppressWarnings("deprecation")
    private static void drawGradientRect(Matrix4f mat, int zLevel, int left, int top, int right, int bottom, int startColor, int endColor)
    {
        float startAlpha = (float) (startColor >> 24 & 255) / 255.0F;
        float startRed = (float) (startColor >> 16 & 255) / 255.0F;
        float startGreen = (float) (startColor >> 8 & 255) / 255.0F;
        float startBlue = (float) (startColor & 255) / 255.0F;
        float endAlpha = (float) (endColor >> 24 & 255) / 255.0F;
        float endRed = (float) (endColor >> 16 & 255) / 255.0F;
        float endGreen = (float) (endColor >> 8 & 255) / 255.0F;
        float endBlue = (float) (endColor & 255) / 255.0F;

        RenderSystem.enableDepthTest();
        RenderSystem.disableTexture();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        BufferBuilder buffer = Tessellator.getInstance().getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        buffer.pos(mat, right, top, zLevel).color(startRed, startGreen, startBlue, startAlpha).endVertex();
        buffer.pos(mat, left, top, zLevel).color(startRed, startGreen, startBlue, startAlpha).endVertex();
        buffer.pos(mat, left, bottom, zLevel).color(endRed, endGreen, endBlue, endAlpha).endVertex();
        buffer.pos(mat, right, bottom, zLevel).color(endRed, endGreen, endBlue, endAlpha).endVertex();

        buffer.finishDrawing();
        WorldVertexBufferUploader.draw(buffer);

        RenderSystem.shadeModel(GL11.GL_FLAT);
        RenderSystem.disableBlend();
        RenderSystem.enableTexture();
    }

    private void close()
    {
        // Super Easy
        if (configCache.difficulty == 0)
        {
            configCache.minTemp = CSMath.convertUnits(40, Temperature.Units.F, Temperature.Units.MC, true);
            configCache.maxTemp = CSMath.convertUnits(120, Temperature.Units.F, Temperature.Units.MC, true);
            configCache.rate = 0.5;
            configCache.showWorldTemp = false;
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
            configCache.showWorldTemp = false;
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
            configCache.showWorldTemp = true;
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
            configCache.showWorldTemp = true;
            configCache.damageScaling = true;
            configCache.fireRes = false;
            configCache.iceRes = false;
        }
        ConfigScreen.MC.displayGuiScreen(parentScreen);
        new ConfigPageOne(parentScreen, configCache).saveConfig(configCache);
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
}
