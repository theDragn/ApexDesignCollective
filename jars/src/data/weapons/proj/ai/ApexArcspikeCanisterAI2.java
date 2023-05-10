package data.weapons.proj.ai;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import org.magiclib.util.MagicTargeting;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;

public class ApexArcspikeCanisterAI2 implements MissileAIPlugin, GuidedMissileAI
{
    public static final float ARMING_TIME = 1.0f;
    private final MissileAPI missile;
    private ShipAPI target;
    private CombatEngineAPI engine;
    private float mirvRange;
    private int firingReps;
    private int numSubmunitionsPerCycle;
    private float rangeLastFrame;
    private String subWepID;
    private boolean isFiring;
    private IntervalUtil firingTimer;
    private IntervalUtil updateTimer = new IntervalUtil(0.1f, 0.33f);
    private int repsDone = 0;

    public ApexArcspikeCanisterAI2(MissileAPI missile, ShipAPI launchingShip)
    {
        boolean isLarge = missile.getProjectileSpecId().contains("arcstorm");
        this.missile = missile;
        this.target = launchingShip.getShipTarget();
        this.engine = Global.getCombatEngine();
        int numSubmunitions = missile.getMirvNumWarheads();
        // compute range for split
        this.mirvRange = missile.getSource().getMutableStats().getMissileWeaponRangeBonus().computeEffective(isLarge ? 2000 : 1000);
        this.numSubmunitionsPerCycle = isLarge ? 1 : 2;
        float firingInterval = isLarge ? 0.2f : 0.05f;
        this.firingTimer = new IntervalUtil(firingInterval, firingInterval);
        this.firingReps = numSubmunitions / numSubmunitionsPerCycle;
        this.rangeLastFrame = Float.MAX_VALUE;
        this.subWepID = isLarge ? "apex_arcstorm" : "apex_arcspike";
        this.isFiring = false;
        if (target == null)
            acquireTarget();
    }

    @Override
    public void advance(float amount)
    {
        if (engine.isPaused() || missile.isFading() || missile.didDamage() || missile.getElapsed() < ARMING_TIME)
            return;
        updateTimer.advance(amount);
        if (!isFiring && !updateTimer.intervalElapsed() && missile.getElapsed() < missile.getMaxFlightTime() * 0.9f)
            return;
        // arming time is elapsed
        // check target, reacquire if null or dead
        if (target == null || !target.isAlive() || target.isHulk())
            acquireTarget();
        // double check in case we couldn't find any targets at all
        if (target == null)
            return;
        // rotate canister towards target
        float aimAngle = MathUtils.getShortestRotation(missile.getFacing(), VectorUtils.getAngle(missile.getLocation(), target.getLocation()));
        missile.setAngularVelocity(aimAngle * 2f);
        if (!isFiring)
        {
            float rangeThisFrame = MathUtils.getDistanceSquared(missile.getLocation(), target.getLocation());
            // if in range and the target is hittable: smack it
            if (rangeThisFrame < mirvRange * mirvRange && !target.isPhased())
                isFiring = true;
            // if range is lower than it was (but presumably not in mirv range, since we got here), just wait
            if (rangeThisFrame < rangeLastFrame)
            {
                // wait
                // if we're running out of time, reacquire target and fire
            } else if (missile.getElapsed() > missile.getMaxFlightTime() * 0.9f)
            {
                acquireTarget();
                if (target != null)
                    isFiring = true;
            }

        } else
        {
            firingTimer.advance(amount);
            if (!firingTimer.intervalElapsed())
                return;
            if (repsDone < firingReps)
            {
                for (int i = 0; i < numSubmunitionsPerCycle; i++)
                {
                    CombatEntityAPI proj = engine.spawnProjectile(missile.getSource(), missile.getWeapon(), subWepID, missile.getLocation(), missile.getFacing() + Misc.random.nextFloat() * 40f - 20f, missile.getVelocity());
                    GuidedMissileAI ai = (GuidedMissileAI) ((MissileAPI) proj).getUnwrappedMissileAI();
                    ai.setTarget(target);
                }
                Global.getSoundPlayer().playSound("squall_fire", 1f, 1f,missile.getLocation(), missile.getVelocity());
            } else
            {
                // fired all submunitions, fade out projectile
                missile.setFlightTime(9999f);
            }
            repsDone++;
        }
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
            this.target = (ShipAPI)target;
    }

    private void acquireTarget()
    {
        target = MagicTargeting.pickMissileTarget(
                missile,
                MagicTargeting.targetSeeking.NO_RANDOM,
                (int)(mirvRange * 1.5),
                360,
                0,
                1,
                2,
                3,
                4
        );
    }

}
