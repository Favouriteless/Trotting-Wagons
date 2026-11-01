package net.favouriteless.trotting_wagons.client.screens;

import net.favouriteless.trotting_wagons.TrottingWagons;
import net.favouriteless.trotting_wagons.common.entities.base.AbstractWagon;
import net.favouriteless.trotting_wagons.common.entities.wagons.LogWagon;
import net.favouriteless.trotting_wagons.common.menus.HarnessSlot;
import net.favouriteless.trotting_wagons.common.menus.LogCargoSlot;
import net.favouriteless.trotting_wagons.common.menus.LogMenu;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class LogScreen extends AbstractContainerScreen<LogMenu> {

    private static final ResourceLocation TEXTURE = TrottingWagons.id("textures/gui/log_wagon.png");
    private static final ResourceLocation BIG_SLOT = TrottingWagons.id("textures/gui/icons/big_slot.png");

    private final TooltipDelay tooltips = new TooltipDelay();
    private List<SeatButton> seatButtons;
    private int shownHitch = -1;
    private int shift;

    public LogScreen(LogMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = LogMenu.IMAGE_WIDTH;
        this.imageHeight = LogMenu.IMAGE_HEIGHT;
        this.titleLabelY = 10000;
        this.inventoryLabelX = LogMenu.INV_X;
        this.inventoryLabelY = 10000;
    }

    @Override
    protected void init() {
        super.init();
        layout();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        layout();
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
    protected void containerTick() {
        layout();
        super.containerTick();
        if(seatButtons != null && menu.getWagon() != null)
            SeatButtons.refresh(seatButtons);
    }

    private void layout() {
        LogWagon wagon = menu.getWagon();
        int placeholder = wagon == null ? -1 : wagon.placeholderSlot();
        List<LogCargoSlot> filled = new ArrayList<>();
        LogCargoSlot placeholderSlot = null;
        for(Slot slot : menu.slots) {
            if(!(slot instanceof LogCargoSlot cargo))
                continue;
            if(cargo.hasItem())
                filled.add(cargo);
            else if(cargo.getSlotIndex() == placeholder)
                placeholderSlot = cargo;
            else
                moveSlot(cargo, -2000, -2000);
        }
        int shown = 0;
        for(LogCargoSlot cargo : filled)
            placeCargo(cargo, shown++);
        if(placeholderSlot != null)
            placeCargo(placeholderSlot, shown++);
        int rows = Math.max(1, (Math.max(shown, 1) + LogMenu.COLUMNS - 1) / LogMenu.COLUMNS);
        shift = (rows - 1) * LogMenu.ROW;
        imageHeight = LogMenu.IMAGE_HEIGHT + shift;
        topPos = (height - imageHeight) / 2;
        inventoryLabelY = LogMenu.INV_Y + shift - 12;
        int wheel = 0;
        int player = 0;
        for(int i = 0; i < menu.slots.size(); i++) {
            Slot slot = menu.slots.get(i);
            if(i >= menu.wheelStart() && i < menu.playerStart()) {
                moveSlot(slot, LogMenu.WHEEL_X + (wheel % 2) * 18, LogMenu.WHEEL_Y + shift + (wheel / 2) * 18);
                wheel++;
            } else if(i >= menu.playerStart() && i < menu.harnessSlot()) {
                if(player < 9)
                    moveSlot(slot, LogMenu.INV_X + player * 18, LogMenu.HOTBAR_Y + shift);
                else {
                    int n = player - 9;
                    moveSlot(slot, LogMenu.INV_X + (n % 9) * 18, LogMenu.INV_Y + shift + (n / 9) * 18);
                }
                player++;
            } else if(i == menu.harnessSlot()) {
                moveSlot(slot, LogMenu.HARNESS_X, LogMenu.HARNESS_Y + shift);
            }
        }
        RepairLayout.place(menu.slots, wagon, LogMenu.REPAIR_X + 17, LogMenu.SEAT_Y + shift);
        placeSeats(wagon);
    }

    private void placeSeats(LogWagon wagon) {
        if(wagon == null)
            return;
        int slots = wagon.hitchSlots();
        if(seatButtons == null || slots != shownHitch)
            buildSeats(wagon, slots);
        if(seatButtons == null || seatButtons.isEmpty())
            return;
        seatButtons.get(0).setX(leftPos + LogMenu.SEAT_X);
        seatButtons.get(0).setY(topPos + LogMenu.SEAT_Y + shift);
        int n = 1;
        if(slots > 0 && n < seatButtons.size()) {
            seatButtons.get(n).setX(leftPos + LogMenu.draftX(slots, 0));
            seatButtons.get(n).setY(topPos + LogMenu.DRAFT_Y + shift);
            n++;
        }
        if(slots > 1 && n < seatButtons.size()) {
            seatButtons.get(n).setX(leftPos + LogMenu.draftX(slots, 1));
            seatButtons.get(n).setY(topPos + LogMenu.DRAFT_Y + shift);
        }
    }

    private void buildSeats(LogWagon wagon, int slots) {
        if(seatButtons != null) {
            for(SeatButton button : seatButtons)
                removeWidget(button);
        }
        List<SeatButton> buttons = new ArrayList<>(3);
        SeatButton driver = SeatButton.placed(wagon, 0, leftPos + LogMenu.SEAT_X, topPos + LogMenu.SEAT_Y + shift, 26);
        driver.refresh();
        buttons.add(driver);
        if(slots > 0) {
            SeatButton left = SeatButton.placedDraft(wagon, AbstractWagon.Side.LEFT,
                    leftPos + LogMenu.draftX(slots, 0), topPos + LogMenu.DRAFT_Y + shift, 26);
            left.refresh();
            buttons.add(left);
        }
        if(slots > 1) {
            SeatButton right = SeatButton.placedDraft(wagon, AbstractWagon.Side.RIGHT,
                    leftPos + LogMenu.draftX(slots, 1), topPos + LogMenu.DRAFT_Y + shift, 26);
            right.refresh();
            buttons.add(right);
        }
        SeatButton.bind(buttons);
        seatButtons = buttons;
        shownHitch = slots;
        for(SeatButton button : buttons)
            addRenderableWidget(button);
    }

    private static void placeCargo(Slot slot, int shown) {
        moveSlot(slot, LogMenu.CARGO_X + (shown % LogMenu.COLUMNS) * 18, LogMenu.CARGO_Y + (shown / LogMenu.COLUMNS) * 18);
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

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        LogWagon wagon = menu.getWagon();
        if(wagon == null)
            return;
        int titleY = LogMenu.TITLE_Y + shift;
        graphics.drawString(font, title, LogMenu.TITLE_X, titleY, 4210752, false);
        Component distance = Component.translatable("gui.trotting_wagons.distance",
                String.format(Locale.ROOT, "%.1f", wagon.getDistance() / 1000.0F));
        graphics.drawString(font, distance, LogMenu.TITLE_X + font.width(title) + LogMenu.DISTANCE_GAP, titleY, 4210752, false);
        String ownerName = wagon.getOwnerName();
        if(!ownerName.isEmpty()) {
            Component owner = Component.translatable("gui.trotting_wagons.owner", ownerName);
            graphics.drawString(font, owner, LogMenu.TITLE_X, titleY + 12, 4210752, false);
        }
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 4210752, false);
        drawCentered(graphics, Component.translatable(harnessKey(wagon.hitchSlots())), LogMenu.HARNESS_X + 8, LogMenu.HARNESS_Y + shift - 12);
        int wheelCenter = LogMenu.WHEEL_X + 17;
        drawCentered(graphics, Component.translatable("gui.trotting_wagons.wheels"), wheelCenter, LogMenu.WHEEL_Y + shift - 22);
        drawCentered(graphics, wagon.wheelSet().label(), wheelCenter, LogMenu.WHEEL_Y + shift - 12);
        int slotsCenter = LogMenu.REPAIR_X + 17;
        drawCentered(graphics, Component.translatable("gui.trotting_wagons.repair"), slotsCenter, LogMenu.SEAT_Y + shift);
        drawCentered(graphics, wagon.conditionName(), slotsCenter, LogMenu.SEAT_Y + shift + 10);
    }

    private static String harnessKey(int slots) {
        if(slots > 1)
            return "gui.trotting_wagons.harness.shafts";
        if(slots == 1)
            return "gui.trotting_wagons.harness.tongue";
        return "gui.trotting_wagons.harness.none";
    }

    private void drawCentered(GuiGraphics graphics, Component text, int centerX, int y) {
        graphics.drawString(font, text, centerX - font.width(text) / 2, y, 4210752, false);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        layout();
        if(shift == 0) {
            graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, LogMenu.IMAGE_HEIGHT, LogMenu.IMAGE_WIDTH, LogMenu.IMAGE_HEIGHT);
        } else {
            graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, LogMenu.CHROME_Y, LogMenu.IMAGE_WIDTH, LogMenu.IMAGE_HEIGHT);
            for(int i = 0; i < shift; i++)
                graphics.blit(TEXTURE, leftPos, topPos + LogMenu.CHROME_Y + i, 0, 10, imageWidth, 1, LogMenu.IMAGE_WIDTH, LogMenu.IMAGE_HEIGHT);
            graphics.blit(TEXTURE, leftPos, topPos + LogMenu.CHROME_Y + shift, 0, LogMenu.CHROME_Y,
                    imageWidth, LogMenu.IMAGE_HEIGHT - LogMenu.CHROME_Y, LogMenu.IMAGE_WIDTH, LogMenu.IMAGE_HEIGHT);
        }
        LogWagon wagon = menu.getWagon();
        if(wagon != null) {
            int slots = wagon.hitchSlots();
            for(int i = 0; i < slots; i++)
                graphics.blit(BIG_SLOT, leftPos + LogMenu.draftX(slots, i), topPos + LogMenu.DRAFT_Y + shift, 0, 0, 26, 26, 26, 26);
            SeatButtons.drawHealth(graphics, font, wagon, leftPos + LogMenu.HEALTH_X, topPos + LogMenu.HEALTH_Y + shift, LogMenu.HEALTH_W);
        }
        for(Slot slot : menu.slots) {
            if(slot.x < 0 || slot.y < 0)
                continue;
            if(slot instanceof LogCargoSlot || (slot instanceof RepairSlot && slot.isActive()))
                WagonGui.drawSlot(graphics, leftPos, topPos, slot);
        }
        WagonGui.drawEmptySlotIcons(graphics, leftPos, topPos, menu);
    }

    @Override
    protected void renderSlot(GuiGraphics graphics, Slot slot) {
        super.renderSlot(graphics, slot);
        if(!(slot instanceof LogCargoSlot) || !slot.hasItem() || slot.x < 0)
            return;
        String text = String.valueOf(slot.getItem().getCount());
        int width = font.width(text);
        if(width <= 16)
            return;
        int x = slot.x;
        int y = slot.y;
        float scale = 16F / width;
        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, 200.0F);
        graphics.fill(x - 10, y + 6, x, y + 16, 0xFFC6C6C6);
        graphics.fill(x, y + 8, x + 16, y + 16, 0xFF8B8B8B);
        graphics.pose().translate(x, y + 16 - 8 * scale, 0);
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.drawString(font, text, 0, 0, 0xFFFFFF, true);
        graphics.pose().popPose();
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
        if(slot instanceof HarnessSlot)
            return Component.translatable("gui.trotting_wagons.harness.slot");
        ItemStack icon = WagonGui.ghostIcon(menu, slot);
        return icon.isEmpty() ? null : icon.getHoverName();
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

}
