package net.favouriteless.trotting_wagons.common.entities.wagons;

import net.favouriteless.trotting_wagons.ServerConfig;
import net.favouriteless.trotting_wagons.TrottingWagons;
import net.favouriteless.trotting_wagons.common.entities.base.AbstractWagon;
import net.favouriteless.trotting_wagons.common.entities.base.WagonSeat;
import net.favouriteless.trotting_wagons.common.init.TWItems;
import net.favouriteless.trotting_wagons.common.init.TWSoundEvents;
import net.favouriteless.trotting_wagons.common.items.WagonItem;
import net.favouriteless.trotting_wagons.common.menus.AnimalMenu;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.ContainerHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.ItemStackHandler;

public class AnimalWagon extends AbstractWagon implements MenuProvider {

    /** 0 none, 1 the tongue item (TongueSingle), 2 the shafts item (Tongue). */
    private static final EntityDataAccessor<Integer> DATA_HARNESS = SynchedEntityData.defineId(AnimalWagon.class, EntityDataSerializers.INT);

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

    private static final int FRONT_LARGE = 3;
    private static final int REAR_LARGE = 6;
    // Mesh tail is about 2.78 from center. Unload uses this, past the collision rear.
    private static final double BODY_REAR = 2.85D;
    /** Bed, front rails, and driver seat. Wheels and the tongue are outside. One pixel of slack. */
    private static final double HIT_HALF_X = 21.5D / 16D;
    private static final double HIT_FRONT = 37.25D / 16D;
    private static final double HIT_REAR = 45.5D / 16D;
    private static final double HIT_HEIGHT = 2.75D;
    private static final int[] FRONT_GROUP = {1, 2, 4, 5};
    private static final int[] REAR_GROUP = {4, 5, 7, 8};

    public static final WagonSeat[] SEATS = new WagonSeat[] {
            new WagonSeat(seat(0, 14, -32.25D), WagonSeat.Kind.DRIVER),
            new WagonSeat(seat(-6, 16.5D, -14), WagonSeat.Kind.ANIMAL),
            new WagonSeat(seat(6, 16.5D, -14), WagonSeat.Kind.ANIMAL),
            new WagonSeat(seat(0, 16.5D, -4), WagonSeat.Kind.ANIMALBIG),
            new WagonSeat(seat(-6, 16.5D, 10), WagonSeat.Kind.ANIMAL),
            new WagonSeat(seat(6, 16.5D, 10), WagonSeat.Kind.ANIMAL),
            new WagonSeat(seat(0, 16.5D, 24), WagonSeat.Kind.ANIMALBIG),
            new WagonSeat(seat(-6, 16.5D, 34), WagonSeat.Kind.ANIMAL),
            new WagonSeat(seat(6, 16.5D, 34), WagonSeat.Kind.ANIMAL)
    };

    public AnimalWagon(EntityType<AnimalWagon> type, Level level) {
        super(type, level, 1.5D, 1.5D, SEATS, ServerConfig.INSTANCE.animalSpeed.get(),
                ServerConfig.INSTANCE.animalTurnRate.get(), ServerConfig.INSTANCE.animalHealth.get(),
                9.5D / 16D, 77.88461D / 16D, 13);
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
    }

