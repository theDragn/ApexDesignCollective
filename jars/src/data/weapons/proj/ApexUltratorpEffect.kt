package data.weapons.proj

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.*
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI
import com.fs.starfarer.api.util.Misc
import data.weapons.ApexInsFlakExp
import org.dark.shaders.distortion.DistortionShader
import org.dark.shaders.distortion.WaveDistortion
import org.dark.shaders.light.LightShader
import org.dark.shaders.light.StandardLight
import org.lazywizard.lazylib.MathUtils
import org.lwjgl.util.vector.Vector2f
import plugins.ApexModPlugin
import java.awt.Color

class ApexUltratorpEffect: OnHitEffectPlugin
{
    val FLUX_FRACTION_SHIELD = 0.05f
    val ARMOR_DAMAGE = 250f

    override fun onHit(
        projectile: DamagingProjectileAPI,
        target: CombatEntityAPI?,
        point: Vector2f,
        shieldHit: Boolean,
        damageResult: ApplyDamageResultAPI?,
        engine: CombatEngineAPI
    )
    {
        engine.addSmoothParticle(point, Misc.ZERO, 300f, 1.0f, 0.05f, Color.WHITE)
        engine.addSmoothParticle(point, Misc.ZERO, 300f, 1.0f, 0.1f, Color.WHITE)
        // explosion vfx first
        engine.addSmoothParticle(point, Misc.ZERO, 90f, 1.5F, 0.05F, Color.WHITE)
        engine.addSmoothParticle(point, Misc.ZERO, 90f, 1.5F, 0.1F, Color.WHITE)
        engine.addHitParticle(point, Misc.ZERO, ApexInsFlakExp.AOE_SIZE * 1.5f, 1f,
            ApexInsFlakExp.AOE_DURATION,
            ApexInsFlakExp.AOE_COLOR
        )
        for (i in 1..16)
        {
            val color = Color(Color.HSBtoRGB(Misc.random.nextFloat(), 1.0f, 0.75f))
            val vel = MathUtils.getRandomPointInCircle(Misc.ZERO, 150f)
            engine.addHitParticle(point, vel, 25f, 3f, 0.66f, color)
        }
        if (ApexModPlugin.POTATO_MODE) return
        engine.getPlayerShip().getMutableStats().getProjectileSpeedMult().modifyMult("idk",0.01f)
        val light = StandardLight(point, Misc.ZERO, Misc.ZERO, null, 2f, 100f)
        light.setColor(Color.WHITE)
        light.setLifetime(0f)
        light.autoFadeOutTime = 0.66f
        LightShader.addLight(light)

        val wave = WaveDistortion(Vector2f(point), Misc.ZERO)
        wave.intensity = 60f
        wave.flip(true)
        wave.setLifetime(0.66f)
        wave.fadeOutIntensity(0.66f)
        wave.size = 130f * 0.75f
        wave.fadeInSize(0.66f)
        wave.size = 130f * 0.25f
        DistortionShader.addDistortion(wave)

        // this loop is just muzzle flash, not trails
        for (i in 0..24)
        {
            val color = Color(Color.HSBtoRGB(Misc.random.nextFloat(), 1.0f, 0.85f))
            val actualColor = Color(color.red, color.green, color.blue, 100)
            engine.addNebulaParticle(
                point,
                MathUtils.getRandomPointInCircle(Misc.ZERO, 190f),
                MathUtils.getRandomNumberInRange(60f, 80f),
                2.2f,
                0.3f,
                0.3f,
                MathUtils.getRandomNumberInRange(0.66f, 1.1f),
                actualColor
            )
        }
        // now damage
        target ?: return
        if (target !is ShipAPI) return
        if (shieldHit)
        {
            target.fluxTracker.increaseFlux(target.fluxTracker.maxFlux * FLUX_FRACTION_SHIELD, true)
        } else {
            for (slot in target.hullSpec.allWeaponSlotsCopy)
            {
                val color = Color(Color.HSBtoRGB(Misc.random.nextFloat(), 1.0f, 0.75f))
                val actualColor = Color(color.red, color.green, color.blue, 100)
                val pos = slot.computePosition(target)
                val from = MathUtils.getRandomPointInCircle(pos, 20f)
                for (i in 0..2)
                {
                    engine.spawnEmpArcVisual(
                        from, target, MathUtils.getRandomPointInCircle(from, 16f), target,
                        10f, actualColor, Color.white
                    )
                }
                engine.applyDamage(target, pos, ARMOR_DAMAGE, DamageType.ENERGY, ARMOR_DAMAGE, true, false, projectile.source)
            }
        }
    }
}