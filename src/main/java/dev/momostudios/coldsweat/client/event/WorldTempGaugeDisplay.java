package dev.momostudios.coldsweat.client.event;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.momostudios.coldsweat.api.temperature.Temperature;
import dev.momostudios.coldsweat.common.capability.ModCapabilities;
import dev.momostudios.coldsweat.config.ClientSettingsConfig;
import dev.momostudios.coldsweat.config.ConfigCache;
import dev.momostudios.coldsweat.util.math.CSMath;
import dev.momostudios.coldsweat.util.registries.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.model.animation.Animation;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import dev.momostudios.coldsweat.api.temperature.Temperature.Units;

@Mod.EventBusSubscriber(Dist.CLIENT)
public class WorldTempGaugeDisplay
{
    private static double PREV_CLIENT_TEMP = 0;
    public static double CLIENT_TEMP = 0;

    public static double MAX_OFFSET = 0;
    public static double MIN_OFFSET = 0;

    static ClientSettingsConfig CLIENT_CONFIG = ClientSettingsConfig.getInstance();

    @SubscribeEvent
    public static void renderAmbientTemperature(RenderGameOverlayEvent.Post event)
    {
        PlayerEntity player = Minecraft.getInstance().player;

        if (event.getType() == RenderGameOverlayEvent.ElementType.ALL &&
        (CSMath.isBetween(player.inventory.getSlotFor(new ItemStack(ModItems.THERMOMETER)), 0, 8) ||
        player.getHeldItemOffhand().getItem()  == ModItems.THERMOMETER || !ConfigCache.getInstance().showWorldTemp))
        {
            // Variables
            int scaleX = event.getWindow().getScaledWidth();
            int scaleY = event.getWindow().getScaledHeight();
            double min = ConfigCache.getInstance().minTemp - MIN_OFFSET;
            double max = ConfigCache.getInstance().maxTemp + MAX_OFFSET;
            double mid = (min + max) / 2;
            boolean bobbing = CLIENT_CONFIG.iconBobbing();

            // Get player ambient temperature
            double temp = CSMath.convertUnits(CLIENT_TEMP, CLIENT_CONFIG.celsius() ? Temperature.Units.C : Temperature.Units.F, Temperature.Units.MC, true);

            // Set default color (white)
            int color = 14737376;

            // Set gauge texture based on temperature
            ResourceLocation gaugeTexture;
            String gaugeLocation = "cold_sweat:textures/gui/overlay/ambient/";

            if (temp > max)
                gaugeTexture = new ResourceLocation(gaugeLocation + "temp_gauge_burning_2.png");
            else if (temp > mid + ((max - mid) * 0.75))
                gaugeTexture = new ResourceLocation(gaugeLocation + "temp_gauge_burning_1.png");
            else if (temp > mid + ((max - mid) * 0.5))
                gaugeTexture = new ResourceLocation(gaugeLocation + "temp_gauge_burning_0.png");
            else if (temp > mid + ((max - mid) * 0.25))
                gaugeTexture = new ResourceLocation(gaugeLocation + "temp_gauge_hot.png");
            else if (temp >= mid - ((mid - min) * 0.25))
                gaugeTexture = new ResourceLocation(gaugeLocation + "temp_gauge_normal.png");
            else if (temp >= mid - ((mid - min) * 0.5))
                gaugeTexture = new ResourceLocation(gaugeLocation + "temp_gauge_cold.png");
            else if (temp >= mid - ((mid - min) * 0.75))
                gaugeTexture = new ResourceLocation(gaugeLocation + "temp_gauge_freezing_0.png");
            else if (temp >= min)
                gaugeTexture = new ResourceLocation(gaugeLocation + "temp_gauge_freezing_1.png");
            else
                gaugeTexture = new ResourceLocation(gaugeLocation + "temp_gauge_freezing_2.png");

            // Render gauge
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();

            Minecraft.getInstance().getTextureManager().bindTexture(gaugeTexture);
            RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
            AbstractGui.blit(event.getMatrixStack(), (scaleX / 2) + 94 + CLIENT_CONFIG.tempGaugeX(), scaleY - 19 + CLIENT_CONFIG.tempGaugeY(), 0, 0, 25, 16, 25, 16);

            RenderSystem.disableBlend();

            // Set text based on temperature
            if (temp > (mid + max) / 2 && temp <= max)
                color = 16297781;
            else if (temp > max)
                color = 16728089;
            else if (temp < (mid + min) / 2 && temp >= min)
                color = 8443135;
            else if (temp < min)
                color = 4236031;

            // Sets the text bobbing offset (or none if disabled)
            int bob = temp > max || temp < min ? (player.ticksExisted % 2 == 0 && bobbing ? 16 : 15) : 15;

            // Render text
            int blendedTemp = (int) CSMath.blend(PREV_CLIENT_TEMP, CLIENT_TEMP, Animation.getPartialTickTime(), 0, 1);

            Minecraft.getInstance().fontRenderer.drawString(event.getMatrixStack(), "" + (blendedTemp + CLIENT_CONFIG.tempOffset()) + "",
            /* X */ scaleX / 2f + 107 + Integer.toString(blendedTemp + CLIENT_CONFIG.tempOffset()).length() * -3 + CLIENT_CONFIG.tempGaugeX(),
            /* Y */ scaleY - bob + CLIENT_CONFIG.tempGaugeY(), color);
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event)
    {
        if (Minecraft.getInstance().renderViewEntity instanceof PlayerEntity)
        {
            PlayerEntity player = (PlayerEntity) Minecraft.getInstance().renderViewEntity;
            player.getCapability(ModCapabilities.PLAYER_TEMPERATURE).ifPresent(cap ->
            {
                boolean celsius = CLIENT_CONFIG.celsius();

                double tempReadout = CSMath.convertUnits(cap.get(Temperature.Types.WORLD), Units.MC, celsius ? Units.C : Units.F, true);
                PREV_CLIENT_TEMP = CLIENT_TEMP;

                CLIENT_TEMP = CLIENT_TEMP + (tempReadout - CLIENT_TEMP) / 10.0;

                MAX_OFFSET = cap.get(Temperature.Types.MAX);
                MIN_OFFSET = cap.get(Temperature.Types.MIN);
            });
        }
    }
}
