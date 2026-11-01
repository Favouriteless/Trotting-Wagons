package net.favouriteless.trotting_wagons.mixin;

import net.favouriteless.trotting_wagons.common.entities.base.AbstractWagon;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** A hitched horse is treated like a ridden one: vanilla never starts the graze. */
@Mixin(AbstractHorse.class)
abstract class AbstractHorseGrazeMixin {

    @Inject(method = "canEatGrass", at = @At("HEAD"), cancellable = true)
    private void trottingWagons$harnessedDoesNotGraze(CallbackInfoReturnable<Boolean> callback) {
        if(AbstractWagon.isHarnessMarked(((AbstractHorse)(Object)this).getUUID()))
            callback.setReturnValue(false);
    }

}
