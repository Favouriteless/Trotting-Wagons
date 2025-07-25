package net.favouriteless.trotting_wagons.common;

import net.favouriteless.trotting_wagons.TrottingWagons;
import net.favouriteless.trotting_wagons.common.entities.base.AbstractWagon;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

@EventBusSubscriber(modid = TrottingWagons.MOD_ID, bus = Bus.FORGE)
public class CommonEvents {

    @SubscribeEvent
    public static void onEntityInteract(final PlayerInteractEvent.EntityInteract event) {
        Level level = event.getLevel();
        Player player = event.getEntity();
        Entity target = event.getTarget();

        if(!player.isCrouching())
            return;

        if(target.getVehicle() instanceof AbstractWagon) {
            if(!level.isClientSide)
                target.stopRiding();
            event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide));
            event.setCanceled(true);
        }
        else if(target instanceof LivingEntity && !target.getType().is(TrottingWagons.CANNOT_MOUNT_WAGON)) {
            if(target instanceof TamableAnimal tamable && !tamable.getOwnerUUID().equals(player.getUUID()))
                return; // Prevent players from kidnapping each other's tamed mobs.

            level.getEntitiesOfClass(AbstractWagon.class, target.getBoundingBox().inflate(3)).stream()
                    .filter(w -> w.getDeltaMovement().lengthSqr() < 0.002D)
                    .findAny()
                    .ifPresent(wagon -> {
                        if(!level.isClientSide)
                            target.startRiding(wagon);
                        event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide));
                        event.setCanceled(true);
                    });
        }
    }

}
