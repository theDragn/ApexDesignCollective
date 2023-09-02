package data.weapons

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.*
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import org.lazywizard.lazylib.MathUtils
import org.lwjgl.util.vector.Vector2f
import utils.ApexUtils
import java.awt.Color


class ApexFlickerbeamEffect: EveryFrameWeaponEffectPlugin
{
    val vfx_interval = IntervalUtil(0.05f, 0.05f)
    var played_sound = false
    var last_played_vfx = 0f;
    val vfx_color = Color(236, 242, 61, 255)
    val vfx_color2 = Color(242, 136, 61, 255)
    var old_turnrate = 0f

    override fun advance(amount: Float, engine: CombatEngineAPI, weapon: WeaponAPI)
    {
        // this is basically isFiring() but also doing a null check at the same time
        if (weapon.beams.isEmpty())
        {
            played_sound = false
            last_played_vfx = 0f
            return
        }
        // we have to do this here because firing sounds on short-duration beams are fucked
        if (!played_sound)
        {
            played_sound = true
            Global.getSoundPlayer().playSound("apex_fusion_l_fire", 1f, 1f, weapon.location, weapon.ship.velocity)
        }

        last_played_vfx += amount
        if (last_played_vfx == 0f || last_played_vfx >= 0.05f)
        {
            last_played_vfx = 0f
            val beam = weapon.beams[0]
            val from_point = Vector2f(beam.from)
            /*
            var length = 0f
            while (length < beam.lengthPrevFrame)
            {
                length += ApexUtils.randBetween(5f, 50f)
                val col = Misc.interpolateColor(vfx_color, vfx_color2, Misc.random.nextFloat())
                val spawnloc = MathUtils.getPointOnCircumference(from_point, length, weapon.currAngle)
                engine.addNebulaParticle(spawnloc, beam.source.velocity, 45f, 0.1f, 0.5f, 1f, 0.15f, col)
                engine.addNegativeSwirlyNebulaParticle(spawnloc, beam.source.velocity, 30f, 0.1f, 0.5f, 1f, 0.15f, col)
            }*/
            engine.addSmoothParticle(from_point, weapon.ship.velocity, 60f, 1f, 0.2f, vfx_color)
            if (beam.damageTarget != null)
                engine.addSmoothParticle(beam.to, beam.damageTarget?.velocity ?: Misc.ZERO, 60f, 1f, 0.2f, vfx_color)
        }
    }

}