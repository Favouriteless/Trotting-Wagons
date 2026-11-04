package net.favouriteless.trotting_wagons.client.screens;

import net.favouriteless.trotting_wagons.TrottingWagons;
import net.favouriteless.trotting_wagons.common.entities.base.AbstractWagon;
import net.favouriteless.trotting_wagons.common.entities.wagons.ConestogaWagon;
import net.favouriteless.trotting_wagons.common.menus.ConestogaMenu;
import net.favouriteless.trotting_wagons.common.menus.RepairSlot;
import net.favouriteless.trotting_wagons.common.network.packets.SelectConestogaPagePayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.neoforged.neoforge.network.PacketDistributor;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class ConestogaScreen extends AbstractContainerScreen<ConestogaMenu> {

    private static final ResourceLocation TEXTURE = TrottingWagons.id("textures/gui/conestoga_wagon.png");
    private static final ResourceLocation BIG_SLOT = TrottingWagons.id("textures/gui/icons/big_slot.png");

    private final TooltipDelay tooltips = new TooltipDelay();
    private final List<ConestogaPageButton> pages = new ArrayList<>();
    private List<SeatButton> seatButtons;
    private int shownHitch = -1;

    public ConestogaScreen(ConestogaMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = ConestogaMenu.IMAGE_WIDTH;
        this.imageHeight = ConestogaMenu.IMAGE_HEIGHT;
        this.titleLabelY = 6;
        this.inventoryLabelX = ConestogaMenu.INV_X;
        this.inventoryLabelY = ConestogaMenu.INV_Y - 12;
    }

    @Override
    protected void init() {
        super.init();
        pages.clear();
        for(int page = 0; page < 2; page++) {
            ConestogaPageButton button = new ConestogaPageButton(
                    leftPos + ConestogaMenu.PAGE_X, topPos + ConestogaMenu.PAGE_Y + page * ConestogaMenu.PAGE_STEP, page, menu);
            pages.add(button);
            addRenderableWidget(button);
        }
        if(menu.getWagon() != null)
            layoutSeats();
        refreshPages(false);
    }

    private void layoutSeats() {
        ConestogaWagon wagon = menu.getWagon();
        if(wagon == null)
            return;
        if(seatButtons != null) {
            for(SeatButton button : seatButtons)
                removeWidget(button);
        }
        List<SeatButton> buttons = new ArrayList<>(ConestogaMenu.SEAT_ORDER.length + 2);
        for(int n = 0; n < ConestogaMenu.SEAT_ORDER.length; n++) {
            int seat = ConestogaMenu.SEAT_ORDER[n];
            SeatButton button = SeatButton.placed(wagon, seat,
                    leftPos + ConestogaMenu.SEAT_X + n * ConestogaMenu.SEAT_STEP, topPos + ConestogaMenu.SEAT_Y, 26);
            button.setMessage(Component.literal(Integer.toString(n + 1)));
            button.refresh();
            buttons.add(button);
        }
        int slots = wagon.hitchSlots();
        if(slots > 0) {
            SeatButton left = SeatButton.placedDraft(wagon, AbstractWagon.Side.LEFT,
                    leftPos + ConestogaMenu.draftX(slots, 0), topPos + ConestogaMenu.DRAFT_Y, 26);
            left.refresh();
            buttons.add(left);
        }
        if(slots > 1) {
            SeatButton right = SeatButton.placedDraft(wagon, AbstractWagon.Side.RIGHT,
                    leftPos + ConestogaMenu.draftX(slots, 1), topPos + ConestogaMenu.DRAFT_Y, 26);
            right.refresh();
            buttons.add(right);
        }
        SeatButton.bind(buttons);
        seatButtons = buttons;
        shownHitch = slots;
        for(SeatButton button : buttons)
            addRenderableWidget(button);
    }

    private void refreshPages(boolean tellServer) {
        ConestogaWagon wagon = menu.getWagon();
        boolean blocked = wagon != null && wagon.hasCabinPassenger();
        if(tellServer && menu.page() == 1 && blocked) {
            menu.setPage(0);
            PacketDistributor.sendToServer(new SelectConestogaPagePayload(0));
        }
        for(ConestogaPageButton button : pages) {
            boolean open = button.page() == 0 || !blocked;
            button.active = open;
            button.setSelected(menu.page() == button.page());
            Component tip = button.page() == 0
                    ? Component.translatable("gui.trotting_wagons.conestoga.cargo")
                    : Component.translatable(open
                            ? "gui.trotting_wagons.conestoga.hold"
                            : "gui.trotting_wagons.conestoga.hold.blocked");
            button.setTooltip(Tooltip.create(tip));
        }
    }

    private void layoutCargo() {
        int shown = menu.page() * ConestogaWagon.CARGO;
        for(int i = 0; i < ConestogaWagon.CARGO * 2 && i < menu.wheelStart(); i++) {
            Slot slot = menu.slots.get(i);
            if(i >= shown && i < shown + ConestogaWagon.CARGO)
                moveSlot(slot, ConestogaMenu.cargoX(i - shown), ConestogaMenu.cargoY(i - shown));
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
        ConestogaWagon wagon = menu.getWagon();
        if(wagon == null)
            return;
        drawCentered(graphics, Component.translatable(harnessKey(wagon.hitchSlots())), ConestogaMenu.HARNESS_X + 8, ConestogaMenu.HARNESS_Y - 12);
        int wheelCenter = ConestogaMenu.WHEEL_X + 17;
        drawCentered(graphics, Component.translatable("gui.trotting_wagons.wheels"), wheelCenter, ConestogaMenu.WHEEL_Y - 22);
        drawCentered(graphics, wagon.wheelSet().label(), wheelCenter, ConestogaMenu.WHEEL_Y - 12);
        int slotsCenter = ConestogaMenu.REPAIR_X + 17;
        drawCentered(graphics, Component.translatable("gui.trotting_wagons.repair"), slotsCenter, ConestogaMenu.SEAT_Y);
        drawCentered(graphics, wagon.conditionName(), slotsCenter, ConestogaMenu.SEAT_Y + 10);
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
        RepairLayout.place(menu.slots, menu.getWagon(), ConestogaMenu.REPAIR_X + 17, ConestogaMenu.SEAT_Y);
        refreshPages(true);
        layoutCargo();
        super.containerTick();
        ConestogaWagon wagon = menu.getWagon();
        if(wagon != null && wagon.hitchSlots() != shownHitch)
            layoutSeats();
        if(seatButtons != null && wagon != null)
            SeatButtons.refresh(seatButtons);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        RepairLayout.place(menu.slots, menu.getWagon(), ConestogaMenu.REPAIR_X + 17, ConestogaMenu.SEAT_Y);
        layoutCargo();
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, ConestogaMenu.IMAGE_WIDTH, ConestogaMenu.IMAGE_HEIGHT);
        ConestogaWagon wagon = menu.getWagon();
        if(wagon != null) {
            int slots = wagon.hitchSlots();
            for(int i = 0; i < slots; i++)
                graphics.blit(BIG_SLOT, leftPos + ConestogaMenu.draftX(slots, i), topPos + ConestogaMenu.DRAFT_Y, 0, 0, 26, 26, 26, 26);
            SeatButtons.drawHealth(graphics, font, wagon, leftPos + ConestogaMenu.HEALTH_X, topPos + ConestogaMenu.HEALTH_Y, ConestogaMenu.HEALTH_W);
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
