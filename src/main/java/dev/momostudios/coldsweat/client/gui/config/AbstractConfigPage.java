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

    public Map<String, List<Widget>> widgetBatches = new HashMap<>();
    public Map<String, List<ITextProperties>> tooltips = new HashMap<>();

    protected int rightSideLength = 0;
    protected int leftSideLength = 0;

    private static final int TITLE_HEIGHT = ConfigScreen.TITLE_HEIGHT;
    private static final int BOTTOM_BUTTON_HEIGHT_OFFSET = ConfigScreen.BOTTOM_BUTTON_HEIGHT_OFFSET;
    private static final int BOTTOM_BUTTON_WIDTH = ConfigScreen.BOTTOM_BUTTON_WIDTH;
    public static Minecraft mc = Minecraft.getInstance();

    static ResourceLocation TEXTURE = new ResourceLocation("cold_sweat:textures/gui/screen/config_gui.png");

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

    /**
     * Adds an empty block to the list on the given side. One unit is the height of a button.
     */
    protected void addEmptySpace(Side side, double units)
    {
        if (side == Side.LEFT)
            this.leftSideLength += ConfigScreen.OPTION_SIZE * units;
        else
            this.rightSideLength += ConfigScreen.OPTION_SIZE * units;
    }

    /**
     * Adds a label with plain text to the list on the given side.
     * @param id The internal id of the label. This widget can be accessed by this id.
     */
    protected void addLabel(String id, Side side, String text, int color)
    {
        int labelX = side == Side.LEFT ? this.width / 2 - 185 : this.width / 2 + 51;
        int labelY = this.height / 4 + (side == Side.LEFT ? leftSideLength : rightSideLength);
        ConfigLabel label = new ConfigLabel(id, text, labelX, labelY, color);

        this.addWidgetBatch(id, Arrays.asList(label));

        if (side == Side.LEFT)
            this.leftSideLength += font.FONT_HEIGHT + 4;
        else
            this.rightSideLength += font.FONT_HEIGHT + 4;
    }

    protected void addLabel(String id, Side side, String text)
    {
        this.addLabel(id, side, text, 16777215);
    }

    /**
     * Adds a button to the list on the given side.
     * @param id The internal id of the button. This widget can be accessed by this id.
     * @param dynamicLabel A supplier that returns the label of the button. The label is updated when the button is pressed.
     * @param onClick The action to perform when the button is pressed.
     * @param requireOP Whether the button should be disabled if the player is not OP.
     * @param setsCustomDifficulty Sets Cold Sweat's difficulty to custom when pressed, if true.
     * @param clientside Whether the button is clientside only (renders the clientside icon).
     * @param tooltip The tooltip of the button when hovered.
     */
    protected void addButton(String id, Side side, Supplier<String> dynamicLabel, Consumer<Button> onClick,
                             boolean requireOP, boolean setsCustomDifficulty, boolean clientside, String... tooltip)
    {
        String label = dynamicLabel.get();

        boolean shouldBeActive = !requireOP || mc.player == null || mc.player.hasPermissionLevel(2);
        int buttonX = this.width / 2;
        int xOffset = side == Side.LEFT ? -179 : 56;
        int buttonY = this.height / 4 - 8 + (side == Side.LEFT ? leftSideLength : rightSideLength);
        // Extend the button if the text is too long
        int buttonWidth = 152 + Math.max(0, font.getStringWidth(label) - 140);

        // Make the button
        Button button = new ConfigButton(buttonX + xOffset, buttonY, buttonWidth, 20, new StringTextComponent(label), button1 ->
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

        // Add the clientside indicator
        if (clientside) this.addListener(new ConfigImage(TEXTURE, this.width / 2 + xOffset - 18, buttonY + 3, 16, 15, 0, 144));

        this.addWidgetBatch(id, Arrays.asList(button));

        // Mark this space as used
        if (side == Side.LEFT)
            this.leftSideLength += ConfigScreen.OPTION_SIZE;
        else
            this.rightSideLength += ConfigScreen.OPTION_SIZE;

        this.setTooltip(id, tooltip);
    }

    /**
     * Adds an input that accepts decimal numbers to the list on the given side.
     * @param id The internal id of the input. This widget can be accessed by this id.
     * @param label The label text of the input.
     * @param onValueWrite The action to perform when the input is changed.
     * @param onInit The action to perform when the input is initialized.
     * @param requireOP Whether the input should be disabled if the player is not OP.
     * @param setsCustomDifficulty Sets Cold Sweat's difficulty to custom when edited, if true.
     * @param clientside Whether the input is clientside only (renders the clientside icon).
     * @param tooltip The tooltip of the input when hovered.
     */
    protected void addDecimalInput(String id, Side side, TextComponent label, Consumer<Double> onValueWrite, Consumer<TextFieldWidget> onInit,
                                   boolean requireOP, boolean setsCustomDifficulty, boolean clientside, String... tooltip)
    {
        boolean shouldBeActive = !requireOP || mc.player == null || mc.player.hasPermissionLevel(2);
        int xOffset = side == Side.LEFT ? -82 : 151;
        int yOffset = (side == Side.LEFT ? this.leftSideLength : this.rightSideLength) - 2;
        int labelOffset = font.getStringWidth(label.getString()) > 90 ?
                font.getStringWidth(label.getString()) - 84 : 0;

        // Make the input
        TextFieldWidget textBox = new TextFieldWidget(this.font, this.width / 2 + xOffset + labelOffset, this.height / 4 - 6 + yOffset, 51, 22, new StringTextComponent(""))
        {
            @Override
            public void writeText(String text)
            {
                super.writeText(text);
                CSMath.tryCatch(() ->
                {
                    if (setsCustomDifficulty)
                        configSettings.difficulty = 4;
                    onValueWrite.accept(Double.parseDouble(this.getText()));
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
                    onValueWrite.accept(Double.parseDouble(this.getText()));
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
                    onValueWrite.accept(Double.parseDouble(this.getText()));
                });
            }
        };

        // Disable the input if the player is not OP
        textBox.setEnabled(shouldBeActive);

        // Set the initial value
        onInit.accept(textBox);

        // Round the input to 2 decimal places
        textBox.setText(ConfigScreen.TWO_PLACES.format(Double.parseDouble(textBox.getText())));

        // Make the label
        ConfigLabel configLabel = new ConfigLabel(id, label.getString(), this.width / 2 + xOffset - 95, this.height / 4 + yOffset, shouldBeActive ? 16777215 : 8421504);
        // Add the clientside indicator
        if (clientside) this.addListener(new ConfigImage(TEXTURE, this.width / 2 + xOffset - 115, this.height / 4 - 4 + yOffset, 16, 15, 0, 144));

        this.setTooltip(id, tooltip);

        // Add the widget
        this.addWidgetBatch(id, Arrays.asList(textBox, configLabel));

        // Mark this space as used
        if (side == Side.LEFT)
            this.leftSideLength += ConfigScreen.OPTION_SIZE * 1.2;
        else
            this.rightSideLength += ConfigScreen.OPTION_SIZE * 1.2;
    }

    /**
     * Adds a 4-way direction button panel with a reset button to the list on the given side.
     * @param id The internal id of the panel. This widget can be accessed by this id.
     * @param label The label text of the panel.
     * @param leftRightPressed The action to perform when the left or right button is pressed. 1 for right, -1 for left.
     * @param upDownPressed The action to perform when the up or down button is pressed. -1 for up, 1 for down.
     * @param reset The action to perform when the reset button is pressed.
     * @param requireOP Whether the panel should be disabled if the player is not OP.
     * @param setsCustomDifficulty Sets Cold Sweat's difficulty to custom when edited, if true.
     * @param clientside Whether the panel is clientside only (renders the clientside icon).
     * @param tooltip The tooltip of the panel when hovered.
     */
    protected void addDirectionPanel(String id, Side side, TextComponent label, Consumer<Integer> leftRightPressed, Consumer<Integer> upDownPressed, Runnable reset,
                                     boolean requireOP, boolean setsCustomDifficulty, boolean clientside, String... tooltip)
    {
        int xOffset = side == Side.LEFT ? -81 : 152;
        int yOffset = side == Side.LEFT ? this.leftSideLength : this.rightSideLength;

        boolean shouldBeActive = !requireOP || mc.player == null || mc.player.hasPermissionLevel(2);

        int labelOffset = font.getStringWidth(label.getString()) > 84 ?
                font.getStringWidth(label.getString()) - 84 : 0;

        // Left button
        ImageButton leftButton = new ImageButton(this.width / 2 + xOffset + labelOffset, this.height / 4 - 8 + yOffset, 14, 20, 0, 0, 20, TEXTURE, button ->
        {
            leftRightPressed.accept(-1);

            if (setsCustomDifficulty)
                configSettings.difficulty = 4;
        });
        leftButton.active = shouldBeActive;

        // Up button
        ImageButton upButton = new ImageButton(this.width / 2 + xOffset + 14 + labelOffset, this.height / 4 - 8 + yOffset, 20, 10, 14, 0, 20, TEXTURE, button ->
        {
            upDownPressed.accept(-1);

            if (setsCustomDifficulty)
                configSettings.difficulty = 4;
        });
        upButton.active = shouldBeActive;

        // Down button
        ImageButton downButton = new ImageButton(this.width / 2 + xOffset + 14 + labelOffset, this.height / 4 + 2 + yOffset, 20, 10, 14, 10, 20, TEXTURE, button ->
        {
            upDownPressed.accept(1);

            if (setsCustomDifficulty)
                configSettings.difficulty = 4;
        });
        downButton.active = shouldBeActive;

        // Right button
        ImageButton rightButton = new ImageButton(this.width / 2 + xOffset + 34 + labelOffset, this.height / 4 - 8 + yOffset, 14, 20, 34, 0, 20, TEXTURE, button ->
        {
            leftRightPressed.accept(1);

            if (setsCustomDifficulty)
                configSettings.difficulty = 4;
        });
        rightButton.active = shouldBeActive;

        // Reset button
        ImageButton resetButton = new ImageButton(this.width / 2 + xOffset + 52 + labelOffset, this.height / 4 - 8 + yOffset, 20, 20, 48, 0, 20, TEXTURE, button ->
        {
            reset.run();

            if (setsCustomDifficulty)
                configSettings.difficulty = 4;
        });
        resetButton.active = shouldBeActive;

        // Add the option text
        ConfigLabel configLabel = new ConfigLabel(id, label.getString(), this.width / 2 + xOffset - 95, this.height / 4 + yOffset, shouldBeActive ? 16777215 : 8421504);
        // Add the clientside indicator
        if (clientside) this.addListener(new ConfigImage(TEXTURE, this.width / 2 + xOffset - 114, this.height / 4 - 8 + yOffset + 5, 16, 15, 0, 144));

        this.setTooltip(id, tooltip);
        this.addWidgetBatch(id, Arrays.asList(upButton, downButton, leftButton, rightButton, resetButton, configLabel));

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
        nextNavButton = new ImageButton(this.width - 32, 12, 20, 20, 0, 88, 20, TEXTURE,
                button -> mc.displayGuiScreen(ConfigScreen.getPage(this.index() + 1, parentScreen, configSettings)));
        if (this.index() < ConfigScreen.LAST_PAGE)
            this.addListener(nextNavButton);

        prevNavButton = new ImageButton(this.width - 76, 12, 20, 20, 20, 88, 20, TEXTURE,
                button -> mc.displayGuiScreen(ConfigScreen.getPage(this.index() - 1, parentScreen, configSettings)));
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
        this.minecraft.textureManager.bindTexture(TEXTURE);
        this.blit(poseStack, this.width / 2 - 202, this.height / 4 - 16, 255, 0, 1, 154);

        if (this.sectionTwoTitle() != null)
        {
            // Section 2 Title
            drawString(poseStack, this.font, this.sectionTwoTitle(), this.width / 2 + 32, this.height / 4 - 28, 16777215);

            // Section 2 Divider
            this.minecraft.textureManager.bindTexture(TEXTURE);
            this.blit(poseStack, this.width / 2 + 34, this.height / 4 - 16, 255, 0, 1, 154);
        }

        super.render(poseStack, mouseX, mouseY, partialTicks);

        // Render Widgets
        for (IGuiEventListener listener : this.children)
        {
            if (listener instanceof Widget)
            {
                Widget widget = (Widget) listener;
                this.minecraft.textureManager.bindTexture(TEXTURE);
                widget.render(poseStack, mouseX, mouseY, partialTicks);
            }
        }

        // Render tooltip
        if (MOUSE_STILL_TIMER >= TOOLTIP_DELAY)
        for (Map.Entry<String, List<Widget>> entry : widgetBatches.entrySet())
        {
            String id = entry.getKey();
            List<Widget> widgets = entry.getValue();

            // if the mouse is hovering over any of the widgets in the batch, print hello
            if (widgets.stream().anyMatch(Widget::isHovered))
            {
                List<ITextProperties> tooltipList = this.tooltips.get(id);
                if (tooltipList != null && !tooltipList.isEmpty())
                {
                    List<ITextComponent> tooltip = this.tooltips.get(id).stream().map(text -> new StringTextComponent(text.getString())).collect(Collectors.toList());
                    this.func_243308_b(poseStack, tooltip, mouseX, mouseY);
                }
                break;
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

    protected void addWidgetBatch(String id, List<Widget> elements)
    {
        for (Widget element : elements)
            this.addListener(element);

        this.widgetBatches.put(id, elements);
    }

    public List<Widget> getWidgetBatch(String id)
    {
        return this.widgetBatches.get(id);
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
