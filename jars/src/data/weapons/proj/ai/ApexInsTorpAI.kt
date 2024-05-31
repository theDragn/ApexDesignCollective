package data.weapons.proj.ai

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.*
import com.fs.starfarer.api.graphics.SpriteAPI
import com.fs.starfarer.api.util.Misc
import org.lazywizard.lazylib.MathUtils
import org.lazywizard.lazylib.VectorUtils
import org.lazywizard.lazylib.combat.AIUtils
import org.lwjgl.util.vector.Vector2f
import org.magiclib.util.MagicRender
import org.magiclib.util.MagicTargeting
import kotlin.math.abs

class ApexInsTorpAI(var missile: MissileAPI, var ship: ShipAPI): GuidedMissileAI, MissileAIPlugin
{
    val DAMPING_FACTOR = 0.075f
    val MAX_ECCM_TURNING = 4f
    var guidanceTarget: CombatEntityAPI? = null
    var doneVFX = false
    var aimTimer = 1f
    var aimOffset = Misc.random.nextFloat() * 6f - 3f
    var eccm = ship.variant.hasHullMod("eccm")
    val missile_sprite: SpriteAPI = Global.getSettings().getSprite("fx","apex_shrieker_sprite")
    val sprite_size = Vector2f(9f, 13f)
    init
    {
        if (missile.weapon.size == WeaponAPI.WeaponSize.LARGE) aimOffset *= 1.5f
        guidanceTarget = ship.shipTarget ?: MagicTargeting.pickTarget(
            missile,
            MagicTargeting.targetSeeking.NO_RANDOM,
            1600,
            180,
            0,
            1,
            2,
            3,
            3,
            false
        )
    }
    override fun getTarget(): CombatEntityAPI?
    {
        return guidanceTarget
    }

    override fun setTarget(target: CombatEntityAPI?)
    {
        this.guidanceTarget = target
    }

    override fun advance(amount: Float)
    {
        // hides the original missile sprite (modified to fit in the launcher) and renders the real one
        missile.spriteAlphaOverride = 0f
        MagicRender.singleframe(missile_sprite, missile.location, sprite_size, missile.facing - 90, missile_sprite.color, true)
        // if target's died or stopped existing since missile launch, retarget
        if (guidanceTarget is ShipAPI && !(guidanceTarget as ShipAPI).isAlive || guidanceTarget == null)
            guidanceTarget = MagicTargeting.pickTarget(
                missile,
                MagicTargeting.targetSeeking.NO_RANDOM,
                1600,
                360,
                0,
                1,
                2,
                3,
                3,
                false
            )
        guidanceTarget ?: return
        guidanceTarget?.location ?: return
        if (aimTimer > 0f)
        {
            aimTimer -= amount
            var intercept = AIUtils.getBestInterceptPoint(missile.location, missile.maxSpeed, guidanceTarget?.location, guidanceTarget?.velocity)
            intercept = intercept ?: guidanceTarget?.location
            val aimAngle = VectorUtils.getAngle(missile.location, intercept) + aimOffset
            val angleDelta = MathUtils.getShortestRotation(missile.facing, aimAngle)
            if (angleDelta < 0) missile.giveCommand(ShipCommand.TURN_RIGHT)
            else if (angleDelta > 0) missile.giveCommand(ShipCommand.TURN_LEFT)
            if (abs(angleDelta) < abs(missile.angularVelocity) * DAMPING_FACTOR)
                missile.angularVelocity = angleDelta / DAMPING_FACTOR
        }
        if (aimTimer < 0.5f && aimTimer > 0f) missile.giveCommand(ShipCommand.DECELERATE)
        if (aimTimer < 0f && !doneVFX)
        {
            doneVFX = true
            aimTimer = 0f
            val engineLoc = MathUtils.getPointOnCircumference(missile.location, 7f, missile.facing + 180f)
            missile.angularVelocity = 0f
        }
        if (aimTimer <= 0f && eccm)
        {
            var intercept = AIUtils.getBestInterceptPoint(missile.location, missile.maxSpeed, guidanceTarget?.location, guidanceTarget?.velocity)
            intercept = intercept ?: guidanceTarget?.location
            val aimAngle = VectorUtils.getAngle(missile.location, intercept) + aimOffset * 1.2f
            val angleDelta = MathUtils.getShortestRotation(missile.facing, aimAngle)
            if (angleDelta < 0) missile.giveCommand(ShipCommand.TURN_RIGHT)
            else if (angleDelta > 0) missile.giveCommand(ShipCommand.TURN_LEFT)
            if (abs(angleDelta) < abs(missile.angularVelocity) * DAMPING_FACTOR)
                missile.angularVelocity = angleDelta / DAMPING_FACTOR
            if (missile.angularVelocity > MAX_ECCM_TURNING) missile.angularVelocity = MAX_ECCM_TURNING
            if (missile.angularVelocity < -MAX_ECCM_TURNING) missile.angularVelocity = -MAX_ECCM_TURNING
            missile.giveCommand(ShipCommand.ACCELERATE)
        }
        if (aimTimer <= 0f) missile.giveCommand(ShipCommand.ACCELERATE)
    }
}