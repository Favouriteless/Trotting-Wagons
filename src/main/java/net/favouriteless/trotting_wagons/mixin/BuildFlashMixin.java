package net.favouriteless.trotting_wagons.mixin;

import com.kltyton.bonehitboxlib.client.selection.service.BonePartSelectionLogic;
import net.favouriteless.trotting_wagons.common.entities.BaseWagon;
import net.minecraft.client.Minecraft;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BonePartSelectionLogic.class)
public class BuildFlashMixin {

    @Inject(method = "recordAttack", at = @At("HEAD"), cancellable = true, remap = false)
    private static void trottingWagons$noRedWithoutAxe(Entity target, CallbackInfo callback) {
        Minecraft minecraft = Minecraft.getInstance();
        if(target instanceof BaseWagon && minecraft.player != null && !minecraft.player.getMainHandItem().is(ItemTags.AXES))
            callback.cancel();
    }

}
