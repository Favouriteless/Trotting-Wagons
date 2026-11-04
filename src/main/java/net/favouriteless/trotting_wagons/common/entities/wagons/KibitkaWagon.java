package net.favouriteless.trotting_wagons.common.entities.wagons;

import net.favouriteless.trotting_wagons.ServerConfig;
import net.favouriteless.trotting_wagons.common.KibitkaSpawnMemory;
import net.favouriteless.trotting_wagons.common.entities.base.AbstractInventoryWagon;
import net.favouriteless.trotting_wagons.common.entities.base.WagonSeat;
import net.favouriteless.trotting_wagons.common.init.TWItems;
import net.favouriteless.trotting_wagons.common.items.WagonItem;
import net.favouriteless.trotting_wagons.common.menus.KibitkaMenu;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class KibitkaWagon extends AbstractInventoryWagon {

    public static final int CARGO = 45;
    /** Two seconds. The pose moves one step per tick, and a new switch waits until it arrives. */
    public static final int CAMP_TICKS = 40;
    /** Hinge is the Door pivot. Negative yaw swings the free edge into the cabin. Camp leaves this off. */
    public static final boolean DOOR_OPENS = false;
    public static final float DOOR_OPEN = (float)Math.toRadians(-90.0D);
    /** Top centre of the Bed mattress. Sleep does not use the seat hip adjustment. */
    private static final double BED_X = 6.8D / 16D;
    private static final double BED_Y = 19D / 16D;
    private static final double BED_Z = -7.9D / 16D;

    private static final EntityDataAccessor<Integer> DATA_CAMP = SynchedEntityData.defineId(KibitkaWagon.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_CAMP_TARGET = SynchedEntityData.defineId(KibitkaWagon.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_LANTERN = SynchedEntityData.defineId(KibitkaWagon.class, EntityDataSerializers.BOOLEAN);
    /** 0 none, 1 shafts (bone shafts, one animal), 2 tongue (bone Tongue, two animals). */
    private static final EntityDataAccessor<Integer> DATA_HARNESS = SynchedEntityData.defineId(KibitkaWagon.class, EntityDataSerializers.INT);
    private static final Vec3 LAMP_POS = pivot(0, 39.6D, -36);

    public static final WagonSeat[] SEATS = new WagonSeat[] {
            new WagonSeat(seat(0, 18, -30.25D), WagonSeat.Kind.DRIVER),
            new WagonSeat(seat(-8.39723D, 22, -11.50666D), WagonSeat.Kind.PLAYER)
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
    private final NonNullList<ItemStack> lantern = NonNullList.withSize(1, ItemStack.EMPTY);
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

    private BlockPos lanternLight;
    private int camp;
    private int campTarget;
    private float campPrev;
    private float campShown;
    private boolean campReady;
    private final int[] sagKinds = new int[WHEEL_COUNT];
    private int sagKey = Integer.MIN_VALUE;
    private WheelSag sagPose = WheelSag.NONE;
    private final Set<UUID> spawnPlayers = new HashSet<>();
    private BlockPos spawnPos;
    private ResourceKey<Level> spawnDimension;

    public KibitkaWagon(EntityType<KibitkaWagon> type, Level level) {
        super(type, level, 19.25D / 16D, 17D / 16D, SEATS, ServerConfig.INSTANCE.kibitkaSpeed.get(),
                ServerConfig.INSTANCE.kibitkaTurnRate.get(), ServerConfig.INSTANCE.kibitkaHealth.get(),
                0, 76.88461D / 16D, 13, CARGO);
    }

    @Override
    public int hitchSlots() {
        int slots = entityData.get(DATA_HARNESS);
        return slots < 0 ? 0 : Math.min(slots, 2);
    }

    @Override
    protected Vec3 hitchOffset(Side side) {
        double z = 76.88461D / 16D;
        if(hitchSlots() < 2)
            return new Vec3(0, 0, z);
        return new Vec3(side == Side.LEFT ? 0.6D : -0.6D, 0, z);
    }

    @Override
    public ItemStackHandler getHarnessItems() {
        return harnessItems;
    }

    @Override
    protected AABB makeBoundingBox() {
        return orientedBody(1.625D, 0.0D, 3.806D, 2.469D, 2.438D);
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
    public WheelSag wheelSag() {
        int mask = getWheelMask() & 0x0F;
        int key = mask | ((campPose() & 63) << 4);
        for(int slot = 0; slot < WHEEL_COUNT; slot++) {
            int kind = getWheelKind(slot).ordinal();
            sagKinds[slot] = kind;
            key |= kind << (10 + slot * 2);
        }
        if(key != sagKey) {
            sagKey = key;
            sagPose = KibitkaWagonSag.pose(mask, sagKinds, tentScale());
        }
        return sagPose;
    }

    /** Same smoothstep {@code WagonModel} uses while the tent scales around its pivot. */
    private float tentScale() {
        float linear = Mth.clamp(campPose() / (float)CAMP_TICKS, 0, 1);
        return linear * linear * (3 - 2 * linear);
    }

    @Override
    protected boolean holdsTravel() {
        return entityData.get(DATA_CAMP) > 0;
    }

    @Override
    public void moveTick() {
        if(holdsTravel())
            setSteeringImpulse(0);
        super.moveTick();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_CAMP, 0);
        builder.define(DATA_CAMP_TARGET, 0);
        builder.define(DATA_LANTERN, false);
        builder.define(DATA_HARNESS, 0);
    }

    @Override
    public void tick() {
        if(!level().isClientSide)
            advanceCamp();
        float shown = entityData.get(DATA_CAMP);
        if(!campReady) {
            campPrev = shown;
            campReady = true;
        } else {
            campPrev = campShown;
        }
        campShown = shown;
        super.tick();
        if(!level().isClientSide && tickCount % 10 == 0)
            updateLanternLight();
    }

    public int campPose() {
        return entityData.get(DATA_CAMP);
    }

    public int campTarget() {
        return entityData.get(DATA_CAMP_TARGET);
    }

    public boolean campSettled() {
        return campPose() == campTarget();
    }

    /** Tent fully open and not already closing. Sleep and respawn stay off during the move. */
    public boolean isCamped() {
        return campPose() == CAMP_TICKS && campTarget() == CAMP_TICKS;
    }

    /** 0 is the closed road tent, {@link #CAMP_TICKS} is the open camp tent. */
    public float campRender(float partialTick) {
        return Mth.lerp(partialTick, campPrev, campShown);
    }

    public void requestCampToggle(Player player) {
        if(!isAlive() || player.getVehicle() != this || camp != campTarget)
            return;
        if(camp != 0)
            endCamp();
        campTarget = camp == 0 ? CAMP_TICKS : 0;
        entityData.set(DATA_CAMP_TARGET, campTarget);
    }

    public void trySleep(Player player) {
        if(!(player instanceof ServerPlayer sleeper) || !canUseCamp(sleeper) || sleeper.isSleeping())
            return;
        Level level = level();
        if(!level.dimensionType().natural()) {
            sleeper.displayClientMessage(Component.translatable("gui.trotting_wagons.kibitka.sleep.dimension"), true);
            return;
        }
        if(level.isDay()) {
            sleeper.displayClientMessage(Component.translatable("block.minecraft.bed.no_sleep"), true);
            return;
        }
        if(!sleeper.isCreative() && monstersNearby(sleeper)) {
            sleeper.displayClientMessage(Component.translatable("block.minecraft.bed.not_safe"), true);
            return;
        }
        int seat = seatOf(sleeper);
        if(seat < 0)
            return;
        sleeper.startSleeping(blockPosition());
        if(!mountSeat(sleeper, seat)) {
            sleeper.stopSleepInBed(true, true);
            return;
        }
        sleeper.setPose(Pose.SLEEPING);
        positionRider(sleeper, Entity::setPos);
        sleeper.closeContainer();
        sleeper.awardStat(Stats.SLEEP_IN_BED);
        CriteriaTriggers.SLEPT_IN_BED.trigger(sleeper);
        ((ServerLevel)level).updateSleepingPlayerList();
        if(!((ServerLevel)level).canSleepThroughNights())
            sleeper.displayClientMessage(Component.translatable("sleep.not_possible"), true);
    }

    public void trySetRespawn(Player player) {
        if(!(player instanceof ServerPlayer sleeper) || !canUseCamp(sleeper))
            return;
        BlockPos pos = reusableSpawn() ? spawnPos : findSpawnFeet();
        if(pos == null) {
            sleeper.displayClientMessage(Component.translatable("block.minecraft.bed.obstructed"), true);
            return;
        }
        ResourceKey<Level> dimension = level().dimension();
        sleeper.setRespawnPosition(dimension, pos, sleeper.getYRot(), true, true);
        if(!pos.equals(sleeper.getRespawnPosition()) || !dimension.equals(sleeper.getRespawnDimension()))
            return;
        rememberSpawn(sleeper, pos);
    }

    private void advanceCamp() {
        if(camp == campTarget)
            return;
        camp += Integer.signum(campTarget - camp);
        entityData.set(DATA_CAMP, camp);
    }

    private void applyCamp(int pose, int target) {
        camp = Mth.clamp(pose, 0, CAMP_TICKS);
        campTarget = target > 0 ? CAMP_TICKS : 0;
        entityData.set(DATA_CAMP, camp);
        entityData.set(DATA_CAMP_TARGET, campTarget);
    }

    public ItemStackHandler getLanternItems() {
        return lanternItems;
    }

    public boolean hasLantern() {
        return entityData.get(DATA_LANTERN);
    }

    private void syncLantern() {
        boolean has = !lantern.getFirst().isEmpty();
        entityData.set(DATA_LANTERN, has);
        if(!has && level() != null && !level().isClientSide)
            clearLanternLight(false);
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
        if(level() == null || level().isClientSide)
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
    public void remove(RemovalReason reason) {
        if(reason.shouldDestroy()) {
            endCamp();
            clearLanternLight(true);
        }
        super.remove(reason);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        if(nbt.contains("Lantern", Tag.TAG_COMPOUND)) {
            lantern.set(0, ItemStack.EMPTY);
            ContainerHelper.loadAllItems(nbt.getCompound("Lantern"), lantern, registryAccess());
        }
        syncLantern();
        if(nbt.contains("Harness", Tag.TAG_COMPOUND)) {
            harness.set(0, ItemStack.EMPTY);
            ContainerHelper.loadAllItems(nbt.getCompound("Harness"), harness, registryAccess());
        }
        syncHarness();
        if(nbt.contains("Camp", Tag.TAG_INT))
            applyCamp(nbt.getInt("Camp"), nbt.getInt("CampTarget"));
        readSpawn(nbt);
        if(!level().isClientSide && !isCamped())
            endCamp();
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
        nbt.putInt("Camp", camp);
        nbt.putInt("CampTarget", campTarget);
        writeSpawn(nbt);
    }

    private void syncHarness() {
        ItemStack stack = harness.get(0);
        int slots = stack.is(TWItems.TONGUE.get()) ? 2 : stack.is(TWItems.SHAFTS.get()) ? 1 : 0;
        entityData.set(DATA_HARNESS, slots);
    }

    @Override
    protected void onDestroyed(boolean dropItem) {
        if(!level().isClientSide) {
            endCamp();
            clearLanternLight(true);
            ItemStack lamp = lantern.getFirst();
            if(!lamp.isEmpty())
                spawnAtLocation(lamp.copy());
            for(int i = 0; i < REPAIR_SLOTS; i++) {
                ItemStack stack = getRepair().getStackInSlot(i);
                if(!stack.isEmpty())
                    spawnAtLocation(stack.copy());
            }
        }
        super.onDestroyed(dropItem);
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new KibitkaMenu(id, inventory, this);
    }

    @Override
    public ItemStack getPickResult() {
        return WagonItem.setupComponents(new ItemStack(TWItems.KIBITKA_WAGON.get()), getColor());
    }

    @Override
    protected boolean dismountsBehind(LivingEntity passenger, int seat) {
        return seat > 0;
    }

    @Override
    protected void positionRider(Entity passenger, MoveFunction callback) {
        if(passenger instanceof LivingEntity living && living.isSleeping() && isCamped()) {
            Vec3 world = offsetToWorld(sleepOffset(), false);
            callback.accept(passenger, world.x, world.y, world.z);
            return;
        }
        super.positionRider(passenger, callback);
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

    private boolean canUseCamp(Player player) {
        return isAlive() && player.getVehicle() == this && isCamped();
    }

    private boolean monstersNearby(ServerPlayer player) {
        Vec3 pos = position();
        AABB box = new AABB(pos.x - 8, pos.y - 5, pos.z - 8, pos.x + 8, pos.y + 5, pos.z + 8);
        return !level().getEntitiesOfClass(Monster.class, box, monster -> monster.isPreventingPlayerRest(player)).isEmpty();
    }

    private int seatOf(Entity entity) {
        int id = entity.getId();
        for(int i = 0; i < SEATS.length; i++) {
            if(getSeatEntityId(i) == id)
                return i;
        }
        return -1;
    }

    private static Vec3 sleepOffset() {
        return new Vec3(BED_X, BED_Y, BED_Z);
    }

    private void endCamp() {
        if(level() == null || level().isClientSide)
            return;
        for(Entity passenger : new ArrayList<>(getPassengers())) {
            if(passenger instanceof ServerPlayer player && player.isSleeping())
                player.stopSleepInBed(true, true);
        }
        forgetSpawn();
    }

    private boolean reusableSpawn() {
        return spawnPos != null && spawnDimension != null && spawnDimension.equals(level().dimension()) && spawnFeetOpen(spawnPos);
    }

    private void rememberSpawn(ServerPlayer player, BlockPos pos) {
        if(spawnPos != null && !spawnPos.equals(pos))
            forgetSpawn();
        spawnPos = pos;
        spawnDimension = level().dimension();
        spawnPlayers.add(player.getUUID());
    }

    private void forgetSpawn() {
        BlockPos pos = spawnPos;
        ResourceKey<Level> dimension = spawnDimension;
        Set<UUID> ids = new HashSet<>(spawnPlayers);
        spawnPlayers.clear();
        spawnPos = null;
        spawnDimension = null;
        if(pos == null || dimension == null || !(level() instanceof ServerLevel server))
            return;
        for(UUID id : ids)
            KibitkaSpawnMemory.revoke(server, id, dimension, pos);
    }

    private BlockPos findSpawnFeet() {
        BlockPos origin = blockPosition();
        BlockPos.MutableBlockPos feet = new BlockPos.MutableBlockPos();
        for(int dx = -4; dx <= 4; dx++) {
            for(int dz = -4; dz <= 4; dz++) {
                if(Math.abs(dx) < 2 && Math.abs(dz) < 2)
                    continue;
                for(int dy = 2; dy >= -3; dy--) {
                    feet.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
                    if(spawnFeetOpen(feet))
                        return feet.immutable();
                }
            }
        }
        return null;
    }

    private boolean spawnFeetOpen(BlockPos feet) {
        BlockPos below = feet.below();
        BlockState ground = level().getBlockState(below);
        if(!ground.isFaceSturdy(level(), below, Direction.UP))
            return false;
        BlockState legs = level().getBlockState(feet);
        BlockState head = level().getBlockState(feet.above());
        return legs.getBlock().isPossibleToRespawnInThis(legs) && head.getBlock().isPossibleToRespawnInThis(head);
    }

    private void readSpawn(CompoundTag nbt) {
        spawnPlayers.clear();
        spawnPos = null;
        spawnDimension = null;
        if(!nbt.contains("SpawnPos", Tag.TAG_LONG))
            return;
        ResourceLocation dimension = ResourceLocation.tryParse(nbt.getString("SpawnDimension"));
        if(dimension == null)
            return;
        spawnPos = BlockPos.of(nbt.getLong("SpawnPos"));
        spawnDimension = ResourceKey.create(Registries.DIMENSION, dimension);
        ListTag list = nbt.getList("SpawnPlayers", Tag.TAG_COMPOUND);
        for(int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            if(entry.hasUUID("Id"))
                spawnPlayers.add(entry.getUUID("Id"));
        }
    }

    private void writeSpawn(CompoundTag nbt) {
        if(spawnPos == null || spawnDimension == null)
            return;
        nbt.putLong("SpawnPos", spawnPos.asLong());
        nbt.putString("SpawnDimension", spawnDimension.location().toString());
        ListTag list = new ListTag();
        for(UUID id : spawnPlayers) {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("Id", id);
            list.add(entry);
        }
        nbt.put("SpawnPlayers", list);
    }

}