    @Override
    protected AABB makeBoundingBox() {
        float yaw = getYRot();
        double minX = Double.POSITIVE_INFINITY;
        double minZ = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxZ = Double.NEGATIVE_INFINITY;
        double[][] corners = {
                {HIT_HALF_X, HIT_FRONT},
                {-HIT_HALF_X, HIT_FRONT},
                {HIT_HALF_X, -HIT_REAR},
                {-HIT_HALF_X, -HIT_REAR}
        };
        for(double[] corner : corners) {
            Vec3 turned = rotateY(corner[0], 0, corner[1], yaw);
            minX = Math.min(minX, turned.x);
            maxX = Math.max(maxX, turned.x);
            minZ = Math.min(minZ, turned.z);
            maxZ = Math.max(maxZ, turned.z);
        }
        return new AABB(getX() + minX, getY(), getZ() + minZ, getX() + maxX, getY() + HIT_HEIGHT, getZ() + maxZ);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        if(nbt.contains("Harness", net.minecraft.nbt.Tag.TAG_COMPOUND)) {
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

    private void syncHarness() {
        ItemStack stack = harness.get(0);
        int slots = stack.is(TWItems.SHAFTS.get()) ? 2 : stack.is(TWItems.TONGUE.get()) ? 1 : 0;
        entityData.set(DATA_HARNESS, slots);
    }

    @Override
    protected double tongueLength() {
        return 55D / 16D;
    }

    @Override
    protected float missingRearPitch() {
        return -8F;
    }

    @Override
    protected float diagonalPitch() {
        return -8F;
    }

    @Override
    protected boolean dismountsBehind(LivingEntity passenger, int seat) {
        return seat > 0 && !(passenger instanceof Player);
    }

    @Override
    protected double dismountRear() {
        return BODY_REAR;
    }

    @Override
    protected boolean acceptsLargeSeat(Entity entity) {
        return isLargePassenger(entity);
    }

    @Override
    public Component seatBlockHint(int index) {
        if(index < 0 || index >= getSeatCount() || getSeatEntityId(index) >= 0)
            return null;
        if((index == FRONT_LARGE && anyTaken(null, FRONT_GROUP)) || (index == REAR_LARGE && anyTaken(null, REAR_GROUP)))
            return Component.translatable("gui.trotting_wagons.tooltip.large_needs_space");
        if((contains(FRONT_GROUP, index) && taken(FRONT_LARGE, null)) || (contains(REAR_GROUP, index) && taken(REAR_LARGE, null)))
            return Component.translatable("gui.trotting_wagons.tooltip.small_blocked");
        return null;
    }

    @Override
    protected boolean seatOpen(int index, Entity entity) {
        if(index == FRONT_LARGE)
            return !anyTaken(entity, FRONT_GROUP);
        if(index == REAR_LARGE)
            return !anyTaken(entity, REAR_GROUP);
        if(contains(FRONT_GROUP, index) && taken(FRONT_LARGE, entity))
            return false;
        return !contains(REAR_GROUP, index) || !taken(REAR_LARGE, entity);
    }

    private boolean anyTaken(Entity entity, int[] seats) {
        for(int seat : seats) {
            if(taken(seat, entity))
                return true;
        }
        return false;
    }

    private boolean taken(int seat, Entity entity) {
        int id = getSeatEntityId(seat);
        return id >= 0 && (entity == null || id != entity.getId());
    }

    private static boolean contains(int[] seats, int index) {
        for(int seat : seats) {
            if(seat == index)
                return true;
        }
        return false;
    }

    @Override
    protected void addPassenger(Entity passenger) {
        super.addPassenger(passenger);
        if(!level().isClientSide)
            awardPigRide();
    }

    private void awardPigRide() {
        boolean pig = false;
        for(Entity passenger : getPassengers()) {
            if(passenger.getType().is(TrottingWagons.PIGS))
                pig = true;
        }
        if(!pig || level().getServer() == null)
            return;
        AdvancementHolder holder = level().getServer().getAdvancements().get(TrottingWagons.id("pig_ride"));
        if(holder == null)
            return;
        for(Entity passenger : getPassengers()) {
            if(passenger instanceof ServerPlayer player && player.getAdvancements().award(holder, "ride_with_pig"))
                player.playNotifySound(TWSoundEvents.PIG_TUNE.get(), SoundSource.RECORDS, 1.0F, 1.0F);
        }
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new AnimalMenu(id, inventory, this);
    }

    @Override
    public ItemStack getPickResult() {
        return WagonItem.setupComponents(new ItemStack(TWItems.ANIMAL_WAGON.get()), getColor());
    }

    @Override
    protected DyeColor getDefaultColor() {
        return null;
    }

    private static Vec3 seat(double x, double y, double z) {
        return new Vec3(x / 16D, y / 16D, -z / 16D).add(0, 0.7D, 0);
    }

}
