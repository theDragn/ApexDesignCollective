package data.weapons.proj

import com.fs.starfarer.api.combat.*
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI
import org.lwjgl.util.vector.Vector2f

class ApexInsTorpOnHit: OnHitEffectPlugin
{
    companion object
    {
        const val DAMAGE = 400f / 800f
    }

    override fun onHit(
        proj: DamagingProjectileAPI?,
        target: CombatEntityAPI?,
        point: Vector2f?,
        shieldHit: Boolean,
        damageResult: ApplyDamageResultAPI?,
        engine: CombatEngineAPI?
    )
    {
        proj ?: return; target ?: return; point ?: return; engine ?: return; if (target !is ShipAPI) return
        engine.applyDamage(target, point, DAMAGE * proj.damageAmount, DamageType.FRAGMENTATION, 0f, false, false, proj.source)
    }
}