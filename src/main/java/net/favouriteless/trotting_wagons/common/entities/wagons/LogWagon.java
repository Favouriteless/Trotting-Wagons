package net.favouriteless.trotting_wagons.common.entities.wagons;

import net.favouriteless.trotting_wagons.ServerConfig;
import net.favouriteless.trotting_wagons.TrottingWagons;
import net.favouriteless.trotting_wagons.common.entities.base.AbstractWagon;
import net.favouriteless.trotting_wagons.common.entities.base.WagonSeat;
import net.favouriteless.trotting_wagons.common.init.TWItems;
import net.favouriteless.trotting_wagons.common.items.WagonItem;
import net.favouriteless.trotting_wagons.common.menus.LogMenu;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.ItemStackHandler;

public class LogWagon extends AbstractWagon implements MenuProvider {

    public static final int CAPACITY = 7040;
    public static final int TYPE_SLOTS = 90;
    public static final int LOG_STEP = 128;
    public static final int LOG_BONES = 11;
    public static final int PARTIAL_LIMIT = 1000;

    private static final EntityDataAccessor<Integer> DATA_CARGO = SynchedEntityData.defineId(LogWagon.class, EntityDataSerializers.INT);
    /** 0 none, 1 shafts (bone shafts, one animal), 2 tongue (bone Tongue, two animals). */
    private static final EntityDataAccessor<Integer> DATA_HARNESS = SynchedEntityData.defineId(LogWagon.class, EntityDataSerializers.INT);

    public static final WagonSeat[] SEATS = new WagonSeat[] {
            new WagonSeat(seat(0, 14, -32.25D), WagonSeat.Kind.DRIVER)
    };

