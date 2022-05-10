package dev.momostudios.coldsweat.client.event;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.momostudios.coldsweat.api.temperature.Temperature;
import dev.momostudios.coldsweat.common.capability.ITemperatureCap;
import dev.momostudios.coldsweat.common.capability.ModCapabilities;
import dev.momostudios.coldsweat.common.capability.PlayerTempCapability;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.model.animation.Animation;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import dev.momostudios.coldsweat.config.ClientSettingsConfig;
import dev.momostudios.coldsweat.util.math.CSMath;

@Mod.EventBusSubscriber(Dist.CLIENT)
public class SelfTempDisplay
{
    public static ITemperatureCap PLAYER_CAP = null;
    static int PLAYER_TEMP = 0;
    static int ICON_BOB = 0;
    static int ICON_BOB_TIMER = 0;

    static int CURRENT_ICON = 0;
    static int PREV_ICON = 0;
    static int TRANSITION_PROGRESS = 0;
    static int BLEND_TIME = 10;

    static int TEMP_SEVERITY = 0;

    @SubscribeEvent
    public static void handleTransition(TickEvent.ClientTickEvent event)
    {
        if (event.phase == TickEvent.Phase.START && Minecraft.getInstance().renderViewEntity instanceof PlayerEntity)
        {
            PlayerEntity player = (PlayerEntity) Minecraft.getInstance().renderViewEntity;
            if (PLAYER_CAP == null || player.ticksExisted % 40 == 0)
                PLAYER_CAP = player.getCapability(ModCapabilities.PLAYER_TEMPERATURE).orElse(new PlayerTempCapability());

            TEMP_SEVERITY = getTempSeverity(PLAYER_TEMP);

            PLAYER_TEMP = (int) PLAYER_CAP.get(Temperature.Types.BODY);

            int neededIcon = (int) CSMath.clamp(TEMP_SEVERITY, -3, 3);

            // Hot Temperatures
            if (CURRENT_ICON != neededIcon)
            {
                CURRENT_ICON = neededIcon;
                TRANSITION_PROGRESS = 0;
            }

            // Tick the transition progress
            if (PREV_ICON != CURRENT_ICON)
            {
                TRANSITION_PROGRESS++;

                if (TRANSITION_PROGRESS >= BLEND_TIME)
                {
                    PREV_ICON = CURRENT_ICON;
                }
            }
        }
    }

