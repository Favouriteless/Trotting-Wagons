package net.favouriteless.trotting_wagons.client.screens;

import net.favouriteless.trotting_wagons.common.entities.wagons.KibitkaWagon;
import net.favouriteless.trotting_wagons.common.init.TWItems;
import net.favouriteless.trotting_wagons.common.network.packets.ToggleKibitkaCampPayload;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

/** Painted house while camped. On the road the house is covered by the ordinary wheel. */
public class KibitkaCampButton extends Button {

    private static final int SIZE = 26;

    private final KibitkaWagon wagon;
    private final ItemStack wheel = new ItemStack(TWItems.WHEEL.get());
    private String shown = "";
    private List<Component> lines = List.of();
    private long hoverStart = -1L;

    public KibitkaCampButton(int x, int y, KibitkaWagon wagon) {
        super(x, y, SIZE, SIZE, Component.empty(), button -> PacketDistributor.sendToServer(new ToggleKibitkaCampPayload(wagon.getId())), DEFAULT_NARRATION);
        this.wagon = wagon;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        boolean camped = wagon.campTarget() > 0;
        boolean settled = wagon.campSettled();
        active = settled;
        String action = camped ? "gui.trotting_wagons.kibitka.road" : "gui.trotting_wagons.kibitka.camp";
        String tip = settled
                ? (camped ? "gui.trotting_wagons.kibitka.road.tip" : "gui.trotting_wagons.kibitka.camp.tip")
                : "gui.trotting_wagons.kibitka.wait";
        String key = action + "|" + tip;
        if(!key.equals(shown)) {
            shown = key;
            setMessage(Component.translatable(action));
            lines = settled
                    ? List.of(Component.translatable(action), Component.translatable(tip))
                    : List.of(Component.translatable(tip));
        }
        if(!camped) {
            graphics.fill(getX() + 4, getY() + 4, getX() + 23, getY() + 21, 0xFF8B8B8B);
            graphics.renderItem(wheel, getX() + (width - 16) / 2, getY() + (height - 16) / 2);
        }
        if(!active)
            graphics.fill(RenderType.guiOverlay(), getX(), getY(), getX() + width, getY() + height, 0x88000000);
        if(!isHovered()) {
            hoverStart = -1L;
            return;
        }
        if(hoverStart < 0)
            hoverStart = Util.getMillis();
        if(Util.getMillis() - hoverStart < TooltipDelay.MILLIS || lines.isEmpty())
            return;
        graphics.renderComponentTooltip(Minecraft.getInstance().font, lines, mouseX, mouseY);
    }

}