    private final NonNullList<ItemStack> cargoStacks = NonNullList.withSize(TYPE_SLOTS, ItemStack.EMPTY);
    private final int[] sagKinds = new int[WHEEL_COUNT];
    private int sagKey = Integer.MIN_VALUE;
    private WheelSag sagPose = WheelSag.NONE;
    private final ItemStackHandler cargo = new ItemStackHandler(cargoStacks) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return acceptsCargo(stack);
        }

        @Override
        public int getSlotLimit(int slot) {
            return CAPACITY;
        }

        @Override
        protected int getStackLimit(int slot, ItemStack stack) {
            return roomIn(slot);
        }

        @Override
        protected void onContentsChanged(int slot) {
            mergeSameKinds();
            syncCargo();
        }
    };
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

    public LogWagon(EntityType<LogWagon> type, Level level) {
        super(type, level, 19.5D / 16D, 19D / 16D, SEATS, ServerConfig.INSTANCE.logSpeedEmpty.get(),
                ServerConfig.INSTANCE.logTurnRate.get(), ServerConfig.INSTANCE.logHealth.get(),
                9.5D / 16D, 77.88461D / 16D, 13);
    }

    public static boolean acceptsCargo(ItemStack stack) {
        return !stack.isEmpty() && stack.is(TrottingWagons.LOG_WAGON_CARGO);
    }

    public static int visibleLogs(int count) {
        if(count <= 0)
            return 0;
        return Math.min(LOG_BONES, (count + LOG_STEP - 1) / LOG_STEP);
    }

    public ItemStackHandler getCargo() {
        return cargo;
    }

    public static ItemStackHandler getItemCapability(LogWagon wagon, Void ctx) {
        return wagon.cargo;
    }

    public int cargoCount() {
        int stored = 0;
        for(ItemStack stack : cargoStacks)
            stored += stack.getCount();
        if(level().isClientSide)
            return Math.max(stored, entityData.get(DATA_CARGO));
        return stored;
    }

    public int syncedCargo() {
        return entityData.get(DATA_CARGO);
    }

    public int roomIn(int slot) {
        if(slot < 0 || slot >= TYPE_SLOTS)
            return 0;
        int inSlot = cargoStacks.get(slot).getCount();
        return Math.max(0, CAPACITY - (cargoCount() - inSlot));
    }

    public int placeholderSlot() {
        if(cargoCount() >= CAPACITY)
            return -1;
        for(int i = 0; i < TYPE_SLOTS; i++) {
            if(cargoStacks.get(i).isEmpty())
                return i;
        }
        return -1;
    }

    @Override
    public WheelSag wheelSag() {
        int mask = getWheelMask() & 0x0F;
        int key = mask;
        for(int slot = 0; slot < WHEEL_COUNT; slot++) {
            int kind = getWheelKind(slot).ordinal();
            sagKinds[slot] = kind;
            key |= kind << (4 + slot * 2);
        }
        if(key != sagKey) {
            sagKey = key;
            sagPose = LogWagonSag.pose(mask, sagKinds);
        }
        return sagPose;
    }

    @Override
    protected AABB makeBoundingBox() {
        return orientedBody(1.625D, 0.0D, 3.207D, 2.562D, 2.266D);
    }

    @Override
    protected double tongueLength() {
        return 57D / 16D;
    }

    @Override
    protected DyeColor getDefaultColor() {
        return null;
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if(!isAlive())
            return InteractionResult.PASS;
        if(!player.isSecondaryUseActive() && player.getVehicle() != this && acceptsCargo(player.getItemInHand(hand))) {
            if(!level().isClientSide)
                offerCargo(player, hand);
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        return super.interact(player, hand);
    }

    private void offerCargo(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        int slot = slotFor(stack);
        if(slot < 0)
            return;
        ItemStack input = player.getAbilities().instabuild ? stack.copy() : stack;
        ItemStack rest = cargo.insertItem(slot, input, false);
        if(!player.getAbilities().instabuild)
            player.setItemInHand(hand, rest);
    }

    private int slotFor(ItemStack stack) {
        if(!acceptsCargo(stack))
            return -1;
        int empty = -1;
        for(int i = 0; i < TYPE_SLOTS; i++) {
            ItemStack existing = cargoStacks.get(i);
            if(existing.isEmpty()) {
                if(empty < 0)
                    empty = i;
                continue;
            }
            if(ItemStack.isSameItemSameComponents(existing, stack))
                return i;
        }
        if(cargoCount() >= CAPACITY)
            return -1;
        return empty;
    }

    public void syncCargo() {
        if(level() == null || level().isClientSide)
            return;
        entityData.set(DATA_CARGO, cargoCount());
        applyCargoSpeed();
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if(DATA_CARGO.equals(key))
            applyCargoSpeed();
    }

    private void applyCargoSpeed() {
        int count = level() != null && level().isClientSide ? syncedCargo() : cargoCount();
        double speed = ServerConfig.INSTANCE.logSpeedFull.get();
        if(count <= 0)
            speed = ServerConfig.INSTANCE.logSpeedEmpty.get();
        else if(count <= PARTIAL_LIMIT)
            speed = ServerConfig.INSTANCE.logSpeedPartial.get();
        setSpeedCoefficient(speed);
    }

    public void mergeSameKinds() {
        for(int i = 0; i < TYPE_SLOTS; i++) {
            ItemStack stack = cargoStacks.get(i);
            if(stack.isEmpty())
                continue;
            for(int earlier = 0; earlier < i; earlier++) {
                ItemStack home = cargoStacks.get(earlier);
                if(home.isEmpty() || !ItemStack.isSameItemSameComponents(home, stack))
                    continue;
                home.grow(stack.getCount());
                cargoStacks.set(i, ItemStack.EMPTY);
                break;
            }
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_CARGO, 0);
        builder.define(DATA_HARNESS, 0);
    }

    @Override
    public int hitchSlots() {
        int slots = entityData.get(DATA_HARNESS);
        return slots < 0 ? 0 : Math.min(slots, 2);
    }

    @Override
    protected double singleHarnessFactor() {
        return 0.6D;
    }

    @Override
    protected Vec3 hitchOffset(Side side) {
        if(hitchSlots() >= 2)
            return super.hitchOffset(side);
        return new Vec3(0, 0, 77.88461D / 16D);
    }

    @Override
    public ItemStackHandler getHarnessItems() {
        return harnessItems;
    }

    private void syncHarness() {
        ItemStack stack = harness.get(0);
        int slots = stack.is(TWItems.TONGUE.get()) ? 2 : stack.is(TWItems.SHAFTS.get()) ? 1 : 0;
        entityData.set(DATA_HARNESS, slots);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        ListTag piles = new ListTag();
        for(ItemStack stack : cargoStacks) {
            if(stack.isEmpty())
                continue;
            CompoundTag pile = new CompoundTag();
            // save() encodes into a copy of the prefix. The compound passed in stays empty.
            Tag saved = stack.copyWithCount(1).save(registryAccess(), new CompoundTag());
            pile.put("item", saved);
            pile.putInt("pile", stack.getCount());
            piles.add(pile);
        }
        nbt.put("Cargo", piles);
        CompoundTag harnessTag = new CompoundTag();
        ContainerHelper.saveAllItems(harnessTag, harness, registryAccess());
        nbt.put("Harness", harnessTag);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        for(int i = 0; i < cargoStacks.size(); i++)
            cargoStacks.set(i, ItemStack.EMPTY);
        if(nbt.contains("Cargo", Tag.TAG_COMPOUND))
            ContainerHelper.loadAllItems(nbt.getCompound("Cargo"), cargoStacks, registryAccess());
        else if(nbt.contains("Cargo", Tag.TAG_LIST))
            loadPiles(nbt.getList("Cargo", Tag.TAG_COMPOUND));
        mergeSameKinds();
        syncCargo();
        if(nbt.contains("Harness", Tag.TAG_COMPOUND)) {
            harness.set(0, ItemStack.EMPTY);
            ContainerHelper.loadAllItems(nbt.getCompound("Harness"), harness, registryAccess());
        }
        syncHarness();
    }

    private void loadPiles(ListTag piles) {
        int slot = 0;
        for(Tag element : piles) {
            if(slot >= TYPE_SLOTS || !(element instanceof CompoundTag pile))
                break;
            Tag saved = pile.get("item");
            if(saved == null)
                continue;
            ItemStack stack = ItemStack.parse(registryAccess(), saved).orElse(ItemStack.EMPTY);
            if(stack.isEmpty())
                continue;
            int count = pile.getInt("pile");
            if(count <= 0)
                continue;
            stack.setCount(count);
            cargoStacks.set(slot++, stack);
        }
    }

    @Override
    protected void onDestroyed(boolean dropItem) {
        if(!level().isClientSide) {
            mergeSameKinds();
            for(ItemStack stack : cargoStacks) {
                int left = stack.getCount();
                int piece = Math.min(99, Math.max(1, stack.getMaxStackSize()));
                while(left > 0) {
                    int count = Math.min(piece, left);
                    spawnAtLocation(stack.copyWithCount(count));
                    left -= count;
                }
            }
        }
        super.onDestroyed(dropItem);
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new LogMenu(id, inventory, this);
    }

    @Override
    public ItemStack getPickResult() {
        return WagonItem.setupComponents(new ItemStack(TWItems.LOG_WAGON.get()), getColor());
    }

    private static Vec3 seat(double x, double y, double z) {
        return new Vec3(x / 16D, y / 16D, -z / 16D).add(0, 0.7D, 0);
    }

}
