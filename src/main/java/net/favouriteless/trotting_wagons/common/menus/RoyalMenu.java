package net.favouriteless.trotting_wagons.common.menus;

import net.favouriteless.trotting_wagons.TrottingWagons;
import net.favouriteless.trotting_wagons.common.entities.base.AbstractWagon;
import net.favouriteless.trotting_wagons.common.entities.wagons.RoyalWagon;
import net.favouriteless.trotting_wagons.common.init.TWItems;
import net.favouriteless.trotting_wagons.common.init.TWMenuTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;

public class RoyalMenu extends AbstractContainerMenu {

    public static final int IMAGE_WIDTH = 320;
    public static final int IMAGE_HEIGHT = 250;
    public static final int COLUMNS = 10;
    public static final int CARGO_X = 71;
    public static final int CARGO_Y = 21;
    public static final int HEALTH_X = 13;
    public static final int HEALTH_Y = 96;
    public static final int HEALTH_W = 294;
    public static final int SEAT_X = 95;
    public static final int SEAT_Y = 114;
    public static final int SEAT_STEP = 26;
    /** Left to right: driver, the bench nearer the driver, then the far bench. */
    public static final int[] SEAT_ORDER = {0, 2, 3, 1, 4};
    public static final int DRAFT_Y = 114;
    public static final int DRAFT_SINGLE_X = 21;
    public static final int DRAFT_PAIR_X = 8;
    public static final int DRAFT_STRIDE = 26;
    public static final int HARNESS_X = 26;
    public static final int HARNESS_Y = 158;
    public static final int INV_X = 80;
    public static final int INV_Y = 166;
    public static final int WHEEL_X = 17;
    public static final int WHEEL_Y = 206;
    public static final int REPAIR_X = 269;

    private final RoyalWagon wagon;
    private final int wheelStart;
    private final int playerStart;
    private final int harnessSlot;
    private final int repairStart;

    public static int draftX(int slots, int index) {
        if(slots < 2)
            return DRAFT_SINGLE_X;
        return DRAFT_PAIR_X + index * DRAFT_STRIDE;
    }

    public RoyalMenu(int id, Inventory inventory, RoyalWagon wagon) {
        super(TWMenuTypes.ROYAL.get(), id);
        this.wagon = wagon;
        if(wagon != null && !wagon.level().isClientSide)
            wagon.refreshRepairSlots();

        for(int i = 0; i < RoyalWagon.CARGO; i++) {
            int column = i % COLUMNS;
            int row = i / COLUMNS;
            addSlot(new SlotItemHandler(wagon.getItems(), i, CARGO_X + column * 18, CARGO_Y + row * 18));
        }

        this.wheelStart = slots.size();
        for(int i = 0; i < AbstractWagon.WHEEL_COUNT; i++)
            addSlot(new WheelSlot(wagon.getWheels(), i, WHEEL_X + (i % 2) * 18, WHEEL_Y + (i / 2) * 18));

        this.playerStart = slots.size();
        for(int x = 0; x < 9; x++)
            addSlot(new Slot(inventory, x, INV_X + x * 18, INV_Y + 58));
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

    public RoyalMenu(int id, Inventory inventory, FriendlyByteBuf data) {
        this(id, inventory, getWagon(inventory, data));
    }

    public RoyalWagon getWagon() {
        return wagon;
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
        } else {
            moved = moveItemStackTo(item, 0, wheelStart, false);
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

    private static RoyalWagon getWagon(Inventory inventory, FriendlyByteBuf data) {
        return inventory.player.level().getEntity(data.readInt()) instanceof RoyalWagon wagon ? wagon : null;
    }

}
