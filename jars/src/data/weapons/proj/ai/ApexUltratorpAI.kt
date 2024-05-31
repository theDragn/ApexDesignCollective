package data.weapons.proj.ai

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.*
import com.fs.starfarer.api.combat.BoundsAPI.SegmentAPI
import com.fs.starfarer.api.util.Misc
import org.lazywizard.lazylib.FastTrig
import org.lazywizard.lazylib.MathUtils
import org.lazywizard.lazylib.VectorUtils
import org.lazywizard.lazylib.combat.AIUtils
import org.lazywizard.lazylib.ext.rotate
import org.lazywizard.lazylib.ext.rotateAroundPivot
import org.lwjgl.util.vector.Vector2f
import org.magiclib.util.MagicTargeting
import utils.ApexUtils
import utils.ApexUtils.lerp
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

// a kotlin adaptation (and improvement) of magiclib missile AI
class ApexUltratorpAI(val missile: MissileAPI, val launchingShip: ShipAPI): MissileAIPlugin, GuidedMissileAI
{
    // SETTINGS
    companion object
    {
        // most features turn off if you set them to -1
        // don't set boolean values to -1 though, that would be bad

        // accelerate if there's no target (most missiles do this)
        const val ENGINE_ON_WITH_NO_TARGET = true

        // cuts forward thrust if facing vector is too far from target vector
        // set to -1 to disable (which will mean the engine is always on)
        const val ENGINE_OFF_ANGLE = -1f

        // time for a full wave (center, then left, then right, then back to center), in seconds. <=0 disables
        const val WAVE_TIME = -1f

        // max angle of wave from target angle. <= 0 disables (but you should disable it with WAVE_TIME instead)
        // this should be less than ENGINE_OFF_ANGLE
        const val WAVE_SIZE = -1f

        // disables waving at this distance.
        // make it zero to wave until impact
        const val WAVE_CUTOFF_DIST = 30f

        // self-explanatory. random offset for waving so not all missiles follow the same path
        const val WAVE_RANDOM = false

        // if it's got ECCM, multiplies wave size by this much
        const val WAVE_ECCM_FACTOR = 0.5f

        // if DOESN'T have ECCM, multiplies target velocity by this much when predicting intercept
        const val ECCM_VEL_FACTOR = 0.66f

        // magic rotation damping factor. smaller numbers cause more aggressive snapping to target angles and allow higher turn speeds.
        // too small and it'll be jerky. 0.5 to 0.1 is usually a good number.
        const val DAMPING_FACTOR = 0.1f

        // engages lateral velocity matching at this range; set to -1 to disable
        // produces *extremely* accurate guidance
        const val LATERAL_MATCHING_DIST = -1

        // enables spread targeting- missiles will aim some random distance off of the target's center, like breaches
        // <=0 is off.  >0 controls the amount of spread (ex, 0.5 means maximum spread is 0.5 * target's collision radius)
        // >1 means they'll never hit without using the spread shrink variables below
        const val SPREAD_AMOUNT = -1f

        // begins to reduce the spread amount after this distance from target
        // good for making missiles spread out mid-flight and the collapse back on the target
        // effectively just multiplies the spread amount by lerp(shrink_factor, 0, max(dist_to_target / shrink_dist, 1))
        const val SPREAD_SHRINK_FACTOR = -1f
        const val SPREAD_SHRINK_DIST = -1f

        // missile begins increasing its check rate and decreasing its wave amplitude at this distance
        // 500 is fine for most purposes, but increase it if you have very fast missiles
        const val HIGH_PERFORMANCE_RANGE = 500f

        // targeting options
        // NO_RANDOM: ship target first, then unselected target near cursor, then closest valid target
        // LOCAL_RANDOM: picks random targets around ship target, then around cursor, then around itself
        // FULL_RANDOM: self-explanatory
        // IGNORE_SOURCE: picks the closest target
        val SEEKING = MagicTargeting.targetSeeking.NO_RANDOM
        // ship class targeting priorities for random options
        const val FIGHTERS = 0
        const val FRIGATES = 1
        const val DESTROYERS = 1
        const val CRUISERS = 1
        const val CAPITALS = 1

        // search cone for new target selection, >= 360 disables cone targeting (should be an int, not a float)
        const val SEARCH_CONE = 360

        // self-explanatory (should be an int, not a float)
        const val SEARCH_RANGE = 5000

        // will cause missiles to target the closest target if no valid one can be found with search parameters
        const val SEARCH_FAILSAFE = false

        // does missile predict an intercept point or just point right at the target?
        const val PREDICTIVE_INTERCEPT = true
    }


    private val OFFSET = if (WAVE_RANDOM) Misc.random.nextFloat() * MathUtils.FPI * 2f else 0f
    private val MAX_SPEED = missile.maxSpeed
    private val ECCM = missile.source.variant.hullMods.contains("eccm")
    private val engine: CombatEngineAPI? = Global.getCombatEngine()
    private var target: CombatEntityAPI? = null
    private var timer = 0f
    private var check = 0f
    private var lead: Vector2f? = Vector2f()
    private var side_offset = 0f
    private var vector_offset = Vector2f()
    private val WAVE_TIME_CONST = 2f * MathUtils.FPI / WAVE_TIME

