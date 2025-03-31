package data.weapons

import com.fs.starfarer.api.combat.CombatEngineAPI
import com.fs.starfarer.api.combat.DamagingProjectileAPI
import com.fs.starfarer.api.combat.OnFireEffectPlugin
import com.fs.starfarer.api.combat.WeaponAPI
import com.fs.starfarer.api.util.Misc
import org.lazywizard.lazylib.MathUtils
import org.lazywizard.lazylib.ext.plus
import utils.ApexUtils
import java.awt.Color

class ApexInsShotty: OnFireEffectPlugin
{
    val PARTICLE_COLOR = Color(0, 200, 255, 155)

    override fun onFire(projectile: DamagingProjectileAPI, weapon: WeaponAPI, engine: CombatEngineAPI)
    {
        // only add inaccuracy for medium (shotgun)
        if (weapon.size == WeaponAPI.WeaponSize.MEDIUM) projectile.facing += ApexUtils.randBetween(-6f, 6f)
        // do visual fx
        for (i in 0..2)
        {
            engine.addHitParticle(
                projectile.location,
                projectile.source.velocity + MathUtils.getRandomPointInCone(Misc.ZERO, 500f, projectile.facing - 10f, projectile.facing + 10f),
                ApexUtils.randBetween(5f, 10f),
                1f,
                0f,
                0.5f + ApexUtils.randBetween(-0.1f, 0.25f),
                PARTICLE_COLOR
            )
        }
    }
}