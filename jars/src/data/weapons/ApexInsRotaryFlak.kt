package data.weapons

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.*
import kotlin.math.max
import kotlin.math.min

// this handles the animation frames and the explosion vfx plugin
class ApexInsRotaryFlak: EveryFrameWeaponEffectPlugin, OnFireEffectPlugin
{
    companion object
    {
        const val loop_end = "apex_rotary_flak_end"
        const val loop = "apex_rotary_flak_loop"
    }

    private var firingLastFrame = false

    override fun advance(amount: Float, engine: CombatEngineAPI?, weapon: WeaponAPI)
    {
        if (weapon.chargeLevel > 0 || firingLastFrame)
            Global.getSoundPlayer().playLoop(loop, weapon, 1f, 1f, weapon.location, weapon.ship.velocity, 0f, 0.2f)
        if (weapon.chargeLevel == 0f && firingLastFrame)
        {
            firingLastFrame = false
            Global.getSoundPlayer().playSound(loop_end, 1f, 1f, weapon.location, weapon.ship.location)
        }
        if (weapon.chargeLevel > 0)
            firingLastFrame = true
    }


    override fun onFire(projectile: DamagingProjectileAPI, weapon: WeaponAPI, engine: CombatEngineAPI)
    {
        val plugin = ApexInsFlakExp(projectile)
        engine.addPlugin(plugin)
    }
}