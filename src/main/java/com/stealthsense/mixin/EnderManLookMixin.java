package com.stealthsense.mixin;

import com.stealthsense.config.MobDetectionProfile;
import com.stealthsense.config.StealthConfig;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EnderMan.class)
public class EnderManLookMixin {

    @Inject(method = "isLookingAtMe", at = @At("HEAD"), cancellable = true)
    private void stealthsense$longRangeEndermanGaze(Player player, CallbackInfoReturnable<Boolean> cir) {
        if (!StealthConfig.INSTANCE.modEnabled) return;
        EnderMan self = (EnderMan) (Object) this;
        MobDetectionProfile profile = StealthConfig.getProfile(self);

        Vec3 playerLook = player.getViewVector(1.0F).normalize();
        Vec3 toEnderman = new Vec3(
                self.getX() - player.getX(),
                self.getEyeY() - player.getEyeY(),
                self.getZ() - player.getZ()
        );
        double dist = toEnderman.length();
        if (dist > profile.maxRange) return;

        Vec3 dir = toEnderman.normalize();
        double dot = playerLook.dot(dir);
        double threshold = 1.0 - (0.035 / Math.max(1.0, dist / 24.0));
        if (dot > threshold && self.hasLineOfSight(player)) {
            cir.setReturnValue(true);
        }
    }
}
