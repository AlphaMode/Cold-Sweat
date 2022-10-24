package dev.momostudios.coldsweat.client.event;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.momostudios.coldsweat.api.temperature.Temperature;
import dev.momostudios.coldsweat.common.capability.ITemperatureCap;
import dev.momostudios.coldsweat.common.capability.ModCapabilities;
import dev.momostudios.coldsweat.common.capability.PlayerTempCap;
import dev.momostudios.coldsweat.config.ClientSettingsConfig;
import dev.momostudios.coldsweat.util.config.ConfigSettings;
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
    public static ITemperatureCap PLAYER_CAP = null;
    public static boolean SHOW_WORLD_TEMP = false;
    public static double WORLD_TEMP = 0;
    public static double TRUE_WORLD_TEMP = 0;
    static double PREV_WORLD_TEMP = 0;

    public static double MAX_OFFSET = 0;
    public static double MIN_OFFSET = 0;

    static ClientSettingsConfig CLIENT_CONFIG = ClientSettingsConfig.getInstance();

    @SubscribeEvent
    public static void renderAmbientTemperature(RenderGameOverlayEvent.Post event)
    {
        PlayerEntity player = Minecraft.getInstance().player;

        if (player != null && event.getType() == RenderGameOverlayEvent.ElementType.ALL && SHOW_WORLD_TEMP)
        {
            MatrixStack ms = event.getMatrixStack();

            int width = event.getWindow().getScaledWidth();
            int height = event.getWindow().getScaledHeight();

            double min = ConfigSettings.getInstance().minTemp - MIN_OFFSET;
            double max = ConfigSettings.getInstance().maxTemp + MAX_OFFSET;

                // Get player ambient temperature
            double temp = CSMath.convertUnits(WORLD_TEMP, CLIENT_CONFIG.celsius() ? Temperature.Units.C : Temperature.Units.F, Temperature.Units.MC, true);

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

            // Render gauge
            ms.push();
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);

            // Set gauge texture
            Minecraft.getInstance().getTextureManager().bindTexture(new ResourceLocation("cold_sweat:textures/gui/overlay/world_temp_gauge.png"));

            AbstractGui.blit(ms, (width / 2) + 94 + CLIENT_CONFIG.tempGaugeX(), height - 19 + CLIENT_CONFIG.tempGaugeY(), 0, 64 - severity * 16, 25, 16, 25, 144);

            RenderSystem.disableBlend();

            // Sets the text bobbing offset (or none if disabled)
            int bob = CLIENT_CONFIG.iconBobbing() && !CSMath.isInRange(temp, min, max) && player.ticksExisted % 2 == 0 ? 1 : 0;

            // Render text
            int blendedTemp = (int) CSMath.blend(PREV_WORLD_TEMP, WORLD_TEMP, Animation.getPartialTickTime(), 0, 1);

            Minecraft.getInstance().fontRenderer.drawString(ms, "" + (blendedTemp + CLIENT_CONFIG.tempOffset()) + "",
            /* X */ width / 2f + 107 + Integer.toString(blendedTemp + CLIENT_CONFIG.tempOffset()).length() * -3 + CLIENT_CONFIG.tempGaugeX(),
            /* Y */ height - 15 - bob + CLIENT_CONFIG.tempGaugeY(), readoutColor);
            ms.pop();
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event)
    {
        if (event.phase == TickEvent.Phase.START && Minecraft.getInstance().renderViewEntity instanceof PlayerEntity)
        {
            PlayerEntity player = (PlayerEntity) Minecraft.getInstance().renderViewEntity;

            // Ensure player temp capability is stored
            if (PLAYER_CAP == null || player.ticksExisted % 40 == 0)
            {
                PLAYER_CAP = player.getCapability(ModCapabilities.PLAYER_TEMPERATURE).orElse(new PlayerTempCap());
            }

            SHOW_WORLD_TEMP = !ConfigSettings.getInstance().requireThermometer
                           || CSMath.isInRange(player.inventory.getSlotFor(new ItemStack(ModItems.THERMOMETER)), 0, 8)
                           || player.getHeldItemOffhand().getItem()  == ModItems.THERMOMETER;

            if (SHOW_WORLD_TEMP)
            {
                boolean celsius = CLIENT_CONFIG.celsius();

                // Get temperature in actual degrees
                TRUE_WORLD_TEMP = PLAYER_CAP.getTemp(Temperature.Type.WORLD);
                double realTemp = CSMath.convertUnits(TRUE_WORLD_TEMP, Units.MC, celsius ? Units.C : Units.F, true);

                // Calculate the blended world temp for this tick
                PREV_WORLD_TEMP = WORLD_TEMP;
                WORLD_TEMP += (realTemp - WORLD_TEMP) / 6.0;

                // Update max/min offset
                MAX_OFFSET = PLAYER_CAP.getTemp(Temperature.Type.MAX);
                MIN_OFFSET = PLAYER_CAP.getTemp(Temperature.Type.MIN);
            }
        }
    }

    static int getSeverity(double temp, double min, double max)
    {
        return (int) CSMath.blend(-4, 4, temp, min, max);
    }
}
