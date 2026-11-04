package net.favouriteless.trotting_wagons.client.screens;

import net.favouriteless.trotting_wagons.TrottingWagons;
import net.favouriteless.trotting_wagons.common.entities.base.AbstractWagon;
import net.favouriteless.trotting_wagons.common.menus.PassengerMenu;
import net.favouriteless.trotting_wagons.common.menus.RepairSlot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

import java.util.ArrayList;
import java.util.List;

public class PassengerScreen extends AbstractContainerScreen<PassengerMenu> {

    private static final ResourceLocation TEXTURE = TrottingWagons.id("textures/gui/passenger_wagon.png");
    private static final ResourceLocation BIG_SLOT = TrottingWagons.id("textures/gui/icons/big_slot.png");

    private List<SeatButton> seatButtons;
    private int shownHitch = -1;

    public PassengerScreen(PassengerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = PassengerMenu.IMAGE_WIDTH;
        this.imageHeight = PassengerMenu.IMAGE_HEIGHT;
        this.titleLabelY = 6;
        this.inventoryLabelX = PassengerMenu.INV_X;
        this.inventoryLabelY = PassengerMenu.INV_Y - 12;
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
        List<SeatButton> buttons = new ArrayList<>(wagon.getSeatCount() + 2);
        for(int i = 0; i < wagon.getSeatCount(); i++) {
            SeatButton button = SeatButton.placed(wagon, i,
                    leftPos + PassengerMenu.SEAT_X + i * PassengerMenu.SEAT_STEP, topPos + PassengerMenu.SEAT_Y, 26);
            button.refresh();
            buttons.add(button);
        }
        int slots = wagon.hitchSlots();
        if(slots > 0) {
            SeatButton left = SeatButton.placedDraft(wagon, AbstractWagon.Side.LEFT,
                    leftPos + PassengerMenu.draftX(slots, 0), topPos + PassengerMenu.DRAFT_Y, 26);
            left.refresh();
            buttons.add(left);
        }
        if(slots > 1) {
            SeatButton right = SeatButton.placedDraft(wagon, AbstractWagon.Side.RIGHT,
                    leftPos + PassengerMenu.draftX(slots, 1), topPos + PassengerMenu.DRAFT_Y, 26);
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
        super.renderLabels(graphics, mouseX, mouseY);
        int ownerLeft = titleLabelX + font.width(title) + 12;
        WagonGui.drawHeader(graphics, font, menu.getWagon(), titleLabelY, imageWidth, ownerLeft);
        AbstractWagon wagon = menu.getWagon();
        if(wagon == null)
            return;
        drawCentered(graphics, Component.translatable(harnessKey(wagon.hitchSlots())), PassengerMenu.HARNESS_X + 8, 70);
        int wheelCenter = PassengerMenu.WHEEL_X + 17;
        drawCentered(graphics, Component.translatable("gui.trotting_wagons.wheels"), wheelCenter, 108);
        drawCentered(graphics, wagon.wheelSet().label(), wheelCenter, 118);
        int slotsCenter = PassengerMenu.REPAIR_X + 17;
        drawCentered(graphics, Component.translatable("gui.trotting_wagons.repair"), slotsCenter, PassengerMenu.SEAT_Y);
        drawCentered(graphics, wagon.conditionName(), slotsCenter, PassengerMenu.SEAT_Y + 10);
        drawCentered(graphics, Component.translatable("gui.trotting_wagons.custom_items"), slotsCenter, PassengerMenu.CUSTOM_Y);
        drawCentered(graphics, Component.translatable("gui.trotting_wagons.custom_items.line2"), slotsCenter, PassengerMenu.CUSTOM_Y + 10);
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
    protected void containerTick() {
        RepairLayout.place(menu.slots, menu.getWagon(), PassengerMenu.REPAIR_X + 17, PassengerMenu.SEAT_Y);
        super.containerTick();
        AbstractWagon wagon = menu.getWagon();
        if(wagon != null && wagon.hitchSlots() != shownHitch)
            layoutSeats();
        if(seatButtons != null && wagon != null)
            SeatButtons.refresh(seatButtons);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        RepairLayout.place(menu.slots, menu.getWagon(), PassengerMenu.REPAIR_X + 17, PassengerMenu.SEAT_Y);
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, PassengerMenu.IMAGE_WIDTH, PassengerMenu.IMAGE_HEIGHT);
        AbstractWagon wagon = menu.getWagon();
        if(wagon != null) {
            int slots = wagon.hitchSlots();
            for(int i = 0; i < slots; i++)
                graphics.blit(BIG_SLOT, leftPos + PassengerMenu.draftX(slots, i), topPos + PassengerMenu.DRAFT_Y, 0, 0, 26, 26, 26, 26);
            SeatButtons.drawHealth(graphics, font, wagon, leftPos + PassengerMenu.HEALTH_X, topPos + PassengerMenu.HEALTH_Y, PassengerMenu.HEALTH_W);
        }
        for(Slot slot : menu.slots) {
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
