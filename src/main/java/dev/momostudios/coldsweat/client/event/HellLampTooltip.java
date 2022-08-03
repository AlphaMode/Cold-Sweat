package dev.momostudios.coldsweat.client.event;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.momostudios.coldsweat.util.config.ConfigHelper;
import dev.momostudios.coldsweat.util.config.LoadedValue;
import dev.momostudios.coldsweat.util.registries.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.client.gui.screen.inventory.CreativeScreen;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import dev.momostudios.coldsweat.config.ItemSettingsConfig;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(Dist.CLIENT)
public class HellLampTooltip
{
    static int FUEL_FADE_TIMER = 0;
    public static LoadedValue<List<Item>> VALID_FUEL = LoadedValue.of(() ->
    {
        List<Item> list = new ArrayList<>();
        for (String itemID : ItemSettingsConfig.getInstance().soulLampItems())
        {
            list.addAll(ConfigHelper.getItems(itemID));
        }
        return list;
    });

    @SubscribeEvent
    public static void renderLanternTooltip(GuiScreenEvent.DrawScreenEvent.Post event)
    {
        if (event.getGui() instanceof ContainerScreen)
        {
            ContainerScreen<?> inventoryScreen = (ContainerScreen<?>) event.getGui();
            if (Minecraft.getInstance().player != null && inventoryScreen.getSlotUnderMouse() != null
            && inventoryScreen.getSlotUnderMouse().getStack().getItem() == ModItems.HELLSPRING_LAMP)
            {
                PlayerEntity player = Minecraft.getInstance().player;
                float fuel = inventoryScreen.getSlotUnderMouse().getStack().getOrCreateTag().getFloat("fuel");
                ItemStack carriedStack = player.inventory.getItemStack();

                if (!carriedStack.isEmpty() && VALID_FUEL.get().contains(carriedStack.getItem()))
                {
                    int fuelValue = player.inventory.getItemStack().getCount();
                    int slotX = inventoryScreen.getSlotUnderMouse().xPos + ((ContainerScreen<?>) event.getGui()).getGuiLeft();
                    int slotY = inventoryScreen.getSlotUnderMouse().yPos + ((ContainerScreen<?>) event.getGui()).getGuiTop();

                    MatrixStack ms = event.getMatrixStack();

                    // If the mouse is above the slot, move the box to the bottom
                    if (event.getMouseY() < slotY + 8)
                        ms.translate(0, 32, 0);

                    event.getGui().renderTooltip(ms, new StringTextComponent("       "), slotX - 18, slotY);

                    RenderSystem.defaultBlendFunc();

                    // Render background
                    Minecraft.getInstance().textureManager.bindTexture(new ResourceLocation("cold_sweat:textures/gui/tooltip/soulspring_lamp_fuel.png"));
                    AbstractGui.blit(ms, slotX - 7, slotY - 12, 401, 0, 0, 30, 8, 24, 30);

                    // Render ghost overlay
                    RenderSystem.enableBlend();
                    RenderSystem.color4f(1f, 1f, 1f, 0.15f + (float) ((Math.sin(FUEL_FADE_TIMER / 5f) + 1f) / 2f) * 0.4f);
                    AbstractGui.blit(ms, slotX - 7, slotY - 12, 401, 0, 8, Math.min(30, (int) ((fuel + fuelValue) / 2.1333f)), 8, 24, 30);
                    RenderSystem.disableBlend();

                    // Render current fuel
                    RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1f);
                    AbstractGui.blit(ms, slotX - 7, slotY - 12, 401, 0, 16, (int) (fuel / 2.1333f), 8, 24, 30);
                }
                else if (carriedStack.isEmpty())
                {
                    int mouseX = event.getMouseX();
                    int mouseY = event.getMouseY();
                    MatrixStack ms = event.getMatrixStack();

                    if (event.getGui() instanceof CreativeScreen && ((CreativeScreen) event.getGui()).getSelectedTabIndex() == ItemGroup.SEARCH.getIndex())
                    {
                        event.getMatrixStack().translate(0, 10, 0);
                    }

                    Minecraft.getInstance().textureManager.bindTexture(new ResourceLocation("cold_sweat:textures/gui/tooltip/soulspring_lamp_fuel.png"));
                    AbstractGui.blit(ms, mouseX + 11, mouseY, 401, 0, 0, 30, 8, 24, 30);
                    AbstractGui.blit(ms, mouseX + 11, mouseY, 401, 0, 16, (int) (fuel / 2.1333f), 8, 24, 30);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event)
    {
        if (event.phase == TickEvent.Phase.END)
        {
            FUEL_FADE_TIMER = (FUEL_FADE_TIMER + 1) % (int) (Math.PI * 10);
        }
    }
}
