package data.weapons.proj

import com.fs.starfarer.api.combat.*
import com.fs.starfarer.api.loading.DamagingExplosionSpec
import java.awt.Color

class ApexUltrabombExp: OnFireEffectPlugin, EveryFrameWeaponEffectPlugin
{
    val projs = mutableListOf<DamagingProjectileAPI>()

    override fun onFire(projectile: DamagingProjectileAPI, weapon: WeaponAPI, engine: CombatEngineAPI)
    {
        projs.add(projectile)
    }

    override fun advance(amount: Float, engine: CombatEngineAPI, weapon: WeaponAPI)
    {
        val projIter = projs.iterator()
        while (projIter.hasNext())
        {
            val proj = projIter.next()
            // KABLOOIE
            if (proj.isFading || (proj as MissileAPI).flightTime > proj.maxFlightTime)
            {
                ApexUltrabombEffect.explodeVFX(engine, proj.location)
                val spec = DamagingExplosionSpec(
                    0.1f,
                    200f,
                    100f,
                    proj.damageAmount,
                    proj.damageAmount * 0.5f,
                    CollisionClass.PROJECTILE_NO_FF,
                    CollisionClass.PROJECTILE_FIGHTER,
                    1f,
                    10f,
                    0.2f,
                    20,
                    Color.WHITE,
                    null
                )
                spec.damageType = DamageType.ENERGY
                spec.isShowGraphic = false
                engine.spawnDamagingExplosion(spec, proj.getSource(), proj.location, false)
                engine.removeEntity(proj)
                projIter.remove()
            } else if (proj.hitpoints <= 0 || !engine.isEntityInPlay(proj))
            {
                projIter.remove()
            }
        }
    }
}