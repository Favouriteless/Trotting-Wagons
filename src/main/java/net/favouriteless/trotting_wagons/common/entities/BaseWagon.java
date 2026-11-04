package net.favouriteless.trotting_wagons.common.entities;

import com.kltyton.bonehitboxlib.api.bone.collision.ObbCollisionMode;
import com.kltyton.bonehitboxlib.api.geckolib.entity.GeoBoneHitboxEntity;
import com.kltyton.bonehitboxlib.api.registration.registrar.ObbBoneRegistrar;
import net.favouriteless.trotting_wagons.common.init.TWItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager.ControllerRegistrar;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * The finished wagon base. Walking stops on the oriented hitbox bone.
 * The vanilla box only lets an axe find it.
 */
public class BaseWagon extends Entity implements GeoEntity, GeoBoneHitboxEntity {

    /** Hitbox bone, model pixels converted with (-x / 16, y / 16, -z / 16). */
    private static final double MIN_X = -22D / 16D;
    private static final double MAX_X = 22D / 16D;
    private static final double MIN_Y = 4D / 16D;
    private static final double MAX_Y = 21D / 16D;
    private static final double MIN_Z = -40D / 16D;
    private static final double MAX_Z = 47D / 16D;

    private final AnimatableInstanceCache animCache = GeckoLibUtil.createInstanceCache(this);
    private final WagonBuild build = new WagonBuild();

    public BaseWagon(EntityType<? extends BaseWagon> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    public void takeBuild(WagonBuild source) {
        build.copyFrom(source);
    }

    /** The body shell. The enclosing vanilla box stays non-solid so the player meets these cubes. */
    @Override
    public void bonehitboxlib$registerObbBones(ObbBoneRegistrar registrar) {
        registrar.register(GeoBoneHitboxEntity.BONEHITBOXLIB_GEO_SOURCE, "hitbox")
                .collision(ObbCollisionMode.HARD);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {}

    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {
        build.load(compound, registryAccess());
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compound) {
        build.save(compound, registryAccess());
    }

    @Override
    public void tick() {
        super.tick();
        setBoundingBox(makeBoundingBox());
    }

    @Override
    protected AABB makeBoundingBox() {
        float yaw = getYRot();
        double left = Double.POSITIVE_INFINITY;
        double bottom = Double.POSITIVE_INFINITY;
        double front = Double.POSITIVE_INFINITY;
        double right = Double.NEGATIVE_INFINITY;
        double top = Double.NEGATIVE_INFINITY;
        double back = Double.NEGATIVE_INFINITY;
        double[] xs = {MIN_X, MAX_X};
        double[] ys = {MIN_Y, MAX_Y};
        double[] zs = {MIN_Z, MAX_Z};
        for(double x : xs) {
            for(double y : ys) {
                for(double z : zs) {
                    Vec3 turned = rotateY(x, y, z, yaw);
                    left = Math.min(left, turned.x);
                    bottom = Math.min(bottom, turned.y);
                    front = Math.min(front, turned.z);
                    right = Math.max(right, turned.x);
                    top = Math.max(top, turned.y);
                    back = Math.max(back, turned.z);
                }
            }
        }
        return new AABB(getX() + left, getY() + bottom, getZ() + front, getX() + right, getY() + top, getZ() + back);
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if(isInvulnerableTo(source))
            return false;
        if(!(source.getEntity() instanceof Player player) || !player.getMainHandItem().is(ItemTags.AXES))
            return false;
        if(level().isClientSide)
            return true;
        if(!player.getAbilities().instabuild && level().getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS)) {
            spawnAtLocation(TWItems.CHASSIS.get());
            build.drop(this);
        }
        playSound(SoundEvents.WOOD_BREAK, 1.0F, 1.0F);
        discard();
        return true;
    }

    @Override
    public void registerControllers(ControllerRegistrar controllers) {}

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animCache;
    }

    private static Vec3 rotateY(double x, double y, double z, float angle) {
        float a = angle * Mth.DEG_TO_RAD;
        float sin = Mth.sin(a);
        float cos = Mth.cos(a);
        return new Vec3(x * cos - z * sin, y, z * cos + x * sin);
    }

}
