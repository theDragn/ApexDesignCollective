package data.weapons

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.CombatEngineAPI
import com.fs.starfarer.api.combat.DamagingProjectileAPI
import com.fs.starfarer.api.combat.EveryFrameCombatPlugin
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

class ApexInsFlakExp(val proj: DamagingProjectileAPI): EveryFrameCombatPlugin
{
    override fun init(engine: CombatEngineAPI?)
    {
        // WHY IS IT DEPRECATED AND MANDATORY
    }

    override fun processInputPreCoreControls(amount: Float, events: MutableList<InputEventAPI>?)
    {
        // do nothing
    }

    override fun advance(amount: Float, events: MutableList<InputEventAPI>?)
    {
        if (proj.isExpired || proj.didDamage() || !Global.getCombatEngine().isEntityInPlay(proj))
        {
            // TODO: explosion vfx
            //engine.addFloatingText(flakShot.location, "boom!", 24f, Color.CYAN, null, 0f, 0f)
            detonate(proj, Global.getCombatEngine())

            Global.getCombatEngine().removePlugin(this)
        }
    }

    // this is very much inspired by nicke's vfx script for the tizona
    private fun detonate(proj: DamagingProjectileAPI, engine: CombatEngineAPI)
    {
        val loc = Vector2f(proj.location)
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

    override fun renderInWorldCoords(viewport: ViewportAPI?)
    {
        // nothing
    }

    override fun renderInUICoords(viewport: ViewportAPI?)
    {
        // nothing
    }

    companion object
    {
        val AOE_COLOR = Color(255, 50, 50, 255)
        val PARTICLE_COLOR = Color(255, 135, 70, 255)
        val AOE_SIZE = 100f
        val AOE_DURATION = 0.33f
    }

    // previous explosion vfx
    /*val flak_puff = Global.getSettings().getSprite("fx", "apex_flak_puff")
    val flak_fire = Global.getSettings().getSprite("fx", "apex_flak_fire")
    // dark cloud vfx
    val randAmount = Misc.random.nextFloat() * 5f
    val puffsize = Vector2f(50f + randAmount, 50f + randAmount)
    val puffgrowth = Vector2f(90f, 90f)
    MagicRender.battlespace(flak_puff,
        proj.location,
        Misc.ZERO,
        puffsize,
        puffgrowth,
        Misc.random.nextFloat() * 360f,
        0f,
        PUFF_COLOR,
        true,
        0f,
        0.33f,
        0.66f)
    // bright flame/shard sprite
    //val flamesize = Vector2f(35f + randAmount, 35f + randAmount)
    //val flamegrowth = Vector2f(5f, 5f)
    MagicRender.battlespace(flak_fire,
        proj.location,
        Misc.ZERO,
        flamesize,
        flamegrowth,
        Misc.random.nextFloat() * 360f,
        0f,
        FIRE_COLOR,
        true,
        0f,
        0.1f,
        0.56f)*/
}