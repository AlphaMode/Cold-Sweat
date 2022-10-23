package dev.momostudios.coldsweat.client.gui.config;

import com.mojang.blaze3d.matrix.MatrixStack;
import dev.momostudios.coldsweat.util.config.ConfigSettings;
import dev.momostudios.coldsweat.util.math.CSMath;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.gui.widget.button.ImageButton;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Mod.EventBusSubscriber(Dist.CLIENT)
public abstract class AbstractConfigPage extends Screen
{
    // Count how many ticks the mouse has been still for
    static int MOUSE_STILL_TIMER = 0;
    static int TOOLTIP_DELAY = 5;
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event)
    {
        MOUSE_STILL_TIMER++;
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY)
    {
        MOUSE_STILL_TIMER = 0;
        super.mouseMoved(mouseX, mouseY);
    }

    private final Screen parentScreen;
    private final ConfigSettings configSettings;

    public Map<String, List<Widget>> elementBatches = new HashMap<>();
    public Map<String, List<ITextProperties>> tooltips = new HashMap<>();

    protected int rightSideLength = 0;
    protected int leftSideLength = 0;

    private static final int TITLE_HEIGHT = ConfigScreen.TITLE_HEIGHT;
    private static final int BOTTOM_BUTTON_HEIGHT_OFFSET = ConfigScreen.BOTTOM_BUTTON_HEIGHT_OFFSET;
    private static final int BOTTOM_BUTTON_WIDTH = ConfigScreen.BOTTOM_BUTTON_WIDTH;
    public static Minecraft mc = Minecraft.getInstance();

    ResourceLocation divider = new ResourceLocation("cold_sweat:textures/gui/screen/configs/style_divider.png");

    ImageButton nextNavButton;
    ImageButton prevNavButton;

    public abstract ITextComponent sectionOneTitle();

    @Nullable
    public abstract ITextComponent sectionTwoTitle();

    public AbstractConfigPage(Screen parentScreen, ConfigSettings configSettings)
    {
        super(new TranslationTextComponent("cold_sweat.config.title"));
        this.parentScreen = parentScreen;
        this.configSettings = configSettings;
    }

    public int index()
    {
        return 0;
    }

    protected void addEmptySpace(Side side, double units)
    {
        if (side == Side.LEFT)
            this.leftSideLength += ConfigScreen.OPTION_SIZE * units;
        else
            this.rightSideLength += ConfigScreen.OPTION_SIZE * units;
    }

    protected void addLabel(String id, Side side, String text)
    {
        this.addLabel(id, side, text, 16777215);
    }

    protected void addLabel(String id, Side side, String text, int color)
    {
        int labelX = side == Side.LEFT ? this.width / 2 - 185 : this.width / 2 + 51;
        int labelY = this.height / 4 + (side == Side.LEFT ? leftSideLength : rightSideLength);
        ConfigLabel label = new ConfigLabel(id, text, labelX, labelY, color);

        this.addListener(label);

        this.addElementBatch(id, Collections.singletonList(label));

        if (side == Side.LEFT)
            this.leftSideLength  += font.FONT_HEIGHT + 4;
        else
            this.rightSideLength += font.FONT_HEIGHT + 4;
    }

    protected void addButton(String id, Side side, Supplier<String> dynamicLabel, Consumer<Button> onClick,
                             boolean requireOP, boolean setsCustomDifficulty, String... tooltip)
    {
        String label = dynamicLabel.get();

        boolean shouldBeActive = !requireOP || mc.player == null || mc.player.hasPermissionLevel(2);
        int buttonX = side == Side.LEFT ? this.width / 2 - 185 : this.width / 2 + 51;
        int buttonY = this.height / 4 - 8 + (side == Side.LEFT ? leftSideLength : rightSideLength);
        int buttonWidth = 152 + Math.max(0, font.getStringWidth(label) - 140);

        if (buttonWidth > 152)
        {
            buttonX -= (buttonWidth - 152) / 2;
        }
        Button button = new ConfigButton(buttonX, buttonY, buttonWidth, 20, new StringTextComponent(label), button1 ->
        {
            onClick.accept(button1);
            button1.setMessage(new StringTextComponent(dynamicLabel.get()));
        })
        {
            @Override
            public boolean setsCustomDifficulty()
            {
                return setsCustomDifficulty;
            }
        };
        button.active = shouldBeActive;
        elementBatches.put(id, Collections.singletonList(button));
        this.addListener(button);

        if (side == Side.LEFT)
            this.leftSideLength += ConfigScreen.OPTION_SIZE;
        else
            this.rightSideLength += ConfigScreen.OPTION_SIZE;

        this.setTooltip(id, tooltip);
    }

    protected void addDecimalInput(String id, Side side, TextComponent label, Consumer<Double> writeValue, Consumer<TextFieldWidget> readValue,
                                   boolean requireOP, boolean setsCustomDifficulty, String... tooltip)
    {
        boolean shouldBeActive = !requireOP || mc.player == null || mc.player.hasPermissionLevel(2);
        int inputX = side == Side.LEFT ? -86 : 147;
        int inputY = (side == Side.LEFT ? this.leftSideLength : this.rightSideLength) - 2;
        int labelOffset = font.getStringWidth(label.getString()) > 90 ?
                          font.getStringWidth(label.getString()) - 84 : 0;

        TextFieldWidget textBox = new TextFieldWidget(this.font, this.width / 2 + inputX + labelOffset, this.height / 4 - 6 + inputY, 51, 22, new StringTextComponent(""))
        {
            @Override
            public void writeText(String text)
            {
                super.writeText(text);
                CSMath.tryCatch(() ->
                {
                    if (setsCustomDifficulty)
                        configSettings.difficulty = 4;
                    writeValue.accept(Double.parseDouble(this.getText()));
                });
            }
            @Override
            public void deleteWords(int i)
            {
                super.deleteWords(i);
                CSMath.tryCatch(() ->
                {
                    if (setsCustomDifficulty)
                        configSettings.difficulty = 4;
                    writeValue.accept(Double.parseDouble(this.getText()));
                });
            }
            @Override
            public void deleteFromCursor(int i)
            {
                super.deleteFromCursor(i);
                CSMath.tryCatch(() ->
                {
                    if (setsCustomDifficulty)
                        configSettings.difficulty = 4;
                    writeValue.accept(Double.parseDouble(this.getText()));
                });
            }
        };
        textBox.setEnabled(shouldBeActive);
        readValue.accept(textBox);
        textBox.setText(ConfigScreen.TWO_PLACES.format(Double.parseDouble(textBox.getText())));

        this.addListener(textBox);
        this.addListener(new ConfigLabel(id, label.getString(), this.width / 2 + (side == Side.LEFT ? -185 : 52), this.height / 4 + inputY, shouldBeActive ? 16777215 : 8421504));

        this.setTooltip(id, tooltip);
        this.addElementBatch(id, Collections.singletonList(textBox));

        if (side == Side.LEFT)
            this.leftSideLength += ConfigScreen.OPTION_SIZE * 1.2;
        else
            this.rightSideLength += ConfigScreen.OPTION_SIZE * 1.2;
    }

    protected void addDirectionPanel(String id, Side side, TranslationTextComponent label, Consumer<Integer> addX, Consumer<Integer> addY, Runnable reset,
                                     boolean requireOP, boolean setsCustomDifficulty, String... tooltip)
    {
        int xOffset = side == Side.LEFT ? -96 : 140;
        int yOffset = side == Side.LEFT ? this.leftSideLength : this.rightSideLength;

        boolean shouldBeActive = !requireOP || mc.player == null || mc.player.hasPermissionLevel(2);

        ResourceLocation texture = new ResourceLocation("cold_sweat:textures/gui/screen/configs/config_buttons.png");

        int labelOffset = font.getStringWidth(label.getString()) > 84 ?
                font.getStringWidth(label.getString()) - 84 : 0;


        // Left button
        ImageButton leftButton = new ImageButton(this.width / 2 + xOffset + labelOffset, this.height / 4 - 8 + yOffset, 14, 20, 0, 0, 20, texture, button ->
        {
            addX.accept(-1);

            if (setsCustomDifficulty)
                configSettings.difficulty = 4;
        });
        leftButton.active = shouldBeActive;
        this.addListener(leftButton);

        // Up button
        ImageButton upButton = new ImageButton(this.width / 2 + xOffset + 14 + labelOffset, this.height / 4 - 8 + yOffset, 20, 10, 14, 0, 20, texture, button ->
        {
            addY.accept(-1);

            if (setsCustomDifficulty)
                configSettings.difficulty = 4;
        });
        upButton.active = shouldBeActive;
        this.addListener(upButton);

        // Down button
        ImageButton downButton = new ImageButton(this.width / 2 + xOffset + 14 + labelOffset, this.height / 4 + 2 + yOffset, 20, 10, 14, 10, 20, texture, button ->
        {
            addY.accept(1);

            if (setsCustomDifficulty)
                configSettings.difficulty = 4;
        });
        downButton.active = shouldBeActive;
        this.addListener(downButton);

        // Right button
        ImageButton rightButton = new ImageButton(this.width / 2 + xOffset + 34 + labelOffset, this.height / 4 - 8 + yOffset, 14, 20, 34, 0, 20, texture, button ->
        {
            addX.accept(1);

            if (setsCustomDifficulty)
                configSettings.difficulty = 4;
        });
        rightButton.active = shouldBeActive;
        this.addListener(rightButton);

        // Reset button
        ImageButton resetButton = new ImageButton(this.width / 2 + xOffset + 52 + labelOffset, this.height / 4 - 8 + yOffset, 20, 20, 0, 128, 20, texture, button ->
        {
            reset.run();

            if (setsCustomDifficulty)
                configSettings.difficulty = 4;
        });
        resetButton.active = shouldBeActive;

        this.addListener(resetButton);
        this.addListener(new ConfigLabel(id, label.getString(), this.width / 2 + 52, this.height / 4 + yOffset, shouldBeActive ? 16777215 : 8421504));

        this.setTooltip(id, tooltip);
        this.addElementBatch(id, Arrays.asList(upButton, downButton, leftButton, rightButton, resetButton));

        // Move down
        if (side == Side.LEFT)
            this.leftSideLength += ConfigScreen.OPTION_SIZE * 1.5;
        else
            this.rightSideLength += ConfigScreen.OPTION_SIZE * 1.5;
    }

    @Override
    protected void init()
    {
        this.leftSideLength = 0;
        this.rightSideLength = 0;

        this.addListener(new Button(
            this.width / 2 - BOTTOM_BUTTON_WIDTH / 2,
            this.height - BOTTOM_BUTTON_HEIGHT_OFFSET,
            BOTTOM_BUTTON_WIDTH, 20,
            new TranslationTextComponent("gui.done"),
            button -> this.close())
        );

        // Navigation
        nextNavButton = new ImageButton(this.width - 32, 12, 20, 20, 0, 88, 20,
            new ResourceLocation("cold_sweat:textures/gui/screen/configs/config_buttons.png"), button ->
                mc.displayGuiScreen(ConfigScreen.getPage(this.index() + 1, parentScreen, configSettings)));
        if (this.index() < ConfigScreen.LAST_PAGE)
            this.addListener(nextNavButton);

        prevNavButton = new ImageButton(this.width - 76, 12, 20, 20, 20, 88, 20,
            new ResourceLocation("cold_sweat:textures/gui/screen/configs/config_buttons.png"), button ->
                mc.displayGuiScreen(ConfigScreen.getPage(this.index() - 1, parentScreen, configSettings)));
        if (this.index() > ConfigScreen.FIRST_PAGE)
            this.addListener(prevNavButton);
    }

    @Override
    public void render(@Nonnull MatrixStack poseStack, int mouseX, int mouseY, float partialTicks)
    {
        this.renderBackground(poseStack);

        drawCenteredString(poseStack, this.font, this.title.getString(), this.width / 2, TITLE_HEIGHT, 0xFFFFFF);

        // Page Number
        drawString(poseStack, this.font, new StringTextComponent(this.index() + 1 + "/" + (ConfigScreen.LAST_PAGE + 1)), this.width - 53, 18, 16777215);

        // Section 1 Title
        drawString(poseStack, this.font, this.sectionOneTitle(), this.width / 2 - 204, this.height / 4 - 28, 16777215);

        // Section 1 Divider
        this.minecraft.textureManager.bindTexture(divider);
        this.blit(poseStack, this.width / 2 - 202, this.height / 4 - 16, 0, 0, 1, 155);

        if (this.sectionTwoTitle() != null)
        {
            // Section 2 Title
            drawString(poseStack, this.font, this.sectionTwoTitle(), this.width / 2 + 32, this.height / 4 - 28, 16777215);

            // Section 2 Divider
            this.minecraft.textureManager.bindTexture(divider);
            this.blit(poseStack, this.width / 2 + 34, this.height / 4 - 16, 0, 0, 1, 155);
        }

        super.render(poseStack, mouseX, mouseY, partialTicks);

        // Render Widgets
        for (IGuiEventListener listener : this.children)
        {
            if (listener instanceof Widget)
            {
                Widget widget = (Widget) listener;
                widget.render(poseStack, mouseX, mouseY, partialTicks);
            }
        }

        // Render tooltip
        if (MOUSE_STILL_TIMER >= TOOLTIP_DELAY)
        for (Map.Entry<String, List<Widget>> widget : this.elementBatches.entrySet())
        {
            int x;
            int y;
            int maxX;
            int maxY;
            ConfigLabel label = null;
            if (widget.getValue().size() == 1 && widget.getValue().get(0) instanceof Button)
            {
                Button button = (Button) widget.getValue().get(0);

                x = button.x;
                y = button.y;
                maxX = x + button.getWidth();
                maxY = y + button.getHeight();
                String id = widget.getKey();

                if (mouseX >= x && mouseX <= maxX - 1
                &&  mouseY >= y && mouseY <= maxY - 1)
                {
                    List<ITextProperties> tooltipList = this.tooltips.get(id);
                    if (tooltipList != null && !tooltipList.isEmpty())
                    {
                        List<ITextComponent> tooltip = this.tooltips.get(id).stream().map(text -> new StringTextComponent(text.getString())).collect(Collectors.toList());
                        this.func_243308_b(poseStack, tooltip, mouseX, mouseY);
                        break;
                    }
                }
            }
            else
            {
                for (IGuiEventListener eventListener : this.children)
                {
                    if (eventListener instanceof ConfigLabel)
                    {
                        ConfigLabel label1 = (ConfigLabel) eventListener;
                        if (label1.id.equals(widget.getKey()))
                        {
                            label = label1;
                            break;
                        }
                    }
                }
                if (label == null) continue;

                x = label.x;
                y = label.y;
                maxX = label.x + font.getStringWidth(label.text);
                maxY = label.y + font.FONT_HEIGHT;

                if (mouseX >= x - 2 && mouseX <= maxX + 2
                &&  mouseY >= y - 5 && mouseY <= maxY + 5)
                {
                    List<ITextProperties> tooltipList = this.tooltips.get(label.id);
                    if (tooltipList != null && !tooltipList.isEmpty())
                    {
                        List<ITextComponent> tooltip = this.tooltips.get(label.id).stream().map(text -> new StringTextComponent(text.getString())).collect(Collectors.toList());
                        this.func_243308_b(poseStack, tooltip, mouseX, mouseY);
                        break;
                    }
                }
            }
        }
    }

    @Override
    public void tick()
    {
        super.tick();
    }

    @Override
    public boolean isPauseScreen()
    {
        return true;
    }

    public void close()
    {
        this.onClose();
        Minecraft.getInstance().displayGuiScreen(this.parentScreen);
    }

    public enum Side
    {
        LEFT,
        RIGHT
    }

    protected void addElementBatch(String id, List<Widget> elements)
    {
        this.elementBatches.put(id, elements);
    }

    public List<Widget> getElementBatch(String id)
    {
        return this.elementBatches.get(id);
    }

    protected void setTooltip(String id, String[] tooltip)
    {
        List<ITextProperties> tooltipList = new ArrayList<>();
        for (String string : tooltip)
        {
            tooltipList.addAll(font.getCharacterManager().func_238365_g_(string, 300, Style.EMPTY));
        }
        this.tooltips.put(id, tooltipList);
    }
}
