package net.favouriteless.trotting_wagons.client.screens;

import net.favouriteless.trotting_wagons.TrottingWagons;
import net.favouriteless.trotting_wagons.common.entities.wagons.BarrelWagon;
import net.favouriteless.trotting_wagons.common.menus.BarrelContentSlot;
import net.favouriteless.trotting_wagons.common.menus.BarrelMenu;
import net.favouriteless.trotting_wagons.common.menus.RepairSlot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Locale;

public class BarrelScreen extends AbstractContainerScreen<BarrelMenu> {

    private static final ResourceLocation TEXTURE = TrottingWagons.id("textures/gui/barrel_wagon.png");
    private static final ResourceLocation BIG_SLOT = TrottingWagons.id("textures/gui/icons/big_slot.png");

    private final TooltipDelay tooltips = new TooltipDelay();
    private List<SeatButton> seatButtons;
    private int shownHitch = -1;

    public BarrelScreen(BarrelMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = BarrelMenu.IMAGE_WIDTH;
        this.imageHeight = BarrelMenu.IMAGE_HEIGHT;
        this.titleLabelY = 10000;
        this.inventoryLabelX = BarrelMenu.INV_X;
        this.inventoryLabelY = BarrelMenu.INV_Y - 12;
    }

    @Override
    protected void init() {
        super.init();
        layoutSeats();
        layoutSlots();
    }

    private void layoutSeats() {
        BarrelWagon wagon = menu.getWagon();
        if(wagon == null)
            return;
        if(seatButtons != null) {
            for(SeatButton button : seatButtons)
                removeWidget(button);
        }
        int slots = wagon.hitchSlots();
        int[] seats = new int[wagon.getSeatCount()];
        for(int i = 0; i < seats.length; i++)
            seats[i] = leftPos + BarrelMenu.SEAT_X;
        seatButtons = SeatButtons.place(wagon, seats, topPos + BarrelMenu.SEAT_Y,
                leftPos + BarrelMenu.draftX(slots, 0), topPos + BarrelMenu.DRAFT_Y, 26, BarrelMenu.DRAFT_STRIDE);
        shownHitch = slots;
        for(SeatButton button : seatButtons)
            addRenderableWidget(button);
    }

