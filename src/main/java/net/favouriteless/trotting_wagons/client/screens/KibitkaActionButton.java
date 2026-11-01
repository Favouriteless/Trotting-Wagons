package net.favouriteless.trotting_wagons.client.screens;

import net.favouriteless.trotting_wagons.common.entities.wagons.KibitkaWagon;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

/** Camp-only kibitka button. The icon is centered; the label stays in the tooltip. */
public class KibitkaActionButton extends Button {

    private final KibitkaWagon wagon;
    private final ItemStack icon;
    private final List<Component> lines;
    private long hoverStart = -1L;

    public KibitkaActionButton(int x, int y, int size, KibitkaWagon wagon, ItemLike icon, String label, String tip, CustomPacketPayload payload) {
        super(x, y, size, size, Component.empty(), button -> PacketDistributor.sendToServer(payload), DEFAULT_NARRATION);
        this.wagon = wagon;
        this.icon = new ItemStack(icon);
        Component labelText = Component.translatable(label);
        setMessage(labelText);
        this.lines = label.equals(tip) ? List.of(labelText) : List.of(labelText, Component.translatable(tip));
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        active = wagon.isCamped();
        int iconX = getX() + (width - 16) / 2;
        int iconY = getY() + (height - 16) / 2;
        graphics.renderItem(icon, iconX, iconY);
        if(!active)
            graphics.fill(RenderType.guiOverlay(), getX(), getY(), getX() + width, getY() + height, 0x88000000);
        drawLines(graphics, mouseX, mouseY);
    }

    private void drawLines(GuiGraphics graphics, int mouseX, int mouseY) {
        if(!isHovered()) {
            hoverStart = -1L;
            return;
        }
        if(hoverStart < 0)
            hoverStart = Util.getMillis();
        if(Util.getMillis() - hoverStart < TooltipDelay.MILLIS)
            return;
        graphics.renderComponentTooltip(Minecraft.getInstance().font, lines, mouseX, mouseY);
    }

}
