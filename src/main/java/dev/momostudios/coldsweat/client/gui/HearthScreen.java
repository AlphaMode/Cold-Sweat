package dev.momostudios.coldsweat.client.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SimpleSound;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import dev.momostudios.coldsweat.ColdSweat;
import dev.momostudios.coldsweat.common.container.HearthContainer;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class HearthScreen extends ContainerScreen<HearthContainer>
{
    private static final ResourceLocation HEARTH_GUI = new ResourceLocation(ColdSweat.MOD_ID, "textures/gui/screen/hearth_gui.png");
    private static final ResourceLocation COLD_FUEL_GAUGE = new ResourceLocation(ColdSweat.MOD_ID, "textures/gui/screen/hearth_cold_fuel.png");
    private static final ResourceLocation HOT_FUEL_GAUGE = new ResourceLocation(ColdSweat.MOD_ID, "textures/gui/screen/hearth_hot_fuel.png");
    private static final ResourceLocation RADIUS_TOGGLE = new ResourceLocation(ColdSweat.MOD_ID, "textures/gui/screen/hearth_radius_toggle.png");
    ITextComponent name = new TranslationTextComponent("container." + ColdSweat.MOD_ID + ".hearth");
    int titleX = 8;
    int coldFuelLevel;
    int hotFuelLevel;

    public HearthScreen(HearthContainer screenContainer, PlayerInventory inv, ITextComponent titleIn)
    {
        super(screenContainer, inv, titleIn);
        this.guiLeft = 0;
        this.guiTop = 0;
        this.xSize = 176;
        this.ySize = 166;
        this.hotFuelLevel = screenContainer.te.getTileData().getInt("hotFuel");
        this.coldFuelLevel = screenContainer.te.getTileData().getInt("coldFuel");
    }

    @SubscribeEvent
    public static void onMouseClick(GuiScreenEvent.MouseClickedEvent event)
    {
        if (Minecraft.getInstance().currentScreen instanceof HearthScreen)
        {
            HearthScreen screen = ((HearthScreen) Minecraft.getInstance().currentScreen);
            if (screen.isHoveringButton(event.getMouseX(), event.getMouseY()))
            {
                boolean showRad = screen.container.te.getTileData().getBoolean("showRadius");

                screen.container.te.getTileData().putBoolean("showRadius", !showRad);
                Minecraft.getInstance().getSoundHandler().play(SimpleSound.master(new SoundEvent(
                        new ResourceLocation("minecraft:block.stone_button.click_on")), !showRad ? 1.9f : 1.5f, 0.75f));
            }
        }
    }

    @Override
    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks)
    {
        this.renderBackground(matrixStack);
        super.render(matrixStack, mouseX, mouseY, partialTicks);
        this.renderHoveredTooltip(matrixStack, mouseX, mouseY);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(MatrixStack matrixStack, int x, int y)
    {
        this.font.drawText(matrixStack, this.playerInventory.getDisplayName(), (float) this.playerInventoryTitleX, (float) this.playerInventoryTitleY, 4210752);
        this.font.drawText(matrixStack, name, titleX, 8f, 4210752);

        this.minecraft.textureManager.bindTexture(RADIUS_TOGGLE);
        blit(matrixStack, 82, 68, isHoveringButton(x, y) ? 12 : 0, isRadiusShowing() ? 0 : 12, 12, 12, 24, 24);

        if (isHoveringButton(x, y))
            this.renderTooltip(matrixStack, new TranslationTextComponent("cold_sweat.screen.hearth.show_particles"), x - guiLeft, y - guiTop);
    }


    @SuppressWarnings("deprecation")
    @Override
    protected void drawGuiContainerBackgroundLayer(MatrixStack matrixStack, float partialTicks, int mouseX, int mouseY)
    {
        RenderSystem.color4f(1f, 1f, 1f, 1f);
        this.minecraft.textureManager.bindTexture(HEARTH_GUI);
        int x = (this.width - this.xSize) / 2;
        int y = (this.height - this.ySize) / 2;
        this.blit(matrixStack, x ,y, 0, 0, this.xSize, this.ySize);

        int hotFuel = (int) (this.container.getHotFuel() / 27.7);
        int coldFuel = (int) (this.container.getColdFuel() / 27.7);

        blit(matrixStack, guiLeft + 61,  guiTop + 66 - hotFuel,  176, 36 - hotFuel,  12, 36, 256, 256);
        blit(matrixStack, guiLeft + 103, guiTop + 66 - coldFuel, 188, 36 - coldFuel, 12, 36, 256, 256);
    }

    boolean isHoveringButton(double mouseX, double mouseY)
    {
        int x = (this.width - this.xSize) / 2;
        int y = (this.height - this.ySize) / 2;

        return (mouseX >= x + 82 && mouseX <= x + 94 &&
                mouseY >= y + 68 && mouseY <= y + 80);
    }

    boolean isRadiusShowing()
    {
        return this.container.te.getTileData().getBoolean("showRadius");
    }
}
