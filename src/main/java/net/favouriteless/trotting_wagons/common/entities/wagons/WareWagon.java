package net.favouriteless.trotting_wagons.common.entities.wagons;

import net.favouriteless.trotting_wagons.ServerConfig;
import net.favouriteless.trotting_wagons.common.entities.base.AbstractWagon;
import net.favouriteless.trotting_wagons.common.entities.base.WagonSeat;
import net.favouriteless.trotting_wagons.common.init.TWItems;
import net.favouriteless.trotting_wagons.common.items.WagonItem;
import net.favouriteless.trotting_wagons.common.menus.WareMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.ItemStackHandler;

public class WareWagon extends AbstractWagon implements MenuProvider {

    public static final int CHESTS = 4;
    /** Saves through cargo version 3 stored three chest items. */
    private static final int STORED_CHESTS = 3;
    /** Painted grid is 9 columns by 3 rows, shared by the wagon tab and each chest. */
    public static final int CARGO = 27;
    public static final int CONTENTS = 27;
    public static final int SLOTS = CHESTS + CARGO + CHESTS * CONTENTS;
    private static final int CARGO_VERSION = 4;
    /** Version 2 stored a 10×3 grid: 30 cargo and 30 slots in each chest. */
    private static final int WIDE_CARGO = 30;
    private static final int WIDE_CONTENTS = 30;
    private static final int WIDE_SLOTS = STORED_CHESTS + WIDE_CARGO + STORED_CHESTS * WIDE_CONTENTS;
    /** Saves from before the grid shrank from 4×9 cargo and 27-slot chests. */
    private static final int LEGACY_CARGO = 36;
    private static final int LEGACY_CONTENTS = 27;
    private static final int LEGACY_SLOTS = STORED_CHESTS + LEGACY_CARGO + STORED_CHESTS * LEGACY_CONTENTS;
    private static final int VERSION3_SLOTS = STORED_CHESTS + CARGO + STORED_CHESTS * CONTENTS;

    private static final int CHEST_MASK = (1 << CHESTS) - 1;
    private static final EntityDataAccessor<Integer> DATA_CHESTS = SynchedEntityData.defineId(WareWagon.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_LANTERN = SynchedEntityData.defineId(WareWagon.class, EntityDataSerializers.BOOLEAN);
    /** 0 none, 1 shafts (bone shafts, one animal), 2 tongue (bone Tongue, two animals). */
    private static final EntityDataAccessor<Integer> DATA_HARNESS = SynchedEntityData.defineId(WareWagon.class, EntityDataSerializers.INT);
    /** Glass centre after the Lamp bone's 90° yaw, in model pixels. */
    private static final Vec3 LAMP_POS = pivot(14.53D, 42.2D, -22.68D);

    public static final WagonSeat[] SEATS = new WagonSeat[] {
            new WagonSeat(seat(0, 24, -27.75D), WagonSeat.Kind.DRIVER)
    };

    private final NonNullList<ItemStack> stacks = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
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
    private final NonNullList<ItemStack> lantern = NonNullList.withSize(1, ItemStack.EMPTY);
    private BlockPos lanternLight;
    private boolean loading;
    private final ItemStackHandler cargo = new ItemStackHandler(stacks) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if(slot < CHESTS)
                return isChest(stack);
            if(slot < CHESTS + CARGO)
                return true;
            int chest = chestOf(slot);
            return chest >= 0 && hasChest(chest);
        }

        @Override
        public int getSlotLimit(int slot) {
            return slot < CHESTS ? 1 : super.getSlotLimit(slot);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if(slot < CHESTS && !simulate && amount > 0 && isChest(getStackInSlot(slot)))
                dropContents(slot);
            return super.extractItem(slot, amount, simulate);
        }

        @Override
        public void setStackInSlot(int slot, ItemStack stack) {
            if(slot < CHESTS)
                releaseChest(slot, stack);
            super.setStackInSlot(slot, stack);
        }

