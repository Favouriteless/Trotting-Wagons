package net.favouriteless.trotting_wagons.client.screens;

import net.favouriteless.trotting_wagons.TrottingWagons;
import net.favouriteless.trotting_wagons.common.entities.base.AbstractWagon;
import net.favouriteless.trotting_wagons.common.menus.AnimalMenu;
import net.favouriteless.trotting_wagons.common.menus.HarnessSlot;
import net.favouriteless.trotting_wagons.common.menus.RepairSlot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class AnimalScreen extends AbstractContainerScreen<AnimalMenu> {

    /** Top edge of the highest passenger button. */
    private static final int SEAT_TOP = 38;
    private static final ResourceLocation TEXTURE = TrottingWagons.id("textures/gui/animal_wagon.png");
    private static final ResourceLocation BIG_SLOT = TrottingWagons.id("textures/gui/icons/big_slot.png");

    private final TooltipDelay tooltips = new TooltipDelay();
    private List<SeatButton> seatButtons;
    private int shownHitch = -1;

    public AnimalScreen(AnimalMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 320;
        this.imageHeight = AnimalMenu.IMAGE_HEIGHT;
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
    }

    private void layoutSeats() {
        AbstractWagon wagon = menu.getWagon();
        if(wagon == null)
            return;
        if(seatButtons != null) {
            for(SeatButton button : seatButtons)
                removeWidget(button);
        }
        // Front is on the left, next to the driver. Top row is the wagon's left, bottom row its right, and the two inset cells are the large seats on the centerline.
        int[] xs = {82, 108, 108, 134, 160, 160, 186, 212, 212};
        int[] ys = {51, 38, 64, 51, 38, 64, 51, 38, 64};
        List<SeatButton> buttons = new ArrayList<>(xs.length + 2);
        for(int i = 0; i < xs.length && i < wagon.getSeatCount(); i++) {
            SeatButton button = SeatButton.placed(wagon, i, leftPos + xs[i], topPos + ys[i], 26);
            button.refresh();
            buttons.add(button);
        }
        int slots = wagon.hitchSlots();
        if(slots > 0) {
            SeatButton left = SeatButton.placedDraft(wagon, AbstractWagon.Side.LEFT, leftPos + AnimalMenu.draftX(slots, 0), topPos + AnimalMenu.DRAFT_Y, 26);
            left.refresh();
            buttons.add(left);
        }
        if(slots > 1) {
            SeatButton right = SeatButton.placedDraft(wagon, AbstractWagon.Side.RIGHT, leftPos + AnimalMenu.draftX(slots, 1), topPos + AnimalMenu.DRAFT_Y, 26);
            right.refresh();
            buttons.add(right);
        }
        SeatButton.bind(buttons);
        seatButtons = buttons;
        shownHitch = slots;
        for(SeatButton button : buttons)
            addRenderableWidget(button);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, 4210752, false);
        AbstractWagon wagon = menu.getWagon();
        if(wagon == null)
            return;
        Component distance = Component.translatable("gui.trotting_wagons.distance", String.format(java.util.Locale.ROOT, "%.1f", wagon.getDistance() / 1000.0F));
        int distanceX = imageWidth - 8 - font.width(distance);
        graphics.drawString(font, distance, distanceX, titleLabelY, 4210752, false);
        String name = wagon.getOwnerName();
        if(!name.isEmpty()) {
            Component owner = Component.translatable("gui.trotting_wagons.owner", name);
            int ownerX = (imageWidth - font.width(owner)) / 2;
            int limit = distanceX - 6;
            if(ownerX + font.width(owner) > limit)
                ownerX = limit - font.width(owner);
            graphics.drawString(font, owner, ownerX, titleLabelY, 4210752, false);
        }
        drawCentered(graphics, Component.translatable(harnessKey(wagon.hitchSlots())), AnimalMenu.HARNESS_X + 8, 96);
        int wheelCenter = AnimalMenu.WHEEL_X + 17;
        drawCentered(graphics, Component.translatable("gui.trotting_wagons.wheels"), wheelCenter, 134);
        drawCentered(graphics, wagon.wheelSet().label(), wheelCenter, 144);
        int slotsCenter = AnimalMenu.REPAIR_X + 17;
        drawCentered(graphics, Component.translatable("gui.trotting_wagons.repair"), slotsCenter, SEAT_TOP);
        drawCentered(graphics, wagon.conditionName(), slotsCenter, SEAT_TOP + 10);
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
        return index >= 0 && index < AbstractWagon.WHEEL_COUNT || slot instanceof HarnessSlot || slot instanceof RepairSlot;
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
        RepairLayout.place(menu.slots, menu.getWagon(), AnimalMenu.REPAIR_X + 17, SEAT_TOP);
        super.containerTick();
        AbstractWagon wagon = menu.getWagon();
        if(wagon != null && wagon.hitchSlots() != shownHitch)
            layoutSeats();
        if(seatButtons != null && wagon != null)
            SeatButtons.refresh(seatButtons);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        RepairLayout.place(menu.slots, menu.getWagon(), AnimalMenu.REPAIR_X + 17, SEAT_TOP);
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, 320, AnimalMenu.IMAGE_HEIGHT);
        AbstractWagon wagon = menu.getWagon();
        if(wagon != null) {
            int slots = wagon.hitchSlots();
            for(int i = 0; i < slots; i++)
                graphics.blit(BIG_SLOT, leftPos + AnimalMenu.draftX(slots, i), topPos + AnimalMenu.DRAFT_Y, 0, 0, 26, 26, 26, 26);
            SeatButtons.drawHealth(graphics, font, wagon, leftPos + AnimalMenu.HEALTH_X, topPos + AnimalMenu.HEALTH_Y, AnimalMenu.HEALTH_W);
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
