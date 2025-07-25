package net.favouriteless.trotting_wagons.common.entities.base;

import net.favouriteless.trotting_wagons.client.TWClient;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager.ControllerRegistrar;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

public abstract class AbstractGeckolibVehicle extends Entity implements GeoEntity {

    private static final RawAnimation ANIM_MOVE = RawAnimation.begin().thenLoop("move.forwards");

    private final AnimatableInstanceCache animCache = GeckoLibUtil.createInstanceCache(this);

    private final double wheelWidth;
    private final double wheelLength;

    private final Vec3[] riders;

    private int lerpSteps = 0;
    private double lerpX = 0;
    private double lerpY = 0;
    private double lerpZ = 0;
    private double lerpXRot = 0;
    private double lerpYRot = 0;

    private boolean isReversing = false;
    private float wheelRot = 0; // Used for animating the wheels
    private float wheelRotO = 0;

    private float leftImpulse = 0; // Steering input

    public AbstractGeckolibVehicle(EntityType<?> type, Level level, double wheelWidth, double wheelLength, Vec3[] riders) {
        super(type, level);
        this.wheelWidth = wheelWidth;
        this.wheelLength = wheelLength;
        this.riders = riders;
    }

    public abstract void moveTick();

    @Override
    public void tick() {
        super.tick();
        tickLerp();

        if(level().isClientSide && getControllingPassenger() == Minecraft.getInstance().player)
            TWClient.controlVehicle(this);

        if(isControlledByLocalInstance()) {
            if(getControllingPassenger() == null)
                leftImpulse = 0;
            moveTick();
        }

        if(level().isClientSide) {
            wheelRotO = wheelRot;
            wheelRot += (float)getDeltaMovement().horizontalDistance() * (isReversing ? -2F : 2F);
        }
    }

    private void tickLerp() {
        if(isControlledByLocalInstance()) {
            lerpSteps = 0;
            syncPacketPositionCodec(getX(), getY(), getZ());
        }

        if(lerpSteps > 0) {
            double f = 1.0D / lerpSteps;
            double x = Mth.lerp(f, getX(), lerpX);
            double y = Mth.lerp(f, getY(), lerpY);
            double z = Mth.lerp(f, getZ(), lerpZ);

            float xRot = (float)Mth.lerp(f, getXRot(), lerpXRot);
            float yRot = (float)Mth.wrapDegrees(lerpYRot - getYRot()) / lerpSteps + getYRot(); // Doing a normal lerp here causes "snapping" at -180/180

            --lerpSteps;
            setPos(x, y, z);
            setRot(yRot, xRot);
        }
    }

    /**
     * @return The tilt of the vehicle (in degrees) based on rays cast under each wheel.
     */
    protected float calculateTargetXRot() {
        Level level = level();
        Vec3 pos = position();

        final double rayLength = wheelWidth*2;
        final float yaw = getYRot();

        Vec3 fl = rotateY(-wheelWidth, 0, wheelLength, yaw).add(pos); // World-space positions of each corner of the bounding box, rotated.
        Vec3 fr = rotateY(wheelWidth, 0, wheelLength, yaw).add(pos);
        Vec3 bl = rotateY(-wheelWidth, 0, -wheelLength, yaw).add(pos);
        Vec3 br = rotateY(wheelWidth, 0, -wheelLength, yaw).add(pos);

        // Cast a ray down from each corner to figure out where we are height-wise. Max length of wheelLength makes a max angle of 45 degrees.
        BlockHitResult flr = level.clip(new ClipContext(fl, fl.subtract(0, rayLength, 0), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        BlockHitResult frr = level.clip(new ClipContext(fr, fr.subtract(0, rayLength, 0), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        BlockHitResult blr = level.clip(new ClipContext(bl, bl.subtract(0, rayLength, 0), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        BlockHitResult brr = level.clip(new ClipContext(br, br.subtract(0, rayLength, 0), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));

        double fMax = Math.max(flr.getLocation().y - pos.y, frr.getLocation().y - pos.y);
        double bMax = Math.max(blr.getLocation().y - pos.y, brr.getLocation().y - pos.y);

        double xDiff = fMax - bMax;
        float f = (float) Mth.clamp(xDiff / rayLength, -1, 1);

        return f * -45;
    }

    @Override
    protected void positionRider(Entity passenger, MoveFunction callback) {
        Vec3 local = riders[getPassengerIndex(passenger)];

        Vec3 mid = new Vec3(0, 0, getXRot() < 0 ? wheelLength : -wheelLength); // Pivot point is different for positive vs negative rotation.

        local = local.subtract(mid);
        local = rotateX(local, getXRot());
        local = local.add(mid);

        local = rotateY(local, getYRot());
        local = local.subtract(0, 0.7D, 0); // Offset player positions because the sit anim makes them float.

        callback.accept(passenger, getX() + local.x, getY() + local.y, getZ() + local.z);
    }

    protected Vec3 rotateY(Vec3 pos, float angle) {
        return rotateY(pos.x, pos.y, pos.z, angle);
    }

    protected Vec3 rotateX(Vec3 pos, float angle) {
        return rotateX(pos.x, pos.y, pos.z, angle);
    }

    protected Vec3 rotateY(double x, double y, double z, float angle) {
        float a = angle * Mth.DEG_TO_RAD;
        float sin = Mth.sin(a);
        float cos = Mth.cos(a);

        return new Vec3(x * cos - z * sin, y, z * cos + x * sin);
    }

    protected Vec3 rotateX(double x, double y, double z, float angle) {
        float a = angle * Mth.DEG_TO_RAD;
        float sin = Mth.sin(a);
        float cos = Mth.cos(a);

        return new Vec3(x, y * cos - z * sin, z * cos + y * sin);
    }

    private int getPassengerIndex(Entity passenger) {
        int i = 0;
        for(Entity ent : getPassengers()) {
            if(ent == passenger)
                break;
            i++;
        }
        return i;
    }

    public float getXRot(float partialTick) {
        return Mth.lerp(partialTick, xRotO, getXRot());
    }

    public float getWheelRot(float partial) {
        return Mth.lerp(partial, wheelRotO, wheelRot);
    }

    protected float getSteeringImpulse() {
        return leftImpulse;
    }

    public void setSteeringImpulse(float impulse) {
        this.leftImpulse = impulse;
    }

    public double getWheelWidth() {
        return wheelWidth;
    }

    public double getWheelLength() {
        return wheelLength;
    }

    protected boolean isReversing() {
        return isReversing;
    }

    public void toggleReversing() {
        this.isReversing = !isReversing;
    }

    @Override
    public LivingEntity getControllingPassenger() {
        return !getPassengers().isEmpty() ? (LivingEntity)getPassengers().get(0) : null;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return passenger instanceof LivingEntity && getPassengers().size() < riders.length;
    }

    @Override
    public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps, boolean teleport) {
        lerpX = x;
        lerpY = y;
        lerpZ = z;
        lerpYRot = yRot;
        lerpXRot = xRot;
        lerpSteps = steps;
    }

    @Override
    public boolean canBeCollidedWith() {
        return true;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {}

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public float getStepHeight() {
        return 1.0F;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animCache;
    }

    @Override
    public void registerControllers(ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, vehicle -> {
            if(getDeltaMovement().horizontalDistance() > 0.01F)
                return vehicle.setAndContinue(ANIM_MOVE);
            return PlayState.STOP;
        }));
    }

}