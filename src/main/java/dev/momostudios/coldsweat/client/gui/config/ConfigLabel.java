package dev.momostudios.coldsweat.client.gui.config;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.util.text.StringTextComponent;

public class ConfigLabel extends Widget implements IGuiEventListener
{
    public final String id;

    public String text;
    public int color;
    public int x;
    public int y;

    public ConfigLabel(String id, String text, int x, int y)
    {
        this(id, text, x, y, 16777215);
    }

    public ConfigLabel(String id, String text, int x, int y, int color)
    {
        super(x, y, Minecraft.getInstance().fontRenderer.getStringWidth(text), Minecraft.getInstance().fontRenderer.FONT_HEIGHT, new StringTextComponent(text));
        this.id = id;
        this.text = text;
        this.x = x;
        this.y = y;
        this.color = color;
    }

    public void setText(String text)
    {
        this.text = text;
    }

    public void setColor(int color)
    {
        this.color = color;
    }

    @Override
    public void render(MatrixStack poseStack, int mouseX, int mouseY, float depth)
    {
        Minecraft.getInstance().fontRenderer.drawStringWithShadow(poseStack, this.text, this.x, this.y, color);
    }

    @Override
    public boolean isHovered()
    {
        int mouseX = ConfigScreen.MOUSE_X;
        int mouseY = ConfigScreen.MOUSE_Y;
        return mouseX >= this.x - 5 && mouseY >= this.y - 5 && mouseX < this.x + this.width + 5 && mouseY < this.y + this.height + 5;
    }
}
