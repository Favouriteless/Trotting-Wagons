package net.favouriteless.trotting_wagons.common.entities.base;

import net.favouriteless.trotting_wagons.ServerConfig;
import net.favouriteless.trotting_wagons.TrottingWagons;
import net.favouriteless.trotting_wagons.client.TWClient;
import net.favouriteless.trotting_wagons.compat.BlockRunnerSpeed;
import net.favouriteless.trotting_wagons.common.init.TWItems;
import net.favouriteless.trotting_wagons.common.util.LevelUtils;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;

import net.favouriteless.trotting_wagons.common.network.packets.SyncSeatsPayload;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.SynchedEntityData.Builder;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.camel.Camel;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public abstract class AbstractWagon extends AbstractGeckolibVehicle {

    private static final Random RANDOM = new Random();
    public static final int WHEEL_COUNT = 4;
    private static final float WHEEL_SAG = 14F;
    private static final float FRONT_SAG = 10F;
    private static final double WHEEL_SINK = 0.45D;

    private static final EntityDataAccessor<Float> DATA_ID_HEALTH = SynchedEntityData.defineId(AbstractWagon.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<OptionalInt> DATA_ID_COLOR = SynchedEntityData.defineId(AbstractWagon.class, EntityDataSerializers.OPTIONAL_UNSIGNED_INT);
    private static final EntityDataAccessor<Byte> DATA_WHEELS = SynchedEntityData.defineId(AbstractWagon.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Byte> DATA_WHEEL_KINDS = SynchedEntityData.defineId(AbstractWagon.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> DATA_HITCH_LEFT = SynchedEntityData.defineId(AbstractWagon.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_HITCH_RIGHT = SynchedEntityData.defineId(AbstractWagon.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_DISTANCE = SynchedEntityData.defineId(AbstractWagon.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<String> DATA_OWNER_NAME = SynchedEntityData.defineId(AbstractWagon.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> DATA_LOCKED = SynchedEntityData.defineId(AbstractWagon.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_SPEED_LEVEL = SynchedEntityData.defineId(AbstractWagon.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_CURRENT_SPEED = SynchedEntityData.defineId(AbstractWagon.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_MAX_SPEED = SynchedEntityData.defineId(AbstractWagon.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<String> DATA_REPAIR = SynchedEntityData.defineId(AbstractWagon.class, EntityDataSerializers.STRING);
    public static final int REPAIR_SLOTS = 4;
    private static final int WHEEL_DURABILITY = 100;
    private static final double WIDE_WHEEL_FACTOR = 0.8D;
    private static final int WHEEL_BREAK_BELOW = 20;

    public static final double GRAVITY = 0.04D;
    public static final int SPEED_LEVELS = 5;
    private static final double BLOCKS_PER_SPEED_ATTRIBUTE = 43.17D;
    public static final double HORIZONTAL_TETHER = 0.5D * 0.5D; // Use sqr for performance
    /** Farther than this from the hitch point, in any direction, and the animal is released. */
    private static final double HITCH_BREAK_DISTANCE_SQR = 2.0D * 2.0D;
    private static final Set<UUID> HARNESS_MARKS = ConcurrentHashMap.newKeySet();
    public static final int BOARD_COOLDOWN_TICKS = 60;
    public static final double ANIMAL_BOARD_RANGE = 1.0D;
    public static final double HITCH_RANGE = 1.5D;
    private static final String BOARD_COOLDOWN = "tw_board_cooldown";
    /** Saves below this stored the driver's right-hand animal in {@code leftHorse}. */
    private static final byte HITCH_SIDES = 1;

    private double speedCoefficient;
    private final float turnRate;
    private final int maxHealth;

    private final double horseX;
    private final double horseY;
    private final float shaftAngle;

    public UUID owner;
    private String ownerName = "";
    private double distance;
    private double kilometerPart;
    private boolean movedRecently;
    private double currentSpeed;

    private final Mob[] horses = new Mob[2];
    private final UUID[] horseUuids = new UUID[2];

    private final WagonSeat[] seats;
    private final Entity[] seatEntities;
    private final int[] seatIds;
    private final boolean[] seatLocked;
    private final Map<UUID, Integer> savedSeatByUuid = new HashMap<>();
    private int pendingSeat = -1;
    private int dismountBehindId = Integer.MIN_VALUE;

    private final NonNullList<ItemStack> wheels = NonNullList.withSize(WHEEL_COUNT, ItemStack.EMPTY);
    private final ItemStackHandler wheelHandler = new ItemStackHandler(wheels) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return stack.is(TrottingWagons.WAGON_WHEELS);
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        protected void onContentsChanged(int slot) {
            updateWheelMask();
        }
    };
    private final NonNullList<ItemStack> repair = NonNullList.withSize(REPAIR_SLOTS, ItemStack.EMPTY);
    private final String[] repairNeed = new String[REPAIR_SLOTS];
    private boolean repairUpdating;
    private long repairBlockedUntil;
    private final ItemStackHandler repairHandler = new ItemStackHandler(repair) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return acceptsRepair(slot, stack);
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        protected void onContentsChanged(int slot) {
            if(!repairUpdating)
                tryFinishRepair();
        }
    };

    public AbstractWagon(EntityType<? extends AbstractWagon> type, Level level, double wheelWidth, double wheelLength, WagonSeat[] seats,
                         double speedCoefficient, double turnRate, int maxHealth, double horseX, double horseY, float shaftAngle) {
        super(type, level, wheelWidth, wheelLength, seatPositions(seats));

        this.speedCoefficient = speedCoefficient;
        this.turnRate = (float)turnRate;
        this.maxHealth = maxHealth;
        this.horseX = horseX;
        this.horseY = horseY;
        this.shaftAngle = shaftAngle;
        this.seats = seats;
        this.seatEntities = new Entity[seats.length];
        this.seatIds = new int[seats.length];
        this.seatLocked = new boolean[seats.length];
        for(int i = 0; i < seatIds.length; i++)
            seatIds[i] = -1;

        setHealth(maxHealth);
        if(!level.isClientSide) {
            ItemStack wheel = new ItemStack(TWItems.WHEEL.get());
            for(int i = 0; i < WHEEL_COUNT; i++)
                wheelHandler.setStackInSlot(i, wheel.copy());
        }
    }

    protected double speedCoefficient() {
        return speedCoefficient;
    }

    /** Cargo wagons call this when a barrel, chest, or log stack changes, not from the movement tick. */
    protected void setSpeedCoefficient(double speedCoefficient) {
        this.speedCoefficient = speedCoefficient;
    }

    private static Vec3[] seatPositions(WagonSeat[] seats) {
        Vec3[] positions = new Vec3[seats.length];
        for(int i = 0; i < seats.length; i++)
            positions[i] = seats[i].position();
        return positions;
    }

    @Override
    public void tick() {
        if(firstTick && level().isClientSide)
            TWClient.createWagonSound(this);

        if(!level().isClientSide) {
            validateHorses();
            if(hasFixedSeats()) {
                keepHarnessedOutOfSeats();
                boardNearbyAnimals();
            }
            if(tickCount % 20 == 0 && movedRecently) {
                maybeBreakWornWheels();
                movedRecently = false;
            }
            if(tickCount % 10 == 0) {
                entityData.set(DATA_DISTANCE, (float)distance);
                refreshRepairSlots();
            }
        }
        super.tick();
    }

    private void validateHorses() {
        if(firstTick || level().getGameTime() % 100 == 0) { // Repeated checks help resolve chunk loading issues.
            retainHorse(Side.LEFT);
            retainHorse(Side.RIGHT);
        }

        validate(Side.LEFT);
        validate(Side.RIGHT);
    }

    // A missing animal stays harnessed until it loads again. Only a loaded dead animal is cleared.
    private void retainHorse(Side side) {
        if(horseUuids[side.ordinal()] == null)
            return;
        Mob horse = getHorse(side);
        if(horse != null) {
            setHorse(horse, side);
            return;
        }
        Entity found = level().getEntities().get(horseUuids[side.ordinal()]);
        if(found != null && !found.isAlive())
            setHorse(null, side);
    }

    private void keepHarnessedOutOfSeats() {
        for(Entity passenger : List.copyOf(getPassengers())) {
            if(passenger instanceof Mob && harnesses(passenger))
                passenger.stopRiding();
        }
    }

    private void validate(Side side) {
        if(hitchSlots() < 1 || (side == Side.RIGHT && hitchSlots() < 2)) {
            if(getHorse(side) != null)
                setHorse(null, side);
            return;
        }
        Mob horse = getHorse(side);
        if(horse == null)
            return;
        if(horse.isBaby()) {
            unhitch(horse);
            return;
        }
        boolean tilted = isTilted();
        Vec3 anchor = tilted ? shaftWorld(side, getYRot()) : hitchWorld(side, getYRot());
        if(horse.position().distanceToSqr(anchor) > HITCH_BREAK_DISTANCE_SQR) {
            unhitch(horse);
            return;
        }
        boolean pullHorizontal = tilted || !isHorseInRange(side);
        double x = pullHorizontal ? anchor.x : horse.getX();
        double z = pullHorizontal ? anchor.z : horse.getZ();
        double y = horse.getY();
        double ground = feetOnGround(x, z);
        if(!Double.isNaN(ground) && (pullHorizontal || y < ground))
            y = ground;
        if(pullHorizontal || y != horse.getY()) {
            horse.setPos(x, y, z);
            if(y == ground)
                horse.setDeltaMovement(horse.getDeltaMovement().multiply(1, 0, 1));
        }
    }

    private boolean isTilted() {
        if(Math.abs(getXRot()) > 0.05F)
            return true;
        WheelSag sag = wheelSag();
        return sag.pitch() != 0 || sag.roll() != 0;
    }

    private boolean isHorseInRange(Side side) {
        final Mob horse = getHorse(side);
        if(horse == null)
            return false;

        Vec3 displacement = horse.position().subtract(getHorsePos(side));
        return horse.isAlive() && displacement.horizontalDistanceSqr() < HORIZONTAL_TETHER;
    }

    private void moveHorse(Mob horse, Vec3 move) {
        horse.setDeltaMovement(move.add(0, horse.getDeltaMovement().y, 0));
        horse.move(MoverType.SELF, horse.getDeltaMovement());
        double ground = feetOnGround(horse.getX(), horse.getZ());
        if(Double.isNaN(ground) || horse.getY() >= ground)
            return;
        horse.setPos(horse.getX(), ground, horse.getZ());
        Vec3 delta = horse.getDeltaMovement();
        if(delta.y < 0)
            horse.setDeltaMovement(delta.x, 0, delta.z);
    }

    private double feetOnGround(double x, double z) {
        double ground = groundUnder(x, z);
        if(Double.isNaN(ground) || ground < getY() - 1.5D || ground > getY() + 1.5D)
            return Double.NaN;
        return ground;
    }

    private double groundUnder(double x, double z) {
        double top = getY() + 2.5D;
        double bottom = getY() - 2.5D;
        for(int i = 0; i < 8 && top > bottom; i++) {
            Vec3 from = new Vec3(x, top, z);
            BlockHitResult hit = level().clip(new ClipContext(from, new Vec3(x, bottom, z), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
            if(hit.getType() != HitResult.Type.BLOCK)
                return Double.NaN;
            if(from.y - hit.getLocation().y > 1.0E-3D)
                return hit.getLocation().y;
            top -= 0.5D;
        }
        return Double.NaN;
    }

    @Override
    public void moveTick() {
        final Mob leftHorse = getHorse(Side.LEFT);
        final Mob rightHorse = getHorse(Side.RIGHT);

        if(!isVehicle() || !(this.getControllingPassenger() instanceof Player) || !hasPullers()) {
            setSpeedLevel(0);
            setSteeringImpulse(0);
        }
        if(!hasAllWheels() || holdsTravel())
            currentSpeed = 0;

        handleSteering();
        handleAcceleration();

        Vec3 forward = getForward().multiply(1, 0, 1);
        Vec3 velocity = forward.scale(currentSpeed);

        // The horses or wagon may get blocked so we simulate first and use the shortest horizontal distance.
        Vec3 leftMove = leftHorse != null ? leftHorse.collide(velocity) : null;
        Vec3 rightMove = rightHorse != null ? rightHorse.collide(velocity) : null;

        Vec3 move = collide(velocity);
        if(leftMove != null && leftMove.horizontalDistanceSqr() < move.horizontalDistanceSqr())
            move = leftMove;
        if(rightMove != null && rightMove.horizontalDistanceSqr() < move.horizontalDistanceSqr())
            move = rightMove;

        move = move.multiply(1, 0, 1); // "Flatten" the move vector so entities can individually handle their step heights.

        if(!isNoGravity())
            move = move.add(0, -GRAVITY, 0);

        if(leftHorse != null)
            moveHorse(leftHorse, move);
        if(rightHorse != null)
            moveHorse(rightHorse, move);

        setDeltaMovement(move.add(0, getDeltaMovement().y, 0));
        Vec3 before = position();
        move(MoverType.SELF, getDeltaMovement());
        if(!level().isClientSide)
            addTravel(Math.sqrt(position().subtract(before).horizontalDistanceSqr()));
        setXRot(rotlerp(getXRot(), calculateTargetXRot(), (float)(9 * currentSpeed / (SPEED_LEVELS * currentSpeed)))); // 7 is just a magic scaling number for smoothing.
    }

    private void handleSteering() {
        double maxSpeed = maxSpeedPerTick();
        float speedPercent = Math.abs(currentSpeed) < 1.0E-4 ? 0.5F : maxSpeed <= 1.0E-6 ? 0.0F : (float)(currentSpeed / maxSpeed);
        float steer = -getSteeringImpulse() * speedPercent * turnRate;

        if(steer != 0) {
            final Mob leftHorse = getHorse(Side.LEFT);
            final Mob rightHorse = getHorse(Side.RIGHT);

            Vec3 leftDisplace = collideSteering(Side.LEFT, steer);
            Vec3 rightDisplace = collideSteering(Side.RIGHT, steer);
            boolean leftBlocked = leftHorse != null && leftDisplace.equals(Vec3.ZERO);
            boolean rightBlocked = rightHorse != null && rightDisplace.equals(Vec3.ZERO);

            if((leftHorse != null || rightHorse != null) && !leftBlocked && !rightBlocked) {
                setYRot(Mth.wrapDegrees(getYRot() + steer));

                float yRot = getYRot();
                if(leftHorse != null)
                    setHorsePos(leftHorse, leftHorse.position().add(leftDisplace), yRot);
                if(rightHorse != null)
                    setHorsePos(rightHorse, rightHorse.position().add(rightDisplace), yRot);
            }
        }
    }

    private void handleAcceleration() {
        double maxSpeed = computeMaxSpeedPerTick();
        double targetSpeed = maxSpeed * getSpeedLevel() / (SPEED_LEVELS - 1);
        if(isReversing())
            targetSpeed *= -0.25D;

        double accel = speedCoefficient() / 400.0D;
        double speedDiff = targetSpeed - currentSpeed;
        currentSpeed += Mth.sign(speedDiff) * Math.min(Math.abs(accel), Math.abs(speedDiff));
        if(!level().isClientSide) {
            entityData.set(DATA_CURRENT_SPEED, (float)currentSpeed);
            entityData.set(DATA_MAX_SPEED, (float)maxSpeed);
        }
    }

    private double computeMaxSpeedPerTick() {
        if(!hasAllWheels() || !hasPullers() || holdsTravel())
            return 0;
        double slowest = Double.POSITIVE_INFINITY;
        for(Mob animal : horses) {
            if(animal == null || !animal.isAlive())
                continue;
            var attribute = animal.getAttribute(Attributes.MOVEMENT_SPEED);
            if(attribute == null)
                continue;
            slowest = Math.min(slowest, BlockRunnerSpeed.movementSpeed(attribute) * BLOCKS_PER_SPEED_ATTRIBUTE);
        }
        if(slowest == Double.POSITIVE_INFINITY)
            return 0;
        double speed = slowest * speedCoefficient() / 20.0D;
        if(hasWideWheel())
            speed *= WIDE_WHEEL_FACTOR;
        speed *= groundSpeedFactor();
        speed *= harnessSpeedFactor();
        return speed;
    }

    /** Two-animal shafts stay at 1. A single tongue multiplies the finished speed. */
    private double harnessSpeedFactor() {
        return hitchSlots() == 1 ? singleHarnessFactor() : 1.0D;
    }

    /** Оглобли, bone TongueSingle. Дышло is always 1. Kibitka and the big cart stay at 1. */
    protected double singleHarnessFactor() {
        return 1.0D;
    }

    private double groundSpeedFactor() {
        double slowest = Double.POSITIVE_INFINITY;
        for(WagonParts.Part part : WagonParts.of(this)) {
            if(part.kind() != WagonParts.Kind.WHEEL || (getWheelMask() & (1 << part.index())) == 0)
                continue;
            Vec3 center = WagonParts.worldCenter(this, part);
            BlockPos pos = BlockPos.containing(center.x, getY() - 1.0E-5D, center.z);
            slowest = Math.min(slowest, BlockRunnerSpeed.factor(level().getBlockState(pos).getBlock()));
        }
        return slowest == Double.POSITIVE_INFINITY ? 1.0D : slowest;
    }

    protected boolean holdsTravel() {
        return false;
    }

    public float syncedMaxSpeed() {
        return entityData.get(DATA_MAX_SPEED);
    }

    public float speedFraction() {
        float maxSpeed = entityData.get(DATA_MAX_SPEED);
        if(maxSpeed <= 1.0E-4F)
            return 0.0F;
        return Mth.clamp(Math.abs(entityData.get(DATA_CURRENT_SPEED)) / maxSpeed, 0.0F, 1.0F);
    }

    private double maxSpeedPerTick() {
        return level().isClientSide ? entityData.get(DATA_MAX_SPEED) : computeMaxSpeedPerTick();
    }

    private void setHorsePos(Mob horse, Vec3 pos, float rot) {
        if(horse == null)
            return;
        horse.setYRot(rot);
        horse.setYBodyRot(rot);
        horse.setYHeadRot(rot);
        horse.setPos(pos);
    }

    private Vec3 collideSteering(Side side, float angle) {
        Mob horse = getHorse(side);
        if(horse == null)
            return Vec3.ZERO;

        Vec3 desired = isTilted() ? shaftWorld(side, getYRot() + angle) : hitchWorld(side, getYRot() + angle);
        Vec3 displacement = new Vec3(desired.x - horse.getX(), 0, desired.z - horse.getZ());
        // The wagon box must not eat the hitch step. Camels stand close enough that a turn used to stop.
        hitchMoving = true;
        Vec3 collidedDisplacement;
        try {
            collidedDisplacement = horse.collide(displacement);
        } finally {
            hitchMoving = false;
        }

        if(!displacement.equals(collidedDisplacement.multiply(1, 0, 1)))
            return Vec3.ZERO;

        return collidedDisplacement;
    }

    private float rotlerp(float angle, float target, float max) {
        float d = Mth.wrapDegrees(target - angle);
        if(d > max)
            d = max;
        if(d < -max)
            d = -max;

        return angle + d;
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if(!isAlive())
            return InteractionResult.PASS;

        final boolean isClientSide = level().isClientSide;

        if(player.isSecondaryUseActive()) {
            ItemStack stack = player.getItemInHand(hand);
            Item item = stack.getItem();

            if(stack.is(TWItems.HORSE_WHIP.get())) {
                if(!isClientSide)
                    toggleReversing();
                return InteractionResult.sidedSuccess(isClientSide);
            }

            if(stack.is(ItemTags.AXES)) {
                if(!isClientSide && player.getUUID().equals(owner))
                    onDestroyed(true);
                return InteractionResult.sidedSuccess(isClientSide);
            }

            if(acceptsDye() && TrottingWagons.DYE_ITEMS.containsKey(item)) {
                setColor(TrottingWagons.DYE_ITEMS.get(item));
                return InteractionResult.sidedSuccess(isClientSide);
            }

            if(player.getVehicle() != this && WagonParts.take(this, player))
                return InteractionResult.sidedSuccess(isClientSide);
        }
        else if(player.getVehicle() != this) {
            if(WagonParts.use(this, player, hand))
                return InteractionResult.sidedSuccess(isClientSide);
            if(!isClientSide) {
                player.setYRot(getYRot());
                player.setXRot(getXRot());

                return mountNearest(player) ? InteractionResult.CONSUME : InteractionResult.PASS;
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    public boolean hitchRiddenAnimal(Mob animal) {
        if(!isAllowedPuller(animal) || !hasFreeHitch() || !isNearTongue(animal) || findHitch(animal) != null)
            return false;
        if(animal.getVehicle() != null)
            animal.stopRiding();
        if(getHorse(Side.LEFT) == null)
            hitchHorse(animal, Side.LEFT);
        else if(hitchSlots() > 1)
            hitchHorse(animal, Side.RIGHT);
        return true;
    }

    public void awardSinner(ServerPlayer player) {
        Mob left = getHorse(Side.LEFT);
        Mob right = getHorse(Side.RIGHT);
        if(left == null || right == null || left.getType() == right.getType())
            return;
        if(player.level().getServer() == null)
            return;
        AdvancementHolder holder = player.level().getServer().getAdvancements().get(TrottingWagons.id("sinner"));
        if(holder != null)
            player.getAdvancements().award(holder, "hitch_different");
    }

    public void unhitch(Mob animal) {
        if(getHorse(Side.LEFT) == animal)
            setHorse(null, Side.LEFT);
        else if(getHorse(Side.RIGHT) == animal)
            setHorse(null, Side.RIGHT);
    }

    public boolean isHitched(Entity entity) {
        return harnesses(entity);
    }

    // Identity, synced entity id, and saved uuid. A stale horse reference must not hide a live harnessed animal.
    public boolean harnesses(Entity entity) {
        if(entity == null)
            return false;
        int id = entity.getId();
        UUID uuid = entity.getUUID();
        for(Side side : Side.values()) {
            if(id >= 0 && getHitchEntityId(side) == id)
                return true;
            UUID stored = horseUuids[side.ordinal()];
            if(stored != null && stored.equals(uuid))
                return true;
            Mob horse = horses[side.ordinal()];
            if(horse == entity || (horse != null && uuid.equals(horse.getUUID())))
                return true;
        }
        return false;
    }

    private void hitchHorse(Mob horse, Side side) {
        setHorse(horse, side);
        initHorse(horse, side);
    }

    private void initHorse(Mob horse, Side side) {
        if(horse == null)
            return;
        horse.ejectPassengers();
        horse.setDeltaMovement(Vec3.ZERO);
        calmHarnessed(horse);
        setHorsePos(horse, getHorsePos(side), getYRot());
    }

    /** Once, at the moment of hitching. Later grazing is refused by AbstractHorseGrazeMixin. */
    private static void calmHarnessed(Mob horse) {
        if(horse instanceof AbstractHorse mount) {
            if(mount.isEating())
                mount.setEating(false);
            if(mount.isStanding())
                mount.setStanding(false);
            if(mount.getXRot() != 0.0F)
                mount.setXRot(0.0F);
        }
        if(horse instanceof Camel camel) {
            if(camel.isDashing())
                camel.setDashing(false);
            if(camel.isCamelSitting() || camel.isInPoseTransition())
                camel.standUpInstantly();
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if(isInvulnerableTo(source))
            return false;

        if(level().isClientSide || isRemoved())
            return true;

        level().playSound(null, blockPosition(), SoundEvents.WOOD_BREAK, SoundSource.MASTER);
        double w = getBbWidth();
        double h = getBbHeight();
        ((ServerLevel)level()).sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SPRUCE_PLANKS.defaultBlockState()),
                getX(), getY(), getZ(), 100, w, h, w, 0);

        setHealth(getHealth() - amount);
        refreshRepairSlots();
        gameEvent(GameEvent.ENTITY_DAMAGE, source.getEntity());

        if(getHealth() <= 0)
            onDestroyed(false);

        return true;
    }

    protected void onDestroyed(boolean dropItem) {
        if(level().isClientSide)
            return;

        setHorse(null, Side.LEFT);
        setHorse(null, Side.RIGHT);

        ejectPassengers();
        Containers.dropContents(level(), blockPosition(), wheels);
        ItemStackHandler gear = getHarnessItems();
        if(gear != null) {
            NonNullList<ItemStack> held = NonNullList.withSize(gear.getSlots(), ItemStack.EMPTY);
            for(int i = 0; i < gear.getSlots(); i++)
                held.set(i, gear.getStackInSlot(i).copy());
            Containers.dropContents(level(), blockPosition(), held);
        }
        discard();

        if(!level().getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS))
            return;

        if(dropItem) {
            ItemStack pickResult = getPickResult();
            if(pickResult != null)
                spawnAtLocation(pickResult);
        }
        else {
            spawnAtLocation(new ItemStack(Items.OAK_PLANKS, RANDOM.nextInt(7)));
            spawnAtLocation(new ItemStack(Items.IRON_INGOT, RANDOM.nextInt(3)));
        }
    }

    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        if(passenger.getId() == dismountBehindId) {
            dismountBehindId = Integer.MIN_VALUE;
            return dismountBehind(passenger);
        }

        final double width = getBbWidth();
        double offset = Math.sqrt(width * width + width * width) / 2 + 0.05D;

        double pWidth = passenger.getBbWidth();
        double pOffset = Math.sqrt(pWidth * pWidth + pWidth * pWidth) / 2;

        float yRot = getYRot();

        pWidth = pWidth * 0.8F;

        Vec3 wOffset = new Vec3(0, passenger.getEyeHeight(), 0);

        Vec3 pos = position().add(rotateY(new Vec3(offset + pOffset, 0, 0), yRot)); // Left
        if(LevelUtils.isEmpty(level(), AABB.ofSize(pos.add(wOffset), pWidth, 1.0E-6D, pWidth)))
            return pos;

        pos = position().add(rotateY(new Vec3(-offset - pOffset, 0, 0), yRot)); // Right
        if(LevelUtils.isEmpty(level(), AABB.ofSize(pos.add(wOffset), pWidth, 1.0E-6D, pWidth)))
            return pos;

        pos = position().add(rotateY(new Vec3(0, 0, offset + pOffset), yRot)); // Front
        if(LevelUtils.isEmpty(level(), AABB.ofSize(pos.add(wOffset), pWidth, 1.0E-6D, pWidth)))
            return pos;

        pos = position().add(rotateY(new Vec3(0, 0, -offset - pOffset), yRot)); // Back
        if(LevelUtils.isEmpty(level(), AABB.ofSize(pos.add(wOffset), pWidth, 1.0E-6D, pWidth)))
            return pos;

        return super.getDismountLocationForPassenger(passenger);
    }

    protected boolean dismountsBehind(LivingEntity passenger, int seat) {
        return false;
    }

    protected boolean claimsRearDismount(LivingEntity passenger) {
        return passenger.getId() == dismountBehindId;
    }

    protected double dismountRear() {
        return getBbWidth() * 0.5D;
    }

    private Vec3 dismountBehind(LivingEntity passenger) {
        double back = dismountRear() + passenger.getBbWidth() * 0.5D + 0.05D;
        Vec3 first = null;
        for(int step = 0; step < 4; step++) {
            Vec3 pos = position().add(rotateY(new Vec3(0, 0, -(back + step * 0.5D)), getYRot()));
            if(first == null)
                first = pos;
            if(dismountFree(passenger, pos))
                return pos;
        }
        return first;
    }

    protected boolean dismountFree(LivingEntity passenger, Vec3 pos) {
        double width = passenger.getBbWidth() * 0.8D;
        Vec3 eye = pos.add(0, passenger.getEyeHeight(), 0);
        return LevelUtils.isEmpty(level(), AABB.ofSize(eye, width, 1.0E-6D, width));
    }

    public boolean touchesLadder(Player player) {
        return false;
    }

    public boolean landFromLadder(Player player) {
        return false;
    }

    public static boolean climbsWagon(Player player) {
        if(player.isSpectator() || player.isPassenger() || player.getAbilities().flying || !player.isAlive())
            return false;
        AABB area = player.getBoundingBox().inflate(1.0D);
        for(AbstractWagon wagon : player.level().getEntitiesOfClass(AbstractWagon.class, area, Entity::isAlive)) {
            if(wagon.touchesLadder(player))
                return true;
        }
        return false;
    }

    public static void finishLadderClimb(Player player) {
        AABB area = player.getBoundingBox().inflate(1.0D);
        for(AbstractWagon wagon : player.level().getEntitiesOfClass(AbstractWagon.class, area, Entity::isAlive)) {
            if(wagon.landFromLadder(player))
                return;
        }
    }

    protected Vec3 yawLocal(Vec3 world) {
        return rotateY(world.subtract(position()), -getYRot());
    }

    protected Vec3 yawWorld(Vec3 local) {
        return position().add(rotateY(local, getYRot()));
    }

    @Override
    protected void defineSynchedData(Builder builder) {
        DyeColor color = getDefaultColor();
        builder.define(DATA_ID_COLOR, color != null ? OptionalInt.of(color.getId()) : OptionalInt.empty());
        builder.define(DATA_ID_HEALTH, (float)0);
        builder.define(DATA_WHEELS, (byte)0x0F);
        builder.define(DATA_WHEEL_KINDS, (byte)0x55);
        builder.define(DATA_HITCH_LEFT, -1);
        builder.define(DATA_HITCH_RIGHT, -1);
        builder.define(DATA_DISTANCE, 0.0F);
        builder.define(DATA_OWNER_NAME, "");
        builder.define(DATA_LOCKED, false);
        builder.define(DATA_SPEED_LEVEL, 0);
        builder.define(DATA_CURRENT_SPEED, 0.0F);
        builder.define(DATA_MAX_SPEED, 0.0F);
        builder.define(DATA_REPAIR, ",,,");
    }

    @Override
    public void readAdditionalSaveData(CompoundTag nbt) {
        if(nbt.contains("color", 99))
            setColor(DyeColor.byId(nbt.getInt("color")));
        if(nbt.contains("owner"))
            owner = nbt.getUUID("owner");
        if(nbt.contains("ownerName"))
            setOwnerName(nbt.getString("ownerName"));
        else
            rememberOwnerName();
        if(nbt.contains("health"))
            setHealth(nbt.getFloat("health"));
        if(nbt.contains("distance"))
            distance = nbt.getDouble("distance");
        if(nbt.contains("distancePart"))
            kilometerPart = nbt.getDouble("distancePart");
        entityData.set(DATA_DISTANCE, (float)distance);
        entityData.set(DATA_LOCKED, nbt.getBoolean("locked"));
        if(nbt.contains("speedLevel"))
            setSpeedLevel(nbt.getInt("speedLevel"));
        UUID left = nbt.hasUUID("leftHorse") ? nbt.getUUID("leftHorse") : null;
        UUID right = nbt.hasUUID("rightHorse") ? nbt.getUUID("rightHorse") : null;
        if(hitchSlots() > 1 && nbt.getByte("hitchSides") < HITCH_SIDES) {
            UUID held = left;
            left = right;
            right = held;
        }
        setHorseUUID(left, Side.LEFT);
        setHorseUUID(right, Side.RIGHT);
        savedSeatByUuid.clear();
        if(nbt.contains("seatLocks", Tag.TAG_BYTE_ARRAY)) {
            byte[] locks = nbt.getByteArray("seatLocks");
            for(int i = 0; i < locks.length && i < seatLocked.length; i++)
                seatLocked[i] = locks[i] != 0;
        }
        if(nbt.contains("seats", Tag.TAG_LIST)) {
            ListTag seatsTag = nbt.getList("seats", Tag.TAG_COMPOUND);
            for(int i = 0; i < seatsTag.size(); i++) {
                CompoundTag seatTag = seatsTag.getCompound(i);
                if(seatTag.hasUUID("id"))
                    savedSeatByUuid.put(seatTag.getUUID("id"), seatTag.getInt("seat"));
            }
        }
        if(nbt.contains("Wheels", Tag.TAG_COMPOUND)) {
            for(int i = 0; i < wheels.size(); i++)
                wheels.set(i, ItemStack.EMPTY);
            ContainerHelper.loadAllItems(nbt.getCompound("Wheels"), wheels, registryAccess());
        }
        updateWheelMask();
        if(nbt.contains("Repair", Tag.TAG_COMPOUND)) {
            for(int i = 0; i < repair.size(); i++)
                repair.set(i, ItemStack.EMPTY);
            ContainerHelper.loadAllItems(nbt.getCompound("Repair"), repair, registryAccess());
        }
        if(nbt.contains("RepairNeeds"))
            readRepairNeeds(nbt.getString("RepairNeeds"));
        refreshRepairSlots();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag nbt) {
        if(getColor() != null)
            nbt.putByte("color", (byte)getColor().getId());
        if(owner != null)
            nbt.putUUID("owner", owner);
        if(!ownerName.isEmpty())
            nbt.putString("ownerName", ownerName);
        nbt.putFloat("health", getHealth());
        nbt.putDouble("distance", distance);
        nbt.putDouble("distancePart", kilometerPart);
        nbt.putBoolean("locked", isLocked());
        nbt.putInt("speedLevel", getSpeedLevel());
        nbt.putByte("hitchSides", HITCH_SIDES);
        if(getHorseUUID(Side.LEFT) != null)
            nbt.putUUID("leftHorse", getHorseUUID(Side.LEFT));
        if(getHorseUUID(Side.RIGHT) != null)
            nbt.putUUID("rightHorse", getHorseUUID(Side.RIGHT));
        ListTag seatsTag = new ListTag();
        for(int i = 0; i < seatEntities.length; i++) {
            if(seatEntities[i] == null)
                continue;
            CompoundTag seatTag = new CompoundTag();
            seatTag.putInt("seat", i);
            seatTag.putUUID("id", seatEntities[i].getUUID());
            seatsTag.add(seatTag);
        }
        nbt.put("seats", seatsTag);
        byte[] locks = new byte[seatLocked.length];
        for(int i = 0; i < seatLocked.length; i++)
            locks[i] = (byte)(seatLocked[i] ? 1 : 0);
        nbt.putByteArray("seatLocks", locks);
        CompoundTag wheelsTag = new CompoundTag();
        ContainerHelper.saveAllItems(wheelsTag, wheels, registryAccess());
        nbt.put("Wheels", wheelsTag);
        CompoundTag repairTag = new CompoundTag();
        ContainerHelper.saveAllItems(repairTag, repair, registryAccess());
        nbt.put("Repair", repairTag);
        nbt.putString("RepairNeeds", repairNeeds());
    }

    public void setOwner(Player player) {
        owner = player.getUUID();
        setOwnerName(player.getGameProfile().getName());
    }

    public void setOwnerName(String name) {
        ownerName = name == null ? "" : name;
        entityData.set(DATA_OWNER_NAME, ownerName);
    }

    public String getOwnerName() {
        return entityData.get(DATA_OWNER_NAME);
    }

    public boolean isOwnedBy(Player player) {
        return owner != null && owner.equals(player.getUUID());
    }

    public float getDistance() {
        return entityData.get(DATA_DISTANCE);
    }

    public boolean isLocked() {
        return entityData.get(DATA_LOCKED);
    }

    public void toggleLock(Player player) {
        if(isOwnedBy(player))
            entityData.set(DATA_LOCKED, !isLocked());
    }

    public Vec3 toWorld(Vec3 local) {
        return offsetToWorld(local, false);
    }

    public Vec3 fromWorld(Vec3 world) {
        Vec3 local = world.subtract(position());
        local = rotateY(local, -getYRot());
        WheelSag sag = wheelSag();
        local = local.add(0, sag.drop(), 0);
        local = rotateX(local, -sag.pitch());
        local = rotateZ(local, -sag.roll());
        Vec3 mid = new Vec3(0, 0, getXRot() < 0 ? getWheelLength() : -getWheelLength());
        local = local.subtract(mid);
        local = rotateX(local, -getXRot());
        return local.add(mid);
    }

    private void addTravel(double blocks) {
        if(blocks <= 1.0E-4D)
            return;
        movedRecently = true;
        distance += blocks;
        kilometerPart += blocks;
        while(kilometerPart >= 1000.0D) {
            kilometerPart -= 1000.0D;
            wearWheels();
        }
    }

    private void wearWheels() {
        boolean broke = false;
        for(int i = 0; i < wheels.size(); i++) {
            ItemStack stack = wheels.get(i);
            if(stack.isEmpty())
                continue;
            ensureWheelDurability(stack);
            int damage = stack.getDamageValue() + 1;
            if(damage >= stack.getMaxDamage()) {
                wheelHandler.setStackInSlot(i, ItemStack.EMPTY);
                broke = true;
            } else {
                stack.setDamageValue(damage);
                wheelHandler.setStackInSlot(i, stack);
            }
        }
        if(broke)
            level().playSound(null, blockPosition(), SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 0.8F, 1.0F);
    }

    private void maybeBreakWornWheels() {
        boolean broke = false;
        for(int i = 0; i < wheels.size(); i++) {
            ItemStack stack = wheels.get(i);
            if(stack.isEmpty())
                continue;
            ensureWheelDurability(stack);
            int remaining = stack.getMaxDamage() - stack.getDamageValue();
            if(remaining < WHEEL_BREAK_BELOW && RANDOM.nextFloat() < 0.01F) {
                wheelHandler.setStackInSlot(i, ItemStack.EMPTY);
                broke = true;
            }
        }
        if(broke)
            level().playSound(null, blockPosition(), SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 0.8F, 1.0F);
    }

    private static void ensureWheelDurability(ItemStack stack) {
        if(stack.getMaxDamage() <= 0)
            stack.set(DataComponents.MAX_DAMAGE, WHEEL_DURABILITY);
    }

    private void rememberOwnerName() {
        if(owner == null || !ownerName.isEmpty() || level().getServer() == null)
            return;
        ServerPlayer online = level().getServer().getPlayerList().getPlayer(owner);
        if(online != null) {
            setOwnerName(online.getGameProfile().getName());
            return;
        }
        if(level().getServer().getProfileCache() != null)
            level().getServer().getProfileCache().get(owner).ifPresent(profile -> setOwnerName(profile.getName()));
    }

    public int getSpeedLevel() {
        return entityData.get(DATA_SPEED_LEVEL);
    }

    public void setSpeedLevel(int level) {
        int clamped = Mth.clamp(level, 0, SPEED_LEVELS - 1);
        if(getSpeedLevel() != clamped)
            entityData.set(DATA_SPEED_LEVEL, clamped);
    }

    public void tryCycleSpeed(Player player) {
        if(player == getControllingPassenger())
            setSpeedLevel(getSpeedLevel() + 1);
    }

    public boolean tryLowerSpeed(Player player) {
        if(player != getControllingPassenger() || getSpeedLevel() <= 0)
            return false;
        setSpeedLevel(getSpeedLevel() - 1);
        return true;
    }

    private Mob getHorse(Side side) {
        UUID uuid = horseUuids[side.ordinal()];
        if(uuid == null)
            return null;

        Mob horse = horses[side.ordinal()];
        if(horse != null && horse.isAlive() && !horse.isRemoved() && horse.level() == level() && uuid.equals(horse.getUUID()))
            return horse;

        if(level().getEntities().get(uuid) instanceof Mob mob && mob.isAlive()) {
            horses[side.ordinal()] = mob;
            return mob;
        }
        horses[side.ordinal()] = null;
        return null;
    }

    private void setHorse(Mob horse, Side side) {
        Mob old = getHorse(side);
        if(old != null && old != horse) {
            old.setNoAi(false);
            setBoardCooldown(old);
            HARNESS_MARKS.remove(old.getUUID());
        }
        if(horse != null) {
            horse.setNoAi(true);
            HARNESS_MARKS.add(horse.getUUID());
        }

        horses[side.ordinal()] = horse;
        horseUuids[side.ordinal()] = horse != null ? horse.getUUID() : null;
        entityData.set(side == Side.LEFT ? DATA_HITCH_LEFT : DATA_HITCH_RIGHT, horse == null ? -1 : horse.getId());
    }

    private UUID getHorseUUID(Side side) {
        return horseUuids[side.ordinal()];
    }

    private void setHorseUUID(UUID uuid, Side side) {
        horseUuids[side.ordinal()] = uuid;
    }

    private Vec3 getHorsePos(Side side) {
        return hitchWorld(side, getYRot());
    }

    private Vec3 hitchWorld(Side side, float yaw) {
        return offsetToWorld(hitchLocal(side, 0), false, yaw);
    }

    private Vec3 shaftWorld(Side side, float yaw) {
        return offsetToWorld(hitchLocal(side, hitchedBackHeight()), false, yaw);
    }

    private Vec3 hitchLocal(Side side, double y) {
        Vec3 local = hitchOffset(side);
        Mob horse = getHorse(side);
        if(horse != null)
            local = local.add(0, 0, draftForward(horse));
        return new Vec3(local.x, y, local.z);
    }

    /** Camels used to stand one block closer. That put their box into the wagon and stopped the turn. */
    protected double draftForward(Mob animal) {
        return 0.0D;
    }

    public float getShaftAngle() {
        return shaftAngle;
    }

    public float tongueAngle(float bodyPitch, float bodyRoll, double drop) {
        if(getWheelMask() == 0)
            return 0;
        double pitch = Math.toRadians(bodyPitch);
        double roll = Math.toRadians(bodyRoll);
        boolean hitched = getHitchEntityId(Side.LEFT) >= 0 || getHitchEntityId(Side.RIGHT) >= 0;
        double reach = tongueLength();
        if(!hitched && pitch > 0)
            reach += 3D * getWheelLength();
        else if(!hitched && pitch < 0)
            reach += 1.25D * getWheelLength();
        double rest = tongueHeight() * Math.cos(roll) * Math.cos(pitch) - reach * Math.sin(pitch) - drop;
        double target = hitched ? hitchedBackHeight() : 0;
        double sine = Mth.clamp((target - rest) / tongueLength(), -1D, 1D);
        return (float)Math.asin(sine);
    }

    protected double tongueLength() {
        return 57.12D / 16D;
    }

    protected double tongueHeight() {
        return 7.5D / 16D;
    }

    protected double hitchedTongueTarget() {
        return 0.85D;
    }

    private double hitchedBackHeight() {
        double lowest = Double.POSITIVE_INFINITY;
        for(Side side : Side.values()) {
            Entity animal = level().getEntity(getHitchEntityId(side));
            if(!(animal instanceof Mob))
                animal = getHorse(side);
            if(animal == null || !animal.isAlive())
                continue;
            lowest = Math.min(lowest, hitchBackHeight(animal));
        }
        return lowest == Double.POSITIVE_INFINITY ? hitchedTongueTarget() : lowest;
    }

    /** Height the shaft aims at on this animal's back. */
    protected double hitchBackHeight(Entity animal) {
        return animal.getBbHeight() * 0.7D;
    }

    public DyeColor getColor() {
        OptionalInt i = entityData.get(DATA_ID_COLOR);
        return i.isPresent() ? DyeColor.byId(i.getAsInt()) : null;
    }

    public void setColor(DyeColor color) {
        entityData.set(DATA_ID_COLOR, color != null ? OptionalInt.of(color.getId()) : OptionalInt.empty());
    }

    protected DyeColor getDefaultColor() {
        return DyeColor.WHITE;
    }

    /** One texture and no dye sheets. Shift-click with a dye then falls through to part removal. */
    protected boolean acceptsDye() {
        return true;
    }

    public float getHealth() {
        return entityData.get(DATA_ID_HEALTH);
    }

    public int getMaxHealth() {
        return maxHealth;
    }

    protected void setHealth(float health) {
        float clamped = Mth.clamp(health, 0, maxHealth);
        if(maxHealth > 0 && Math.round(clamped) >= maxHealth)
            clamped = maxHealth;
        entityData.set(DATA_ID_HEALTH, clamped);
    }

    @Override
    public boolean isControlledByLocalInstance() {
        return isEffectiveAi();
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, DamageSource source) {
        return !getType().is(EntityTypeTags.FALL_DAMAGE_IMMUNE);
    }

    public boolean hasFixedSeats() {
        return seats != null;
    }

    public int getSeatCount() {
        return seats.length;
    }

    public WagonSeat getSeat(int index) {
        return seats[index];
    }

    public int getSeatEntityId(int index) {
        return seatIds[index];
    }

    public boolean isSeatLocked(int index) {
        return seatLocked[index];
    }

    // Empty seat closed by another animal. Manual locks stay on isSeatLocked. Null on wagons without that rule.
    public Component seatBlockHint(int index) {
        return null;
    }

    public boolean[] seatLocks() {
        return seatLocked.clone();
    }

    public boolean toggleSeatLock(Player player, int seat) {
        if(player.getVehicle() != this || seat < 0 || seat >= seats.length || !seats[seat].canLockAnimals())
            return false;
        seatLocked[seat] = !seatLocked[seat];
        syncSeats();
        return true;
    }

    public Vec3 getTongueAnchor() {
        return position().add(rotateY(new Vec3(0, 0, horseY), getYRot()));
    }

    public static void setBoardCooldown(Entity entity) {
        entity.getPersistentData().putLong(BOARD_COOLDOWN, entity.level().getGameTime() + BOARD_COOLDOWN_TICKS);
    }

    public static boolean isBoardCooldown(Entity entity) {
        return entity.level().getGameTime() < entity.getPersistentData().getLong(BOARD_COOLDOWN);
    }

    public static boolean isAllowedPuller(Entity entity) {
        if(entity instanceof LivingEntity living && living.isBaby())
            return false;
        try {
            return ServerConfig.INSTANCE.allowedEntities.get().contains(entity.getType().builtInRegistryHolder().key().location().toString());
        } catch(IllegalStateException ignored) {
            return true;
        }
    }

    public static AbstractWagon findHitch(Entity entity) {
        AABB box = entity.getBoundingBox().inflate(16.0D);
        for(AbstractWagon wagon : entity.level().getEntitiesOfClass(AbstractWagon.class, box)) {
            if(wagon.harnesses(entity))
                return wagon;
        }
        return null;
    }

    public static boolean isHarnessed(Entity entity) {
        return findHitch(entity) != null;
    }

    /** O(1) mark written when the hitch is set. Used so a horse tick can refuse grazing without searching wagons. */
    public static boolean isHarnessMarked(UUID uuid) {
        return uuid != null && HARNESS_MARKS.contains(uuid);
    }

    public static AbstractWagon findHitchTarget(Mob animal) {
        if(!isAllowedPuller(animal))
            return null;

        AbstractWagon best = null;
        double bestDist = HITCH_RANGE * HITCH_RANGE;
        AABB search = animal.getBoundingBox().inflate(8.0D);
        for(AbstractWagon wagon : animal.level().getEntitiesOfClass(AbstractWagon.class, search)) {
            if(!wagon.hasFreeHitch() || !wagon.isNearTongue(animal))
                continue;
            double dist = horizontalDistanceSqr(animal.position(), wagon.getTongueAnchor());
            if(dist <= bestDist) {
                bestDist = dist;
                best = wagon;
            }
        }
        return best;
    }

    public boolean mountNearest(Entity entity) {
        int seat = findNearestSeat(entity);
        if(seat < 0)
            return false;
        pendingSeat = seat;
        boolean mounted = entity.startRiding(this);
        pendingSeat = -1;
        return mounted;
    }

    /** Puts the rider in one seat. {@code startRiding} clears a sleep pose, so the caller restores it. */
    public boolean mountSeat(Entity entity, int seat) {
        if(seat < 0 || seat >= seats.length)
            return false;
        pendingSeat = seat;
        boolean mounted = entity.startRiding(this);
        pendingSeat = -1;
        return mounted;
    }

    public boolean ejectSeat(Player player, int seat) {
        if(player.getVehicle() != this || seat < 0 || seat >= seats.length)
            return false;
        if(!(seatEntities[seat] instanceof Mob mob) || seatEntities[seat] instanceof Player)
            return false;
        mob.stopRiding();
        return true;
    }

    public boolean canPlaceAnimal(int index, Entity animal) {
        if(index < 0 || index >= seats.length || !(animal instanceof LivingEntity) || animal instanceof Player)
            return false;
        int occupied = seatIds[index];
        if(occupied >= 0)
            return false;
        return seatAccepts(index, animal, true);
    }

    public int hitchSlots() {
        return 2;
    }

    /** Null until that wagon's screen has a harness cell. */
    public ItemStackHandler getHarnessItems() {
        return null;
    }

    public boolean canPlaceDraft(Entity animal, Side side) {
        if(hitchSlots() < 1 || (side == Side.RIGHT && hitchSlots() < 2))
            return false;
        if(!(animal instanceof Mob) || animal instanceof Player || !isAllowedPuller(animal))
            return false;
        int occupied = getHitchEntityId(side);
        if(occupied >= 0 && occupied != animal.getId())
            return false;
        AbstractWagon other = findHitch(animal);
        return other == null || other == this;
    }

    public boolean moveAnimal(Player player, int animalId, int to, boolean draft) {
        if(player.getVehicle() != this)
            return false;
        int fromSeat = -1;
        LivingEntity animal = null;
        for(int i = 0; i < seatEntities.length; i++) {
            if(seatEntities[i] != null && seatEntities[i].getId() == animalId) {
                fromSeat = i;
                animal = seatEntities[i] instanceof LivingEntity living ? living : null;
            }
        }
        Side fromSide = null;
        if(hitchMatches(Side.LEFT, animalId))
            fromSide = Side.LEFT;
        else if(hitchMatches(Side.RIGHT, animalId))
            fromSide = Side.RIGHT;
        if(animal == null && fromSide != null && getHorse(fromSide) instanceof LivingEntity living)
            animal = living;
        if(!(animal instanceof Mob))
            return false;
        Mob mob = (Mob)animal;
        if(fromSeat < 0 && fromSide == null)
            return false;

        if(draft) {
            if(to < 0 || to >= hitchSlots())
                return false;
            Side target = to == 0 ? Side.LEFT : Side.RIGHT;
            if(fromSide == target || !canPlaceDraft(mob, target))
                return false;
            if(mob.getVehicle() != null)
                mob.stopRiding();
            if(fromSide != null)
                setHorse(null, fromSide);
            hitchHorse(mob, target);
            if(player instanceof ServerPlayer serverPlayer)
                awardSinner(serverPlayer);
            return true;
        }

        if(fromSeat == to || !canPlaceAnimal(to, mob))
            return false;
        // Already aboard: only the seat record changes. startRiding would refuse a current passenger.
        if(mob.getVehicle() == this) {
            if(fromSide != null)
                setHorse(null, fromSide);
            if(fromSeat >= 0) {
                seatEntities[fromSeat] = null;
                seatIds[fromSeat] = -1;
            }
            seatEntities[to] = mob;
            seatIds[to] = mob.getId();
            syncSeats();
            return true;
        }
        if(fromSeat >= 0) {
            seatEntities[fromSeat] = null;
            seatIds[fromSeat] = -1;
        }
        // Vanilla startRiding fails for 60 ticks after a dismount. Force the mount, and put the animal back if it misses the seat.
        if(fromSide != null)
            setHorse(null, fromSide);
        pendingSeat = to;
        boolean seated = mob.startRiding(this, true);
        pendingSeat = -1;
        if(seated && seatIndexOf(mob) == to)
            return true;
        if(mob.getVehicle() != null)
            mob.stopRiding();
        if(fromSide != null)
            hitchHorse(mob, fromSide);
        else if(fromSeat >= 0) {
            seatEntities[fromSeat] = mob;
            seatIds[fromSeat] = mob.getId();
        }
        if(fromSeat >= 0)
            syncSeats();
        return false;
    }

    public boolean ejectDraft(Player player, Side side) {
        if(player.getVehicle() != this || getHorse(side) == null)
            return false;
        setHorse(null, side);
        return true;
    }

    public ItemStackHandler getWheels() {
        return wheelHandler;
    }

    public ItemStackHandler getRepair() {
        return repairHandler;
    }

    public int repairSlots() {
        int max = getMaxHealth();
        if(max <= 0 || Math.round(getHealth()) >= max)
            return 0;
        float percent = Math.round(getHealth()) / (float)max * 100F;
        if(percent >= 75F)
            return 1;
        if(percent >= 50F)
            return 2;
        if(percent >= 25F)
            return 3;
        return 4;
    }

    public Component conditionName() {
        String key = switch(repairSlots()) {
            case 0 -> "ok";
            case 1 -> "scratched";
            case 2 -> "damaged";
            case 3 -> "severe";
            default -> "critical";
        };
        return Component.translatable("gui.trotting_wagons.condition." + key);
    }

    public boolean acceptsRepair(int slot, ItemStack stack) {
        if(level() != null && level().getGameTime() < repairBlockedUntil)
            return false;
        return switch(repairNeed(slot)) {
            case "planks" -> stack.is(ItemTags.PLANKS);
            case "iron" -> stack.is(Items.IRON_INGOT);
            case "wool" -> stack.is(ItemTags.WOOL);
            case "leather" -> stack.is(Items.LEATHER);
            case "string" -> stack.is(Items.STRING);
            case "slime" -> stack.is(Items.SLIME_BALL);
            default -> false;
        };
    }

    public boolean isRepairMaterial(ItemStack stack) {
        return stack.is(ItemTags.PLANKS) || stack.is(Items.IRON_INGOT) || stack.is(ItemTags.WOOL)
                || stack.is(Items.LEATHER) || stack.is(Items.STRING) || stack.is(Items.SLIME_BALL);
    }

    public ItemStack repairIcon(int slot) {
        return switch(repairNeed(slot)) {
            case "planks" -> new ItemStack(Items.OAK_PLANKS);
            case "iron" -> new ItemStack(Items.IRON_INGOT);
            case "wool" -> new ItemStack(Items.WHITE_WOOL);
            case "leather" -> new ItemStack(Items.LEATHER);
            case "string" -> new ItemStack(Items.STRING);
            case "slime" -> new ItemStack(Items.SLIME_BALL);
            default -> ItemStack.EMPTY;
        };
    }

    public Component repairTooltip(int slot) {
        return switch(repairNeed(slot)) {
            case "planks" -> Component.translatable("gui.trotting_wagons.repair.planks");
            case "wool" -> Component.translatable("gui.trotting_wagons.repair.wool");
            default -> repairIcon(slot).getHoverName();
        };
    }

    private String repairNeed(int slot) {
        String raw = entityData.get(DATA_REPAIR);
        String[] parts = raw.split(",", -1);
        return slot >= 0 && slot < parts.length ? parts[slot] : "";
    }

    private String repairNeeds() {
        return String.join(",", repairNeed[0] == null ? "" : repairNeed[0], repairNeed[1] == null ? "" : repairNeed[1],
                repairNeed[2] == null ? "" : repairNeed[2], repairNeed[3] == null ? "" : repairNeed[3]);
    }

    private void readRepairNeeds(String raw) {
        String[] parts = raw.split(",", -1);
        for(int i = 0; i < repairNeed.length; i++)
            repairNeed[i] = i < parts.length && !parts[i].isEmpty() ? parts[i] : null;
        publishRepairNeeds();
    }

    private void publishRepairNeeds() {
        entityData.set(DATA_REPAIR, repairNeeds());
    }

    public void refreshRepairSlots() {
        if(level().isClientSide)
            return;
        int open = repairSlots();
        boolean changed = false;
        for(int i = 0; i < REPAIR_SLOTS; i++) {
            if(i >= open) {
                if(repairNeed[i] != null) {
                    repairNeed[i] = null;
                    changed = true;
                }
                if(!repair.get(i).isEmpty()) {
                    spawnAtLocation(repair.get(i));
                    setRepair(i, ItemStack.EMPTY);
                }
            } else if(repairNeed[i] == null) {
                repairNeed[i] = rollRepairNeed();
                changed = true;
            }
        }
        if(changed)
            publishRepairNeeds();
    }

    private String rollRepairNeed() {
        int roll = getRandom().nextInt(100);
        if(roll < 30)
            return "planks";
        if(roll < 60)
            return "iron";
        if(roll < 70)
            return "wool";
        if(roll < 80)
            return "leather";
        if(roll < 90)
            return "string";
        return "slime";
    }

    private void setRepair(int slot, ItemStack stack) {
        repairUpdating = true;
        repairHandler.setStackInSlot(slot, stack);
        repairUpdating = false;
    }

    private void tryFinishRepair() {
        if(level() == null || level().isClientSide)
            return;
        int open = repairSlots();
        if(open == 0)
            return;
        for(int i = 0; i < open; i++) {
            if(!acceptsRepair(i, repair.get(i)))
                return;
        }
        for(int i = 0; i < open; i++)
            setRepair(i, ItemStack.EMPTY);
        setHealth(Math.min(getMaxHealth(), getHealth() + 5F * open));
        level().playSound(null, getX(), getY(), getZ(), SoundEvents.ANVIL_USE, SoundSource.MASTER, 0.3F, 1);
        repairBlockedUntil = level().getGameTime() + 1L;
        for(int i = 0; i < REPAIR_SLOTS; i++)
            repairNeed[i] = null;
        refreshRepairSlots();
    }

    public int getWheelMask() {
        return entityData.get(DATA_WHEELS) & 0x0F;
    }

    public WheelKind getWheelKind(int index) {
        int raw = entityData.get(DATA_WHEEL_KINDS) & 0xFF;
        return WheelKind.BY_ID[(raw >> (index * 2)) & 3];
    }

    public boolean hasAllWheels() {
        return getWheelMask() == 0x0F;
    }

    private boolean hasWideWheel() {
        for(int i = 0; i < WHEEL_COUNT; i++) {
            if(getWheelKind(i) == WheelKind.WIDE)
                return true;
        }
        return false;
    }

    public WheelSet wheelSet() {
        Item same = null;
        boolean mismatch = false;
        for(int i = 0; i < wheels.size(); i++) {
            ItemStack stack = wheels.get(i);
            if(stack.isEmpty())
                return WheelSet.MISSING;
            if(same == null)
                same = stack.getItem();
            else if(stack.getItem() != same)
                mismatch = true;
        }
        if(mismatch)
            return WheelSet.INCOMPATIBLE;
        if(same == TWItems.WIDE_WHEEL.get())
            return WheelSet.WIDE;
        if(same == TWItems.REINFORCED_WHEEL.get())
            return WheelSet.REINFORCED;
        return WheelSet.REGULAR;
    }

    public WheelSag wheelSag() {
        boolean fl = wheelOn(0);
        boolean fr = wheelOn(1);
        boolean rl = wheelOn(2);
        boolean rr = wheelOn(3);
        int count = (fl ? 1 : 0) + (fr ? 1 : 0) + (rl ? 1 : 0) + (rr ? 1 : 0);
        if(count == 0)
            return new WheelSag(0, 0, WHEEL_SINK);
        if(count >= 3)
            return WheelSag.NONE;

        float half = WHEEL_SAG * 0.25F;
        float pitch = 0;
        float roll = 0;
        if(!fl && !fr && rl && rr)
            pitch = FRONT_SAG;
        else if(fl && fr && !rl && !rr)
            pitch = missingRearPitch();
        else if(!fl && fr && !rl && rr)
            roll = -WHEEL_SAG;
        else if(fl && !fr && rl && !rr)
            roll = WHEEL_SAG;
        else if(!fl && fr && rl && !rr) {
            pitch = diagonalPitch();
            roll = WHEEL_SAG;
        } else if(fl && !fr && !rl && rr) {
            pitch = diagonalPitch();
            roll = -WHEEL_SAG;
        } else if(fl) {
            pitch = -half;
            roll = half;
        } else if(fr) {
            pitch = -half;
            roll = -half;
        } else if(rl) {
            pitch = half;
            roll = half;
        } else {
            pitch = half;
            roll = -half;
        }

        double drop = sagAnchor(pitch, roll, fl, fr, rl, rr);
        drop += sagDropCorrection(count, pitch, roll, drop);
        return new WheelSag(pitch, roll, drop);
    }

    /**
     * Extra vertical shift after the remaining wheels are anchored. Positive lowers the body.
     */
    protected double sagDropCorrection(int count, float pitch, float roll, double anchored) {
        if(pitch == 0 && roll != 0)
            return 3D / 16D;
        if(count == 2 && pitch != 0 && roll != 0)
            return -12D / 16D;
        if(count == 1)
            return 2D / 16D;
        return 0;
    }

    protected float missingRearPitch() {
        return -WHEEL_SAG;
    }

    protected float diagonalPitch() {
        return -WHEEL_SAG;
    }

    @Override
    protected Vec3 adjustLocalOffset(Vec3 local) {
        WheelSag sag = wheelSag();
        local = rotateZ(local, sag.roll());
        local = rotateX(local, sag.pitch());
        return local.subtract(0, sag.drop(), 0);
    }

    private boolean wheelOn(int index) {
        return (getWheelMask() & (1 << index)) != 0;
    }

    private double sagAnchor(float pitch, float roll, boolean fl, boolean fr, boolean rl, boolean rr) {
        double max = Double.NEGATIVE_INFINITY;
        double width = getWheelWidth();
        double length = getWheelLength();
        if(fl)
            max = Math.max(max, sagCornerY(-width, length, pitch, roll));
        if(fr)
            max = Math.max(max, sagCornerY(width, length, pitch, roll));
        if(rl)
            max = Math.max(max, sagCornerY(-width, -length, pitch, roll));
        if(rr)
            max = Math.max(max, sagCornerY(width, -length, pitch, roll));
        return max == Double.NEGATIVE_INFINITY ? 0 : Math.max(0, max);
    }

    private double sagCornerY(double x, double z, float pitch, float roll) {
        Vec3 turned = rotateX(rotateZ(x, 0, z, roll), pitch);
        return turned.y;
    }

    public int getHitchEntityId(Side side) {
        return entityData.get(side == Side.LEFT ? DATA_HITCH_LEFT : DATA_HITCH_RIGHT);
    }

    private boolean hitchMatches(Side side, int animalId) {
        return animalId >= 0 && getHitchEntityId(side) == animalId;
    }

    private void updateWheelMask() {
        int mask = 0;
        int kinds = 0;
        for(int i = 0; i < wheels.size(); i++) {
            WheelKind kind = WheelKind.fromStack(wheels.get(i));
            if(kind != WheelKind.NONE)
                mask |= 1 << i;
            kinds |= kind.ordinal() << (i * 2);
        }
        entityData.set(DATA_WHEELS, (byte)mask);
        entityData.set(DATA_WHEEL_KINDS, (byte)kinds);
    }

    public boolean movePlayerToSeat(Player player, int seat) {
        if(player.getVehicle() != this)
            return false;
        if(seat < 0 || seat >= seats.length || !seats[seat].acceptsPlayers() || !seatAccepts(seat, player))
            return false;
        if(seatEntities[seat] != null && seatEntities[seat] != player)
            return false;

        int current = seatIndexOf(player);
        if(current < 0 || current == seat)
            return false;

        seatEntities[current] = null;
        seatIds[current] = -1;
        seatEntities[seat] = player;
        seatIds[seat] = player.getId();
        syncSeats();
        return true;
    }

    @Override
    public LivingEntity getControllingPassenger() {
        for(int i = 0; i < seats.length; i++) {
            if(seats[i].kind() != WagonSeat.Kind.DRIVER)
                continue;
            Entity driver = seatEntities[i];
            if(driver == null && seatIds[i] >= 0)
                driver = level().getEntity(seatIds[i]);
            return driver instanceof Player player ? player : null;
        }
        return null;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        if(!(passenger instanceof LivingEntity))
            return false;
        if(pendingSeat >= 0)
            return seatEntities[pendingSeat] == null && seatAccepts(pendingSeat, passenger);
        return findNearestSeat(passenger) >= 0;
    }

    @Override
    protected Vec3 getRiderOffset(Entity passenger) {
        int index = seatIndexOf(passenger);
        return index < 0 ? null : seats[index].position();
    }

    @Override
    protected void addPassenger(Entity passenger) {
        if(!level().isClientSide)
            assignSeat(passenger);
        super.addPassenger(passenger);
    }

    @Override
    protected void removePassenger(Entity passenger) {
        int seat = seatIndexOf(passenger);
        boolean behind = !level().isClientSide && passenger instanceof LivingEntity living && dismountsBehind(living, seat);
        super.removePassenger(passenger);
        clearSeat(passenger);
        if(behind)
            dismountBehindId = passenger.getId();
        if(!level().isClientSide) {
            if(passenger instanceof Mob)
                setBoardCooldown(passenger);
            syncSeats();
        }
    }

    public void applySeatState(int[] ids, boolean[] locks) {
        for(int i = 0; i < seatIds.length && i < ids.length; i++)
            seatIds[i] = ids[i];
        for(int i = 0; i < seatLocked.length && i < locks.length; i++)
            seatLocked[i] = locks[i];
    }

    private void assignSeat(Entity passenger) {
        int seat = -1;
        Integer saved = savedSeatByUuid.remove(passenger.getUUID());
        if(pendingSeat >= 0 && seatEntities[pendingSeat] == null && seatAccepts(pendingSeat, passenger))
            seat = pendingSeat;
        else if(saved != null && saved >= 0 && saved < seats.length && seatEntities[saved] == null && seatAccepts(saved, passenger))
            seat = saved;
        else
            seat = findNearestSeat(passenger);

        if(seat < 0)
            return;
        seatEntities[seat] = passenger;
        seatIds[seat] = passenger.getId();
        syncSeats();
    }

    private void clearSeat(Entity passenger) {
        for(int i = 0; i < seatEntities.length; i++) {
            if(seatEntities[i] == passenger) {
                seatEntities[i] = null;
                seatIds[i] = -1;
            }
        }
    }

    private int seatIndexOf(Entity passenger) {
        for(int i = 0; i < seatEntities.length; i++) {
            if(seatEntities[i] == passenger)
                return i;
        }
        int id = passenger.getId();
        for(int i = 0; i < seatIds.length; i++) {
            if(seatIds[i] == id)
                return i;
        }
        return -1;
    }

    private int findNearestSeat(Entity entity) {
        int best = -1;
        double bestDist = Double.MAX_VALUE;
        for(int i = 0; i < seats.length; i++) {
            if(seatEntities[i] != null)
                continue;
            if(!seatAccepts(i, entity))
                continue;
            double dist = entity.position().distanceToSqr(offsetToWorld(seats[i].position(), false));
            if(dist < bestDist) {
                bestDist = dist;
                best = i;
            }
        }
        return best;
    }

    private boolean seatAccepts(int index, Entity entity) {
        return seatAccepts(index, entity, false);
    }

    // manual is a drag from this wagon's hitch onto a seat. That unhitches first. Every other path refuses a harnessed animal.
    private boolean seatAccepts(int index, Entity entity, boolean manual) {
        if(entity.getType().is(TrottingWagons.CANNOT_MOUNT_WAGON))
            return false;
        boolean harnessedHere = harnesses(entity);
        if(!countsAsPlayer(entity) && (harnessedHere || isHarnessed(entity)) && !(manual && harnessedHere))
            return false;
        WagonSeat seat = seats[index];
        boolean person = countsAsPlayer(entity);
        if(seatLocked[index] && !person && seat.canLockAnimals())
            return false;
        if(!person && isLargePassenger(entity) && seat.kind() != WagonSeat.Kind.ANIMALBIG)
            return false;
        boolean accepted = switch(seat.kind()) {
            case DRIVER -> entity instanceof Player;
            case PLAYER -> person;
            case ANIMAL -> !person;
            case UNIVERSAL -> true;
            case ANIMALBIG -> !person && acceptsLargeSeat(entity);
            case DRAFT -> false;
        };
        return accepted && seatOpen(index, entity);
    }

    // Animal wagon large seats are large passengers only. Other wagons still accept any non-player there.
    protected boolean acceptsLargeSeat(Entity entity) {
        return true;
    }

    /**
     * Extra seat rule for a wagon. The animal being moved does not count as occupying its current seat.
     */
    protected boolean seatOpen(int index, Entity entity) {
        return true;
    }

    /** A villager or wandering trader takes a passenger seat, not an animal seat. Only a real player may drive. */
    public static boolean countsAsPlayer(Entity entity) {
        return entity instanceof Player || entity instanceof AbstractVillager;
    }

    protected static boolean isLargePassenger(Entity entity) {
        EntityType<?> type = entity.getType();
        return type == EntityType.HORSE || type == EntityType.MULE || type == EntityType.CAMEL || type == EntityType.COW || type.is(TrottingWagons.LARGE_WAGON_PASSENGER);
    }

    private void boardNearbyAnimals() {
        AABB box = getBoundingBox().inflate(ANIMAL_BOARD_RANGE);
        List<Mob> mobs = level().getEntitiesOfClass(Mob.class, box, this::canAutoBoard);
        mobs.sort(Comparator.comparingDouble(mob -> mob.distanceToSqr(this)));
        for(Mob mob : mobs)
            mountNearest(mob);
    }

    private boolean canAutoBoard(Mob mob) {
        if(!mob.isAlive() || mob.isPassenger() || mob.isVehicle() || harnesses(mob) || isHarnessed(mob) || isBoardCooldown(mob))
            return false;
        if(mob.getType().is(TrottingWagons.CANNOT_MOUNT_WAGON))
            return false;
        if(mob instanceof TamableAnimal tamable && tamable.getOwnerUUID() != null) {
            Entity owner = tamable.getOwner();
            if(owner == null || (owner.getVehicle() != this && owner.distanceToSqr(mob) > 9.0D))
                return false;
        }
        return findNearestSeat(mob) >= 0;
    }

    private boolean hasPullers() {
        if(hitchSlots() < 1 || getHorse(Side.LEFT) == null)
            return false;
        return hitchSlots() < 2 || getHorse(Side.RIGHT) != null;
    }

    /** Driver's left is +X when the wagon faces +Z. */
    protected Vec3 hitchOffset(Side side) {
        return new Vec3(side == Side.LEFT ? horseX : -horseX, 0, horseY);
    }

    public boolean hasDraftAnimal() {
        for(Side side : Side.values()) {
            if(getHitchEntityId(side) >= 0 || horseUuids[side.ordinal()] != null)
                return true;
        }
        return false;
    }

    private boolean hasFreeHitch() {
        if(hitchSlots() < 1)
            return false;
        if(getHorse(Side.LEFT) == null)
            return true;
        return hitchSlots() > 1 && getHorse(Side.RIGHT) == null;
    }

    private boolean isNearTongue(Entity entity) {
        return horizontalDistanceSqr(entity.position(), getTongueAnchor()) <= HITCH_RANGE * HITCH_RANGE;
    }

    private static double horizontalDistanceSqr(Vec3 a, Vec3 b) {
        double dx = a.x - b.x;
        double dz = a.z - b.z;
        return dx * dx + dz * dz;
    }

    private void refreshSeatIds() {
        for(int i = 0; i < seatEntities.length; i++)
            seatIds[i] = seatEntities[i] == null ? -1 : seatEntities[i].getId();
    }

    public void sendSeatSync(ServerPlayer player) {
        refreshSeatIds();
        PacketDistributor.sendToPlayer(player, seatPayload());
    }

    private SyncSeatsPayload seatPayload() {
        return new SyncSeatsPayload(getId(), seatIds.clone(), seatLocked.clone());
    }

    private void syncSeats() {
        if(level().isClientSide)
            return;
        refreshSeatIds();
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(this, seatPayload());
    }

    public enum Side {
        LEFT, RIGHT
    }

    public enum WheelKind {
        NONE,
        REGULAR,
        WIDE,
        REINFORCED;

        private static final WheelKind[] BY_ID = values();

        public static WheelKind fromStack(ItemStack stack) {
            if(stack.is(TWItems.WIDE_WHEEL.get()))
                return WIDE;
            if(stack.is(TWItems.REINFORCED_WHEEL.get()))
                return REINFORCED;
            if(stack.is(TWItems.WHEEL.get()))
                return REGULAR;
            return NONE;
        }
    }

    public enum WheelSet {
        REGULAR("gui.trotting_wagons.wheels.regular"),
        WIDE("gui.trotting_wagons.wheels.wide"),
        REINFORCED("gui.trotting_wagons.wheels.reinforced"),
        MISSING("gui.trotting_wagons.wheels.missing"),
        INCOMPATIBLE("gui.trotting_wagons.wheels.incompatible");

        private final String key;

        WheelSet(String key) {
            this.key = key;
        }

        public Component label() {
            return Component.translatable(key);
        }
    }

    public record WheelSag(float pitch, float roll, double drop) {
        public static final WheelSag NONE = new WheelSag(0, 0, 0);
    }

}
