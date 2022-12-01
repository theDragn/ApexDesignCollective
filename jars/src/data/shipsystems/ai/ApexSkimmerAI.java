package data.shipsystems.ai;

import com.fs.starfarer.api.combat.CollisionClass;
import com.fs.starfarer.api.combat.CombatAssignmentType;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.CombatFleetManagerAPI.AssignmentInfo;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipSystemAIScript;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import com.fs.starfarer.api.combat.ShipwideAIFlags;
import com.fs.starfarer.api.combat.ShipwideAIFlags.AIFlags;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponType;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;

import java.util.*;

import data.shipsystems.ApexInertialSkimmer;
import org.lazywizard.lazylib.*;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;


// quite literally copy/pasted from VIC's Omni-Lunge AI script
// (with permission)
// I think most of this was originally written by DR?
public class ApexSkimmerAI implements ShipSystemAIScript {

    private CombatEngineAPI engine;
    private ShipwideAIFlags flags;
    private ShipAPI ship;
    private ShipSystemAPI system;

    private final IntervalUtil tracker = new IntervalUtil(0.1f, 0.15f);

    private static final float FIGHTER_CRITICAL_RANGE = 500f;
    private static final float FIGHTER_STRIKE_RANGE = 1200f;

    private static final boolean DEBUG = false;
    private final Object STATUSKEY1 = new Object();
    private final Object STATUSKEY2 = new Object();
    private float desireShow = 0f;
    private float targetDesireShow = 0f;
    private float angleToTargetShow = 0f;

    private final Map<ShipAPI.HullSize, Float> strafeMulti = new HashMap<>();
    {
        strafeMulti.put(ShipAPI.HullSize.FIGHTER, 1f);
        strafeMulti.put(ShipAPI.HullSize.FRIGATE, 1f);
        strafeMulti.put(ShipAPI.HullSize.DESTROYER, 0.75f);
        strafeMulti.put(ShipAPI.HullSize.CRUISER, 0.5f);
        strafeMulti.put(ShipAPI.HullSize.CAPITAL_SHIP, 0.25f);
    }