    @Override
    protected void containerTick() {
        RepairLayout.place(menu.slots, menu.getWagon(), BarrelMenu.REPAIR_X + 17, BarrelMenu.SEAT_Y);
        super.containerTick();
        layoutSlots();
        BarrelWagon wagon = menu.getWagon();
        if(wagon != null && wagon.hitchSlots() != shownHitch)
            layoutSeats();
        if(seatButtons != null && wagon != null)
            SeatButtons.refresh(seatButtons);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        graphics.drawString(font, title, BarrelMenu.TITLE_X, BarrelMenu.TITLE_Y, 4210752, false);
        BarrelWagon wagon = menu.getWagon();
        if(wagon == null)
            return;
        Component distance = Component.translatable("gui.trotting_wagons.distance",
                String.format(Locale.ROOT, "%.1f", wagon.getDistance() / 1000.0F));
        graphics.drawString(font, distance, BarrelMenu.TITLE_X + font.width(title) + BarrelMenu.DISTANCE_GAP, BarrelMenu.TITLE_Y, 4210752, false);
        String ownerName = wagon.getOwnerName();
        if(!ownerName.isEmpty()) {
            Component owner = Component.translatable("gui.trotting_wagons.owner", ownerName);
            graphics.drawString(font, owner, BarrelMenu.TITLE_X, BarrelMenu.TITLE_Y + 12, 4210752, false);
        }
        drawCentered(graphics, Component.translatable(harnessKey(wagon.hitchSlots())), BarrelMenu.HARNESS_X + 8, 146);
        int wheelCenter = BarrelMenu.WHEEL_X + 17;
        drawCentered(graphics, Component.translatable("gui.trotting_wagons.wheels"), wheelCenter, 184);
        drawCentered(graphics, wagon.wheelSet().label(), wheelCenter, 194);
        int slotsCenter = BarrelMenu.REPAIR_X + 17;
        drawCentered(graphics, Component.translatable("gui.trotting_wagons.repair"), slotsCenter, BarrelMenu.SEAT_Y);
        drawCentered(graphics, wagon.conditionName(), slotsCenter, BarrelMenu.SEAT_Y + 10);
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
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        RepairLayout.place(menu.slots, menu.getWagon(), BarrelMenu.REPAIR_X + 17, BarrelMenu.SEAT_Y);
        layoutSlots();
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, BarrelMenu.IMAGE_WIDTH, BarrelMenu.IMAGE_HEIGHT);
        BarrelWagon drawn = menu.getWagon();
        if(drawn != null) {
            int hitch = drawn.hitchSlots();
            for(int i = 0; i < hitch; i++)
                graphics.blit(BIG_SLOT, leftPos + BarrelMenu.draftX(hitch, i), topPos + BarrelMenu.DRAFT_Y, 0, 0, 26, 26, 26, 26);
            SeatButtons.drawHealth(graphics, font, drawn,
                    leftPos + BarrelMenu.HEALTH_X, topPos + BarrelMenu.HEALTH_Y, BarrelMenu.HEALTH_W);
        }
        for(Slot slot : menu.slots) {
            if(slot.x < 0 || slot.y < 0)
                continue;
            if(slot instanceof RepairSlot && slot.isActive())
                WagonGui.drawSlot(graphics, leftPos, topPos, slot);
            if(slot instanceof BarrelContentSlot && slot.isActive())
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

    @Override
    protected void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        if(hoveredSlot != null && hoveredSlot.hasItem()) {
            tooltips.ready(null);
            super.renderTooltip(graphics, mouseX, mouseY);
            return;
        }
        if(!tooltips.ready(hoveredSlot) || hoveredSlot == null || hoveredSlot.x < 0)
            return;
        Component tip = emptySlotTip(hoveredSlot);
        if(tip != null)
            graphics.renderTooltip(font, tip, mouseX, mouseY);
    }

    private Component emptySlotTip(Slot slot) {
        if(slot instanceof RepairSlot repair && menu.getWagon() != null)
            return menu.getWagon().repairTooltip(repair.getSlotIndex());
        ItemStack icon = WagonGui.ghostIcon(menu, slot);
        return icon.isEmpty() ? null : icon.getHoverName();
    }

    private void layoutSlots() {
        BarrelWagon wagon = menu.getWagon();
        int shown = 0;
        for(Slot slot : menu.slots) {
            if(!(slot instanceof BarrelContentSlot))
                continue;
            int barrel = shown / BarrelWagon.CONTENTS;
            int offset = shown % BarrelWagon.CONTENTS;
            shown++;
            if(wagon != null && wagon.barrelFilled(barrel))
                moveSlot(slot, BarrelMenu.contentX(barrel, offset), BarrelMenu.contentY(barrel, offset));
            else
                moveSlot(slot, -2000, -2000);
        }
    }

    private static void moveSlot(Slot slot, int x, int y) {
        SlotMove.UNSAFE.putInt(slot, SlotMove.X, x);
        SlotMove.UNSAFE.putInt(slot, SlotMove.Y, y);
    }

    private static final class SlotMove {
        private static final Unsafe UNSAFE;
        private static final long X;
        private static final long Y;

        static {
            try {
                Field field = Unsafe.class.getDeclaredField("theUnsafe");
                field.setAccessible(true);
                UNSAFE = (Unsafe)field.get(null);
                X = UNSAFE.objectFieldOffset(Slot.class.getDeclaredField("x"));
                Y = UNSAFE.objectFieldOffset(Slot.class.getDeclaredField("y"));
            } catch(ReflectiveOperationException exception) {
                throw new ExceptionInInitializerError(exception);
            }
        }
    }

}
