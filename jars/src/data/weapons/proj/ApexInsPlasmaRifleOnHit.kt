package data.weapons.proj

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.*
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI
import com.fs.starfarer.api.input.InputEventAPI
import com.fs.starfarer.api.loading.DamagingExplosionSpec
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import org.lazywizard.lazylib.MathUtils
import org.lwjgl.util.vector.Vector2f
import utils.ApexUtils
import java.awt.Color

class ApexInsPlasmaRifleOnHit: OnHitEffectPlugin
{
    companion object
    {
        val color1 = Color(130, 4, 189, 75)
        val color2 = Color(255, 10, 255, 75)
        val exp_radius = 100f
    }

    override fun onHit(
        projectile: DamagingProjectileAPI,
        target: CombatEntityAPI,
        point: Vector2f,
        shieldHit: Boolean,
        damageResult: ApplyDamageResultAPI,
        engine: CombatEngineAPI
    )
    {
        engine.addPlugin(ApexInsPlasmaField(projectile))
    }

    class ApexInsPlasmaField(val proj: DamagingProjectileAPI): BaseEveryFrameCombatPlugin()
    {
        val damageTimer = IntervalUtil(0.15f, 0.45f)
        val fxTimer = IntervalUtil(0.05f, 0.1f)
        val damage = proj.damageAmount * 0.1f
        var exp_left = 5

        override fun advance(amount: Float, events: MutableList<InputEventAPI>)
        {
            if (Global.getCombatEngine().isPaused) return

            damageTimer.advance(amount)
            fxTimer.advance(amount)

            if (damageTimer.intervalElapsed())
            {
                exp_left -= 1
                val spec = DamagingExplosionSpec(
                    0.05f,
                    exp_radius,
                    exp_radius,
                    damage,
                    damage,
                    CollisionClass.PROJECTILE_NO_FF,
                    CollisionClass.PROJECTILE_FIGHTER,
                    1f,
                    10f,
                    0.2f,
                    1,
                    Color.WHITE,
                    null
                )
                spec.damageType = DamageType.ENERGY
                spec.isShowGraphic = false
                Global.getCombatEngine().spawnDamagingExplosion(spec, proj.source, proj.location, false)
                Global.getSoundPlayer().playSound("system_emp_emitter_impact", 1f, 0.7f, proj.location, Misc.ZERO)
            }

            if (fxTimer.intervalElapsed())
            {
                // do vfx
                for (i in 1..3)
                {
                    val radius = ApexUtils.randBetween(10f, exp_radius);
                    val baseangle = ApexUtils.randBetween(0f, 360f);
                    val point1 = MathUtils.getPointOnCircumference(proj.location, radius, baseangle - 15f)
                    val point2 = MathUtils.getPointOnCircumference(proj.location, radius, baseangle + 15f)

                    val empcolor = Misc.interpolateColor(color1, color2, Misc.random.nextFloat())
                    Global.getCombatEngine().spawnEmpArcVisual(point1, null, point2, null, 5f, empcolor, empcolor)
                    Global.getCombatEngine().addSwirlyNebulaParticle(
                        MathUtils.getRandomPointInCircle(proj.location, exp_radius/2),
                        MathUtils.getRandomPointInCircle(Misc.ZERO, 50f),
                        20f,
                        4f,
                        0.5f,
                        0.5f,
                        1f,
                        empcolor,
                        true
                    )
                }
                for (i in 0..3)
                {
                    val empcolor = Misc.interpolateColor(color1, color2, Misc.random.nextFloat())
                    Global.getCombatEngine().spawnEmpArcVisual(
                        proj.location, null, MathUtils.getRandomPointInCircle(proj.location, exp_radius), null,
                        10f, empcolor, empcolor
                    )
                }
            }

            if (exp_left == 0)
            {
                Global.getCombatEngine().removePlugin(this);
            }
        }


    }

}