    @Override
    public void advance(float amount, Vector2f missileDangerDir, Vector2f collisionDangerDir, ShipAPI target) {
        if (engine == null) {
            return;
        }

        if (engine.isPaused()) {
            if (DEBUG) {
                if (engine.getPlayerShip() == ship) {
                    engine.maintainStatusForPlayerShip(STATUSKEY1, system.getSpecAPI().getIconSpriteName(),
                            "AI", "Desire: " + Math.round(100f * desireShow) + "/" + Math.round(100f * targetDesireShow), desireShow < targetDesireShow);
                    engine.maintainStatusForPlayerShip(STATUSKEY2, system.getSpecAPI().getIconSpriteName(),
                            "AI", "Angle: " + Math.round(angleToTargetShow), false);
                }
            }
            return;
        }

        tracker.advance(amount);

        if (tracker.intervalElapsed()) {

            /* Skip if we're flamed out.  Except for Armor, which wants to immediately use the system if it's flamed out. */
            float desire = 0f;
            CombatEntityAPI immediateTarget = null;

            //stuff for fighters
            //TODO: check all places its used and delete it
            boolean returning = false;
            if ((ship.getWing() != null) && ship.getWing().isReturning(ship)) {
                returning = true;
            }

            float engageRange;
            if (ship.getWing() != null) {
                if (returning) {
                    engageRange = 500f;
                } else {
                    engageRange = ship.getWing().getSpec().getAttackRunRange();
                }
            } else {
                engageRange = 700f;
                for (WeaponAPI weapon : ship.getUsableWeapons()) {
                    if (weapon.getType() == WeaponType.MISSILE) {
                        continue;
                    }
                    if (weapon.getRange() > engageRange * 0.9f) {
                        engageRange = weapon.getRange() * 0.9f;
                    }
                }
            }

            //get main target
            ShipAPI carrier = null;
            if ((ship.getWing() != null) && ship.getWing().getSourceShip() != null) {
                carrier = ship.getWing().getSourceShip();
                if (returning) {
                    immediateTarget = carrier;
                } else if ((carrier.getAIFlags() != null) && carrier.getAIFlags().getCustom(AIFlags.CARRIER_FIGHTER_TARGET) instanceof CombatEntityAPI) {
                    immediateTarget = (CombatEntityAPI) carrier.getAIFlags().getCustom(AIFlags.CARRIER_FIGHTER_TARGET);
                } else if ((carrier.getAIFlags() != null) && carrier.getAIFlags().getCustom(AIFlags.MANEUVER_TARGET) instanceof CombatEntityAPI) {
                    immediateTarget = (CombatEntityAPI) carrier.getAIFlags().getCustom(AIFlags.MANEUVER_TARGET);
                } else {
                    immediateTarget = carrier.getShipTarget();
                }
            }
            if ((immediateTarget == null) && ship.isFighter() && (flags.getCustom(AIFlags.CARRIER_FIGHTER_TARGET) instanceof CombatEntityAPI)) {
                immediateTarget = (CombatEntityAPI) flags.getCustom(AIFlags.CARRIER_FIGHTER_TARGET);
            }
            //fighter stuff end
            if ((immediateTarget == null) && (flags.getCustom(AIFlags.MANEUVER_TARGET) instanceof CombatEntityAPI)) {
                immediateTarget = (CombatEntityAPI) flags.getCustom(AIFlags.MANEUVER_TARGET);
            }
            if (immediateTarget == null) {
                immediateTarget = ship.getShipTarget();
            }

            //get secondary target location
            AssignmentInfo assignment = engine.getFleetManager(ship.getOwner()).getTaskManager(ship.isAlly()).getAssignmentFor(ship);
            Vector2f targetSpot;
            if (ship.isFighter()) {
                assignment = null;
                targetSpot = null;
            } else {
                if ((assignment != null) && (assignment.getTarget() != null) && (assignment.getType() != CombatAssignmentType.AVOID)) {
                    targetSpot = assignment.getTarget().getLocation();
                } else {
                    targetSpot = null;
                }
            }

            //get direction of boost
            Vector2f newVector = new Vector2f();
            if (ship.getEngineController().isAccelerating()) {
                newVector.y += 1 * ship.getAcceleration();
            }
            if(ship.getEngineController().isAcceleratingBackwards() || ship.getEngineController().isDecelerating()){
                newVector.y -= 1 * ship.getDeceleration();
            }
            if (ship.getEngineController().isStrafingLeft()) {
                newVector.x -=  1 * ship.getAcceleration() * strafeMulti.get(ship.getHullSize());
            }
            if (ship.getEngineController().isStrafingRight()) {
                newVector.x += 1 * ship.getAcceleration() * strafeMulti.get(ship.getHullSize());
            }
            VectorUtils.rotate(newVector, ship.getFacing() - 90);
            if (VectorUtils.isZeroVector(newVector)) newVector = new Vector2f(ship.getVelocity());
            Vector2f direction = newVector;
            float range = (ApexInertialSkimmer.SPEED_BONUS + ship.getMaxSpeed()) * system.getChargeActiveDur() * 1.1f;
            Misc.normalise(direction);

            float angleToTargetSpot = 0f;
            if (targetSpot != null) {
                float targetSpotDir = VectorUtils.getAngleStrict(ship.getLocation(), targetSpot);
                angleToTargetSpot = MathUtils.getShortestRotation(VectorUtils.getFacing(direction), targetSpotDir);
            }
            float angleToImmediateTarget = 0f;
            if (immediateTarget != null) {
                float immediateTargetDir = VectorUtils.getAngleStrict(ship.getLocation(), immediateTarget.getLocation());
                angleToImmediateTarget = MathUtils.getShortestRotation(VectorUtils.getFacing(direction), immediateTargetDir);
            }
            angleToTargetShow = angleToImmediateTarget;

            float onTargetThreshold;
            if (ship.isFighter()) {
                onTargetThreshold = 45f;
            } else {
                onTargetThreshold = 60f;
            }

            if (!ship.isFighter()) {
                if (flags.hasFlag(AIFlags.RUN_QUICKLY)) {
                    if (immediateTarget != null) {
                        if (Math.abs(angleToImmediateTarget) >= 90f) {
                            desire += 1.25f;
                        }
                    } else {
                        desire += 0.75f;
                    }
                }
                if (flags.hasFlag(AIFlags.PURSUING)) {
                    if (immediateTarget != null) {
                        if (Math.abs(angleToImmediateTarget) <= onTargetThreshold) {
                            desire += 0.75f;
                        }
                    } else if (targetSpot != null) {
                        if (Math.abs(angleToTargetSpot) <= onTargetThreshold) {
                            desire += 0.5f;
                        }
                    } else {
                        desire += 0.25f;
                    }
                }
                if (flags.hasFlag(AIFlags.HARASS_MOVE_IN)) {
                    if (immediateTarget != null) {
                        if (Math.abs(angleToImmediateTarget) <= onTargetThreshold) {
                            desire += 1f;
                        }
                    } else if (targetSpot != null) {
                        if (Math.abs(angleToTargetSpot) <= onTargetThreshold) {
                            desire += 0.75f;
                        }
                    } else {
                        desire += 0.5f;
                    }
                }
            } else {
                //fighter part
                if (!returning && flags.hasFlag(AIFlags.IN_ATTACK_RUN)) {
                    if (immediateTarget != null) {
                        if (Math.abs(angleToImmediateTarget) <= onTargetThreshold) {
                            if (MathUtils.getDistance(immediateTarget, ship) < (FIGHTER_CRITICAL_RANGE - ship.getCollisionRadius())) {
                                desire -= 1f;
                            } else if (MathUtils.getDistance(immediateTarget, ship) < (FIGHTER_STRIKE_RANGE - ship.getCollisionRadius())) {
                            } else {
                                desire += 1.25f;
                            }
                        } else if (Math.abs(angleToImmediateTarget) >= (180f - onTargetThreshold)) {
                            if (MathUtils.getDistance(immediateTarget, ship) < (FIGHTER_CRITICAL_RANGE - ship.getCollisionRadius())) {
                                desire += 1.25f;
                            } else if (MathUtils.getDistance(immediateTarget, ship) < (FIGHTER_STRIKE_RANGE - ship.getCollisionRadius())) {
                                if (flags.hasFlag(AIFlags.POST_ATTACK_RUN)) {
                                    desire += 1f;
                                }
                            } else {
                                desire -= 1.25f;
                            }
                        } else {
                            if (MathUtils.getDistance(immediateTarget, ship) < (FIGHTER_CRITICAL_RANGE - ship.getCollisionRadius())) {
                                desire += 1f;
                            } else if (MathUtils.getDistance(immediateTarget, ship) < (FIGHTER_STRIKE_RANGE - ship.getCollisionRadius())) {
                                if (flags.hasFlag(AIFlags.POST_ATTACK_RUN)) {
                                    desire += 1.25f;
                                } else {
                                    desire += 0.75f;
                                }
                            }
                        }
                    } else if (targetSpot != null) {
                        if (Math.abs(angleToTargetSpot) <= onTargetThreshold) {
                            desire += 1f;
                        }
                    } else {
                        desire += 0.5f;
                    }
                }
            }

            //get closer if far from target
            boolean immediateTargetInRange = false;
            if ((immediateTarget != null) && (MathUtils.getDistance(immediateTarget, ship) < (engageRange - ship.getCollisionRadius()))) {
                immediateTargetInRange = true;
            }

            if ((immediateTarget != null) && !immediateTargetInRange) {
                if ((carrier == null) || !carrier.isPullBackFighters() || (immediateTarget == carrier)) {
                    if (Math.abs(angleToImmediateTarget) <= onTargetThreshold) {
                        desire += 0.5f;
                    }
                }
            }

            if (!ship.isFighter()) {
                float desiredRange = 500f;
                if ((assignment != null)
                        && ((assignment.getType() == CombatAssignmentType.ENGAGE)
                        || (assignment.getType() == CombatAssignmentType.HARASS)
                        || (assignment.getType() == CombatAssignmentType.INTERCEPT)
                        || (assignment.getType() == CombatAssignmentType.LIGHT_ESCORT)
                        || (assignment.getType() == CombatAssignmentType.MEDIUM_ESCORT)
                        || (assignment.getType() == CombatAssignmentType.HEAVY_ESCORT)
                        || (assignment.getType() == CombatAssignmentType.STRIKE))) {
                    desiredRange = engageRange;
                }
                if ((targetSpot != null) && (MathUtils.getDistance(targetSpot, ship.getLocation()) >= desiredRange) && !immediateTargetInRange) {
                    if ((immediateTarget != null) && (MathUtils.getDistance(immediateTarget, targetSpot) <= engageRange)) {
                        if (Math.abs(angleToTargetSpot) <= onTargetThreshold) {
                            desire += 0.5f; // Adds to the other 0.5
                        }
                    } else if (immediateTarget != null) {
                        if (Math.abs(angleToTargetSpot) <= onTargetThreshold) {
                            desire += 0.25f; // Adds to the other 0.5
                        }
                    } else {
                        if (Math.abs(angleToTargetSpot) <= onTargetThreshold) {
                            desire += 0.75f;
                        }
                    }
                }
            } else {
                if (returning && !immediateTargetInRange) {
                    if (immediateTarget != null) {
                        if (Math.abs(angleToImmediateTarget) <= onTargetThreshold) {
                            desire += 2f;
                        }
                    } else if (targetSpot != null) {
                        if (Math.abs(angleToTargetSpot) <= onTargetThreshold) {
                            desire += 2f;
                        }
                    } else {
                        desire += 1f;
                    }
                }
            }

            if (!ship.isFighter()) {
                if (flags.hasFlag(AIFlags.TURN_QUICKLY)) {
                    desire += 0.35f;
                }

                if (flags.hasFlag(AIFlags.BACKING_OFF)) {
                    if (immediateTarget != null) {
                        if (Math.abs(angleToImmediateTarget) >= 90f) {
                            desire += 0.75f;
                        }
                    } else {
                        desire += 0.5f;
                    }
                }

                if (flags.hasFlag(AIFlags.DO_NOT_PURSUE)) {
                    if (immediateTarget != null) {
                        if (Math.abs(angleToImmediateTarget) <= onTargetThreshold) {
                            desire -= 1f;
                        }
                    } else {
                        desire -= 0.5f;
                    }
                }

                if (flags.hasFlag(AIFlags.DO_NOT_USE_FLUX)) {
                    desire += 0.35f;
                }

                if (flags.hasFlag(AIFlags.NEEDS_HELP) || flags.hasFlag(AIFlags.IN_CRITICAL_DPS_DANGER)) {
                    if (immediateTarget != null) {
                        if (Math.abs(angleToImmediateTarget) <= onTargetThreshold) {
                            desire -= 1f;
                        } else if (Math.abs(angleToImmediateTarget) >= (180f - onTargetThreshold)) {
                            desire += 1.5f;
                        } else {
                            desire += 1f;
                        }
                    } else {
                        desire += 1f;
                    }
                }

                if (flags.hasFlag(AIFlags.HAS_INCOMING_DAMAGE)) {
                    if (immediateTarget != null) {
                        if (Math.abs(angleToImmediateTarget) <= onTargetThreshold) {
                            desire -= 0.5f;
                        } else if (Math.abs(angleToImmediateTarget) >= (180f - onTargetThreshold)) {
                            desire += 0.5f;
                        } else {
                            desire += 0.75f;
                        }
                    } else {
                        desire += 0.75f;
                    }
                }

                if ((assignment != null) && (assignment.getType() == CombatAssignmentType.RETREAT)) {
                    float retreatDirection = (ship.getOwner() == 0) ? 270f : 90f;
                    if (Math.abs(MathUtils.getShortestRotation(VectorUtils.getFacing(direction), retreatDirection)) <= onTargetThreshold) {
                        desire += 1.5f;
                    } else if (Math.abs(MathUtils.getShortestRotation(VectorUtils.getFacing(direction), retreatDirection)) >= 90f) {
                        desire -= 1.5f;
                    }
                }
            } else {
                if (flags.hasFlag(AIFlags.WANTED_TO_SLOW_DOWN)) {
                    desire -= 0.5f;
                }
            }

            //float range = 500f;
            List<ShipAPI> directTargets = CombatUtils.getShipsWithinRange(ship.getLocation(), range);
            if (!directTargets.isEmpty() && !ship.isFighter()) {
                Vector2f endpoint = new Vector2f(direction);
                endpoint.scale(range);
                Vector2f.add(endpoint, ship.getLocation(), endpoint);

                Collections.sort(directTargets, new CollectionUtils.SortEntitiesByDistance(ship.getLocation()));
                ListIterator<ShipAPI> iter = directTargets.listIterator();
                while (iter.hasNext()) {
                    ShipAPI tmp = iter.next();
                    if ((tmp != ship) && (ship.getCollisionClass() != CollisionClass.NONE) && !tmp.isFighter() && !tmp.isDrone()) {
                        Vector2f loc = tmp.getLocation();
                        float areaChange = 1f;
                        if (tmp.getOwner() == ship.getOwner()) {
                            areaChange *= 1.5f;
                        }
                        if (CollisionUtils.getCollides(ship.getLocation(), endpoint, loc,
                                (tmp.getCollisionRadius() * 0.5f) + (ship.getCollisionRadius() * 0.75f * areaChange))) {
                            if (ship.isFrigate()) {
                                if (tmp.isFrigate()) {
                                    desire -= 1f;
                                } else if (tmp.isDestroyer()) {
                                    desire -= 2f;
                                } else if (tmp.isCruiser()) {
                                    desire -= 4f;
                                } else {
                                    desire -= 8f;
                                }
                            } else if (ship.isDestroyer()) {
                                if (tmp.isFrigate() && !tmp.isHulk()) {
                                    desire -= 2f;
                                } else if (tmp.isDestroyer()) {
                                    desire -= 1f;
                                } else if (tmp.isCruiser()) {
                                    desire -= 2f;
                                } else {
                                    desire -= 4f;
                                }
                            } else if (ship.isCruiser()) {
                                if (tmp.isFrigate() && !tmp.isHulk()) {
                                    desire -= 4f;
                                } else if (tmp.isDestroyer() && !tmp.isHulk()) {
                                    desire -= 2f;
                                } else if (tmp.isCruiser()) {
                                    desire -= 1f;
                                } else {
                                    desire -= 2f;
                                }
                            } else {
                                if (tmp.isFrigate() && !tmp.isHulk()) {
                                    desire -= 8f;
                                } else if (tmp.isDestroyer() && !tmp.isHulk()) {
                                    desire -= 4f;
                                } else if (tmp.isCruiser() && !tmp.isHulk()) {
                                    desire -= 2f;
                                } else {
                                    desire -= 1f;
                                }
                            }
                        }
                    }
                }
            }
            //Added desire based on charge amount
            float targetDesire;
            if (system.getMaxAmmo() <= 2) {
                if (system.getAmmo() <= 1) {
                    targetDesire = 1f;
                } else { // 2
                    targetDesire = 0.5f;
                }
            } else if (system.getMaxAmmo() == 3) {
                if (system.getAmmo() <= 1) {
                    targetDesire = 1.1f;
                } else if (system.getAmmo() == 2) {
                    targetDesire = 0.667f;
                } else { // 3
                    targetDesire = 0.45f;
                }
            } else if (system.getMaxAmmo() == 4) {
                if (system.getAmmo() <= 1) {
                    targetDesire = 1.2f;
                } else if (system.getAmmo() == 2) {
                    targetDesire = 0.8f;
                } else if (system.getAmmo() == 3) {
                    targetDesire = 0.533f;
                } else { // 4
                    targetDesire = 0.4f;
                }
            } else { // 6
                if (system.getAmmo() <= 1) {
                    targetDesire = 1.4f;
                } else if (system.getAmmo() == 2) {
                    targetDesire = 1.033f;
                } else if (system.getAmmo() == 3) {
                    targetDesire = 0.74f;
                } else if (system.getAmmo() == 4) {
                    targetDesire = 0.52f;
                } else if (system.getAmmo() == 5) {
                    targetDesire = 0.373f;
                } else { // 6
                    targetDesire = 0.3f;
                }
            }
            desireShow = desire;
            targetDesireShow = targetDesire;
            if (desire >= targetDesire) {
                ship.useSystem();
            }
        }

        if (DEBUG) {
            if (engine.getPlayerShip() == ship) {
                engine.maintainStatusForPlayerShip(STATUSKEY1, system.getSpecAPI().getIconSpriteName(),
                        "AI", "Desire: " + Math.round(100f * desireShow) + "/" + Math.round(100f * targetDesireShow), desireShow < targetDesireShow);
                engine.maintainStatusForPlayerShip(STATUSKEY2, system.getSpecAPI().getIconSpriteName(),
                        "AI", "Angle: " + Math.round(angleToTargetShow), false);
            }
        }
    }

    @Override
    public void init(ShipAPI ship, ShipSystemAPI system, ShipwideAIFlags flags, CombatEngineAPI engine) {
        this.ship = ship;
        this.flags = flags;
        this.system = system;
        this.engine = engine;
    }
}
