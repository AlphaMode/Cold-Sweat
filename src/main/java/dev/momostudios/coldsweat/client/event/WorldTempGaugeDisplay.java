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
            int scaleX = event.getWindow().getScaledWidth();
            int scaleY = event.getWindow().getScaledHeight();

            double min = ConfigCache.getInstance().minTemp - MIN_OFFSET;
            double max = ConfigCache.getInstance().maxTemp + MAX_OFFSET;

            boolean bobbing = CLIENT_CONFIG.iconBobbing();

            // Get player ambient temperature
            double temp = CSMath.convertUnits(CLIENT_TEMP, CLIENT_CONFIG.celsius() ? Temperature.Units.C : Temperature.Units.F, Temperature.Units.MC, true);

            // Get the temperature severity
            int severity = getSeverity(temp, min, max);

            // Set text color (white)
            int readoutColor;
            switch (severity)
            {
                case  2: case  3: readoutColor = 16297781;  break;
                case -2: case -3: readoutColor = 8443135;   break;
                case  4    : readoutColor = 16728089;  break;
                case -4    : readoutColor = 4236031;   break;
                default    : readoutColor = 14737376;  break;
            };

            // Set default gauge texture
            String gaugeLocation = "cold_sweat:textures/gui/overlay/world_temp_gauge/";
            ResourceLocation gaugeTexture;
            switch (severity)
            {
                case  1: gaugeTexture = new ResourceLocation(gaugeLocation + "temp_gauge_hot.png");         break;
                case  2: gaugeTexture = new ResourceLocation(gaugeLocation + "temp_gauge_burning_0.png");   break;
                case  3: gaugeTexture = new ResourceLocation(gaugeLocation + "temp_gauge_burning_1.png");   break;
                case  4: gaugeTexture = new ResourceLocation(gaugeLocation + "temp_gauge_burning_2.png");   break;
                case -1: gaugeTexture = new ResourceLocation(gaugeLocation + "temp_gauge_cold.png");        break;
                case -2: gaugeTexture = new ResourceLocation(gaugeLocation + "temp_gauge_freezing_0.png");  break;
                case -3: gaugeTexture = new ResourceLocation(gaugeLocation + "temp_gauge_freezing_1.png");  break;
                case -4: gaugeTexture = new ResourceLocation(gaugeLocation + "temp_gauge_freezing_2.png");  break;
                default: gaugeTexture = new ResourceLocation(gaugeLocation + "temp_gauge_normal.png");      break;
            };

            // Render gauge
            event.getMatrixStack().push();
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            Minecraft.getInstance().getTextureManager().bindTexture(gaugeTexture);
            RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);

            AbstractGui.blit(event.getMatrixStack(), (scaleX / 2) + 94 + CLIENT_CONFIG.tempGaugeX(), scaleY - 19 + CLIENT_CONFIG.tempGaugeY(), 0, 0, 25, 16, 25, 16);

            RenderSystem.disableBlend();

            // Sets the text bobbing offset (or none if disabled)
            int bob = temp > max || temp < min ? (player.ticksExisted % 2 == 0 && bobbing ? 16 : 15) : 15;

            // Render text
            int blendedTemp = (int) CSMath.blend(PREV_CLIENT_TEMP, CLIENT_TEMP, Animation.getPartialTickTime(), 0, 1);

            Minecraft.getInstance().fontRenderer.drawString(event.getMatrixStack(), "" + (blendedTemp + CLIENT_CONFIG.tempOffset()) + "",
            /* X */ scaleX / 2f + 107 + Integer.toString(blendedTemp + CLIENT_CONFIG.tempOffset()).length() * -3 + CLIENT_CONFIG.tempGaugeX(),
            /* Y */ scaleY - bob + CLIENT_CONFIG.tempGaugeY(), readoutColor);
            event.getMatrixStack().pop();
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

    static int getSeverity(double temp, double min, double max)
    {
        double mid = (min + max) / 2;

        return
        (temp > max)
            ? 4
        : (temp > mid + ((max - mid) * 0.75))
            ? 3
        : (temp > mid + ((max - mid) * 0.5))
            ? 2
        : (temp > mid + ((max - mid) * 0.25))
            ? 1
        : (temp >= mid - ((mid - min) * 0.25))
            ? 0
        : (temp >= mid - ((mid - min) * 0.5))
            ? -1
        : (temp >= mid - ((mid - min) * 0.75))
            ? -2
        : (temp >= min)
            ? -3
        : -4;
    }
}
