package dev.momostudios.coldsweat.client.gui.config;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.util.ResourceLocation;

public class ConfigImage extends Widget implements IGuiEventListener
{
    ResourceLocation texture;
    int x, y, width, height, u, v;

    public ConfigImage(ResourceLocation texture, int x, int y, int width, int height, int u, int v)
    {
        super(x, y, width, height, null);
        this.texture = texture;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.u = u;
        this.v = v;
    }

    @Override
    public void render(MatrixStack poseStack, int x, int y, float partialTick)
    {
        Minecraft.getInstance().textureManager.bindTexture(texture);
        this.blit(poseStack, this.x, this.y, u, v, width, height);
    }

    @Override
    public boolean isHovered()
    {
        int mouseX = ConfigScreen.MOUSE_X;
        int mouseY = ConfigScreen.MOUSE_Y;
        return mouseX >= this.x && mouseY >= this.y && mouseX < this.x + this.width && mouseY < this.y + this.height;
    }
}
