package com.stealthsense.logic;

import net.minecraft.world.phys.Vec3;

public class MobAlertState {
    public double suspicion = 0.0;
    public int alertTicks = 0;
    public int searchScanTicks = 0;
    public float targetSearchYaw = 0.0f;
    public Vec3 lastSuspectedPos = null;
}
