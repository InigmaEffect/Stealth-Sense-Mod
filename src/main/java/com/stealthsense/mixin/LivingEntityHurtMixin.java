package com.stealthsense.mixin;

import com.stealthsense.logic.DetectionEngine;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class LivingEntityHurtMixin {

    @Inject(method = "hurtServer", at = @At("TAIL"))
    private void stealthsense$onMobHurt(ServerLevel level, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue()) return;
        if ((Object) this instanceof Mob mob) {
            DetectionEngine.onMobDamaged(mob, source.getEntity(), source.getDirectEntity());
        }
    }
}
