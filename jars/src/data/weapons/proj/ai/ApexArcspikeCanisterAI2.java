package data.weapons.proj.ai;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lazywizard.lazylib.MathUtils;

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

    public ApexArcspikeCanisterAI2(MissileAPI missile, CombatEntityAPI target)
    {
        this.missile = missile;
        this.target = target instanceof ShipAPI ? (ShipAPI)target : null;
        this.engine = Global.getCombatEngine();
        int numSubmunitions = missile.getMirvNumWarheads();
        this.mirvRange = missile.getProjectileSpecId().contains("arcstorm") ? 2000 : 1000;
        this.mirvRange = missile.getSource().getMutableStats().getMissileWeaponRangeBonus().computeEffective(mirvRange);
        this.numSubmunitionsPerCycle = missile.getProjectileSpecId().contains("arcstorm") ? 1 : 2;
        float firingInterval = missile.getProjectileSpecId().contains("arcstorm") ? 0.2f : 0.05f;
        this.firingTimer = new IntervalUtil(firingInterval, firingInterval);
        this.firingReps = numSubmunitions / numSubmunitionsPerCycle;
        this.rangeLastFrame = Float.MAX_VALUE;
        this.subWepID = missile.getProjectileSpecId().contains("arcstorm") ? "apex_arcstorm" : "apex_arcspike";
        this.isFiring = false;
    }

    @Override
    public void advance(float amount)
    {
        if (engine.isPaused() || missile.isFading() || missile.didDamage() || missile.getElapsed() < ARMING_TIME)
            return;
        // arming time is elapsed
        // check target, reacquire if null or dead
        if (target == null || !target.isAlive() || target.isHulk())
            acquireTarget();
        // double check in case we couldn't find any targets at all
        if (target == null)
            return;
        if (!isFiring)
        {
            float rangeThisFrame = MathUtils.getDistanceSquared(missile.getLocation(), target.getLocation());
            // if in range and the target is hittable: smack it
            if (rangeThisFrame < mirvRange * mirvRange && !target.isPhased())
                mirv();
            // if range is lower than it was (but presumably not in mirv range, since we got here), just wait
            if (rangeThisFrame < rangeLastFrame)
            {
                // wait
                // if range difference is growing and we're running out of time, reacquire target and fire
            } else if (rangeThisFrame > rangeLastFrame && missile.getElapsed() > missile.getFlightTime() * 0.9f)
            {
                acquireTarget();
                if (target != null)
                    mirv();
            }
        } else
        {
            firingTimer.advance(amount);
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
        this.target = target;
    }

}
