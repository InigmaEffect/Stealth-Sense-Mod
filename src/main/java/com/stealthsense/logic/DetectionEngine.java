package com.stealthsense.logic;

import com.stealthsense.config.MobDetectionProfile;
import com.stealthsense.config.StealthConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

public class DetectionEngine {
    private static final Map<UUID, MobAlertState> STATES = new ConcurrentHashMap<>();
    private static final Map<UUID, Vec3> PREV_PLAYER_POS = new WeakHashMap<>();
    private static final Map<UUID, Double> PLAYER_SPEED = new WeakHashMap<>();

    public static MobAlertState getState(Mob mob) {
        return STATES.computeIfAbsent(mob.getUUID(), id -> new MobAlertState());
    }

    public static void tickWorld(ServerLevel level) {
        if (!StealthConfig.INSTANCE.modEnabled) return;

        for (ServerPlayer player : level.players()) {
            Vec3 current = player.position();
            Vec3 prev = PREV_PLAYER_POS.put(player.getUUID(), current);
            double speed = (prev != null) ? current.distanceTo(prev) : 0.0;
            PLAYER_SPEED.put(player.getUUID(), speed);
        }

        for (Entity entity : level.getAllEntities()) {
            if (!(entity instanceof Mob mob) || !mob.isAlive()) continue;
            if (mob instanceof Warden) continue;

            MobAlertState state = STATES.get(mob.getUUID());
            if (state == null) continue;

            if (state.alertTicks > 0) {
                state.alertTicks--;
            } else if (state.suspicion > 0.0) {
                state.suspicion = Math.max(0.0, state.suspicion - 0.015);
            }

            if (state.searchScanTicks > 0 && mob.getTarget() == null) {
                state.searchScanTicks--;
                float sweep = Mth.sin(state.searchScanTicks * 0.25f) * 55.0f;
                float desiredYaw = state.targetSearchYaw + sweep;
                mob.setYHeadRot(desiredYaw);
                mob.setYRot(Mth.rotLerp(0.15f, mob.getYRot(), desiredYaw));
            }
        }
    }

    public static boolean canDetect(Mob observer, LivingEntity target) {
        if (!StealthConfig.INSTANCE.modEnabled) return true;
        if (observer instanceof Warden) return true;

        MobDetectionProfile profile = StealthConfig.getProfile(observer);
        MobAlertState state = getState(observer);

        Vec3 eyePos = observer.getEyePosition();
        Vec3 targetEye = target.getEyePosition();
        Vec3 targetCenter = target.position().add(0, target.getBbHeight() * 0.5, 0);

        double distance = eyePos.distanceTo(targetCenter);
        double effectiveMaxRange = profile.maxRange * (state.alertTicks > 0 ? 1.25 : 1.0);
        if (distance > effectiveMaxRange) {
            return false;
        }

        if (!hasClearBlockSight(observer.level(), eyePos, targetEye)
                && !hasClearBlockSight(observer.level(), eyePos, targetCenter)) {
            return false;
        }

        Vec3 lookVec = observer.getViewVector(1.0f).normalize();
        Vec3 toTarget = targetCenter.subtract(eyePos).normalize();
        double dot = Mth.clamp(lookVec.dot(toTarget), -1.0, 1.0);
        double angleDeg = Math.toDegrees(Math.acos(dot));

        double effectiveHalfFov = (profile.fovDegrees * 0.5) * (state.alertTicks > 0 ? 1.20 : 1.0);

        boolean bumped = distance < 1.6 && !target.isCrouching();
        if (!bumped && angleDeg > effectiveHalfFov) {
            return false;
        }

        double clarity = computeVisualClarity(observer.level(), target, eyePos, targetCenter);
        if (clarity < profile.clarityThreshold && distance > 3.0) {
            return false;
        }

        double speed = getEntitySpeed(target);
        if (speed >= StealthConfig.INSTANCE.globalSpeedBypassThreshold && profile.speedBlurFactor > 0.1) {
            double lateralFactor = Math.sin(Math.toRadians(Math.max(15.0, angleDeg)));
            double blurDifficulty = speed * profile.speedBlurFactor * (0.6 + lateralFactor);
            if (blurDifficulty > 0.55 && state.suspicion < 0.75) {
                state.suspicion = Math.min(0.70, state.suspicion + 0.12);
                state.alertTicks = Math.max(state.alertTicks, 40);
                sendDebug(target, observer, "BLUR BYPASS (speed=" + String.format("%.2f", speed) + ")");
                return false;
            }
        }

        double distFactor = 1.0 - (distance / effectiveMaxRange);
        double fovFactor = bumped ? 1.0 : (1.0 - (angleDeg / effectiveHalfFov) * 0.65);
        double sneakFactor = target.isCrouching() ? profile.sneakMultiplier : 1.0;
        if (target.isSprinting()) sneakFactor *= 1.35;

        double visibility = distFactor * fovFactor * clarity * sneakFactor;

        if (visibility >= 0.28 || state.suspicion >= 1.0) {
            state.suspicion = 1.0;
            state.alertTicks = profile.memoryTicks;
            return true;
        }

        if (visibility > 0.08) {
            state.suspicion = Math.min(1.0, state.suspicion + visibility * 0.35);
            state.alertTicks = Math.max(state.alertTicks, 60);
            sendDebug(target, observer, String.format("Suspicion: %.0f%% (vis=%.2f)", state.suspicion * 100, visibility));
            return state.suspicion >= 1.0;
        }

        return false;
    }

