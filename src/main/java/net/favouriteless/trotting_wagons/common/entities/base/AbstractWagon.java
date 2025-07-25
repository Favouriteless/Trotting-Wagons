package net.favouriteless.trotting_wagons.common.entities.base;

import net.favouriteless.trotting_wagons.ServerConfig;
import net.favouriteless.trotting_wagons.TrottingWagons;
import net.favouriteless.trotting_wagons.client.TWClient;
import net.favouriteless.trotting_wagons.common.init.TWItems;
import net.favouriteless.trotting_wagons.common.util.LevelUtils;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.OptionalInt;
import java.util.Random;
import java.util.UUID;

public abstract class AbstractWagon extends AbstractGeckolibVehicle {

    private static final Random RANDOM = new Random();
    private static final EntityDataAccessor<Float> DATA_ID_HEALTH = SynchedEntityData.defineId(AbstractWagon.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<OptionalInt> DATA_ID_COLOR = SynchedEntityData.defineId(AbstractWagon.class, EntityDataSerializers.OPTIONAL_UNSIGNED_INT);

    public static final double GRAVITY = 0.04D;
    public static final int SPEED_LEVELS = 3;
    public static final double HORIZONTAL_TETHER = 0.5D * 0.5D; // Use sqr for performance

    public final double speed;
    private final double acceleration;
    private final float turnRate;
    private final int maxHealth;

    private final double horseX;
    private final double horseY;
    private final float shaftAngle;

    public UUID owner;
    private int speedLevel = 0;
    private double currentSpeed;

    private final Horse[] horses = new Horse[2];
    private final UUID[] horseUuids = new UUID[2];

    public AbstractWagon(EntityType<? extends AbstractWagon> type, Level level, double wheelWidth, double wheelLength, Vec3[] riders,
                         double speed, double acceleration, double turnRate, int maxHealth, double horseX, double horseY, float shaftAngle) {
        super(type, level, wheelWidth, wheelLength, riders);

        this.speed = speed;
        this.acceleration = acceleration;
        this.turnRate = (float)turnRate;
        this.maxHealth = maxHealth;
        this.horseX = horseX;
        this.horseY = horseY;
        this.shaftAngle = shaftAngle;

        setHealth(maxHealth);
    }

    @Override
    public void tick() {
        if(firstTick && level().isClientSide)
            TWClient.createWagonSound(this);

        if(!level().isClientSide)
            validateHorses();
        super.tick();
    }

    private void validateHorses() {
        if(firstTick || level().getGameTime() % 100 == 0) { // Repeated checks help resolve chunk loading issues.
            Horse horse = tryGetHorse(Side.LEFT);
            if(horse != null)
                setHorse(horse, Side.LEFT);

            horse = tryGetHorse(Side.RIGHT);
            if(horse != null)
                setHorse(horse, Side.RIGHT);
        }

        if(!isHorseInRange(Side.LEFT))
            setHorse(null, Side.LEFT);
        if(!isHorseInRange(Side.LEFT))
            setHorse(null, Side.RIGHT);
    }

    private Horse tryGetHorse(Side side) {
        final Horse horse = getHorse(side);
        final UUID uuid = getHorseUUID(side);

        if(uuid == null || horse != null)
            return null;

        return level().getEntitiesOfClass(Horse.class, getBoundingBox().inflate(5))
                .stream()
                .filter(h -> h.getUUID().equals(uuid))
                .findFirst()
                .orElse(null);
    }

    private boolean isHorseInRange(Side side) {
        final Horse horse = getHorse(side);
        if(horse == null)
            return false;

        Vec3 displacement = horse.position().subtract(getHorsePos(side));
        return horse.isAlive() && displacement.horizontalDistanceSqr() < HORIZONTAL_TETHER;
    }

    @Override
    public void moveTick() {
        final Horse leftHorse = getHorse(Side.LEFT);
        final Horse rightHorse = getHorse(Side.RIGHT);

        if(!isVehicle() || !(this.getControllingPassenger() instanceof Player) || leftHorse == null || rightHorse == null) {
            speedLevel = 0;
            setSteeringImpulse(0);
        }

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

        if(leftHorse != null) {
            leftHorse.setDeltaMovement(move.add(0, leftHorse.getDeltaMovement().y, 0));
            leftHorse.move(MoverType.SELF, leftHorse.getDeltaMovement());
        }
        if(rightHorse != null) {
            rightHorse.setDeltaMovement(move.add(0, rightHorse.getDeltaMovement().y, 0));
            rightHorse.move(MoverType.SELF, rightHorse.getDeltaMovement());
        }

        setDeltaMovement(move.add(0, getDeltaMovement().y, 0));
        move(MoverType.SELF, getDeltaMovement());
        setXRot(rotlerp(getXRot(), calculateTargetXRot(), (float)(9 * currentSpeed / (SPEED_LEVELS * currentSpeed)))); // 7 is just a magic scaling number for smoothing.
    }

    private void handleSteering() {
        float speedPercent = (float)(currentSpeed / (SPEED_LEVELS * speed));
        float steer = -getSteeringImpulse() * speedPercent * turnRate;

        if(steer != 0) {
            final Horse leftHorse = getHorse(Side.LEFT);
            final Horse rightHorse = getHorse(Side.RIGHT);

            Vec3 leftDisplace = collideSteering(Side.LEFT, steer);
            Vec3 rightDisplace = collideSteering(Side.RIGHT, steer);

            // If either horse is going to hit a block by steering, don't allow it.
            if(!leftDisplace.equals(Vec3.ZERO) && !rightDisplace.equals(Vec3.ZERO)) {
                setYRot(Mth.wrapDegrees(getYRot() + steer));

                float yRot = getYRot();

                setHorsePos(leftHorse, leftHorse.position().add(leftDisplace), yRot);
                setHorsePos(rightHorse, rightHorse.position().add(rightDisplace), yRot);
            }
        }
    }

    private void handleAcceleration() {
        double targetSpeed = speed * speedLevel;
        if(isReversing())
            targetSpeed *= -0.25D;

        double accel = speed / (acceleration * 20.0D);
        double speedDiff = targetSpeed - currentSpeed;

        currentSpeed += Mth.sign(speedDiff) * Math.min(Math.abs(accel), Math.abs(speedDiff));
    }

    private void setHorsePos(Horse horse, Vec3 pos, float rot) {
        if(horse == null)
            return;
        horse.setYRot(rot);
        horse.setYBodyRot(rot);
        horse.setYHeadRot(rot);
        horse.setPos(pos);
    }

    private Vec3 collideSteering(Side side, float angle) {
        Horse horse = getHorse(side);
        if(horse == null)
            return Vec3.ZERO;

        Vec3 desired = rotateY(new Vec3(side == Side.LEFT ? -horseX : horseX, 0, horseY), getYRot() + angle); // Local post-rotation position
        Vec3 local = horse.position().subtract(position()); // Local starting position

        Vec3 displacement = desired.subtract(local);
        Vec3 collidedDisplacement = horse.collide(displacement);

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

            if(getHealth() < maxHealth && stack.is(ForgeRegistries.ITEMS.getValue(new ResourceLocation(ServerConfig.INSTANCE.repairItem.get())))) {
                if(!isClientSide) {
                    stack.shrink(1);
                    setHealth(getHealth() + ServerConfig.INSTANCE.repairPerItem.get());
                    level().playSound(null, getX(), getY(), getZ(), SoundEvents.ANVIL_USE, SoundSource.MASTER, 0.3F, 1);
                }
                return InteractionResult.sidedSuccess(isClientSide);
            }

            if(stack.is(ItemTags.AXES)) {
                if(!isClientSide && player.getUUID().equals(owner))
                    onDestroyed(true);
                return InteractionResult.sidedSuccess(isClientSide);
            }

            if(stack.is(Items.LEAD)) {
                if(isClientSide)
                    return InteractionResult.SUCCESS;

                for(Entity passenger : getPassengers()) {
                    if(passenger instanceof Mob mob) {
                        mob.stopRiding();
                        player.getItemInHand(hand).shrink(1);
                        if(mob.canBeLeashed(player))
                            mob.setLeashedTo(player, true);
                        break;
                    }
                }
                return InteractionResult.CONSUME;
            }

            if(TrottingWagons.DYE_ITEMS.containsKey(item)) {
                setColor(TrottingWagons.DYE_ITEMS.get(item));
                return InteractionResult.sidedSuccess(isClientSide);
            }

            if(tryHitchHorse(player) || tryMountMob(player))
                return InteractionResult.sidedSuccess(isClientSide);
        }
        else if(player.getVehicle() != this) {
            if(!isClientSide) {
                player.setYRot(getYRot());
                player.setXRot(getXRot());

                return player.startRiding(this) ? InteractionResult.CONSUME : InteractionResult.PASS;
            }
            return InteractionResult.sidedSuccess(isClientSide);
        }
        return InteractionResult.PASS;
    }

