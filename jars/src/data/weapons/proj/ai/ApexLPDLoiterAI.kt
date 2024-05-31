package data.weapons.proj.ai

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.*
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import org.lazywizard.lazylib.MathUtils
import org.lazywizard.lazylib.VectorUtils
import org.lazywizard.lazylib.ext.plus
import java.awt.Color
import kotlin.math.abs

class ApexLPDLoiterAI(val missile: MissileAPI, val launchingShip: ShipAPI): MissileAIPlugin, GuidedMissileAI
{
    private var target: CombatEntityAPI? = null
    var offset_timer = IntervalUtil(0.5f, 1.0f)
    val engine = Global.getCombatEngine()
    var loiter_weapon = missile.weapon
    var loiter_offset = Misc.ZERO

    override fun advance(amount: Float)
    {
        if (engine.isPaused) return

        // if our controller weapon is null or our ship is dead, self-destruct the missile
        if (loiter_weapon == null || !launchingShip.isAlive)
            engine.applyDamage(missile, missile.location, missile.hitpoints, DamageType.FRAGMENTATION, 0f, true, false, null, false)

        // update location so it keeps moving around
        offset_timer.advance(amount)
        if (offset_timer.intervalElapsed()) loiter_offset = MathUtils.getRandomPointInCircle(Misc.ZERO, 100f)
        val loiter_point = loiter_offset + missile.weapon.location
        // compute actual target destination

        // okay actually that's only one line

        // rotate missile - if we're kinda close to the location just face the same way the ship is
        var target_angle = 0f
        if (MathUtils.isWithinRange(missile.location, loiter_point, 500f)) {
            // short-range loitering, just face the same way the ship is and strafe around
            target_angle = VectorUtils.getAngle(missile.location, loiter_point)
            val angle_delta = MathUtils.getShortestRotation(missile.facing, target_angle)

            val rotate_delta = MathUtils.getShortestRotation(missile.facing, missile.source.facing)
            if (rotate_delta > 0) missile.giveCommand(ShipCommand.TURN_LEFT) else missile.giveCommand(ShipCommand.TURN_RIGHT)
            if (angle_delta > 0) missile.giveCommand(ShipCommand.STRAFE_LEFT) else missile.giveCommand(ShipCommand.STRAFE_RIGHT)
            if (abs(angle_delta) < abs(missile.angularVelocity) * 0.1f) missile.angularVelocity = angle_delta * 10f
            // reduce speed if we're close to the loiter point
            missile.engineStats.maxSpeed.modifyMult("apex_loiter", 0.25f)
            missile.engineStats.acceleration.modifyMult("apex_loiter", 0.25f)

            // translate missile
            // we always strafe regardless of angle (rotation will sort itself out)
            if (abs(angle_delta) < 45f) // if it's in front of us
                missile.giveCommand(ShipCommand.ACCELERATE)
            else if (abs(angle_delta) > 135f) // if it's behind us
                missile.giveCommand(ShipCommand.ACCELERATE_BACKWARDS)
        } else
        {
            missile.engineStats.maxSpeed.unmodify("apex_loiter")
            missile.engineStats.acceleration.unmodify("apex_loiter")
            // this is for long-range flight, face towards destination and crank the engine
            target_angle = VectorUtils.getAngle(missile.location, loiter_point)
            val angle_delta = MathUtils.getShortestRotation(missile.facing, target_angle)
            if (angle_delta > 0) missile.giveCommand(ShipCommand.TURN_LEFT) else missile.giveCommand(ShipCommand.TURN_RIGHT)
            if (abs(angle_delta) < abs(missile.angularVelocity) * 0.1f) missile.angularVelocity = angle_delta * 10f
            // translate missile
            // we always strafe regardless of angle (rotation will sort itself out)
            if (abs(angle_delta) < 45f) // if it's in front of us
                missile.giveCommand(ShipCommand.ACCELERATE)
            else if (abs(angle_delta) > 135f) // if it's behind us
                missile.giveCommand(ShipCommand.ACCELERATE_BACKWARDS)
        }

        //if (angle_delta > 0f) // if it's to our left(?)
        //    missile.giveCommand(ShipCommand.STRAFE_LEFT)
        //else if (angle_delta < 0f)
        //    missile.giveCommand(ShipCommand.STRAFE_RIGHT)

    }

    override fun getTarget(): CombatEntityAPI?
    {
        return target
    }

    override fun setTarget(target: CombatEntityAPI?)
    {
        this.target = target
    }
}