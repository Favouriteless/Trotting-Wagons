package net.favouriteless.trotting_wagons.client.screens;

import net.favouriteless.trotting_wagons.TrottingWagons;
import net.favouriteless.trotting_wagons.common.entities.base.AbstractWagon;
import net.favouriteless.trotting_wagons.common.entities.wagons.BigCartWagon;
import net.favouriteless.trotting_wagons.common.menus.BigCartCargoSlot;
import net.favouriteless.trotting_wagons.common.menus.BigCartMenu;
import net.favouriteless.trotting_wagons.common.menus.RepairSlot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class BigCartScreen extends AbstractContainerScreen<BigCartMenu> {

    private static final ResourceLocation TEXTURE = TrottingWagons.id("textures/gui/big_cart.png");
    private static final ResourceLocation BIG_SLOT = TrottingWagons.id("textures/gui/icons/big_slot.png");
    private static final Component CARGO_ANIMALS = Component.translatable("gui.trotting_wagons.big_cart.animals");
    private static final Component CARGO_PLAYERS = Component.translatable("gui.trotting_wagons.big_cart.players");

    private final TooltipDelay tooltips = new TooltipDelay();
    private List<SeatButton> seatButtons;
    private int shownHitch = -1;

    public BigCartScreen(BigCartMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = BigCartMenu.IMAGE_WIDTH;
        this.imageHeight = BigCartMenu.IMAGE_HEIGHT;
        this.titleLabelY = 6;
        this.inventoryLabelX = BigCartMenu.INV_X;
        this.inventoryLabelY = BigCartMenu.INV_Y - 12;
    }

    @Override
    protected void init() {
        super.init();
        if(menu.getWagon() != null)
            layoutSeats();
    }

    private void layoutSeats() {
        BigCartWagon wagon = menu.getWagon();
        if(wagon == null)
            return;
        if(seatButtons != null) {
            for(SeatButton button : seatButtons)
                removeWidget(button);
        }
        List<SeatButton> buttons = new ArrayList<>(BigCartMenu.SEAT_ORDER.length + 2);
        for(int n = 0; n < BigCartMenu.SEAT_ORDER.length; n++) {
            int seat = BigCartMenu.SEAT_ORDER[n];
            SeatButton button = SeatButton.placed(wagon, seat,
                    leftPos + BigCartMenu.SEAT_X + n * BigCartMenu.SEAT_STEP, topPos + BigCartMenu.SEAT_Y, 26);
            button.setMessage(Component.literal(Integer.toString(n + 1)));
            button.refresh();
            buttons.add(button);
        }
        int slots = wagon.hitchSlots();
        if(slots > 0) {
            SeatButton left = SeatButton.placedDraft(wagon, AbstractWagon.Side.LEFT,
                    leftPos + BigCartMenu.draftX(slots, 0), topPos + BigCartMenu.DRAFT_Y, 26);
            left.refresh();
            buttons.add(left);
        }
        if(slots > 1) {
            SeatButton right = SeatButton.placedDraft(wagon, AbstractWagon.Side.RIGHT,
                    leftPos + BigCartMenu.draftX(slots, 1), topPos + BigCartMenu.DRAFT_Y, 26);
            right.refresh();
            buttons.add(right);
        }
        SeatButton.bind(buttons);
        seatButtons = buttons;
        shownHitch = slots;
        for(SeatButton button : buttons)
            addRenderableWidget(button);
    }

    private void layoutCargo() {
        BigCartWagon wagon = menu.getWagon();
        boolean open = wagon == null || !wagon.hasCabinPassenger();
        for(int i = 0; i < BigCartWagon.CARGO && i < menu.wheelStart(); i++) {
            Slot slot = menu.slots.get(i);
            if(open)
                moveSlot(slot, BigCartMenu.cargoX(i), BigCartMenu.cargoY(i));
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

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        int ownerLeft = titleLabelX + font.width(title) + 12;
        WagonGui.drawHeader(graphics, font, menu.getWagon(), titleLabelY, imageWidth, ownerLeft);
        BigCartWagon wagon = menu.getWagon();
        if(wagon == null)
            return;
        if(wagon.hasCabinPassenger()) {
            Component taken = wagon.bedHasAnimal() ? CARGO_ANIMALS : CARGO_PLAYERS;
            int center = BigCartMenu.CARGO_X + BigCartMenu.COLUMNS * 18 / 2;
            graphics.drawString(font, taken, center - font.width(taken) / 2, 44, 4210752, false);
        }
        drawCentered(graphics, Component.translatable(harnessKey(wagon.hitchSlots())), BigCartMenu.HARNESS_X + 8, BigCartMenu.HARNESS_Y - 12);
        int wheelCenter = BigCartMenu.WHEEL_X + 17;
        drawCentered(graphics, Component.translatable("gui.trotting_wagons.wheels"), wheelCenter, BigCartMenu.WHEEL_Y - 22);
        drawCentered(graphics, wagon.wheelSet().label(), wheelCenter, BigCartMenu.WHEEL_Y - 12);
        int slotsCenter = BigCartMenu.LANTERN_X + 8;
        drawCentered(graphics, Component.translatable("gui.trotting_wagons.repair"), slotsCenter, BigCartMenu.SEAT_Y);
        drawCentered(graphics, wagon.conditionName(), slotsCenter, BigCartMenu.SEAT_Y + 10);
        drawCentered(graphics, Component.translatable("gui.trotting_wagons.custom_items"), slotsCenter, BigCartMenu.CUSTOM_Y);
        drawCentered(graphics, Component.translatable("gui.trotting_wagons.custom_items.line2"), slotsCenter, BigCartMenu.CUSTOM_Y + 10);
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
        RepairLayout.place(menu.slots, menu.getWagon(), BigCartMenu.LANTERN_X + 8, BigCartMenu.SEAT_Y);
        layoutCargo();
        super.containerTick();
        BigCartWagon wagon = menu.getWagon();
        if(wagon != null && wagon.hitchSlots() != shownHitch)
            layoutSeats();
        if(seatButtons != null && wagon != null)
            SeatButtons.refresh(seatButtons);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        RepairLayout.place(menu.slots, menu.getWagon(), BigCartMenu.LANTERN_X + 8, BigCartMenu.SEAT_Y);
        layoutCargo();
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, BigCartMenu.IMAGE_WIDTH, BigCartMenu.IMAGE_HEIGHT);
        BigCartWagon wagon = menu.getWagon();
        if(wagon != null) {
            int slots = wagon.hitchSlots();
            for(int i = 0; i < slots; i++)
                graphics.blit(BIG_SLOT, leftPos + BigCartMenu.draftX(slots, i), topPos + BigCartMenu.DRAFT_Y, 0, 0, 26, 26, 26, 26);
            SeatButtons.drawHealth(graphics, font, wagon, leftPos + BigCartMenu.HEALTH_X, topPos + BigCartMenu.HEALTH_Y, BigCartMenu.HEALTH_W);
        }
        for(Slot slot : menu.slots) {
            if(slot instanceof BigCartCargoSlot && slot.isActive())
                WagonGui.drawSlot(graphics, leftPos, topPos, slot);
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
        for(SeatButton button : seatButtons)
            button.renderDelayedTooltip(graphics, font, mouseX, mouseY);
    }

}
