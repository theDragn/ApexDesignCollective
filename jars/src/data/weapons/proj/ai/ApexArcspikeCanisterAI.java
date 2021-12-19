package data.weapons.proj.ai;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.AIUtils;

import java.util.List;

public class ApexArcspikeCanisterAI implements MissileAIPlugin, GuidedMissileAI
{
    public static final int MED_CANISTER_REPS = 5;
    public static final int MED_SHOTS_PER_REP = 2;
    public static final float MED_CANISTER_SEPARATION = 0.05f;
    public static final float MED_MISSILE_RANGE = 1000f;

    public static final int LARGE_CANISTER_REPS = 20;
    public static final int LARGE_SHOTS_PER_REP = 1;
    public static final float LARGE_CANISTER_SEPARATION = 0.20f;
    public static final float LARGE_MISSILE_RANGE = 2000f;

    public static final float AUTO_ACQUIRE_TIMER = 3f;

    private boolean isLargeCanister;
    private boolean isFiring;
    private boolean openedCanister;
    private int repsDone;
    private int repsToDo;
    private int missilesPerRep;
    private float aliveTime;
    private MissileAPI missile;
    private ShipAPI target;
    private IntervalUtil updateTimer;
    private IntervalUtil firingTimer;
    private String weaponID;
    public static CombatEngineAPI engine;

    public ApexArcspikeCanisterAI(MissileAPI missile, ShipAPI launchingShip)
    {
        this.missile = missile;

        // yes, this is janky, shut up
        if (missile.getProjectileSpecId().contains("arcstorm"))
        {
            isLargeCanister = true;
            firingTimer = new IntervalUtil(LARGE_CANISTER_SEPARATION, LARGE_CANISTER_SEPARATION);
            repsToDo = LARGE_CANISTER_REPS;
            missilesPerRep = LARGE_SHOTS_PER_REP;
            weaponID = "apex_arcstorm";
        } else if (missile.getProjectileSpecId().contains("fighter"))
        {
            isLargeCanister = false;
            firingTimer = new IntervalUtil(MED_CANISTER_SEPARATION, MED_CANISTER_SEPARATION);
            repsToDo = MED_CANISTER_REPS - 2;
            missilesPerRep = MED_SHOTS_PER_REP;
            weaponID = "apex_arcspike";
        } else {
            isLargeCanister = false;
            firingTimer = new IntervalUtil(MED_CANISTER_SEPARATION, MED_CANISTER_SEPARATION);
            repsToDo = MED_CANISTER_REPS;
            missilesPerRep = MED_SHOTS_PER_REP;
            weaponID = "apex_arcspike";
        }
        updateTimer = new IntervalUtil(0.33f, 0.66f);
        repsDone = 0;
        aliveTime = 0;
        isFiring = false;
        openedCanister = false;
        engine = Global.getCombatEngine();
        target = launchingShip.getShipTarget();


    }

    @Override
    public void advance(float amount)
    {
        if (engine.isPaused() || missile.isFading() || missile.didDamage())
            return;

        updateTimer.advance(amount);
        aliveTime += amount;
        // first bit is to track nearest target
        // only bother with target checks every update interval (this will also give us a brief delay before firing, if launched at point-blank range)
        if (isLargeCanister)
            missile.giveCommand(ShipCommand.DECELERATE);
        if (!isFiring && updateTimer.intervalElapsed())
        {
            float range = (isLargeCanister) ? LARGE_MISSILE_RANGE : MED_MISSILE_RANGE;
            // if the target is null, phased, or dead, or it's out of range and we're past the auto-target timer, get a new target
            if (target == null || !target.isAlive() || target.isPhased() || (aliveTime > AUTO_ACQUIRE_TIMER && MathUtils.getDistanceSquared(missile, target) > range * range))
                acquireTarget();
            if (target != null && MathUtils.getDistanceSquared(missile, target) < range * range)
                isFiring = true;
        } else if (isFiring)
        {
            firingTimer.advance(amount);
            if (firingTimer.intervalElapsed())
            {
                if (!openedCanister)
                {
                    openedCanister = true;
                }
                if (repsDone < repsToDo)
                {
                    for (int i = 0; i < missilesPerRep; i++)
                    {
                        CombatEntityAPI proj = engine.spawnProjectile(missile.getSource(), missile.getWeapon(), weaponID, missile.getLocation(), missile.getFacing() + Misc.random.nextFloat() * 40f - 20f, missile.getVelocity());
                        GuidedMissileAI ai = (GuidedMissileAI) ((MissileAPI) proj).getUnwrappedMissileAI();
                        ai.setTarget(target);
                    }
                    Global.getSoundPlayer().playSound("squall_fire", 1f, 1f,missile.getLocation(), missile.getVelocity());
                } else
                {
                    //engine.applyDamage(missile, missile.getLocation(), missile.getHitpoints() + 1f, DamageType.FRAGMENTATION, 0f, true, false, missile.getSource());
                    missile.setFlightTime(9999f);
                }
                repsDone++;
            }
            if (target != null)
            {
                float aimAngle = MathUtils.getShortestRotation(missile.getFacing(), VectorUtils.getAngle(missile.getLocation(), target.getLocation()));
                missile.setAngularVelocity(aimAngle * 2f);
            }
        }

    }

    private void acquireTarget()
    {
        float range = (isLargeCanister) ? LARGE_MISSILE_RANGE : MED_MISSILE_RANGE;
        List<ShipAPI> targetList = AIUtils.getNearbyEnemies(missile, range);
        float targetRange = 9999 * 9999;
        ShipAPI newTarget = null;
        if (!targetList.isEmpty())
        {
            for (ShipAPI potTarget : targetList)
            {
                if (potTarget == null || potTarget.getHullSize() == ShipAPI.HullSize.FIGHTER || potTarget.isPhased() || !potTarget.isAlive())
                    continue;
                float tmp = MathUtils.getDistanceSquared(potTarget.getLocation(), missile.getLocation());
                if (tmp < targetRange)
                {
                    targetRange = tmp;
                    newTarget = potTarget;
                }

            }
        }
        this.target = newTarget;
    }

    @Override
    public CombatEntityAPI getTarget()
    {
        return target;
    }

    @Override
    public void setTarget(CombatEntityAPI target)
    {
        if (target instanceof ShipAPI)
            this.target = (ShipAPI) target;
    }
}
