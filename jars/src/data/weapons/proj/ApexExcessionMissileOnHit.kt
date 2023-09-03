package data.weapons.proj

import com.fs.starfarer.api.combat.CombatEngineAPI
import com.fs.starfarer.api.combat.CombatEntityAPI
import com.fs.starfarer.api.combat.DamagingProjectileAPI
import com.fs.starfarer.api.combat.OnHitEffectPlugin
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI
import com.fs.starfarer.api.util.Misc
import org.lwjgl.util.vector.Vector2f
import java.awt.Color

class ApexExcessionMissileOnHit: OnHitEffectPlugin
{

    override fun onHit(
        projectile: DamagingProjectileAPI,
        target: CombatEntityAPI?,
        point: Vector2f?,
        shieldHit: Boolean,
        damageResult: ApplyDamageResultAPI,
        engine: CombatEngineAPI
    )
    {
        if (target is ShipAPI && !shieldHit && Misc.random.nextFloat() < 0.25f)
        {
            val core_color = Misc.setAlpha(Color.WHITE, 150)
            val fringe_color = Color(0,157,255,150)
            engine.spawnEmpArc(
                projectile.source,
                point,
                target,
                target,
                projectile.damageType,
                projectile.damageAmount,
                projectile.empAmount,
                9999f,
                "tachyon_lance_emp_impact",
                20f,
                fringe_color,
                core_color)
        }
    }
}