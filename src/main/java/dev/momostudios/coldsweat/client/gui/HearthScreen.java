package dev.momostudios.coldsweat.client.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.datafixers.util.Pair;
import dev.momostudios.coldsweat.common.event.HearthPathManagement;
import dev.momostudios.coldsweat.core.network.ColdSweatPacketHandler;
import dev.momostudios.coldsweat.core.network.message.DisableHearthParticlesMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SimpleSound;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.client.gui.widget.button.ImageButton;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import dev.momostudios.coldsweat.ColdSweat;
import dev.momostudios.coldsweat.common.container.HearthContainer;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;

import java.lang.reflect.Field;
import java.util.Arrays;

public class HearthScreen extends ContainerScreen<HearthContainer>
{
    private static final ResourceLocation HEARTH_GUI = new ResourceLocation(ColdSweat.MOD_ID, "textures/gui/screen/hearth_gui.png");
    ITextComponent name = new TranslationTextComponent("container." + ColdSweat.MOD_ID + ".hearth");
    int titleX = 8;

    public HearthScreen(HearthContainer screenContainer, PlayerInventory inv, ITextComponent titleIn)
    {
        super(screenContainer, inv, titleIn);
        this.guiLeft = 0;
        this.guiTop = 0;
        this.xSize = 176;
        this.ySize = 166;
    }

    Pair<BlockPos, String> levelPos = Pair.of(this.container.te.getPos(), this.container.te.getWorld().getDimensionKey().getLocation().toString());
    boolean hideParticles = HearthPathManagement.DISABLED_HEARTHS.contains(levelPos);
    boolean hideParticlesOld = hideParticles;

    @Override
    public void init()
    {
        super.init();
        this.addButton(new ImageButton(guiLeft + 82, guiTop + 68, 12, 12, 176 + (!hideParticles ? 0 : 12), 36, 12, HEARTH_GUI, (button) ->
        {
            ImageButton hearthButton = (ImageButton) button;
            hideParticles = !hideParticles;
            if (hideParticles)
            {
                HearthPathManagement.DISABLED_HEARTHS.add(levelPos);
                if (HearthPathManagement.DISABLED_HEARTHS.size() > 20)
                {
                    HearthPathManagement.DISABLED_HEARTHS.remove(HearthPathManagement.DISABLED_HEARTHS.stream().findFirst().get());
                }
            }
            else
            {
                HearthPathManagement.DISABLED_HEARTHS.remove(levelPos);
            }
            Field imageX = ObfuscationReflectionHelper.findField(ImageButton.class, "field_191747_p");
            imageX.setAccessible(true);
            try
            {
                imageX.set(hearthButton, 176 + (!hideParticles ? 0 : 12));
            }
            catch (Exception ignored) {}
        })
        {
            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button)
            {
                if (this.active && this.visible && this.isValidClickButton(button) && this.clicked(mouseX, mouseY))
                {
                    Minecraft.getInstance().getSoundHandler().play(SimpleSound.master(SoundEvents.BLOCK_STONE_BUTTON_CLICK_ON, hideParticles ? 1.5f : 1.9f, 0.75f));
                    this.onClick(mouseX, mouseY);
                    return true;
                }
                return false;
            }

            @Override
            public void renderToolTip(MatrixStack matrixStack, int mouseX, int mouseY)
            {
                HearthScreen.this.renderWrappedToolTip(matrixStack, Arrays.asList(new TranslationTextComponent("cold_sweat.screen.hearth.show_particles")), mouseX, mouseY, font);
            }
        });
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
    }

    @SuppressWarnings("deprecation")
    @Override
    protected void drawGuiContainerBackgroundLayer(MatrixStack matrixStack, float partialTicks, int mouseX, int mouseY)
    {
        RenderSystem.color4f(1f, 1f, 1f, 1f);
        this.minecraft.textureManager.bindTexture(HEARTH_GUI);
        int x = (this.width - this.xSize) / 2;
        int y = (this.height - this.ySize) / 2;
        this.blit(matrixStack, x, y, 0, 0, this.xSize, this.ySize);

        int hotFuel  = (int) (this.container.getHotFuel()  / 27.7);
        int coldFuel = (int) (this.container.getColdFuel() / 27.7);

        blit(matrixStack, guiLeft + 61,  guiTop + 66 - hotFuel,  176, 36 - hotFuel,  12, hotFuel, 256, 256);
        blit(matrixStack, guiLeft + 103, guiTop + 66 - coldFuel, 188, 36 - coldFuel, 12, coldFuel, 256, 256);
    }

    @Override
    public void onClose()
    {
        super.onClose();
        if (this.minecraft.player != null && hideParticlesOld != hideParticles)
        {
            ColdSweatPacketHandler.INSTANCE.sendToServer(new DisableHearthParticlesMessage(Minecraft.getInstance().player, HearthPathManagement.serializeDisabledHearths()));
        }
    }
}
