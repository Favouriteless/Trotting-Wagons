package net.favouriteless.trotting_wagons.common.menus;

import net.favouriteless.trotting_wagons.TrottingWagons;
import net.favouriteless.trotting_wagons.common.entities.base.AbstractWagon;
import net.favouriteless.trotting_wagons.common.entities.wagons.LogWagon;
import net.favouriteless.trotting_wagons.common.init.TWItems;
import net.favouriteless.trotting_wagons.common.init.TWMenuTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class LogMenu extends AbstractContainerMenu {

    public static final int COLUMNS = 15;
    public static final int IMAGE_WIDTH = 320;
    /** Painted height. One cargo row fits above the driver; more rows push this half down. */
    public static final int IMAGE_HEIGHT = 190;
    public static final int CARGO_X = 25;
    public static final int CARGO_Y = 14;
    public static final int ROW = 18;
    /** First pixel of the driver, harness, wheels, and player inventory. */
    public static final int CHROME_Y = 54;
    public static final int HEALTH_X = 13;
    public static final int HEALTH_Y = 36;
    public static final int HEALTH_W = 294;
    public static final int SEAT_X = 69;
    public static final int SEAT_Y = 54;
    public static final int TITLE_X = 101;
    public static final int TITLE_Y = 56;
    public static final int DISTANCE_GAP = 27;
    public static final int HARNESS_X = 26;
    public static final int HARNESS_Y = 98;
    public static final int INV_X = 80;
    public static final int INV_Y = 106;
    public static final int HOTBAR_Y = 164;
    public static final int WHEEL_X = 17;
    public static final int WHEEL_Y = 146;
    public static final int REPAIR_X = 269;
    public static final int DRAFT_Y = 54;
    public static final int DRAFT_SINGLE_X = 21;
    public static final int DRAFT_PAIR_X = 8;
    public static final int DRAFT_STEP = 26;

    private final LogWagon wagon;
    private final int wheelStart;
    private final int playerStart;
    private final int harnessSlot;
    private final int repairStart;

    public static int draftX(int slots, int index) {
        if(slots < 2)
            return DRAFT_SINGLE_X;
        return DRAFT_PAIR_X + index * DRAFT_STEP;
    }

    public LogMenu(int id, Inventory inventory, LogWagon wagon) {
        super(TWMenuTypes.LOG.get(), id);
        this.wagon = wagon;
        if(wagon != null && !wagon.level().isClientSide)
            wagon.refreshRepairSlots();

        for(int i = 0; i < LogWagon.TYPE_SLOTS; i++)
            addSlot(new LogCargoSlot(wagon, wagon.getCargo(), i, CARGO_X, CARGO_Y));

        this.wheelStart = slots.size();
        for(int i = 0; i < AbstractWagon.WHEEL_COUNT; i++)
            addSlot(new WheelSlot(wagon.getWheels(), i, WHEEL_X + (i % 2) * 18, WHEEL_Y + (i / 2) * 18));

        this.playerStart = slots.size();
        for(int x = 0; x < 9; x++)
            addSlot(new Slot(inventory, x, INV_X + x * 18, HOTBAR_Y));
        for(int y = 0; y < 3; y++) {
            for(int x = 0; x < 9; x++)
                addSlot(new Slot(inventory, 9 + x + y * 9, INV_X + x * 18, INV_Y + y * 18));
        }

        this.harnessSlot = slots.size();
        addSlot(new HarnessSlot(wagon, wagon.getHarnessItems(), 0, HARNESS_X, HARNESS_Y));
        this.repairStart = slots.size();
        for(int i = 0; i < AbstractWagon.REPAIR_SLOTS; i++)
            addSlot(new RepairSlot(wagon, wagon.getRepair(), i, REPAIR_X, SEAT_Y + 22));
    }

    public LogMenu(int id, Inventory inventory, FriendlyByteBuf data) {
        this(id, inventory, getWagon(inventory, data));
    }

    public LogWagon getWagon() {
        return wagon;
    }

    public int wheelStart() {
        return wheelStart;
    }

    public int playerStart() {
        return playerStart;
    }

    public int harnessSlot() {
        return harnessSlot;
    }

    public int repairStart() {
        return repairStart;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if(!slot.hasItem() || !slot.mayPickup(player))
            return ItemStack.EMPTY;
        ItemStack item = slot.getItem();
        ItemStack copy = item.copy();
        boolean moved;
        boolean repairHeld = false;
        if(index < playerStart || index >= harnessSlot) {
            moved = moveItemStackTo(item, playerStart, harnessSlot, false);
        } else if(item.is(TrottingWagons.WAGON_WHEELS)) {
            moved = moveItemStackTo(item, wheelStart, playerStart, false);
        } else if(TWItems.isHarness(item)) {
            moved = moveItemStackTo(item, harnessSlot, harnessSlot + 1, false);
        } else if(wagon != null && wagon.isRepairMaterial(item)
                && RepairShift.fill(item, repairStart, slots.size(), this::moveItemStackTo)) {
            moved = true;
            repairHeld = true;
        } else if(LogWagon.acceptsCargo(item)) {
            moved = moveItemStackTo(item, 0, wheelStart, false);
        } else {
            return ItemStack.EMPTY;
        }
        if(!moved)
            return ItemStack.EMPTY;
        if(item.isEmpty())
            slot.setByPlayer(ItemStack.EMPTY);
        else
            slot.setChanged();
        return repairHeld ? ItemStack.EMPTY : copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return wagon != null && wagon.isAlive() && player.getVehicle() == wagon;
    }

    private static LogWagon getWagon(Inventory inventory, FriendlyByteBuf data) {
        return inventory.player.level().getEntity(data.readInt()) instanceof LogWagon wagon ? wagon : null;
    }

}
