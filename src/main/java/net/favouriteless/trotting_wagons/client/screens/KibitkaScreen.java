package net.favouriteless.trotting_wagons.client.screens;

import net.favouriteless.trotting_wagons.TrottingWagons;
import net.favouriteless.trotting_wagons.common.entities.base.AbstractWagon;
import net.favouriteless.trotting_wagons.common.entities.wagons.KibitkaWagon;
import net.favouriteless.trotting_wagons.common.menus.KibitkaMenu;
import net.favouriteless.trotting_wagons.common.menus.RepairSlot;
import net.favouriteless.trotting_wagons.common.network.packets.KibitkaRespawnPayload;
import net.favouriteless.trotting_wagons.common.network.packets.KibitkaSleepPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Items;

import java.util.List;

public class KibitkaScreen extends AbstractContainerScreen<KibitkaMenu> {

    private static final ResourceLocation TEXTURE = TrottingWagons.id("textures/gui/kibitka_wagon.png");
    private static final ResourceLocation BIG_SLOT = TrottingWagons.id("textures/gui/icons/big_slot.png");

    private List<SeatButton> seatButtons;
    private int shownHitch = -1;

    public KibitkaScreen(KibitkaMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = KibitkaMenu.IMAGE_WIDTH;
        this.imageHeight = KibitkaMenu.IMAGE_HEIGHT;
        this.titleLabelY = 6;
        this.inventoryLabelX = KibitkaMenu.INV_X;
        this.inventoryLabelY = KibitkaMenu.INV_Y - 12;
    }

    @Override
    protected void init() {
        super.init();
        AbstractWagon wagon = menu.getWagon();
        if(wagon == null)
            return;
        layoutSeats();
        if(wagon instanceof KibitkaWagon kibitka) {
            addRenderableWidget(new KibitkaCampButton(leftPos + 150, topPos + 97, kibitka));
            addRenderableWidget(new KibitkaActionButton(leftPos + 185, topPos + 97, 26, kibitka, Items.RED_BED,
                    "gui.trotting_wagons.kibitka.bed", "gui.trotting_wagons.kibitka.sleep", new KibitkaSleepPayload(kibitka.getId())));
            addRenderableWidget(new KibitkaActionButton(leftPos + 220, topPos + 97, 26, kibitka, Items.COMPASS,
                    "gui.trotting_wagons.kibitka.respawn", "gui.trotting_wagons.kibitka.respawn", new KibitkaRespawnPayload(kibitka.getId())));
        }
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
        seatButtons = SeatButtons.place(wagon, new int[] {leftPos + 74, leftPos + 100}, topPos + KibitkaMenu.SEAT_Y,
                leftPos + KibitkaMenu.draftX(slots, 0), topPos + KibitkaMenu.DRAFT_Y, 26, KibitkaMenu.DRAFT_STRIDE);
        shownHitch = slots;
        for(SeatButton button : seatButtons)
            addRenderableWidget(button);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        WagonGui.drawHeader(graphics, font, menu.getWagon(), titleLabelY, imageWidth);
        AbstractWagon wagon = menu.getWagon();
        if(wagon == null)
            return;
        drawCentered(graphics, Component.translatable(harnessKey(wagon.hitchSlots())), KibitkaMenu.HARNESS_X + 8, 129);
        int wheelCenter = KibitkaMenu.WHEEL_X + 17;
        drawCentered(graphics, Component.translatable("gui.trotting_wagons.wheels"), wheelCenter, 167);
        drawCentered(graphics, wagon.wheelSet().label(), wheelCenter, 177);
        int slotsCenter = KibitkaMenu.REPAIR_X + 17;
        drawCentered(graphics, Component.translatable("gui.trotting_wagons.repair"), slotsCenter, KibitkaMenu.SEAT_Y);
        drawCentered(graphics, wagon.conditionName(), slotsCenter, KibitkaMenu.SEAT_Y + 10);
        drawCentered(graphics, Component.translatable("gui.trotting_wagons.custom_items"), slotsCenter, KibitkaMenu.CUSTOM_Y);
        drawCentered(graphics, Component.translatable("gui.trotting_wagons.custom_items.line2"), slotsCenter, KibitkaMenu.CUSTOM_Y + 10);
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
        RepairLayout.place(menu.slots, menu.getWagon(), KibitkaMenu.REPAIR_X + 17, KibitkaMenu.SEAT_Y);
        super.containerTick();
        AbstractWagon wagon = menu.getWagon();
        if(wagon != null && wagon.hitchSlots() != shownHitch)
            layoutSeats();
        if(seatButtons != null && wagon != null)
            SeatButtons.refresh(seatButtons);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        RepairLayout.place(menu.slots, menu.getWagon(), KibitkaMenu.REPAIR_X + 17, KibitkaMenu.SEAT_Y);
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, KibitkaMenu.IMAGE_WIDTH, KibitkaMenu.IMAGE_HEIGHT);
        AbstractWagon wagon = menu.getWagon();
        if(wagon != null) {
            int hitch = wagon.hitchSlots();
            for(int i = 0; i < hitch; i++)
                graphics.blit(BIG_SLOT, leftPos + KibitkaMenu.draftX(hitch, i), topPos + KibitkaMenu.DRAFT_Y, 0, 0, 26, 26, 26, 26);
            SeatButtons.drawHealth(graphics, font, wagon, leftPos + KibitkaMenu.HEALTH_X, topPos + KibitkaMenu.HEALTH_Y, KibitkaMenu.HEALTH_W);
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
