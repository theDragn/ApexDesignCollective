package data.weapons.proj;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.Misc;
import org.jetbrains.annotations.NotNull;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.List;

// Originally by Nicke535, substantially modified by theDragn
// unlike the rest of this mod's code, this is CC BY-NC-SA; do whatever you want with it


public class ApexQGPDHomingScript extends BaseEveryFrameCombatPlugin
{


    private static final float TARGET_REACQUIRE_RANGE = 500;
    private static final float TARGET_REACQUIRE_ANGLE = 90;
    private static final float TURN_RATE = 720f;


    private DamagingProjectileAPI proj;
    private CombatEntityAPI target;
    private Vector2f lastTargetPos;
    private int reacquireAttempts;

    public ApexQGPDHomingScript(@NotNull DamagingProjectileAPI proj, CombatEntityAPI target)
    {
        this.proj = proj;
        this.target = target;
        reacquireAttempts = 0;
        lastTargetPos = target != null ? target.getLocation() : new Vector2f(proj.getLocation());
    }


    //Main advance method
    @Override
    public void advance(float amount, List<InputEventAPI> events)
    {
        //Sanity checks
        if (Global.getCombatEngine() == null || Global.getCombatEngine().isPaused())
        {
            return;
        }

        //Checks if our script should be removed from the combat engine
        if (proj == null || proj.didDamage() || proj.isFading() || !Global.getCombatEngine().isEntityInPlay(proj))
        {
            Global.getCombatEngine().removePlugin(this);
            return;
        }

        // don't try to reacquire targets more than twice
        if (target == null && reacquireAttempts <= 1)
        {
            reacquireAttempts++;
            reacquireTarget();
        } else if (target == null)
        {
            return;
        } else
        {
            //Vector2f interceptPoint = AIUtils.getBestInterceptPoint(proj.getLocation(), proj.getMoveSpeed(), target.getLocation(), target.getVelocity());
            //if (interceptPoint == null)
             //   return;
            // don't bother intercepting, just point directly at the target every frame
            float interceptAngle = VectorUtils.getAngle(proj.getLocation(), target.getLocation());
            float rotateAmount = Math.min(Math.abs(MathUtils.getShortestRotation(proj.getFacing(), interceptAngle)),TURN_RATE)  * Misc.getClosestTurnDirection(proj.getFacing(), interceptAngle);
            proj.setFacing(proj.getFacing() + rotateAmount);
            proj.getVelocity().set(VectorUtils.rotate(proj.getVelocity(), rotateAmount));
        }
    }

    private void reacquireTarget()
    {
        CombatEntityAPI newTarget = null;
        List<CombatEntityAPI> potentialTargets = new ArrayList<>();
        for (CombatEntityAPI potTarget : CombatUtils.getMissilesWithinRange(lastTargetPos, proj.getSource().getMutableStats().getEnergyWeaponRangeBonus().computeEffective(TARGET_REACQUIRE_RANGE) * 1.5f))
        {
            if (potTarget.getOwner() != proj.getOwner() && Math.abs(VectorUtils.getAngle(proj.getLocation(), potTarget.getLocation()) - proj.getFacing()) < TARGET_REACQUIRE_ANGLE)
            {
                potentialTargets.add(potTarget);
            }
        }
        for (ShipAPI potTarget : CombatUtils.getShipsWithinRange(lastTargetPos, proj.getSource().getMutableStats().getEnergyWeaponRangeBonus().computeEffective(TARGET_REACQUIRE_RANGE) * 1.5f))
        {
            if (potTarget.getOwner() == proj.getOwner()
                    || Math.abs(VectorUtils.getAngle(proj.getLocation(), potTarget.getLocation()) - proj.getFacing()) > TARGET_REACQUIRE_ANGLE
                    || potTarget.isHulk())
            {
                continue;
            }
            if (potTarget.getHullSize().equals(ShipAPI.HullSize.FIGHTER))
            {
                potentialTargets.add(potTarget);
            }
        }
        if (!potentialTargets.isEmpty())
        {
            for (CombatEntityAPI potTarget : potentialTargets)
            {
                if (newTarget == null)
                {
                    newTarget = potTarget;
                } else if (MathUtils.getDistanceSquared(newTarget, lastTargetPos) > MathUtils.getDistanceSquared(potTarget, lastTargetPos))
                {
                    newTarget = potTarget;
                }
            }
            target = newTarget;
        }
    }
}