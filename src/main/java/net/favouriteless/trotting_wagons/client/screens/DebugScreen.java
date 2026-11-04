package net.favouriteless.trotting_wagons.client.screens;

import net.favouriteless.trotting_wagons.TrottingWagons;
import net.favouriteless.trotting_wagons.common.entities.base.AbstractWagon;
import net.favouriteless.trotting_wagons.common.entities.wagons.DebugWagon;
import net.favouriteless.trotting_wagons.common.menus.BannerSlot;
import net.favouriteless.trotting_wagons.common.menus.DebugMenu;
import net.favouriteless.trotting_wagons.common.menus.HarnessSlot;
import net.favouriteless.trotting_wagons.common.menus.LanternSlot;
import net.favouriteless.trotting_wagons.common.menus.RepairSlot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class DebugScreen extends AbstractContainerScreen<DebugMenu> {

    private static final ResourceLocation TEXTURE = TrottingWagons.id("textures/gui/debug_wagon.png");
    private static final ResourceLocation BIG_SLOT = TrottingWagons.id("textures/gui/icons/big_slot.png");
    private final TooltipDelay tooltips = new TooltipDelay();
    private List<SeatButton> seatButtons;
    private int shownHitch = -1;

    public DebugScreen(DebugMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 320;
        this.imageHeight = 197;
        this.titleLabelY = 6;
        this.inventoryLabelY = 1000;
    }

    @Override
    protected void init() {
        super.init();
        AbstractWagon wagon = menu.getWagon();
        if(wagon == null)
            return;
        layoutSeats();
        if(wagon instanceof DebugWagon debugWagon)
            addRenderableWidget(new LadderButton(leftPos + imageWidth - 24, topPos + 6 + font.lineHeight + 2, debugWagon));
    }

    private void layoutSeats() {
        AbstractWagon wagon = menu.getWagon();
        if(wagon == null)
            return;
        if(seatButtons != null) {
            for(SeatButton button : seatButtons)
                removeWidget(button);
        }
        int slots = wagon.hitchSlots();
        int[] seatX = new int[] {leftPos + DebugMenu.SEAT_X0, leftPos + 134, leftPos + 160, leftPos + 186};
        seatButtons = SeatButtons.place(wagon, seatX, topPos + DebugMenu.SEAT_Y, leftPos + DebugMenu.draftX(slots, 0), topPos + DebugMenu.DRAFT_Y, 26, DebugMenu.DRAFT_STRIDE);
        for(SeatButton button : seatButtons)
            addRenderableWidget(button);
        shownHitch = slots;
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, 4210752, false);
        AbstractWagon wagon = menu.getWagon();
        if(wagon == null)
            return;
        WagonGui.drawHeader(graphics, font, wagon, titleLabelY, imageWidth, titleLabelX + font.width(title) + 8);
        int harnessCenter = DebugMenu.HARNESS_X + 8;
        drawCentered(graphics, Component.translatable(harnessKey(wagon.hitchSlots())), harnessCenter, 93);
        int wheelCenter = DebugMenu.WHEEL_X + 17;
        drawCentered(graphics, Component.translatable("gui.trotting_wagons.wheels"), wheelCenter, 132);
        drawCentered(graphics, wagon.wheelSet().label(), wheelCenter, 142);
        int repairCenter = DebugMenu.REPAIR_X + 17;
        drawCentered(graphics, Component.translatable("gui.trotting_wagons.repair"), repairCenter, DebugMenu.SEAT_Y);
        drawCentered(graphics, wagon.conditionName(), repairCenter, DebugMenu.SEAT_Y + 10);
        drawCentered(graphics, Component.translatable("gui.trotting_wagons.custom_items"), repairCenter, 132);
        drawCentered(graphics, Component.translatable("gui.trotting_wagons.custom_items.line2"), repairCenter, 142);
    }

    private static String harnessKey(int slots) {
        if(slots > 1)
            return "gui.trotting_wagons.harness.tongue";
        if(slots == 1)
            return "gui.trotting_wagons.harness.shafts";
        return "gui.trotting_wagons.harness.none";
    }

    private void drawCentered(GuiGraphics graphics, Component text, int centerX, int y) {
        graphics.drawString(font, text, centerX - font.width(text) / 2, y, 4210752, false);
    }

    @Override
    protected void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        if(hoveredSlot != null && hoveredSlot.hasItem()) {
            tooltips.ready(null);
            super.renderTooltip(graphics, mouseX, mouseY);
            return;
        }
        if(!tooltips.ready(hoveredSlot) || hoveredSlot == null || !isWagonSlot(hoveredSlot))
            return;
        Component tip = emptySlotTip(hoveredSlot);
        if(tip != null)
            graphics.renderTooltip(font, tip, mouseX, mouseY);
    }

    private boolean isWagonSlot(Slot slot) {
        int index = menu.slots.indexOf(slot);
        return (index >= 0 && index < 8) || slot instanceof RepairSlot || slot instanceof HarnessSlot || slot instanceof BannerSlot || slot instanceof LanternSlot;
    }

    private Component emptySlotTip(Slot slot) {
        if(slot instanceof HarnessSlot)
            return Component.translatable("gui.trotting_wagons.harness.slot");
        if(slot instanceof RepairSlot repair && menu.getWagon() != null)
            return menu.getWagon().repairTooltip(repair.getSlotIndex());
        ItemStack icon = WagonGui.ghostIcon(menu, slot);
        return icon.isEmpty() ? null : icon.getHoverName();
    }

    @Override
    protected void containerTick() {
        RepairLayout.place(menu.slots, menu.getWagon(), DebugMenu.REPAIR_X + 17, DebugMenu.SEAT_Y);
        super.containerTick();
        AbstractWagon wagon = menu.getWagon();
        if(wagon != null && wagon.hitchSlots() != shownHitch)
            layoutSeats();
        if(seatButtons != null && wagon != null)
            SeatButtons.refresh(seatButtons);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        RepairLayout.place(menu.slots, menu.getWagon(), DebugMenu.REPAIR_X + 17, DebugMenu.SEAT_Y);
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, 320, 197);
        AbstractWagon wagon = menu.getWagon();
        if(wagon != null) {
            int slots = wagon.hitchSlots();
            for(int i = 0; i < slots; i++)
                graphics.blit(BIG_SLOT, leftPos + DebugMenu.draftX(slots, i), topPos + DebugMenu.DRAFT_Y, 0, 0, 26, 26, 26, 26);
            SeatButtons.drawHealth(graphics, font, wagon, leftPos + DebugMenu.HEALTH_X, topPos + DebugMenu.HEALTH_Y, DebugMenu.HEALTH_W);
        }
        for(var slot : menu.slots) {
            if(slot instanceof RepairSlot && slot.isActive())
                WagonGui.drawSlot(graphics, leftPos, topPos, slot);
        }
        WagonGui.drawEmptySlotIcons(graphics, leftPos, topPos, menu);
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

}
