package data.shipsystems.ai;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import data.ApexUtils;
import org.lazywizard.lazylib.CollisionUtils;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;

import static plugins.ApexModPlugin.EUROBEAT_MODE;

public class ApexCruiseModeAI implements ShipSystemAIScript
{
    private ShipAPI ship;
    private ShipSystemAPI system;
    private CombatEngineAPI engine;
    private ShipwideAIFlags flags;
    private IntervalUtil timer = new IntervalUtil(0.25f, 0.33f);

    @Override
    public void init(ShipAPI ship, ShipSystemAPI system, ShipwideAIFlags flags, CombatEngineAPI engine)
    {
        this.ship = ship;
        this.system = system;
        this.engine = engine;
        this.flags = flags;
    }

    @Override
    public void advance(float amount, Vector2f missileDangerDir, Vector2f collisionDangerDir, ShipAPI target)
    {
        if (!ship.isAlive() || engine.isPaused())
            return;
        timer.advance(amount);
        if (EUROBEAT_MODE && system.getState().equals(ShipSystemAPI.SystemState.IDLE))
            ship.useSystem();
        if (timer.intervalElapsed())
        {
            // get this ship's destination from
            Vector2f destination = null;
            CombatTaskManagerAPI taskMan = Global.getCombatEngine().getFleetManager(ship.getOwner()).getTaskManager(false);
            CombatFleetManagerAPI.AssignmentInfo assInfo = null;
            if (taskMan != null)
                assInfo = taskMan.getAssignmentFor(ship);
            if (assInfo != null && assInfo.getTarget() != null)
                destination = assInfo.getTarget().getLocation();
            // if we can activate system, and we don't have flux (easy check to make sure we're not being pursued by missiles or something)
            boolean hasNearbyEnemies = !AIUtils.getNearbyEnemies(ship, 1000f).isEmpty();
            if (system.getState().equals(ShipSystemAPI.SystemState.IDLE) && ship.getFluxLevel() == 0f && !isTooCloseToAlly())
            {
                // if we've got a destination from the fleet manager, use that to determine if we should engage system
                if (destination != null)
                {
                    // activate if we're facing our destination, it's far enough away to justify using the system, and there's no nearby baddies
                    if (MathUtils.getShortestRotation(ship.getFacing(), VectorUtils.getAngle(ship.getLocation(), destination)) < 30f
                            && !hasNearbyEnemies
                            && MathUtils.getDistanceSquared(ship.getLocation(), destination) > 2500f * 2500f)
                    {
                        ship.useSystem();
                    }
                }
                // otherwise (because the shitty player didn't give us any orders), activate if our velocity vector mostly matches up with our ship's facing and we don't have any nearby hostiles
                else if (Math.abs(MathUtils.getShortestRotation(VectorUtils.getAngle(Misc.ZERO, ship.getVelocity()), ship.getFacing())) < 30f
                        && !hasNearbyEnemies)
                {
                    ship.useSystem();
                }

            } // else, if system is active
            else if (system.getState().equals(ShipSystemAPI.SystemState.ACTIVE))
            {
                if (destination != null)
                {
                    // deactivate if we're facing too far away from the destination (ie, passing it or bounced off of something), getting close to our destination, or there's a nearby enemy
                    if (MathUtils.getShortestRotation(ship.getFacing(), VectorUtils.getAngle(ship.getLocation(), destination)) > 40f
                            || MathUtils.getDistance(destination, ship.getLocation()) < 1000f
                            || hasNearbyEnemies)
                    {
                        ship.useSystem();
                    }
                }
                // otherwise, if there's an enemy within danger range or there *aren't* any enemies, deactivate
                else if (hasNearbyEnemies)// || AIUtils.getEnemiesOnMap(ship).isEmpty())
                {
                    ship.useSystem();
                }
                else
                {
                    if (willCollideWithAlly() || isTooCloseToAlly())
                    {
                        ship.useSystem();
                    }

                }
            }
        }
    }

    private boolean willCollideWithAlly()
    {
        for (ShipAPI ally : AIUtils.getNearbyAllies(ship, 1500f))
        {
            Vector2f dest = Vector2f.add(ship.getLocation(), ship.getVelocity(), null);
            dest = VectorUtils.clampLength(dest, 1000f, 1500f);
            Vector2f allyDest = Vector2f.add(ally.getLocation(), ally.getVelocity(), null);
            if (ally.getHullSize() != ShipAPI.HullSize.FIGHTER && CollisionUtils.getCollides(ship.getLocation(), dest, allyDest, ally.getCollisionRadius()*1.5f))
            {
                return true;
            }
        }
        return false;
    }

    private boolean isTooCloseToAlly()
    {
        for (ShipAPI ally : AIUtils.getNearbyAllies(ship, 1200f))
        {
            if (ally.getHullSize() != ShipAPI.HullSize.FIGHTER && ApexUtils.isEntityInArc(ally, ship.getLocation(), ship.getFacing(), 45f))
            {
                return true;
            }
        }
        return false;
    }
}