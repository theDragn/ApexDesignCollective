package data.weapons.proj.ai

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.*
import com.fs.starfarer.api.util.Misc
import org.lazywizard.lazylib.MathUtils
import org.lazywizard.lazylib.VectorUtils
import org.lazywizard.lazylib.combat.AIUtils
import org.lazywizard.lazylib.ext.getAngle
import org.lazywizard.lazylib.ext.getFacing
import org.lwjgl.util.vector.Vector2f
import java.awt.Color
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

// a kotlin adaptation (and improvement) of magiclib missile AI
class ApexLPDMissileAI(val missile: MissileAPI, val launchingShip: ShipAPI): MissileAIPlugin, GuidedMissileAI
{
    // SETTINGS
    companion object
    {
        // heavily modified from other missile scripts
        // don't start with this if you're looking for a PD missile script to steal, ping me and I will explain it

        const val EXPLOSION_RANGE = 50f
        const val EXPLOSION_ARC = 60f
        val EXPLOSION_COLOR = Color(255,225,125,255)
        val PARTICLE_COLOR = Color(255,225,125,255)
        // cuts forward thrust if facing vector is too far from target vector
        // set to -1 to disable (which will mean the engine is always on)
        const val ENGINE_OFF_ANGLE = 30f

        // if DOESN'T have ECCM, multiplies target velocity by this much when predicting intercept
        const val ECCM_VEL_FACTOR = 1f

        // magic rotation damping factor. smaller numbers cause more aggressive snapping to target angles and allow higher turn speeds.
        // too small and it'll be jerky. 0.5 to 0.1 is usually a good number.
        const val DAMPING_FACTOR = 0.1f

        // engages lateral velocity matching at this range; set to -1 to disable
        // produces *extremely* accurate guidance
        const val LATERAL_MATCHING_DIST = 1500

        // missile begins increasing its check rate and decreasing its wave amplitude at this distance
        // 500 is fine for most purposes, but increase it if you have very fast missiles
        const val HIGH_PERFORMANCE_RANGE = 750f

        // does missile predict an intercept point or just point right at the target?
        const val PREDICTIVE_INTERCEPT = true
    }


    private val MAX_SPEED = missile.maxSpeed
    private val ECCM = missile.source.variant.hullMods.contains("eccm")
    private val engine: CombatEngineAPI = Global.getCombatEngine()
    private var target: CombatEntityAPI? = null
    private var timer = 0f
    private var check = 0f
    private var lead: Vector2f? = Vector2f()
    private var side_offset = 0f
    private var vector_offset = Vector2f()

    override fun advance(amount: Float)
    {
        if (engine == null || engine.isPaused || missile.isFading || missile.isFizzling) return
        // if we don't have a target or our target died, return to loitering
        if (target == null || (target is ShipAPI && !(target as ShipAPI).isAlive) || !engine.isEntityInPlay(target))
        {
            missile.missileAI = ApexLPDLoiterAI(missile, missile.source)
            (missile.unwrappedMissileAI as ApexLPDLoiterAI).loiter_weapon = missile.weapon
            val plugin = engine.customData["apex_LPDS"] as ApexLPDSystem
            plugin.available_missiles.addLast(missile)
            return
        }
        missile.engineStats.maxSpeed.unmodify("apex_loiter")
        missile.engineStats.acceleration.unmodify("apex_loiter")
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
        val aimAngle = MathUtils.getShortestRotation(missile.facing, correctAngle)

        // do flak explosion if target is within a 60deg cone that's 50 units long
        if (targetDist < 2500 && abs(aimAngle) < 30) explode()

        if (ENGINE_OFF_ANGLE <= 0 || abs(aimAngle) < ENGINE_OFF_ANGLE)
            missile.giveCommand(ShipCommand.ACCELERATE)
        // no turning if target is phased and we don't have eccm
        if (target is ShipAPI && (target as ShipAPI).isPhased && !ECCM)
            return
        if (aimAngle > 0) missile.giveCommand(ShipCommand.TURN_LEFT) else missile.giveCommand(ShipCommand.TURN_RIGHT)

        // strafing to zero out lateral velocity
        val vel_angle = missile.velocity.getFacing()
        val vel_aim_angle = MathUtils.getShortestRotation(vel_angle, correctAngle)
        if (vel_aim_angle > 10f) missile.giveCommand(ShipCommand.STRAFE_LEFT) else if (vel_aim_angle < -10f) missile.giveCommand(ShipCommand.STRAFE_RIGHT)

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
        if (target !is CombatEntityAPI) return
    }

    // flak explosion
    fun explode()
    {
        // play sound
        Global.getSoundPlayer().playSound("devastator_explosion", 1.0f, 1.0f, missile.location, Misc.ZERO)
        // get targets in cone and damage them
        // using collision grid here because it's probably a lot faster for small areas
        for (entity in engine.allObjectGrid.getCheckIterator(missile.location, EXPLOSION_RANGE * 2f, EXPLOSION_RANGE * 2f))
        {
            // type checks: only damage enemy missiles and fighters
            if (entity !is CombatEntityAPI) continue
            if (entity.owner == missile.owner) continue
            if (entity !is MissileAPI && entity !is ShipAPI) continue
            if (entity is ShipAPI && entity.hullSize != ShipAPI.HullSize.FIGHTER) continue
            if (!MathUtils.isWithinRange(missile, entity, EXPLOSION_RANGE)) continue
            // damage it
            engine.applyDamage(entity, entity.location, missile.damageAmount, DamageType.FRAGMENTATION, 0f, false, false, missile.source)
        }
        // do vfx
        for (i in 0..20)
        {
            val axis = Math.random().toFloat() * 360
            val range = Math.random().toFloat() * 100
            engine.addHitParticle(
                MathUtils.getPoint(missile.location, range * 0.2f, axis),
                MathUtils.getPoint(Vector2f(), range, axis),
                7 + Math.random().toFloat() * 4,
                1f,
                1 + Math.random().toFloat(),
                PARTICLE_COLOR
            )
        }
        engine.addHitParticle(missile.location, Misc.ZERO, 150f, 1f, 0.25f, EXPLOSION_COLOR)
        // destroy missile
        // engine.applyDamage(missile, missile.location, missile.hitpoints, DamageType.FRAGMENTATION, 0f, true, true, missile)
        engine.removeEntity(missile)
    }


}