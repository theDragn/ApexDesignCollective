package data.weapons

import com.fs.starfarer.api.combat.*
import kotlin.math.max
import kotlin.math.min

// this handles the animation frames and the explosion vfx plugin
class ApexInsRotaryFlak: EveryFrameWeaponEffectPlugin, OnFireEffectPlugin
{
    private var fired = false
    private var modifiedCooldown = 0f
    private var modifiedCooldownLeft = 0f
    private var spool = 5f

    override fun advance(amount: Float, engine: CombatEngineAPI?, weapon: WeaponAPI)
    {
        if (fired && spool < 5f)
        {
            fired = false
            val baseCooldown = weapon.cooldown
            modifiedCooldown = baseCooldown + (baseCooldown * spool/5f)
            modifiedCooldownLeft = modifiedCooldown
        }
        if (modifiedCooldownLeft > 0)
        {
            // Cooldown is off for some reason compared to unmodified weapon cooldown
            if (modifiedCooldownLeft < amount * 3f)
            {
                modifiedCooldownLeft = 0f
                weapon.setRemainingCooldownTo(0f)
            } else
            {
                modifiedCooldownLeft -= amount * weapon.ship.mutableStats.ballisticRoFMult.modifiedValue
                weapon.setRemainingCooldownTo(modifiedCooldownLeft / modifiedCooldown)
            }
        }
        spool = min(spool + amount * 1.5f, 5f)
    }


    override fun onFire(projectile: DamagingProjectileAPI, weapon: WeaponAPI, engine: CombatEngineAPI)
    {
        fired = true
        spool = max(spool - 2f * weapon.ship.mutableStats.ballisticRoFMult.modifiedValue, 0f)
        val plugin = ApexInsFlakExp(projectile)
        engine.addPlugin(plugin)
    }
}