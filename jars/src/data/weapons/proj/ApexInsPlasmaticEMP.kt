package data.weapons.proj

import com.fs.starfarer.api.combat.*
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI
import com.fs.starfarer.api.util.Misc
import org.lazywizard.lazylib.MathUtils
import org.lwjgl.util.vector.Vector2f
import utils.ApexUtils
import java.awt.Color

class ApexInsPlasmaticEMP: OnHitEffectPlugin
{
    override fun onHit(
        proj: DamagingProjectileAPI,
        target: CombatEntityAPI?,
        point: Vector2f,
        shieldHit: Boolean,
        damageResult: ApplyDamageResultAPI,
        engine: CombatEngineAPI
    )
    {
        if (!shieldHit && target is ShipAPI && Misc.random.nextFloat() <= 0.5f)
        {
            engine.spawnEmpArc(
                proj.source, point, target, target,
                DamageType.ENERGY,
                0f,
                proj.empAmount,  // emp
                100000f,  // max range
                "tachyon_lance_emp_impact",
                20f,  // thickness
                proj.projectileSpec.coreColor,
                Color.WHITE
            )
        }
        for (i in 1..3)
        {
            engine.spawnEmpArcVisual(
                proj.location,
                null,
                MathUtils.getRandomPointInCircle(proj.location, 80f),
                null,
                1f,
                proj.projectileSpec.coreColor,
                Color.white
            )
        }
    }
}