package net.favouriteless.trotting_wagons.client.screens;

import net.favouriteless.trotting_wagons.TrottingWagons;
import net.favouriteless.trotting_wagons.common.entities.base.AbstractWagon;
import net.favouriteless.trotting_wagons.common.entities.wagons.WareWagon;
import net.favouriteless.trotting_wagons.common.menus.RepairSlot;
import net.favouriteless.trotting_wagons.common.menus.WareMenu;
import net.favouriteless.trotting_wagons.common.network.packets.SelectWareTabPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Locale;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class WareScreen extends AbstractContainerScreen<WareMenu> {

    private static final ResourceLocation TEXTURE = TrottingWagons.id("textures/gui/ware_wagon.png");
    private static final ResourceLocation BIG_SLOT = TrottingWagons.id("textures/gui/icons/big_slot.png");

    private final List<WareTabButton> tabs = new ArrayList<>();
    private List<SeatButton> seatButtons;
    private int shownHitch = -1;

    public WareScreen(WareMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = WareMenu.IMAGE_WIDTH;
        this.imageHeight = WareMenu.IMAGE_HEIGHT;
        this.titleLabelY = 10000;
        this.inventoryLabelY = 10000;
    }

    @Override
    protected void init() {
        super.init();
        tabs.clear();
        for(int tab = 0; tab <= WareWagon.CHESTS; tab++) {
            WareTabButton button = new WareTabButton(
                    leftPos + WareMenu.TAB_X + tab * WareMenu.TAB_STEP, topPos + WareMenu.TAB_Y, tab, menu);
            tabs.add(button);
            addRenderableWidget(button);
        }
        layoutSeats();
        refreshTabs(false);
        layoutSlots();
    }

    @Override
    protected void containerTick() {
        RepairLayout.place(menu.slots, menu.getWagon(), WareMenu.REPAIR_X + 17, WareMenu.SEAT_Y);
        super.containerTick();
        refreshTabs(true);
        layoutSlots();
        WareWagon wagon = menu.getWagon();
        if(wagon != null && wagon.hitchSlots() != shownHitch)
            layoutSeats();
        if(seatButtons != null && wagon != null)
            SeatButtons.refresh(seatButtons);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        WareWagon wagon = menu.getWagon();
        if(wagon == null)
            return;
        graphics.drawString(font, title, WareMenu.TITLE_X, WareMenu.TITLE_Y, 4210752, false);
        Component distance = Component.translatable("gui.trotting_wagons.distance",
                String.format(Locale.ROOT, "%.1f", wagon.getDistance() / 1000.0F));
        graphics.drawString(font, distance, WareMenu.TITLE_X + font.width(title) + WareMenu.DISTANCE_GAP, WareMenu.TITLE_Y, 4210752, false);
        String ownerName = wagon.getOwnerName();
        if(!ownerName.isEmpty()) {
            Component owner = Component.translatable("gui.trotting_wagons.owner", ownerName);
            graphics.drawString(font, owner, WareMenu.TITLE_X, WareMenu.TITLE_Y + 12, 4210752, false);
        }
        drawCentered(graphics, Component.translatable(harnessKey(wagon.hitchSlots())), WareMenu.HARNESS_X + 8, 146);
        int wheelCenter = WareMenu.WHEEL_X + 17;
        drawCentered(graphics, Component.translatable("gui.trotting_wagons.wheels"), wheelCenter, 184);
        drawCentered(graphics, wagon.wheelSet().label(), wheelCenter, 194);
        int slotsCenter = WareMenu.REPAIR_X + 17;
        drawCentered(graphics, Component.translatable("gui.trotting_wagons.repair"), slotsCenter, WareMenu.SEAT_Y);
        drawCentered(graphics, wagon.conditionName(), slotsCenter, WareMenu.SEAT_Y + 10);
        drawCentered(graphics, Component.translatable("gui.trotting_wagons.custom_items"), slotsCenter, WareMenu.CUSTOM_Y);
        drawCentered(graphics, Component.translatable("gui.trotting_wagons.custom_items.line2"), slotsCenter, WareMenu.CUSTOM_Y + 10);
    }

    private void drawCentered(GuiGraphics graphics, Component text, int centerX, int y) {
        graphics.drawString(font, text, centerX - font.width(text) / 2, y, 4210752, false);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        RepairLayout.place(menu.slots, menu.getWagon(), WareMenu.REPAIR_X + 17, WareMenu.SEAT_Y);
        layoutSlots();
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, WareMenu.IMAGE_WIDTH, WareMenu.IMAGE_HEIGHT);
        WareWagon wagon = menu.getWagon();
        if(wagon != null) {
            int slots = wagon.hitchSlots();
            for(int i = 0; i < slots; i++)
                graphics.blit(BIG_SLOT, leftPos + WareMenu.draftX(slots, i), topPos + WareMenu.DRAFT_Y, 0, 0, 26, 26, 26, 26);
            SeatButtons.drawHealth(graphics, font, wagon,
                    leftPos + WareMenu.HEALTH_X, topPos + WareMenu.HEALTH_Y, WareMenu.HEALTH_W);
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

    private void layoutSeats() {
        WareWagon wagon = menu.getWagon();
        if(wagon == null)
            return;
        if(seatButtons != null) {
            for(SeatButton button : seatButtons)
                removeWidget(button);
        }
        List<SeatButton> buttons = new ArrayList<>();
        SeatButton driver = SeatButton.placed(wagon, 0, leftPos + WareMenu.SEAT_X, topPos + WareMenu.SEAT_Y, 26);
        driver.refresh();
        buttons.add(driver);
        int slots = wagon.hitchSlots();
        if(slots > 0) {
            SeatButton left = SeatButton.placedDraft(wagon, AbstractWagon.Side.LEFT,
                    leftPos + WareMenu.draftX(slots, 0), topPos + WareMenu.DRAFT_Y, 26);
            left.refresh();
            buttons.add(left);
        }
        if(slots > 1) {
            SeatButton right = SeatButton.placedDraft(wagon, AbstractWagon.Side.RIGHT,
                    leftPos + WareMenu.draftX(slots, 1), topPos + WareMenu.DRAFT_Y, 26);
            right.refresh();
            buttons.add(right);
        }
        SeatButton.bind(buttons);
        seatButtons = buttons;
        shownHitch = slots;
        for(SeatButton button : buttons)
            addRenderableWidget(button);
    }

    private static String harnessKey(int slots) {
        if(slots > 1)
            return "gui.trotting_wagons.harness.tongue";
        if(slots == 1)
            return "gui.trotting_wagons.harness.shafts";
        return "gui.trotting_wagons.harness.none";
    }

    private void refreshTabs(boolean tellServer) {
        WareWagon wagon = menu.getWagon();
        if(tellServer && menu.tab() > 0 && (wagon == null || !wagon.hasChest(menu.tab() - 1))) {
            menu.setTab(0);
            PacketDistributor.sendToServer(new SelectWareTabPayload(0));
        }
        for(WareTabButton button : tabs) {
            button.active = button.tab() == 0 || (wagon != null && wagon.hasChest(button.tab() - 1));
            button.setSelected(menu.tab() == button.tab());
        }
    }

    private void layoutSlots() {
        int cargo = 0;
        for(int index = menu.cargoStart(); index < menu.contentStart(); index++) {
            Slot slot = menu.slots.get(index);
            if(menu.tab() == 0)
                moveSlot(slot, WareMenu.gridX(cargo % WareMenu.GRID_COLUMNS), WareMenu.gridY(cargo / WareMenu.GRID_COLUMNS));
            else
                moveSlot(slot, -2000, -2000);
            cargo++;
        }
        for(int index = menu.contentStart(); index < menu.playerStart(); index++) {
            int relative = index - menu.contentStart();
            int chest = relative / WareWagon.CONTENTS;
            int offset = relative % WareWagon.CONTENTS;
            Slot slot = menu.slots.get(index);
            if(menu.tab() == chest + 1)
                moveSlot(slot, WareMenu.gridX(offset % WareMenu.GRID_COLUMNS), WareMenu.gridY(offset / WareMenu.GRID_COLUMNS));
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
