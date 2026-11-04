package net.favouriteless.trotting_wagons.common.items;

import net.favouriteless.trotting_wagons.common.entities.Chassis;
import net.favouriteless.trotting_wagons.common.init.TWEntityTypes;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class ChassisItem extends Item {

    public ChassisItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if(context.getClickedFace() != Direction.UP)
            return InteractionResult.PASS;
        if(context.getPlayer() == null)
            return InteractionResult.PASS;
        if(level.isClientSide)
            return InteractionResult.SUCCESS;

        Chassis chassis = TWEntityTypes.CHASSIS.get().create(level);
        chassis.setPos(Vec3.atBottomCenterOf(context.getClickedPos().above()).add(0, 0.05D, 0));
        float yaw = context.getPlayer().getYHeadRot();
        chassis.setYRot(yaw);
        chassis.yRotO = yaw;
        level.addFreshEntity(chassis);
        level.playSound(null, chassis.blockPosition(), SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
        context.getItemInHand().shrink(1);
        return InteractionResult.CONSUME;
    }

}