    override fun advance(amount: Float)
    {
        if (engine == null || engine.isPaused || missile.isFading || missile.isFizzling) return
        // get a target if there isn't one, or current target is dead
        if (target == null || (target is ShipAPI && !(target as ShipAPI).isAlive) || !engine.isEntityInPlay(target))
        {
            setTarget(MagicTargeting.pickTarget(
                missile,
                SEEKING,
                SEARCH_RANGE * SEARCH_RANGE,
                SEARCH_CONE,
                FIGHTERS, FRIGATES, DESTROYERS, CRUISERS, CAPITALS,
                SEARCH_FAILSAFE
            ))
            if (ENGINE_ON_WITH_NO_TARGET) missile.giveCommand(ShipCommand.ACCELERATE)

            return
        }
        timer += amount
        // do expensive angle and distance calculations on a timer
        val targetDist = MathUtils.getDistanceSquared(missile.location, target!!.location)
        if (timer >= check)
        {
            // math is a little fancy but: timer is decreased as missile
            check = min(0.25f, max(
                0.05f,
                0.2f * targetDist / (HIGH_PERFORMANCE_RANGE * HIGH_PERFORMANCE_RANGE)
            )
            )
            var targetLoc = Vector2f(target!!.location)
            if (SPREAD_AMOUNT > 0)
            {
                //targetLoc = Vector2f(vector_offset)
                //targetLoc.rotate(target!!.facing)
                //Vector2f.add(targetLoc,target!!.location, targetLoc)
                var offset_mult = 1f
                if (SPREAD_SHRINK_FACTOR > 0)
                    offset_mult = lerp(SPREAD_SHRINK_FACTOR, 1f, min(sqrt(targetDist) / SPREAD_SHRINK_DIST, 1f))

                targetLoc = Vector2f(0f, side_offset * offset_mult).rotate(VectorUtils.getAngle(missile.location, target!!.location))
                Vector2f.add(target!!.location, targetLoc, targetLoc)
                //engine.addSmoothParticle(targetLoc, Misc.ZERO, 10f, 1f, 0.1f, Color.CYAN)
            }
            lead = if (PREDICTIVE_INTERCEPT)
            {

                AIUtils.getBestInterceptPoint(
                    missile.location,
                    MAX_SPEED * (if (ECCM) ECCM_VEL_FACTOR else 1f),
                    targetLoc,
                    target!!.velocity)

            } else
                Vector2f(targetLoc)
            lead = lead ?: targetLoc
        }
        if (missile.location == null || lead == null) return // idk but someone got an NPE on the next line so let's make sure
        var correctAngle = VectorUtils.getAngle(missile.location, lead)
        if (WAVE_TIME > 0)
        {
            var amplitude_mult = 1f
            if (ECCM) amplitude_mult *= WAVE_ECCM_FACTOR
            if (WAVE_CUTOFF_DIST > 0)
            {
                if (targetDist < WAVE_CUTOFF_DIST * WAVE_CUTOFF_DIST)
                    amplitude_mult = 0f
            }
            correctAngle += amplitude_mult * WAVE_SIZE * (4f * check) *
                    FastTrig.cos((OFFSET + missile.elapsed * WAVE_TIME_CONST).toDouble()).toFloat()
        }
        val aimAngle = MathUtils.getShortestRotation(missile.facing, correctAngle)
        if (ENGINE_OFF_ANGLE <= 0 || abs(aimAngle) < ENGINE_OFF_ANGLE)
            missile.giveCommand(ShipCommand.ACCELERATE)
        // no turning if target is phased and we don't have eccm
        if (target is ShipAPI && (target as ShipAPI).isPhased && !ECCM)
            return
        if (aimAngle > 0) missile.giveCommand(ShipCommand.TURN_LEFT) else missile.giveCommand(ShipCommand.TURN_RIGHT)
        if (LATERAL_MATCHING_DIST > 0)
        {
            // missile velocity - target velocity

            // missile velocity - target velocity
            val relativeVelToTarget = Vector2f.sub(missile.velocity, target!!.velocity, null)
            // relative velocity angle; 0 deg is "relative velocity is directly forwards from missile facing"
            val relativeVelAngle =
                MathUtils.getShortestRotation(VectorUtils.getAngle(Misc.ZERO, relativeVelToTarget), missile.facing)
            // only begin strafing to zero out relative velocities if we're close to the target
            // this prevents clumping up early on, helping missiles converge from multiple directions and overwhelm PD
            if (targetDist < LATERAL_MATCHING_DIST * LATERAL_MATCHING_DIST)
            {
                // this could be zero degrees instead of 10, it would work fine
                // but we leave a little bit of wiggle room to get some missiles to miss very fast-maneuvering targets on their first pass
                // really amps up the feel if a few miss and then come right back around to smack em
                if (relativeVelAngle < -10f)
                    missile.giveCommand(ShipCommand.STRAFE_RIGHT)
                else if (relativeVelAngle > 10f)
                    missile.giveCommand(ShipCommand.STRAFE_LEFT)
            }
        }
        if (abs(aimAngle) < abs(missile.angularVelocity) * DAMPING_FACTOR)
            missile.angularVelocity = aimAngle / DAMPING_FACTOR
    }

    override fun getTarget(): CombatEntityAPI?
    {
        return target
    }

    override fun setTarget(newTarget: CombatEntityAPI?)
    {
        target = newTarget
        if (target !is ShipAPI || SPREAD_AMOUNT <= 0) return
        // yeah look I had an NPE here and don't want to deal with it
        target ?: return; target?.exactBounds ?: return; target?.location ?: return; target?.facing ?: return
        target!!.exactBounds.update(target!!.location, target!!.facing)
        vector_offset = ApexUtils.getRandomPointOnShipBounds(target as ShipAPI)
        Vector2f.sub(vector_offset, target!!.location, vector_offset)
        vector_offset.rotate(-target!!.facing)

        side_offset = (2f * Misc.random.nextFloat() - 1f) * target!!.collisionRadius * SPREAD_AMOUNT
    }


}