    private boolean tryHitchHorse(Player player) {
        Horse horse = level().getEntitiesOfClass(Horse.class, new AABB(
                player.getX()-7, player.getY()-7, player.getZ()-7,
                player.getX()+7, player.getY()+7, player.getZ()+7
        ), h -> h.getLeashHolder() == player).stream()
                .findFirst().orElse(null);

        if(!level().isClientSide && horse != null) {
            if(getHorse(Side.LEFT) == null)
                hitchHorse(horse, Side.LEFT);
            else if(getHorse(Side.RIGHT) == null)
                hitchHorse(horse, Side.RIGHT);

            horse.dropLeash(true, true);
        }
        return horse != null;
    }

    private boolean tryMountMob(Player player) {
        Mob mob = level().getEntitiesOfClass(Mob.class, new AABB(
                player.getX()-7, player.getY()-7, player.getZ()-7,
                player.getX()+7, player.getY()+7, player.getZ()+7
                ), h -> h.getLeashHolder() == player && !h.getType().is(TrottingWagons.CANNOT_MOUNT_WAGON)).stream()
                .findFirst().orElse(null);

        if(mob != null && !level().isClientSide && canAddPassenger(mob))
            mob.startRiding(this);

        return mob != null;
    }

    private void hitchHorse(Horse horse, Side side) {
        setHorse(horse, side);
        initHorse(horse, side);
    }

