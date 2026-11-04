package net.favouriteless.trotting_wagons.common.entities.wagons;

import net.favouriteless.trotting_wagons.ServerConfig;
import net.favouriteless.trotting_wagons.common.entities.base.AbstractWagon;
import net.favouriteless.trotting_wagons.common.entities.base.WagonSeat;
import net.favouriteless.trotting_wagons.common.init.TWItems;
import net.favouriteless.trotting_wagons.common.items.WagonItem;
import net.favouriteless.trotting_wagons.common.menus.PassengerMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
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

public class PassengerWagon extends AbstractWagon implements MenuProvider {

    public static final int LANTERNS = 2;
    /** Sides are a quarter block inside the previous 3.5-wide square. */
    private static final double BODY_HALF_X = 1.5D;
    /** Local +Z stays at the old front. Local −Z, the rear, is half a block longer. */
    private static final double BODY_FRONT = 1.75D;
    private static final double BODY_REAR = 2.75D;
    private static final double BODY_HEIGHT = 3.5D;
    private static final int LANTERN_MASK = (1 << LANTERNS) - 1;
    private static final EntityDataAccessor<Integer> DATA_LANTERNS = SynchedEntityData.defineId(PassengerWagon.class, EntityDataSerializers.INT);
    /** 0 none, 1 shafts (bone shafts, one animal), 2 tongue (bone Tongue, two animals). */
    private static final EntityDataAccessor<Integer> DATA_HARNESS = SynchedEntityData.defineId(PassengerWagon.class, EntityDataSerializers.INT);
    private static final Vec3[] LAMP_POS = {
            pivot(0, 39.6D, -30),
            pivot(0, 37.85D, 50)
    };

