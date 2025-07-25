package net.favouriteless.trotting_wagons.common.items;

import net.favouriteless.trotting_wagons.common.entities.base.AbstractWagon;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class HorseWhipItem extends Item {

    public HorseWhipItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if(player.getVehicle() instanceof AbstractWagon wagon) {
            if(!level.isClientSide)
                wagon.tryCycleSpeed(player);

            return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
        }

        return InteractionResultHolder.pass(player.getItemInHand(hand));
    }



}