    private void initHorse(Horse horse, Side side) {
        if(horse == null)
            return;
        horse.ejectPassengers();
        horse.setDeltaMovement(Vec3.ZERO);
        setHorsePos(horse, getHorsePos(side), getYRot());
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
        gameEvent(GameEvent.ENTITY_DAMAGE, source.getEntity());

        if(getHealth() <= 0)
            onDestroyed(false);

        return true;
    }

    protected void onDestroyed(boolean dropItem) {
        if(level().isClientSide)
            return;

        Horse horse = getHorse(Side.LEFT);
        if(horse != null)
            horse.setNoAi(false);

        horse = getHorse(Side.RIGHT);
        if(horse != null)
            horse.setNoAi(false);

        ejectPassengers();
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

    @Override
    protected void defineSynchedData() {
        DyeColor color = getDefaultColor();
        entityData.define(DATA_ID_COLOR, color != null ? OptionalInt.of(color.getId()) : OptionalInt.empty());
        entityData.define(DATA_ID_HEALTH, (float)0);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag nbt) {
        if(nbt.contains("color", 99))
            setColor(DyeColor.byId(nbt.getInt("color")));
        if(nbt.contains("owner"))
            owner = nbt.getUUID("owner");
        if(nbt.contains("leftHorse"))
            setHorseUUID(nbt.getUUID("leftHorse"), Side.LEFT);
        if(nbt.contains("rightHorse"))
            setHorseUUID(nbt.getUUID("rightHorse"), Side.RIGHT);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag nbt) {
        if(getColor() != null)
            nbt.putByte("color", (byte)getColor().getId());
        if(owner != null)
            nbt.putUUID("owner", owner);
        if(getHorseUUID(Side.LEFT) != null)
            nbt.putUUID("leftHorse", getHorseUUID(Side.LEFT));
        if(getHorseUUID(Side.RIGHT) != null)
            nbt.putUUID("rightHorse", getHorseUUID(Side.RIGHT));
    }

    public void tryCycleSpeed(Player player) {
        if(player == getControllingPassenger())
            speedLevel = (speedLevel + 1) % SPEED_LEVELS;
    }

    private Horse getHorse(Side side) {
        return horses[side.ordinal()];
    }

    private void setHorse(Horse horse, Side side) {
        Horse old = getHorse(side);
        if(old != null)
            old.setNoAi(false);
        if(horse != null)
            horse.setNoAi(true);

        horses[side.ordinal()] = horse;
        horseUuids[side.ordinal()] = horse != null ? horse.getUUID() : null;
    }

    private UUID getHorseUUID(Side side) {
        return horseUuids[side.ordinal()];
    }

    private void setHorseUUID(UUID uuid, Side side) {
        horseUuids[side.ordinal()] = uuid;
    }

    private Vec3 getHorsePos(Side side) {
        return position().add(rotateY(new Vec3(side == Side.LEFT ? -horseX : horseX, 0, horseY), getYRot()));
    }

    public float getShaftAngle() {
        return shaftAngle;
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

    public float getHealth() {
        return entityData.get(DATA_ID_HEALTH);
    }

    protected void setHealth(float health) {
        entityData.set(DATA_ID_HEALTH, Mth.clamp(health, 0, maxHealth));
    }

    public double getCurrentSpeed() {
        return currentSpeed;
    }

    @Override
    public boolean isControlledByLocalInstance() {
        return isEffectiveAi();
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, DamageSource source) {
        return !getType().is(EntityTypeTags.FALL_DAMAGE_IMMUNE);
    }

    private enum Side {
        LEFT, RIGHT
    }

}
