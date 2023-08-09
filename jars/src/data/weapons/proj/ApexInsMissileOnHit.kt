package data.weapons.proj

import com.fs.starfarer.api.combat.CombatEngineAPI
import com.fs.starfarer.api.combat.CombatEntityAPI
import com.fs.starfarer.api.combat.DamagingProjectileAPI
import com.fs.starfarer.api.combat.OnHitEffectPlugin
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI
import com.fs.starfarer.api.impl.combat.BreachOnHitEffect
import org.lwjgl.util.vector.Vector2f

class ApexInsMissileOnHit: OnHitEffectPlugin
{
    override fun onHit(
        projectile: DamagingProjectileAPI?,
        target: CombatEntityAPI?,
        point: Vector2f?,
        shieldHit: Boolean,
        damageResult: ApplyDamageResultAPI?,
        engine: CombatEngineAPI?
    )
    {
        projectile ?: return
        point ?: return
        if (target !is ShipAPI) return
        if (shieldHit) return
        // lmao this is so much easier
        BreachOnHitEffect.dealArmorDamage(projectile, target, point, projectile.damageAmount * (0.25f))
    }
}