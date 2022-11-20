package dev.momostudios.coldsweat.api.event.client;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.world.World;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;

@Cancelable
public class RenderWorldEvent extends Event
{
    World world;
    MatrixStack matrixStack;
    float partialTicks;
    long nanoTime;
    boolean renderBlockOutline;
    ActiveRenderInfo camera;
    WorldRenderer levelRenderer;
    GameRenderer renderer;
    LightTexture lightTexture;
    Matrix4f lastMatrix;
    ViewFrustum frustum;

    public RenderWorldEvent(MatrixStack matrixStack, float partialTicks, long nanoTime, boolean renderBlockOutline,
                            ActiveRenderInfo camera, WorldRenderer levelRenderer, GameRenderer gameRenderer,
                            LightTexture lightTexture, Matrix4f matrix4f, ViewFrustum frustum)
    {
        this.world = Minecraft.getInstance().world;
        this.matrixStack = matrixStack;
        this.partialTicks = partialTicks;
        this.nanoTime = nanoTime;
        this.renderBlockOutline = renderBlockOutline;
        this.camera = camera;
        this.levelRenderer = levelRenderer;
        this.renderer = gameRenderer;
        this.lightTexture = lightTexture;
        this.lastMatrix = matrix4f;
        this.frustum = frustum;
    }

    public World getWorld()
    {
        return world;
    }

    public MatrixStack getMatrixStack()
    {
        return matrixStack;
    }

    public float getPartialTicks()
    {
        return partialTicks;
    }

    public long getNanoTime()
    {
        return nanoTime;
    }

    public boolean renderBlockOutline()
    {
        return renderBlockOutline;
    }

    public ActiveRenderInfo getCamera()
    {
        return camera;
    }

    public WorldRenderer getLevelRenderer()
    {
        return levelRenderer;
    }

    public GameRenderer getRenderer()
    {
        return renderer;
    }

    public LightTexture getLightTexture()
    {
        return lightTexture;
    }

    public Matrix4f getLastMatrix()
    {
        return lastMatrix;
    }

    public ViewFrustum getFrustum()
    {
        return frustum;
    }
}