    @SubscribeEvent
    public static void eventHandler(RenderGameOverlayEvent.Post event)
    {
        ClientSettingsConfig CCS = ClientSettingsConfig.getInstance();
        Minecraft mc = Minecraft.getInstance();

        if (mc.getRenderViewEntity() instanceof PlayerEntity)
        {
            PlayerEntity player = (PlayerEntity) mc.getRenderViewEntity();

            if (event.getType() == RenderGameOverlayEvent.ElementType.HOTBAR && !player.abilities.isCreativeMode && !player.isSpectator())
            {
                int scaleX = event.getWindow().getScaledWidth();
                int scaleY = event.getWindow().getScaledHeight();
                PlayerEntity entity = (PlayerEntity) Minecraft.getInstance().getRenderViewEntity();

                if (PLAYER_CAP == null || entity.ticksExisted % 40 == 0)
                    PLAYER_CAP = entity.getCapability(ModCapabilities.PLAYER_TEMPERATURE).orElse(new PlayerTempCapability());

                int temp = (int) PLAYER_CAP.get(Temperature.Types.BODY);

                // Get the general severity of the temperature
                int threatLevel;
                switch (CURRENT_ICON)
                {
                    case  2: case -2: threatLevel = 1; break;
                    case  3: case -3: threatLevel = 2; break;
                    default: threatLevel = 0;
                }

                ResourceLocation icon;
                switch (CURRENT_ICON)
                {
                    case  1: icon = new ResourceLocation("cold_sweat:textures/gui/overlay/temp_gauge_hot_0.png");   break;
                    case  2: icon = new ResourceLocation("cold_sweat:textures/gui/overlay/temp_gauge_hot_1.png");   break;
                    case  3: icon = new ResourceLocation("cold_sweat:textures/gui/overlay/temp_gauge_hot_2.png");   break;
                    case -1: icon = new ResourceLocation("cold_sweat:textures/gui/overlay/temp_gauge_cold_0.png");  break;
                    case -2: icon = new ResourceLocation("cold_sweat:textures/gui/overlay/temp_gauge_cold_1.png");  break;
                    case -3: icon = new ResourceLocation("cold_sweat:textures/gui/overlay/temp_gauge_cold_2.png");  break;
                    default: icon = new ResourceLocation("cold_sweat:textures/gui/overlay/temp_gauge_default.png"); break;
                }

                ResourceLocation lastIcon;
                switch (PREV_ICON)
                {
                    case  1: lastIcon = new ResourceLocation("cold_sweat:textures/gui/overlay/temp_gauge_hot_0.png");   break;
                    case  2: lastIcon = new ResourceLocation("cold_sweat:textures/gui/overlay/temp_gauge_hot_1.png");   break;
                    case  3: lastIcon = new ResourceLocation("cold_sweat:textures/gui/overlay/temp_gauge_hot_2.png");   break;
                    case -1: lastIcon = new ResourceLocation("cold_sweat:textures/gui/overlay/temp_gauge_cold_0.png");  break;
                    case -2: lastIcon = new ResourceLocation("cold_sweat:textures/gui/overlay/temp_gauge_cold_1.png");  break;
                    case -3: lastIcon = new ResourceLocation("cold_sweat:textures/gui/overlay/temp_gauge_cold_2.png");  break;
                    default: lastIcon = new ResourceLocation("cold_sweat:textures/gui/overlay/temp_gauge_default.png"); break;
                }

                // Get text color
                int color =
                        PLAYER_TEMP > 0 ? 16744509 :
                        PLAYER_TEMP < 0 ? 4233468 :
                        11513775;

                // Get outline color
                int colorBG =
                        PLAYER_TEMP < 0 ? 1122643 :
                        PLAYER_TEMP > 0 ? 5376516 :
                        0;

                // Get the outer border color when readout is > 100
                int colorBG2;
                switch (TEMP_SEVERITY)
                {
                    case  7: case -7: colorBG2 = 16777215; break;
                    case  6: colorBG2 = 16771509; break;
                    case  5: colorBG2 = 16766325; break;
                    case  4: colorBG2 = 16755544; break;
                    case  3: colorBG2 = 16744509; break;
                    case -3: colorBG2 = 6866175;  break;
                    case -4: colorBG2 = 7390719;  break;
                    case -5: colorBG2 = 9824511;  break;
                    case -6: colorBG2 = 12779519; break;
                    default: colorBG2 = 0; break;
                }

                RenderSystem.defaultBlendFunc();

                // Bob the icon if temperature is critical
                int threatOffset = 0;
                if (CCS.iconBobbing())
                {
                    if (threatLevel == 1) threatOffset = ICON_BOB;
                    else if (threatLevel == 2) threatOffset = entity.ticksExisted % 2 == 0 ? 1 : 0;
                }

                /*
                 Render Icon
                 */
                if (TRANSITION_PROGRESS < BLEND_TIME)
                {
                    mc.getTextureManager().bindTexture(lastIcon);
                    AbstractGui.blit(event.getMatrixStack(), (scaleX / 2) - 5 + CCS.tempIconX(), scaleY - 53 - threatOffset + CCS.tempIconY(), 0, 0, 10, 10, 10, 10);
                    RenderSystem.enableBlend();
                    RenderSystem.color4f(1, 1, 1, (Animation.getPartialTickTime() + TRANSITION_PROGRESS) / BLEND_TIME);
                }
                mc.getTextureManager().bindTexture(icon);
                AbstractGui.blit(event.getMatrixStack(), (scaleX / 2) - 5 + CCS.tempIconX(), scaleY - 53 - threatOffset + CCS.tempIconY(), 0, 0, 10, 10, 10, 10);
                RenderSystem.color4f(1, 1, 1, 1);

                /*
                 Render the temperature readout
                 */
                FontRenderer fontRenderer = mc.fontRenderer;
                int scaledWidth = mc.getMainWindow().getScaledWidth();
                int scaledHeight = mc.getMainWindow().getScaledHeight();
                MatrixStack matrixStack = event.getMatrixStack();

                String s = "" + (int) Math.ceil(Math.min(Math.abs(temp), 100));
                float x = (scaledWidth - fontRenderer.getStringWidth(s)) / 2f + CCS.tempReadoutX();
                float y = scaledHeight - 41f + CCS.tempReadoutY();

                // If temperature is critical, draw the outer border
                if (!CSMath.isBetween(PLAYER_TEMP, -99, 99))
                {
                    fontRenderer.drawString(matrixStack, s, x + 2f, y, colorBG2);
                    fontRenderer.drawString(matrixStack, s, x - 2f, y, colorBG2);
                    fontRenderer.drawString(matrixStack, s, x, y + 2f, colorBG2);
                    fontRenderer.drawString(matrixStack, s, x, y - 2f, colorBG2);
                    fontRenderer.drawString(matrixStack, s, x + 1f, y + 1f, colorBG2);
                    fontRenderer.drawString(matrixStack, s, x + 1f, y - 1f, colorBG2);
                    fontRenderer.drawString(matrixStack, s, x - 1f, y - 1f, colorBG2);
                    fontRenderer.drawString(matrixStack, s, x - 1f, y + 1f, colorBG2);
                }
                // Draw the readout/outline
                fontRenderer.drawString(matrixStack, s, x + 1, y, colorBG);
                fontRenderer.drawString(matrixStack, s, x - 1, y, colorBG);
                fontRenderer.drawString(matrixStack, s, x, y + 1, colorBG);
                fontRenderer.drawString(matrixStack, s, x, y - 1, colorBG);
                fontRenderer.drawString(matrixStack, s, x, y, color);
            }
        }
    }

    @SubscribeEvent
    public static void setRandomIconOffset(TickEvent.ClientTickEvent event)
    {
        ICON_BOB_TIMER++;
        ICON_BOB = Math.random() < 0.3 && ICON_BOB_TIMER >= 3 ? 1 : 0;
        if (ICON_BOB_TIMER >= 3) ICON_BOB_TIMER = 0;
    }

    static int getTempSeverity(int temp)
    {
        return temp >= 140  ?  7
                : temp >= 130  ?  6
                : temp >= 120  ?  5
                : temp >= 110  ?  4
                : temp >= 100  ?  3
                : temp >= 66   ?  2
                : temp >= 33   ?  1
                : temp >= 0    ?  0
                : temp >= -32  ?  0
                : temp >= -65  ? -1
                : temp >= -99  ? -2
                : temp >= -109 ? -3
                : temp >= -119 ? -4
                : temp >= -129 ? -5
                : temp >= -139 ? -6
                : -7;
    }
}

