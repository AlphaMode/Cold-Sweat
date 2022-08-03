package dev.momostudios.coldsweat.client.event;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.momostudios.coldsweat.api.temperature.Temperature;
import dev.momostudios.coldsweat.common.capability.ITemperatureCap;
import dev.momostudios.coldsweat.common.capability.ModCapabilities;
import dev.momostudios.coldsweat.common.capability.PlayerTempCap;
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

    // Stuff for body temperature
    static boolean SHOW_BODY_TEMP = false;
    static double BODY_TEMP = 0;
    static double PREV_BODY_TEMP = 0;
    static int BLEND_BODY_TEMP = 0;
    static int ICON_BOB = 0;
    static int BODY_ICON = 0;
    static int PREV_BODY_ICON = 0;
    static int BODY_TRANSITION_PROGRESS = 0;
    static int BODY_BLEND_TIME = 10;
    static int BODY_TEMP_SEVERITY = 0;

    static ClientSettingsConfig CLIENT_CONFIG = ClientSettingsConfig.getInstance();

    @SubscribeEvent
    public static void handleTransition(TickEvent.ClientTickEvent event)
    {
        if (event.phase == TickEvent.Phase.START && Minecraft.getInstance().renderViewEntity instanceof PlayerEntity)
        {
            PlayerEntity player = (PlayerEntity) Minecraft.getInstance().renderViewEntity;

            if (PLAYER_CAP == null || player.ticksExisted % 40 == 0)
                PLAYER_CAP = player.getCapability(ModCapabilities.PLAYER_TEMPERATURE).orElse(new PlayerTempCap());

            SHOW_BODY_TEMP = !player.abilities.isCreativeMode && !player.isSpectator();

            if (SHOW_BODY_TEMP)
            {
                // Get icon bob
                ICON_BOB = player.ticksExisted % 3 == 0 && Math.random() < 0.3 ? 1 : 0;

                // Blend body temp (per tick)
                PREV_BODY_TEMP = BODY_TEMP;
                BODY_TEMP += (PLAYER_CAP.get(Temperature.Types.BODY) - BODY_TEMP) / 5;

                // Get the severity of the player's body temperature
                BODY_TEMP_SEVERITY = getTempSeverity(BLEND_BODY_TEMP);
            }


            BLEND_BODY_TEMP = (int) CSMath.blend(PREV_BODY_TEMP, BODY_TEMP, Animation.getPartialTickTime(), 0, 1);

            int neededIcon = (int) CSMath.clamp(BODY_TEMP_SEVERITY, -3, 3);

            // Hot Temperatures
            if (BODY_ICON != neededIcon)
            {
                BODY_ICON = neededIcon;
                BODY_TRANSITION_PROGRESS = 0;
            }

            // Tick the transition progress
            if (PREV_BODY_ICON != BODY_ICON)
            {
                BODY_TRANSITION_PROGRESS++;

                if (BODY_TRANSITION_PROGRESS >= BODY_BLEND_TIME)
                {
                    PREV_BODY_ICON = BODY_ICON;
                }
            }
        }
    }

    @SubscribeEvent
    public static void eventHandler(RenderGameOverlayEvent.Post event)
    {
        Minecraft mc = Minecraft.getInstance();

        if (mc.getRenderViewEntity() instanceof PlayerEntity)
        {
            PlayerEntity player = (PlayerEntity) mc.getRenderViewEntity();

            if (event.getType() == RenderGameOverlayEvent.ElementType.HOTBAR && SHOW_BODY_TEMP)
            {
                int width = event.getWindow().getScaledWidth();
                int height = event.getWindow().getScaledHeight();
                MatrixStack ms = event.getMatrixStack();

                if (PLAYER_CAP == null || player.ticksExisted % 40 == 0)
                    PLAYER_CAP = player.getCapability(ModCapabilities.PLAYER_TEMPERATURE).orElse(new PlayerTempCap());

                // Blend body temp (per frame)
                BLEND_BODY_TEMP = (int) CSMath.blend(PREV_BODY_TEMP, BODY_TEMP, Animation.getPartialTickTime(), 0, 1);

                // Get text color
                int color = BLEND_BODY_TEMP > 0 ? 16744509
                        : BLEND_BODY_TEMP < 0 ? 4233468
                        : 11513775;
                switch (BODY_TEMP_SEVERITY)
                {
                    case  7: case -7: color = 16777215; break;
                    case  6: color = 16777132; break;
                    case  5: color = 16767856; break;
                    case  4: color = 16759634; break;
                    case  3: color = 16751174; break;
                    case -3: color = 6078975; break;
                    case -4: color = 7528447; break;
                    case -5: color = 8713471; break;
                    case -6: color = 11599871; break;
                };

                // Get the outer border color when readout is > 100
                int colorBG =
                        BLEND_BODY_TEMP < 0 ? 1122643 :
                        BLEND_BODY_TEMP > 0 ? 5376516 :
                        0;


                int bobLevel = Math.min(Math.abs(BODY_TEMP_SEVERITY), 3);
                int threatOffset =
                        !CLIENT_CONFIG.iconBobbing() ? 0
                        : bobLevel == 2 ? ICON_BOB
                        : bobLevel == 3 ? player.ticksExisted % 2
                        : 0;


                /*
                 Render Icon
                 */

                RenderSystem.defaultBlendFunc();
                mc.getTextureManager().bindTexture(new ResourceLocation("cold_sweat:textures/gui/overlay/body_temp_gauge.png"));

                if (BODY_TRANSITION_PROGRESS < BODY_BLEND_TIME)
                {
                    AbstractGui.blit(ms, (width / 2) - 5 + CLIENT_CONFIG.tempIconX(), height - 53 - threatOffset + CLIENT_CONFIG.tempIconY(), 0, 30 - PREV_BODY_ICON * 10, 10, 10, 10, 70);
                    RenderSystem.enableBlend();
                    RenderSystem.color4f(1, 1, 1, (Animation.getPartialTickTime() + BODY_TRANSITION_PROGRESS) / BODY_BLEND_TIME);
                }
                // Render new icon on top of old icon (if blending)
                // Otherwise this is just the regular icon
                AbstractGui.blit(ms, (width / 2) - 5 + CLIENT_CONFIG.tempIconX(), height - 53 - threatOffset + CLIENT_CONFIG.tempIconY(), 0, 30 - BODY_ICON * 10, 10, 10, 10, 70);
                RenderSystem.color4f(1, 1, 1, 1);

                /*
                 Render the temperature readout
                 */
                FontRenderer fontRenderer = mc.fontRenderer;
                int scaledWidth = mc.getMainWindow().getScaledWidth();
                int scaledHeight = mc.getMainWindow().getScaledHeight();
                MatrixStack matrixStack = event.getMatrixStack();

                String s = "" + Math.min(Math.abs(BLEND_BODY_TEMP), 100);
                float x = (scaledWidth - fontRenderer.getStringWidth(s)) / 2f + CLIENT_CONFIG.tempReadoutX();
                float y = scaledHeight - 31f - 10f + CLIENT_CONFIG.tempReadoutY();

                // Draw the outline
                fontRenderer.drawString(matrixStack, s, x + 1, y, colorBG);
                fontRenderer.drawString(matrixStack, s, x - 1, y, colorBG);
                fontRenderer.drawString(matrixStack, s, x, y + 1, colorBG);
                fontRenderer.drawString(matrixStack, s, x, y - 1, colorBG);

                // Draw the readout
                fontRenderer.drawString(matrixStack, s, x, y, color);
            }
        }
    }

    static int getTempSeverity(int temp)
    {
        int sign = CSMath.getSign(temp);
        int absTemp = Math.abs(temp);

        return
          absTemp < 100 ? (int) Math.floor(CSMath.blend(0, 3, absTemp, 0, 100)) * sign
        : (int) CSMath.blend(3, 7, absTemp, 100, 150) * sign;
    }

    public static void setBodyTemp(double temp)
    {
        BODY_TEMP = temp;
        PREV_BODY_TEMP = temp;
        BLEND_BODY_TEMP = (int) temp;
    }
}

