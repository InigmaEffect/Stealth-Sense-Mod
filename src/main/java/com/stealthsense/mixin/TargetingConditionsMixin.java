package com.stealthsense.mixin;

import com.stealthsense.logic.DetectionEngine;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TargetingConditions.class)
public class TargetingConditionsMixin {

    @Inject(method = "test", at = @At("RETURN"), cancellable = true)
    private void stealthsense$filterTargetDetection(ServerLevel level, LivingEntity observer, LivingEntity target, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue()) return;
        if (observer instanceof Mob mob && target != null) {
            if (!DetectionEngine.canDetect(mob, target)) {
                cir.setReturnValue(false);
            }
        }
    }
}
