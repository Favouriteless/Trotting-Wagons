package net.favouriteless.trotting_wagons.common.entities.wagons;

import net.favouriteless.trotting_wagons.ServerConfig;
import net.favouriteless.trotting_wagons.common.entities.base.AbstractInventoryWagon;
import net.favouriteless.trotting_wagons.common.entities.base.WagonSeat;
import net.favouriteless.trotting_wagons.common.init.TWItems;
import net.favouriteless.trotting_wagons.common.items.WagonItem;
import net.favouriteless.trotting_wagons.common.menus.ConestogaMenu;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.ItemStackHandler;

public class ConestogaWagon extends AbstractInventoryWagon {

    public static final int CARGO = 42;
    /** Main chest, then the optional hold. Older saves kept only the first page. */
    public static final int CARGO_VERSION = 2;
    private static final int LEFT = 1;
    private static final int RIGHT = 2;
    private static final int LARGE = 3;

    /** 0 none, 1 shafts (bone shafts, one animal), 2 tongue (bone Tongue, two animals). */
    private static final EntityDataAccessor<Integer> DATA_HARNESS = SynchedEntityData.defineId(ConestogaWagon.class, EntityDataSerializers.INT);

    private final NonNullList<ItemStack> harness = NonNullList.withSize(1, ItemStack.EMPTY);
    private final ItemStackHandler harnessItems = new ItemStackHandler(harness) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return TWItems.isHarness(stack);
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        protected void onContentsChanged(int slot) {
            syncHarness();
        }
    };

    public static final WagonSeat[] SEATS = new WagonSeat[] {
            new WagonSeat(new Vec3(0, 1.25D, 2.1D), WagonSeat.Kind.DRIVER),
            new WagonSeat(new Vec3(-0.4375D, 1.18D, 1.1D), WagonSeat.Kind.UNIVERSAL),
            new WagonSeat(new Vec3(0.4375D, 1.18D, 1.1D), WagonSeat.Kind.UNIVERSAL),
            new WagonSeat(new Vec3(0, 1.18D, 0.19D), WagonSeat.Kind.ANIMALBIG)
    };

    public ConestogaWagon(EntityType<ConestogaWagon> type, Level level) {
        super(type, level, 1, 1, SEATS, ServerConfig.INSTANCE.conestogaSpeed.get(),
                ServerConfig.INSTANCE.conestogaTurnRate.get(), ServerConfig.INSTANCE.conestogaHealth.get(), 0.6D, 3.6D,
                13, CARGO * 2);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_HARNESS, 0);
    }

    @Override
    public int hitchSlots() {
        int slots = entityData.get(DATA_HARNESS);
        return slots < 0 ? 0 : Math.min(slots, 2);
    }

    /** Axle foot at y=6. The side hook is drawn from y=-2, already below the wheel plane. */
    @Override
    protected double bareDrop() {
        return 6D / 16D;
    }

    @Override
    protected AABB makeBoundingBox() {
        return orientedBody(1.382D, 0.0D, 2.815D, 2.382D, 2.445D);
    }

    @Override
    protected double singleHarnessFactor() {
        return 0.7D;
    }

    @Override
    protected Vec3 hitchOffset(Side side) {
        if(hitchSlots() >= 2)
            return super.hitchOffset(side);
        return new Vec3(0, 0, 3.6D);
    }

    @Override
    public ItemStackHandler getHarnessItems() {
        return harnessItems;
    }

    /** Anyone except the driver. The hold and these seats cannot be used together. */
    public boolean hasCabinPassenger() {
        for(int seat = 1; seat < getSeatCount(); seat++) {
            if(getSeatEntityId(seat) >= 0)
                return true;
        }
        return false;
    }

    public boolean holdHasItems() {
        ItemStackHandler items = getItems();
        for(int i = CARGO; i < items.getSlots(); i++) {
            if(!items.getStackInSlot(i).isEmpty())
                return true;
        }
        return false;
    }

    @Override
    protected boolean acceptsLargeSeat(Entity entity) {
        return isLargePassenger(entity);
    }

    @Override
    public Component seatBlockHint(int index) {
        if(index <= 0 || index >= getSeatCount() || getSeatEntityId(index) >= 0)
            return null;
        if(holdHasItems())
            return Component.translatable("gui.trotting_wagons.tooltip.hold_blocks_seats");
        if(index == LARGE && (occupied(LEFT) || occupied(RIGHT)))
            return Component.translatable("gui.trotting_wagons.tooltip.large_needs_space");
        if((index == LEFT || index == RIGHT) && occupied(LARGE))
            return Component.translatable("gui.trotting_wagons.tooltip.small_blocked");
        return null;
    }

    @Override
    protected boolean seatOpen(int index, Entity entity) {
        if(index > 0 && holdHasItems())
            return false;
        if(index == LARGE)
            return !occupied(LEFT, entity) && !occupied(RIGHT, entity);
        if(index == LEFT || index == RIGHT)
            return !occupied(LARGE, entity);
        return true;
    }

    private boolean occupied(int seat) {
        return occupied(seat, null);
    }

    private boolean occupied(int seat, Entity moving) {
        int id = getSeatEntityId(seat);
        return id >= 0 && (moving == null || id != moving.getId());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag nbt) {
        if(nbt.getInt("CargoVersion") < CARGO_VERSION)
            dropFromSlot(nbt, CARGO);
        super.readAdditionalSaveData(nbt);
        if(nbt.contains("Harness", Tag.TAG_COMPOUND)) {
            harness.set(0, ItemStack.EMPTY);
            ContainerHelper.loadAllItems(nbt.getCompound("Harness"), harness, registryAccess());
        }
        syncHarness();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        nbt.putInt("CargoVersion", CARGO_VERSION);
        CompoundTag harnessTag = new CompoundTag();
        ContainerHelper.saveAllItems(harnessTag, harness, registryAccess());
        nbt.put("Harness", harnessTag);
    }

    /** Items past the old single chest are dropped. A versioned save already stores both pages. */
    private void dropFromSlot(CompoundTag nbt, int first) {
        if(level() == null || level().isClientSide || !nbt.contains(ContainerHelper.TAG_ITEMS, Tag.TAG_LIST))
            return;
        ListTag items = nbt.getList(ContainerHelper.TAG_ITEMS, Tag.TAG_COMPOUND);
        ListTag kept = new ListTag();
        for(Tag element : items) {
            if(!(element instanceof CompoundTag item))
                continue;
            int slot = item.getByte("Slot") & 255;
            if(slot < first) {
                kept.add(item);
                continue;
            }
            ItemStack stack = ItemStack.parse(registryAccess(), item).orElse(ItemStack.EMPTY);
            if(!stack.isEmpty())
                spawnAtLocation(stack);
        }
        nbt.put(ContainerHelper.TAG_ITEMS, kept);
    }

    private void syncHarness() {
        ItemStack stack = harness.get(0);
        int slots = stack.is(TWItems.TONGUE.get()) ? 2 : stack.is(TWItems.SHAFTS.get()) ? 1 : 0;
        entityData.set(DATA_HARNESS, slots);
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new ConestogaMenu(id, inventory, this);
    }

    @Override
    public ItemStack getPickResult() {
        return WagonItem.setupComponents(new ItemStack(TWItems.CONESTOGA_WAGON.get()), this.getColor());
    }

}
