package net.favouriteless.trotting_wagons.common.util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;

public class LevelUtils {

    public static boolean isEmpty(Level level, AABB aabb) {
        return BlockPos.betweenClosedStream(aabb).noneMatch(pos -> {
            BlockState state = level.getBlockState(pos);
            return !state.isAir() && state.isSuffocating(level, pos) &&
                    Shapes.joinIsNotEmpty(state.getCollisionShape(level, pos).move(pos.getX(), pos.getY(), pos.getZ()), Shapes.create(aabb), BooleanOp.AND);
        });
    }

}
