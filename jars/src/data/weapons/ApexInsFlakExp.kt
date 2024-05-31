package data.weapons

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.CombatEngineAPI
import com.fs.starfarer.api.combat.DamagingProjectileAPI
import com.fs.starfarer.api.combat.EveryFrameCombatPlugin
import com.fs.starfarer.api.combat.ProximityExplosionEffect
import com.fs.starfarer.api.combat.ViewportAPI
import com.fs.starfarer.api.input.InputEventAPI
import com.fs.starfarer.api.util.Misc
import org.dark.shaders.distortion.DistortionShader
import org.dark.shaders.distortion.WaveDistortion
import org.dark.shaders.light.LightShader
import org.dark.shaders.light.StandardLight
import org.lazywizard.lazylib.MathUtils
import org.lwjgl.util.vector.Vector2f
import org.magiclib.util.MagicRender
import plugins.ApexModPlugin.Companion.POTATO_MODE
import java.awt.Color

class ApexInsFlakExp: ProximityExplosionEffect
{
    // this is very much inspired by nicke's vfx script for the tizona
    companion object
    {
        val AOE_COLOR = Color(255, 50, 50, 255)
        val PARTICLE_COLOR = Color(255, 135, 70, 255)
        val AOE_SIZE = 100f
        val AOE_DURATION = 0.33f
    }

    override fun onExplosion(explosion: DamagingProjectileAPI, originalProjectile: DamagingProjectileAPI)
    {
        val engine = Global.getCombatEngine()
        val loc = Vector2f(explosion.location)
        engine.addSmoothParticle(loc, Misc.ZERO, 90f, 1.5F, 0.05F, Color.WHITE)
        engine.addSmoothParticle(loc, Misc.ZERO, 90f, 1.5F, 0.1F, Color.WHITE)
        engine.addHitParticle(loc, Misc.ZERO, AOE_SIZE * 1.5f, 1f, AOE_DURATION, AOE_COLOR)
        for (i in 1..4)
        {
            val vel = MathUtils.getRandomPointInCircle(Misc.ZERO, 150f)
            engine.addHitParticle(loc, vel, 25f, 2f, AOE_DURATION, PARTICLE_COLOR)
        }
        if (POTATO_MODE) return
        val light = StandardLight(loc, Misc.ZERO, Misc.ZERO, null, 2f, AOE_SIZE * 1.5f)
        light.setColor(AOE_COLOR)
        light.setLifetime(0f)
        light.autoFadeOutTime = AOE_DURATION
        LightShader.addLight(light)

        val wave = WaveDistortion(Vector2f(loc), Misc.ZERO)
        wave.intensity = 60f
        wave.flip(true)
        wave.setLifetime(AOE_DURATION)
        wave.fadeOutIntensity(AOE_DURATION)
        wave.size = AOE_SIZE * 0.75f
        wave.fadeInSize(AOE_DURATION)
        wave.size = AOE_SIZE * 0.25f
        DistortionShader.addDistortion(wave)
    }
}