        @Override
        protected void onContentsChanged(int slot) {
            if(slot < CHESTS)
                syncChests();
        }
    };
    private final ItemStackHandler lanternItems = new ItemStackHandler(lantern) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return stack.is(Items.LANTERN);
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        protected void onContentsChanged(int slot) {
            syncLantern();
        }
    };

    public WareWagon(EntityType<WareWagon> type, Level level) {
        super(type, level, 19.5D / 16D, 19D / 16D, SEATS, ServerConfig.INSTANCE.wareSpeedEmpty.get(),
                ServerConfig.INSTANCE.wareTurnRate.get(), ServerConfig.INSTANCE.wareHealth.get(),
                9.5D / 16D, 77.88461D / 16D, 13);
    }

    public static boolean isChest(ItemStack stack) {
        return stack.is(Items.CHEST);
    }

    public static int chestOf(int slot) {
        if(slot < CHESTS + CARGO)
            return -1;
        return (slot - CHESTS - CARGO) / CONTENTS;
    }

    public static int contentIndex(int chest, int offset) {
        return CHESTS + CARGO + chest * CONTENTS + offset;
    }

    public ItemStackHandler getCargo() {
        return cargo;
    }

    public ItemStackHandler getLanternItems() {
        return lanternItems;
    }

    public boolean hasChest(int index) {
        return index >= 0 && index < CHESTS && (entityData.get(DATA_CHESTS) & (1 << index)) != 0;
    }

    public int chestCount() {
        return Integer.bitCount(entityData.get(DATA_CHESTS) & CHEST_MASK);
    }

    public boolean hasLantern() {
        return entityData.get(DATA_LANTERN);
    }

    /** Ladder foot at y=6. */
    @Override
    protected double bareDrop() {
        return 6D / 16D;
    }

    @Override
    protected AABB makeBoundingBox() {
        return orientedBody(1.625D, 0.0D, 2.981D, 2.062D, 2.078D);
    }

    @Override
    protected double tongueLength() {
        return 57D / 16D;
    }

    @Override
    protected double tongueHeight() {
        return 8.5D / 16D;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_CHESTS, 0);
        builder.define(DATA_LANTERN, false);
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

    @Override
    public void tick() {
        super.tick();
        if(!level().isClientSide && tickCount % 10 == 0)
            updateLanternLight();
    }

    @Override
    public void remove(RemovalReason reason) {
        if(reason.shouldDestroy())
            clearLanternLight(true);
        super.remove(reason);
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if(!isAlive())
            return InteractionResult.PASS;
        if(!player.isSecondaryUseActive() && player.getVehicle() != this && isChest(player.getItemInHand(hand))) {
            if(!level().isClientSide)
                offerChest(player, hand);
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        return super.interact(player, hand);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        loading = true;
        for(int i = 0; i < stacks.size(); i++)
            stacks.set(i, ItemStack.EMPTY);
        if(nbt.contains("Cargo", Tag.TAG_COMPOUND))
            readCargo(nbt);
        for(int i = 0; i < lantern.size(); i++)
            lantern.set(i, ItemStack.EMPTY);
        if(nbt.contains("Lantern", Tag.TAG_COMPOUND))
            ContainerHelper.loadAllItems(nbt.getCompound("Lantern"), lantern, registryAccess());
        loading = false;
        syncChests();
        syncLantern();
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
        CompoundTag cargoTag = new CompoundTag();
        ContainerHelper.saveAllItems(cargoTag, stacks, registryAccess());
        nbt.put("Cargo", cargoTag);
        CompoundTag lanternTag = new CompoundTag();
        ContainerHelper.saveAllItems(lanternTag, lantern, registryAccess());
        nbt.put("Lantern", lanternTag);
        CompoundTag harnessTag = new CompoundTag();
        ContainerHelper.saveAllItems(harnessTag, harness, registryAccess());
        nbt.put("Harness", harnessTag);
    }

    private void syncHarness() {
        ItemStack stack = harness.get(0);
        int slots = stack.is(TWItems.TONGUE.get()) ? 2 : stack.is(TWItems.SHAFTS.get()) ? 1 : 0;
        entityData.set(DATA_HARNESS, slots);
    }

    @Override
    protected void onDestroyed(boolean dropItem) {
        if(!level().isClientSide) {
            clearLanternLight(true);
            loading = true;
            for(int i = 0; i < stacks.size(); i++) {
                ItemStack stack = stacks.get(i);
                if(stack.isEmpty())
                    continue;
                stacks.set(i, ItemStack.EMPTY);
                spawnAtLocation(stack);
            }
            for(int i = 0; i < lantern.size(); i++) {
                ItemStack stack = lantern.get(i);
                if(stack.isEmpty())
                    continue;
                lantern.set(i, ItemStack.EMPTY);
                spawnAtLocation(stack);
            }
            ItemStackHandler repair = getRepair();
            for(int i = 0; i < REPAIR_SLOTS; i++) {
                ItemStack stack = repair.getStackInSlot(i).copy();
                if(stack.isEmpty())
                    continue;
                repair.setStackInSlot(i, ItemStack.EMPTY);
                spawnAtLocation(stack);
            }
            loading = false;
            syncChests();
            syncLantern();
        }
        super.onDestroyed(dropItem);
    }

    /**
     * A stack whose count is already zero reports as air, so a shift-click that emptied the slot is handled by the menu.
     */
    public void dropChestContents(int chest) {
        dropContents(chest);
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new WareMenu(id, inventory, this);
    }

    @Override
    public ItemStack getPickResult() {
        return WagonItem.setupComponents(new ItemStack(TWItems.WARE_WAGON.get()), getColor());
    }

    @Override
    protected DyeColor getDefaultColor() {
        return null;
    }

    /**
     * Version 4 is 139 slots: 4 chests, 27 cargo, then four blocks of 27.
     * Version 3 is 111 slots: 3 chests, 27 cargo, then three blocks of 27. The fourth chest stays empty.
     * Version 2 was 123 slots: 3 chests, 30 cargo, then three blocks of 30. The last 3 cargo
     * stacks and the last 3 of each chest drop beside the wagon.
     * A missing version is the old 120-slot list: 3 chests, 36 cargo, then three blocks of 27.
     * The first 27 cargo stacks stay. The last 9 drop. Each chest's 27 stacks move intact.
     */
    private void readCargo(CompoundTag nbt) {
        CompoundTag cargoTag = nbt.getCompound("Cargo");
        int version = nbt.getInt("CargoVersion");
        if(version >= CARGO_VERSION) {
            ContainerHelper.loadAllItems(cargoTag, stacks, registryAccess());
            return;
        }
        if(version == 3) {
            readThree(cargoTag);
            return;
        }
        if(version == 2) {
            readWide(cargoTag);
            return;
        }
        readLegacy(cargoTag);
    }

    private void readThree(CompoundTag cargoTag) {
        NonNullList<ItemStack> old = NonNullList.withSize(VERSION3_SLOTS, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(cargoTag, old, registryAccess());
        for(int i = 0; i < STORED_CHESTS; i++)
            stacks.set(i, old.get(i));
        for(int i = 0; i < CARGO; i++)
            stacks.set(CHESTS + i, old.get(STORED_CHESTS + i));
        for(int chest = 0; chest < STORED_CHESTS; chest++) {
            int from = STORED_CHESTS + CARGO + chest * CONTENTS;
            for(int offset = 0; offset < CONTENTS; offset++)
                stacks.set(contentIndex(chest, offset), old.get(from + offset));
        }
    }

    private void readWide(CompoundTag cargoTag) {
        NonNullList<ItemStack> wide = NonNullList.withSize(WIDE_SLOTS, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(cargoTag, wide, registryAccess());
        for(int i = 0; i < STORED_CHESTS; i++)
            stacks.set(i, wide.get(i));
        for(int i = 0; i < CARGO; i++)
            stacks.set(CHESTS + i, wide.get(STORED_CHESTS + i));
        for(int i = CARGO; i < WIDE_CARGO; i++)
            dropMigrated(wide.get(STORED_CHESTS + i));
        for(int chest = 0; chest < STORED_CHESTS; chest++) {
            int from = STORED_CHESTS + WIDE_CARGO + chest * WIDE_CONTENTS;
            for(int offset = 0; offset < CONTENTS; offset++)
                stacks.set(contentIndex(chest, offset), wide.get(from + offset));
            for(int offset = CONTENTS; offset < WIDE_CONTENTS; offset++)
                dropMigrated(wide.get(from + offset));
        }
    }

    private void readLegacy(CompoundTag cargoTag) {
        NonNullList<ItemStack> legacy = NonNullList.withSize(LEGACY_SLOTS, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(cargoTag, legacy, registryAccess());
        for(int i = 0; i < STORED_CHESTS; i++)
            stacks.set(i, legacy.get(i));
        for(int i = 0; i < CARGO; i++)
            stacks.set(CHESTS + i, legacy.get(STORED_CHESTS + i));
        for(int i = CARGO; i < LEGACY_CARGO; i++)
            dropMigrated(legacy.get(STORED_CHESTS + i));
        for(int chest = 0; chest < STORED_CHESTS; chest++) {
            for(int offset = 0; offset < LEGACY_CONTENTS; offset++) {
                int from = STORED_CHESTS + LEGACY_CARGO + chest * LEGACY_CONTENTS + offset;
                stacks.set(contentIndex(chest, offset), legacy.get(from));
            }
        }
    }

    private void dropMigrated(ItemStack stack) {
        if(stack.isEmpty() || level() == null || level().isClientSide)
            return;
        spawnAtLocation(stack);
    }

    private void offerChest(Player player, InteractionHand hand) {
        int slot = firstEmptyChest();
        if(slot < 0)
            return;
        ItemStack stack = player.getItemInHand(hand);
        ItemStack one = stack.copyWithCount(1);
        if(!cargo.insertItem(slot, one, false).isEmpty())
            return;
        if(!player.getAbilities().instabuild)
            stack.shrink(1);
    }

    private int firstEmptyChest() {
        for(int i = 0; i < CHESTS; i++) {
            if(stacks.get(i).isEmpty())
                return i;
        }
        return -1;
    }

    private void releaseChest(int slot, ItemStack next) {
        if(loading || level() == null || level().isClientSide)
            return;
        ItemStack previous = stacks.get(slot);
        if(previous == next || !isChest(previous))
            return;
        dropContents(slot);
    }

    private void dropContents(int chest) {
        if(level() == null || level().isClientSide || chest < 0 || chest >= CHESTS)
            return;
        for(int i = 0; i < CONTENTS; i++) {
            int slot = contentIndex(chest, i);
            ItemStack stack = stacks.get(slot);
            if(stack.isEmpty())
                continue;
            stacks.set(slot, ItemStack.EMPTY);
            spawnAtLocation(stack);
        }
    }

    private void syncChests() {
        if(loading || level() == null || level().isClientSide)
            return;
        int mask = 0;
        for(int i = 0; i < CHESTS; i++) {
            if(isChest(stacks.get(i)))
                mask |= 1 << i;
        }
        entityData.set(DATA_CHESTS, mask);
        applyCargoSpeed();
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if(DATA_CHESTS.equals(key))
            applyCargoSpeed();
    }

    private void applyCargoSpeed() {
        double speed = ServerConfig.INSTANCE.wareSpeedEmpty.get() - chestCount() * ServerConfig.INSTANCE.wareSpeedPerChest.get();
        setSpeedCoefficient(Math.max(0, speed));
    }

    private void syncLantern() {
        boolean filled = !lantern.getFirst().isEmpty();
        if(!filled && level() != null && !level().isClientSide)
            clearLanternLight(false);
        entityData.set(DATA_LANTERN, filled);
    }

    private void updateLanternLight() {
        if(!hasLantern()) {
            clearLanternLight(false);
            return;
        }
        BlockPos next = lampBlockPos();
        if(next.equals(lanternLight))
            return;
        if(lanternLight == null && level().getBlockState(next).is(Blocks.LIGHT)) {
            lanternLight = next;
            return;
        }
        clearLanternLight(false);
        if(level().getBlockState(next).isAir()) {
            level().setBlock(next, Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, 15), 3);
            lanternLight = next;
        }
    }

    private BlockPos lampBlockPos() {
        return BlockPos.containing(position().add(rotateY(LAMP_POS, getYRot())));
    }

    private void clearLanternLight(boolean atLamp) {
        if(level().isClientSide)
            return;
        removeLightBlock(lanternLight);
        if(atLamp)
            removeLightBlock(lampBlockPos());
        lanternLight = null;
    }

    private void removeLightBlock(BlockPos pos) {
        if(pos != null && level().getBlockState(pos).is(Blocks.LIGHT))
            level().setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
    }

    @Override
    public boolean touchesLadder(Player player) {
        Vec3 feet = yawLocal(player.position());
        double rear = -getBbWidth() * 0.5D;
        if(feet.x < -4D / 16D - 0.35D || feet.x > 12D / 16D + 0.35D)
            return false;
        // Behind the rear face and below the roof. The roof is not a ladder, so the player can step off.
        if(feet.z < rear - 0.9D || feet.z > rear - 0.05D)
            return false;
        return feet.y >= -0.2D && feet.y < getBbHeight();
    }

    @Override
    public boolean landFromLadder(Player player) {
        Vec3 feet = yawLocal(player.position());
        double half = getBbWidth() * 0.5D;
        double rear = -half;
        if(feet.x < -4D / 16D - 0.35D || feet.x > 12D / 16D + 0.35D)
            return false;
        if(feet.z < rear - 0.9D || feet.z > rear - 0.05D)
            return false;
        if(feet.y < getBbHeight() || feet.y > getBbHeight() + 0.6D)
            return false;
        double x = Mth.clamp(feet.x, -half + 0.4D, half - 0.4D);
        Vec3 landed = yawWorld(new Vec3(x, getBbHeight() + 0.01D, rear + 0.6D));
        player.setPos(landed.x, landed.y, landed.z);
        Vec3 motion = player.getDeltaMovement();
        player.setDeltaMovement(motion.x, 0, motion.z);
        player.resetFallDistance();
        player.setOnGround(true);
        return true;
    }

    private static Vec3 seat(double x, double y, double z) {
        // positionRider subtracts 0.7. Sitting hips are 0.75 above the entity, so y is the hip height.
        return new Vec3(x / 16D, y / 16D - 0.05D, -z / 16D);
    }

    private static Vec3 pivot(double x, double y, double z) {
        return new Vec3(x / 16D, y / 16D, -z / 16D);
    }

}
