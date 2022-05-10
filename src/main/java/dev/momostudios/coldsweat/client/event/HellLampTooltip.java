package dev.momostudios.coldsweat.client.event;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.momostudios.coldsweat.util.config.ConfigEntry;
import dev.momostudios.coldsweat.util.registries.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.client.gui.screen.inventory.CreativeScreen;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import dev.momostudios.coldsweat.config.ItemSettingsConfig;

@Mod.EventBusSubscriber(Dist.CLIENT)
public class HellLampTooltip
{
    static int time = 0;

    @SubscribeEvent
    public static void renderLanternTooltip(GuiScreenEvent.DrawScreenEvent.Post event)
    {
        if (event.getGui() instanceof ContainerScreen)
        {
            ContainerScreen<?> inventoryScreen = (ContainerScreen<?>) event.getGui();
            if (inventoryScreen.getSlotUnderMouse() != null && inventoryScreen.getSlotUnderMouse().getStack().getItem() == ModItems.HELLSPRING_LAMP)
            {
                int fuelValue = getItemEntry(Minecraft.getInstance().player.inventory.getItemStack()).value * Minecraft.getInstance().player.inventory.getItemStack().getCount();
                if (!Minecraft.getInstance().player.inventory.getItemStack().isEmpty())
                {
                    if (fuelValue > 0)
                    {
                        float fuel = inventoryScreen.getSlotUnderMouse().getStack().getOrCreateTag().getFloat("fuel");
                        int slotX = inventoryScreen.getSlotUnderMouse().xPos + ((ContainerScreen<?>) event.getGui()).getGuiLeft();
                        int slotY = inventoryScreen.getSlotUnderMouse().yPos + ((ContainerScreen<?>) event.getGui()).getGuiTop();

                        MatrixStack ms = event.getMatrixStack();
                        Matrix4f matrix = ms.getLast().getMatrix();
                        if (event.getMouseY() < slotY + 8)
                            matrix.translate(new Vector3f(0, 32, 0));
                        event.getGui().renderTooltip(ms, new StringTextComponent("       "), slotX - 18, slotY);

                        Minecraft.getInstance().textureManager.bindTexture(new ResourceLocation("cold_sweat:textures/gui/screen/soulfire_lamp_fuel_empty.png"));
                        event.getGui().blit(ms, slotX - 7, slotY - 12, 400, 0, 0, 30, 8, 8, 30);
                        RenderSystem.enableBlend();
                        RenderSystem.defaultBlendFunc();
                        RenderSystem.color4f(1f, 1f, 1f, 0.15f + (float) ((Math.sin(time / 5f) + 1f) / 2f) * 0.4f);
                        Minecraft.getInstance().textureManager.bindTexture(new ResourceLocation("cold_sweat:textures/gui/screen/soulfire_lamp_fuel_ghost.png"));
                        event.getGui().blit(ms, slotX - 7, slotY - 12, 400, 0, 0, Math.min(30, (int) ((fuel + fuelValue) / 2.1333f)), 8, 8, 30);
                        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1f);
                        Minecraft.getInstance().textureManager.bindTexture(new ResourceLocation("cold_sweat:textures/gui/screen/soulfire_lamp_fuel.png"));
                        event.getGui().blit(ms, slotX - 7, slotY - 12, 400, 0, 0, (int) (fuel / 2.1333f), 8, 8, 30);
                    }
                }
                else
                {
                    int mouseX = event.getMouseX();
                    int mouseY = event.getMouseY();
                    MatrixStack ms = event.getMatrixStack();
                    float fuel = inventoryScreen.getSlotUnderMouse().getStack().getOrCreateTag().getFloat("fuel");

                    if (event.getGui() instanceof CreativeScreen && ((CreativeScreen) event.getGui()).getSelectedTabIndex() == ItemGroup.SEARCH.getIndex())
                    {
                        event.getMatrixStack().translate(0, 10, 0);
                    }

                    Minecraft.getInstance().textureManager.bindTexture(new ResourceLocation("cold_sweat:textures/gui/screen/soulfire_lamp_fuel_empty.png"));
                    event.getGui().blit(ms, mouseX + 12, mouseY, 400, 0, 0, 30, 8, 8, 30);
                    Minecraft.getInstance().textureManager.bindTexture(new ResourceLocation("cold_sweat:textures/gui/screen/soulfire_lamp_fuel.png"));
                    event.getGui().blit(ms, mouseX + 12, mouseY, 400, 0, 0, (int) (fuel / 2.1333f), 8, 8, 30);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event)
    {
        if (event.phase == TickEvent.Phase.END)
        {
            time = (time + 1) % (int) (5 * (Math.PI * 2));
        }
    }

    public static ConfigEntry getItemEntry(ItemStack stack)
    {
        for (String entry : ItemSettingsConfig.getInstance().soulLampItems())
        {
            if (entry.equals(ForgeRegistries.ITEMS.getKey(stack.getItem()).toString()))
            {
                return new ConfigEntry(entry, 1);
            }
        }
        return new ConfigEntry(ForgeRegistries.ITEMS.getKey(Items.AIR).toString(), 0);
    }
}