    public static final WagonSeat[] SEATS = new WagonSeat[] {
            new WagonSeat(seat(0, 17, -30.75D), WagonSeat.Kind.DRIVER),
            new WagonSeat(seat(8.5D, 24, -10), WagonSeat.Kind.PLAYER),
            new WagonSeat(seat(-8.5D, 24, -10), WagonSeat.Kind.PLAYER),
            new WagonSeat(seat(8.5D, 24, 10), WagonSeat.Kind.PLAYER),
            new WagonSeat(seat(-8.5D, 24, 11), WagonSeat.Kind.PLAYER),
            new WagonSeat(seat(8.5D, 24, 31), WagonSeat.Kind.PLAYER),
            new WagonSeat(seat(-8.5D, 24, 31), WagonSeat.Kind.PLAYER)
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
    private final NonNullList<ItemStack> lantern = NonNullList.withSize(LANTERNS, ItemStack.EMPTY);
    private final BlockPos[] lanternLight = new BlockPos[LANTERNS];
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
            syncLanterns();
        }
    };

    public PassengerWagon(EntityType<PassengerWagon> type, Level level) {
        super(type, level, 19.5D / 16D, 1.5D, SEATS, ServerConfig.INSTANCE.passengerSpeed.get(),
                ServerConfig.INSTANCE.passengerTurnRate.get(), ServerConfig.INSTANCE.passengerHealth.get(),
                9.5D / 16D, 77.88461D / 16D, 13);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_LANTERNS, 0);
        builder.define(DATA_HARNESS, 0);
    }

    @Override
    public int hitchSlots() {
        int slots = entityData.get(DATA_HARNESS);
        return slots < 0 ? 0 : Math.min(slots, 2);
    }

    @Override
    protected double singleHarnessFactor() {
        return 0.7D;
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
        setBoundingBox(makeBoundingBox());
        if(!level().isClientSide && tickCount % 10 == 0)
            updateLanternLight();
    }

    @Override
    protected AABB makeBoundingBox() {
        return orientedBody(1.625D, 0.0D, 4.198D, 3.312D, 2.266D);
    }

    @Override
    public void remove(RemovalReason reason) {
        if(reason.shouldDestroy())
            clearLanternLight(true);
        super.remove(reason);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        if(nbt.contains("Lantern", Tag.TAG_COMPOUND)) {
            for(int i = 0; i < lantern.size(); i++)
                lantern.set(i, ItemStack.EMPTY);
            ContainerHelper.loadAllItems(nbt.getCompound("Lantern"), lantern, registryAccess());
        }
        syncLanterns();
        if(nbt.contains("Harness", Tag.TAG_COMPOUND)) {
            harness.set(0, ItemStack.EMPTY);
            ContainerHelper.loadAllItems(nbt.getCompound("Harness"), harness, registryAccess());
        }
        syncHarness();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
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
            Containers.dropContents(level(), blockPosition(), lantern);
        }
        super.onDestroyed(dropItem);
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
    protected float missingRearPitch() {
        return -8F;
    }

    @Override
    protected float diagonalPitch() {
        return -8F;
    }

    public ItemStackHandler getLanternItems() {
        return lanternItems;
    }

    public boolean hasLantern(int slot) {
        return slot >= 0 && slot < LANTERNS && (entityData.get(DATA_LANTERNS) & (1 << slot)) != 0;
    }

    public int firstEmptyLantern() {
        int mask = entityData.get(DATA_LANTERNS) & LANTERN_MASK;
        for(int i = 0; i < LANTERNS; i++) {
            if((mask & (1 << i)) == 0)
                return i;
        }
        return -1;
    }

    private void syncLanterns() {
        int mask = 0;
        for(int i = 0; i < LANTERNS; i++) {
            if(!lantern.get(i).isEmpty())
                mask |= 1 << i;
            else if(level() != null && !level().isClientSide)
                clearLanternLight(i, false);
        }
        entityData.set(DATA_LANTERNS, mask);
    }

    private void updateLanternLight() {
        for(int i = 0; i < LANTERNS; i++) {
            if(!hasLantern(i)) {
                clearLanternLight(i, false);
                continue;
            }
            BlockPos next = lampBlockPos(i);
            if(next.equals(lanternLight[i]))
                continue;
            if(lanternLight[i] == null && level().getBlockState(next).is(Blocks.LIGHT)) {
                lanternLight[i] = next;
                continue;
            }
            clearLanternLight(i, false);
            if(level().getBlockState(next).isAir()) {
                level().setBlock(next, Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, 15), 3);
                lanternLight[i] = next;
            }
        }
    }

    private BlockPos lampBlockPos(int slot) {
        return BlockPos.containing(position().add(rotateY(LAMP_POS[slot], getYRot())));
    }

    private void clearLanternLight(boolean atLamp) {
        if(level().isClientSide)
            return;
        for(int i = 0; i < LANTERNS; i++)
            clearLanternLight(i, atLamp);
    }

    private void clearLanternLight(int slot, boolean atLamp) {
        if(level().isClientSide)
            return;
        removeLightBlock(lanternLight[slot]);
        if(atLamp)
            removeLightBlock(lampBlockPos(slot));
        lanternLight[slot] = null;
    }

    private void removeLightBlock(BlockPos pos) {
        if(pos != null && level().getBlockState(pos).is(Blocks.LIGHT))
            level().setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new PassengerMenu(id, inventory, this);
    }

    @Override
    public ItemStack getPickResult() {
        return WagonItem.setupComponents(new ItemStack(TWItems.PASSENGER_WAGON.get()), getColor());
    }

    @Override
    protected boolean dismountsBehind(LivingEntity passenger, int seat) {
        return seat > 0;
    }

    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        if(claimsRearDismount(passenger))
            return super.getDismountLocationForPassenger(passenger);
        return besideDriver(passenger);
    }

    /** Ground spot level with the driver seat, outside the body. +X first, then the other side, then farther out. */
    private Vec3 besideDriver(LivingEntity passenger) {
        double z = SEATS[0].position().z;
        double base = BODY_HALF_X + passenger.getBbWidth() * 0.5D + 0.05D;
        Vec3 first = null;
        for(int step = 0; step < 4; step++) {
            double out = base + step * 0.5D;
            for(int sign = 1; sign >= -1; sign -= 2) {
                Vec3 pos = position().add(rotateY(new Vec3(out * sign, 0, z), getYRot()));
                if(first == null)
                    first = pos;
                if(dismountFree(passenger, pos))
                    return pos;
            }
        }
        return first;
    }

    @Override
    protected double dismountRear() {
        return BODY_REAR;
    }

    @Override
    protected DyeColor getDefaultColor() {
        return null;
    }

    private static Vec3 seat(double x, double y, double z) {
        // positionRider subtracts 0.7. Sitting hips are 0.75 above the entity, so y is the hip height.
        return new Vec3(x / 16D, y / 16D - 0.05D, -z / 16D);
    }

    private static Vec3 pivot(double x, double y, double z) {
        return new Vec3(x / 16D, y / 16D, -z / 16D);
    }

}
