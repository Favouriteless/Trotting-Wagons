package net.favouriteless.trotting_wagons.common.items;

import net.favouriteless.trotting_wagons.common.entities.base.AbstractWagon;
import net.favouriteless.trotting_wagons.common.init.TWSoundEvents;
import net.minecraft.sounds.SoundSource;
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

            level.playSound(player, player.getX(), player.getY(), player.getZ(), TWSoundEvents.WHIP.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
            return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
        }

        return InteractionResultHolder.pass(player.getItemInHand(hand));
    }



}
