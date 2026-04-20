package net.favouriteless.trotting_wagons.common.entities.wagons;

import net.favouriteless.trotting_wagons.ServerConfig;
import net.favouriteless.trotting_wagons.common.entities.base.AbstractInventoryWagon;
import net.favouriteless.trotting_wagons.common.init.TWItems;
import net.favouriteless.trotting_wagons.common.items.WagonItem;
import net.favouriteless.trotting_wagons.common.menus.CombatMenu;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CombatWagon extends AbstractInventoryWagon {

    private static final double RANGE = 1.5D;
    private static final double DAMAGE_MIN = 0;
    private static final double DAMAGE_MAX = 8.0F;

    private final Set<LivingEntity> alreadyInside = new HashSet<>(); // Keep track of entities so they don't get attacked multiple times

    public static final Vec3[] RIDERS = new Vec3[] {
            new Vec3(0, 1.25D, 0D),
            new Vec3(0D, 0D, 0D)
    };

    public CombatWagon(EntityType<CombatWagon> type, Level level) {
        super(type, level, 1, 1, RIDERS, ServerConfig.INSTANCE.combatSpeed.get(), ServerConfig.INSTANCE.combatAcceleration.get(),
                ServerConfig.INSTANCE.combatTurnRate.get(), ServerConfig.INSTANCE.combatHealth.get(), 0.6D, 3.6D,
                13, 6);
    }

    @Override
    public void tick() {
        super.tick();

        if(level().isClientSide)
            return;

        List<LivingEntity> targets = level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(RANGE), this::validTarget);

        for(LivingEntity target : targets) { // Hit and add new targets to the inside list
            if(!target.isAlive())
                continue;

            if(!alreadyInside.contains(target)) {
                int damage = (int)Math.round(Mth.lerp(getCurrentSpeed() / speed, DAMAGE_MIN, DAMAGE_MAX));
                if(damage == 0)
                    continue;


                LivingEntity controller = getControllingPassenger();
                if(controller == null)
                    continue;

                DamageSource source = controller instanceof Player p ? damageSources().playerAttack(p) : damageSources().mobAttack(controller);
                target.hurt(source, damage);
                alreadyInside.add(target);
            }
        }

        alreadyInside.removeIf(entity -> !targets.contains(entity)); // Remove entities which left the bounds
    }

    private boolean validTarget(LivingEntity target) {
        return target != getHorse(Side.LEFT) && target != getHorse(Side.RIGHT) && !getPassengers().contains(target);
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new CombatMenu(id, inventory, this);
    }

    @Override
    public ItemStack getPickResult() {
        return WagonItem.setupNbt(new ItemStack(TWItems.COMBAT_WAGON.get()), getColor());
    }

}
