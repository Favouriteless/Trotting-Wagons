package net.favouriteless.trotting_wagons.client.screens;

import net.favouriteless.trotting_wagons.common.entities.base.AbstractWagon;
import net.favouriteless.trotting_wagons.common.menus.AbstractWagonMenu;
import net.favouriteless.trotting_wagons.common.menus.RepairSlot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

import java.util.List;

public abstract class AbstractWagonScreen<T extends AbstractWagonMenu> extends AbstractContainerScreen<T> {

    private final ResourceLocation texture;
    private final int texWidth;
    private final int texHeight;
    private static final int SEAT_STRIP = 60;
    private final TooltipDelay tooltips = new TooltipDelay();
    private List<SeatButton> seatButtons;

    public AbstractWagonScreen(T menu, Inventory inventory, Component title, ResourceLocation texture, int texWidth, int texHeight) {
        super(menu, inventory, title);
        this.texture = texture;
        this.texWidth = texWidth;
        this.texHeight = texHeight;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if(SeatButton.press(mouseX, mouseY, button))
            return true;
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        SeatButton.markDrag(mouseX, mouseY);
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if(SeatButton.finishDrag(mouseX, mouseY))
            return true;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        SeatButton.renderCarried(graphics, mouseX, mouseY);
        renderTooltip(graphics, mouseX, mouseY);
        if(seatButtons == null)
            return;
        for(SeatButton button : seatButtons) {
            button.renderDelayedTooltip(graphics, font, mouseX, mouseY);
        }
    }

    protected int seatBarY() {
        return -1;
    }

    protected int artWidth() {
        return imageWidth;
    }

    protected int inventoryArtY() {
        return seatBarY() - 1;
    }

    @Override
    protected void init() {
        super.init();
        int bar = seatBarY();
        AbstractWagon wagon = menu.getWagon();
        if(bar >= 0 && wagon != null && wagon.hasFixedSeats())
            seatButtons = SeatButtons.create(wagon, leftPos, topPos + bar + 18, imageWidth, topPos + bar + 18);
        if(seatButtons != null) {
            for(SeatButton button : seatButtons)
                addRenderableWidget(button);
        }
        addWagonLock(wagon);
    }

    protected void addWagonLock(AbstractWagon wagon) {
        if(wagon != null && minecraft != null && minecraft.player != null && wagon.isOwnedBy(minecraft.player))
            addRenderableWidget(new WagonLockButton(leftPos + 6, topPos + imageHeight - 22, wagon));
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        WagonGui.drawHeader(graphics, font, menu.getWagon(), titleLabelY, imageWidth);
        WagonGui.drawRepairCaption(graphics, font, menu, menu.getWagon());
    }

    @Override
    protected void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        if(hoveredSlot != null && hoveredSlot.hasItem()) {
            tooltips.ready(null);
            super.renderTooltip(graphics, mouseX, mouseY);
            return;
        }
        if(!(hoveredSlot instanceof RepairSlot repair) || !tooltips.ready(hoveredSlot))
            return;
        AbstractWagon wagon = menu.getWagon();
        if(wagon == null)
            return;
        Component tip = wagon.repairTooltip(repair.getSlotIndex());
        if(tip != null)
            graphics.renderTooltip(font, tip, mouseX, mouseY);
    }

    @Override
    protected void containerTick() {
        placeRepair();
        super.containerTick();
        if(seatButtons != null && menu.getWagon() != null)
            SeatButtons.refresh(seatButtons);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        placeRepair();
        int bar = seatBarY();
        if(bar < 0) {
            graphics.blit(texture, leftPos, topPos, 0, 0, imageWidth, imageHeight, texWidth, texHeight);
            return;
        }

        int gap = SEAT_STRIP;
        int art = Math.min(artWidth(), imageWidth);
        int artX = leftPos + (imageWidth - art) / 2;
        graphics.blit(texture, artX, topPos, 0, 0, art, bar, texWidth, texHeight);
        drawSeatStrip(graphics, artX, art);
        int restTop = topPos + bar + gap - 1;
        int rest = imageHeight - (bar + gap - 1);
        if(rest > 0)
            graphics.blit(texture, artX, restTop, 0, inventoryArtY(), art, rest, texWidth, texHeight);
        WagonGui.drawWheelCaption(graphics, font, menu, menu.getWagon(), leftPos, topPos);
        drawRepairSlots(graphics);
        WagonGui.drawEmptySlotIcons(graphics, leftPos, topPos, menu);
    }

    protected void placeRepair() {
        int bar = seatBarY();
        if(bar < 0)
            return;
        RepairLayout.place(menu.slots, menu.getWagon(), 287, bar + 18);
    }

    protected void drawRepairSlots(GuiGraphics graphics) {
        for(Slot slot : menu.slots) {
            if(slot instanceof RepairSlot && slot.isActive())
                WagonGui.drawSlot(graphics, leftPos, topPos, slot);
        }
    }

    protected void drawSeatStrip(GuiGraphics graphics, int frameX, int frameWidth) {
        int bar = seatBarY();
        int stripTop = topPos + bar;
        int stripBottom = stripTop + SEAT_STRIP - 1;
        graphics.fill(frameX + 3, stripTop, frameX + frameWidth - 3, stripBottom, 0xFFC6C6C6);
        graphics.fill(frameX, stripTop, frameX + 1, stripBottom, 0xFF000000);
        graphics.fill(frameX + 1, stripTop, frameX + 3, stripBottom, 0xFFFFFFFF);
        graphics.fill(frameX + frameWidth - 3, stripTop, frameX + frameWidth - 1, stripBottom, 0xFF555555);
        graphics.fill(frameX + frameWidth - 1, stripTop, frameX + frameWidth, stripBottom, 0xFF000000);
        AbstractWagon wagon = menu.getWagon();
        if(wagon != null)
            SeatButtons.drawHealth(graphics, font, wagon, frameX + 16, stripTop + 2, frameWidth - 32);
        for(var slot : menu.slots) {
            if(slot instanceof net.favouriteless.trotting_wagons.common.menus.WheelSlot)
                WagonGui.drawSlot(graphics, frameX, topPos, slot);
        }
    }

}
