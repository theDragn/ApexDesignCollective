package data.weapons.proj

import com.fs.starfarer.api.combat.*
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI
import com.fs.starfarer.api.util.Misc
import org.lwjgl.util.vector.Vector2f
import utils.ApexUtils.lerp

class ApexInsMHDOnHit: OnHitEffectPlugin
{
    override fun onHit(
        proj: DamagingProjectileAPI?,
        target: CombatEntityAPI?,
        point: Vector2f?,
        shieldHit: Boolean,
        damageResult: ApplyDamageResultAPI?,
        engine: CombatEngineAPI?
    )
    {
        proj ?: return; engine ?: return; point ?: return
        if (target !is ShipAPI) return
        if (!shieldHit) return
        val baseDam = proj.damage.damage
        val shieldMult = target.shield.fluxPerPointOfDamage * target.mutableStats.shieldDamageTakenMult.modifiedValue
        if (shieldMult > 1f) return
        // if the shield takes full damage, no need to add hardflux
        var toDeal = lerp(0.5f, 1f, shieldMult) * baseDam - baseDam * shieldMult
        toDeal *= 2f * target.mutableStats.kineticShieldDamageTakenMult.modifiedValue
        if (toDeal < 0) return
        target.fluxTracker.increaseFlux(toDeal , true)
        engine.addFloatingDamageText(point, toDeal, Misc.FLOATY_SHIELD_DAMAGE_COLOR, target, proj.source)

        // TODO: some vfx?
    }
}