package net.favouriteless.trotting_wagons.common.menus;

import net.favouriteless.trotting_wagons.TrottingWagons;
import net.favouriteless.trotting_wagons.common.entities.base.AbstractWagon;
import net.favouriteless.trotting_wagons.common.init.TWItems;
import net.favouriteless.trotting_wagons.common.init.TWMenuTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class ArmoredMenu extends AbstractContainerMenu {

    public static final int IMAGE_WIDTH = 320;
    public static final int IMAGE_HEIGHT = 174;
    public static final int HEALTH_X = 13;
    public static final int HEALTH_Y = 20;
    public static final int HEALTH_W = 294;
    public static final int SEAT_X = 69;
    public static final int SEAT_Y = 38;
    public static final int SEAT_STEP = 26;
    public static final int DRAFT_Y = 38;
    public static final int DRAFT_SINGLE_X = 21;
    public static final int DRAFT_PAIR_X = 8;
    public static final int DRAFT_STRIDE = 26;
    public static final int HARNESS_X = 26;
    public static final int HARNESS_Y = 82;
    public static final int INV_X = 80;
    public static final int INV_Y = 90;
    public static final int WHEEL_X = 17;
    public static final int WHEEL_Y = 130;
    public static final int REPAIR_X = 269;

    private static final int WHEEL_END = AbstractWagon.WHEEL_COUNT;

    private final AbstractWagon wagon;
    private final int harnessSlot;
    private final int repairStart;

    public static int draftX(int slots, int index) {
        if(slots < 2)
            return DRAFT_SINGLE_X;
        return DRAFT_PAIR_X + index * DRAFT_STRIDE;
    }

    public ArmoredMenu(int id, Inventory inventory, AbstractWagon wagon) {
        super(TWMenuTypes.ARMORED.get(), id);
        this.wagon = wagon;
        if(wagon != null && !wagon.level().isClientSide)
            wagon.refreshRepairSlots();

        for(int i = 0; i < AbstractWagon.WHEEL_COUNT; i++)
            addSlot(new WheelSlot(wagon.getWheels(), i, WHEEL_X + (i % 2) * 18, WHEEL_Y + (i / 2) * 18));

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
            addSlot(new RepairSlot(wagon, wagon.getRepair(), i, REPAIR_X, 60));
    }

    public ArmoredMenu(int id, Inventory inventory, FriendlyByteBuf data) {
        this(id, inventory, getWagon(inventory, data));
    }

    public AbstractWagon getWagon() {
        return wagon;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if(!slot.hasItem() || !slot.mayPickup(player))
            return ItemStack.EMPTY;
        ItemStack item = slot.getItem();
        ItemStack copy = item.copy();
        boolean fromPlayer = index >= WHEEL_END && index < harnessSlot;
        boolean moved;
        if(index < WHEEL_END || index == harnessSlot || index >= repairStart) {
            moved = moveItemStackTo(item, WHEEL_END, harnessSlot, false);
        } else if(!fromPlayer) {
            return ItemStack.EMPTY;
        } else if(item.is(TrottingWagons.WAGON_WHEELS)) {
            moved = moveItemStackTo(item, 0, WHEEL_END, false);
        } else if(TWItems.isHarness(item)) {
            moved = moveItemStackTo(item, harnessSlot, harnessSlot + 1, false);
        } else if(wagon != null && wagon.isRepairMaterial(item)) {
            moved = RepairShift.fill(item, repairStart, slots.size(), this::moveItemStackTo);
        } else {
            return ItemStack.EMPTY;
        }
        if(!moved)
            return ItemStack.EMPTY;
        if(item.isEmpty())
            slot.setByPlayer(ItemStack.EMPTY);
        else
            slot.setChanged();
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return wagon != null && wagon.isAlive() && player.getVehicle() == wagon;
    }

    private static AbstractWagon getWagon(Inventory inventory, FriendlyByteBuf data) {
        return inventory.player.level().getEntity(data.readInt()) instanceof AbstractWagon wagon ? wagon : null;
    }

}