    public static void onMobDamaged(Mob mob, Entity attacker, Entity directSource) {
        if (!StealthConfig.INSTANCE.modEnabled) return;
        if (mob instanceof Warden) return;
        if (!(attacker instanceof LivingEntity livingAttacker)) return;

        MobDetectionProfile profile = StealthConfig.getProfile(mob);
        MobAlertState state = getState(mob);

        boolean isRangedAmbush = (directSource != null && directSource != attacker);
        boolean canSeeAttackerNow = canDetect(mob, livingAttacker);

        if (canSeeAttackerNow) {
            state.suspicion = 1.0;
            state.alertTicks = profile.memoryTicks;
            mob.setTarget(livingAttacker);
            return;
        }

        Vec3 approxOrigin = (directSource != null) ? directSource.position() : attacker.position();
        Vec3 dir = approxOrigin.subtract(mob.position());
        float rawYaw = (float) (Mth.atan2(dir.z, dir.x) * (180.0 / Math.PI)) - 90.0f;

        float errorCone = (float) ((1.0 - profile.intelligence) * 140.0);
        float randomOffset = (mob.getRandom().nextFloat() - 0.5f) * errorCone;

        state.targetSearchYaw = rawYaw + randomOffset;
        state.searchScanTicks = (int) (40 + profile.intelligence * 80);
        state.alertTicks = profile.memoryTicks;
        state.lastSuspectedPos = approxOrigin;

        double suspicionGain = isRangedAmbush
                ? (0.20 + profile.intelligence * 0.45)
                : (0.45 + profile.intelligence * 0.45);
        state.suspicion = Math.min(1.0, state.suspicion + suspicionGain);

        if (state.suspicion >= 1.0 && hasClearBlockSight(mob.level(), mob.getEyePosition(), livingAttacker.getEyePosition())) {
            mob.setTarget(livingAttacker);
        } else {
            mob.setTarget(null);
            mob.setLastHurtByMob(null);
            sendDebug(livingAttacker, mob, String.format("Ambushed! Intel=%.0f%% Suspicion=%.0f%%",
                    profile.intelligence * 100, state.suspicion * 100));
        }
    }

    private static boolean hasClearBlockSight(Level level, Vec3 from, Vec3 to) {
        BlockHitResult hit = level.clip(new ClipContext(
                from, to, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, net.minecraft.world.phys.shapes.CollisionContext.empty()
        ));
        return hit.getType() == HitResult.Type.MISS;
    }

    private static double computeVisualClarity(Level level, LivingEntity target, Vec3 from, Vec3 to) {
        if (target.hasEffect(MobEffects.INVISIBILITY)) {
            return 0.08;
        }
        if (target.hasEffect(MobEffects.GLOWING)) {
            return 1.0;
        }

        BlockPos targetPos = target.blockPosition();
        int light = level.getMaxLocalRawBrightness(targetPos);
        double lightClarity = 0.22 + (light / 15.0) * 0.78;

        double foliagePenalty = 0.0;
        BlockState headState = level.getBlockState(targetPos.above());
        BlockState feetState = level.getBlockState(targetPos);
        if (!headState.isAir() && !headState.canOcclude()) foliagePenalty += 0.35;
        if (!feetState.isAir() && !feetState.canOcclude()) foliagePenalty += 0.25;

        Vec3 mid = from.lerp(to, 0.5);
        BlockState midState = level.getBlockState(BlockPos.containing(mid));
        if (!midState.isAir() && !midState.canOcclude()) foliagePenalty += 0.30;

        return Mth.clamp(lightClarity - foliagePenalty, 0.05, 1.0);
    }

    private static double getEntitySpeed(LivingEntity entity) {
        if (entity instanceof ServerPlayer sp) {
            return PLAYER_SPEED.getOrDefault(sp.getUUID(), entity.getDeltaMovement().horizontalDistance());
        }
        return entity.getDeltaMovement().horizontalDistance();
    }

    private static void sendDebug(LivingEntity target, Mob observer, String msg) {
        if (StealthConfig.INSTANCE.showDebugActionbar && target instanceof ServerPlayer sp) {
            sp.sendSystemMessage(Component.literal("§7[StealthSense] §e" + observer.getName().getString() + "§7: §f" + msg), true);
        }
    }
}
