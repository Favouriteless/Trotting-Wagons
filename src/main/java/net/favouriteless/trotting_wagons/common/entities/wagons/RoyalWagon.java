package net.favouriteless.trotting_wagons.common.entities.wagons;

import net.favouriteless.trotting_wagons.ServerConfig;
import net.favouriteless.trotting_wagons.common.entities.base.AbstractInventoryWagon;
import net.favouriteless.trotting_wagons.common.entities.base.WagonSeat;
import net.favouriteless.trotting_wagons.common.init.TWItems;
import net.favouriteless.trotting_wagons.common.items.WagonItem;
import net.favouriteless.trotting_wagons.common.menus.RoyalMenu;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.ItemStackHandler;

public class RoyalWagon extends AbstractInventoryWagon {

    public static final int CARGO = 40;
    /** Two riders share a bench. 7px off center matches the bench that already held a pair. */
    private static final double BENCH_X = 7D / 16D;
    private static final double BENCH_Y = 1.45D;
    private static final double NEAR_Z = 0.7D;
    private static final double FAR_Z = -0.7D;

    /** 0 none, 1 shafts (bone shafts, one animal), 2 tongue (bone Tongue, two animals). */
    private static final EntityDataAccessor<Integer> DATA_HARNESS = SynchedEntityData.defineId(RoyalWagon.class, EntityDataSerializers.INT);

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
            new WagonSeat(new Vec3(0, 1.3D, 1.8D), WagonSeat.Kind.DRIVER),
            new WagonSeat(new Vec3(BENCH_X, BENCH_Y, FAR_Z), WagonSeat.Kind.PLAYER),
            new WagonSeat(new Vec3(BENCH_X, BENCH_Y, NEAR_Z), WagonSeat.Kind.PLAYER),
            new WagonSeat(new Vec3(-BENCH_X, BENCH_Y, NEAR_Z), WagonSeat.Kind.PLAYER),
            new WagonSeat(new Vec3(-BENCH_X, BENCH_Y, FAR_Z), WagonSeat.Kind.PLAYER)
    };

    public RoyalWagon(EntityType<RoyalWagon> type, Level level) {
        super(type, level, 1.1D, 1.3D, SEATS, ServerConfig.INSTANCE.royalSpeed.get(),
                ServerConfig.INSTANCE.conestogaTurnRate.get(),
                ServerConfig.INSTANCE.royalHealth.get(), 0.6D, 4.3D, 7, CARGO);
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

    @Override
    protected double singleHarnessFactor() {
        return 0.9D;
    }

    @Override
    protected Vec3 hitchOffset(Side side) {
        if(hitchSlots() >= 2)
            return super.hitchOffset(side);
        return new Vec3(0, 0, 4.3D);
    }

    @Override
    public ItemStackHandler getHarnessItems() {
        return harnessItems;
    }

    @Override
    public void readAdditionalSaveData(CompoundTag nbt) {
        dropOverflow(nbt);
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
        CompoundTag harnessTag = new CompoundTag();
        ContainerHelper.saveAllItems(harnessTag, harness, registryAccess());
        nbt.put("Harness", harnessTag);
    }

    /** Older saves had 54 cargo slots. Anything past the new grid is dropped beside the wagon. */
    private void dropOverflow(CompoundTag nbt) {
        if(level() == null || level().isClientSide || !nbt.contains(ContainerHelper.TAG_ITEMS, Tag.TAG_LIST))
            return;
        ListTag items = nbt.getList(ContainerHelper.TAG_ITEMS, Tag.TAG_COMPOUND);
        for(Tag element : items) {
            if(!(element instanceof CompoundTag item))
                continue;
            int slot = item.getByte("Slot") & 255;
            if(slot < CARGO)
                continue;
            ItemStack stack = ItemStack.parse(registryAccess(), item).orElse(ItemStack.EMPTY);
            if(!stack.isEmpty())
                spawnAtLocation(stack);
        }
    }

    private void syncHarness() {
        ItemStack stack = harness.get(0);
        int slots = stack.is(TWItems.TONGUE.get()) ? 2 : stack.is(TWItems.SHAFTS.get()) ? 1 : 0;
        entityData.set(DATA_HARNESS, slots);
    }

    /** Rotated cart skirt. Its lowest corner is 4.0542px. */
    @Override
    protected double bareDrop() {
        return 0.2533850438077787D;
    }

    @Override
    protected AABB makeBoundingBox() {
        return orientedBody(1.438D, 0.0D, 3.312D, 2.266D, 2.424D);
    }

    @Override
    protected double tongueLength() {
        return 64.12D / 16D;
    }

    @Override
    protected double tongueHeight() {
        return 10.5D / 16D;
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new RoyalMenu(id, inventory, this);
    }

    @Override
    public ItemStack getPickResult() {
        return WagonItem.setupComponents(new ItemStack(TWItems.ROYAL_WAGON.get()), this.getColor());
    }

}
