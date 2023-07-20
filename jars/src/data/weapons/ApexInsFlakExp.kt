package data.weapons

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.CombatEngineAPI
import com.fs.starfarer.api.combat.DamagingProjectileAPI
import com.fs.starfarer.api.combat.EveryFrameCombatPlugin
import com.fs.starfarer.api.combat.ViewportAPI
import com.fs.starfarer.api.input.InputEventAPI
import com.fs.starfarer.api.util.Misc
import org.lwjgl.util.vector.Vector2f
import org.magiclib.util.MagicRender
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
            val engine = Global.getCombatEngine()
            // TODO: explosion vfx
            //engine.addFloatingText(flakShot.location, "boom!", 24f, Color.CYAN, null, 0f, 0f)
            val flak_puff = Global.getSettings().getSprite("fx", "apex_flak_puff")
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
            val flamesize = Vector2f(35f + randAmount, 35f + randAmount)
            val flamegrowth = Vector2f(5f, 5f)
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
                0.56f)
            engine.addSmoothParticle(proj.location, Misc.ZERO, 90f, 1.0F, 0.05F, Color.WHITE);
            engine.addSmoothParticle(proj.location, Misc.ZERO, 90f, 1.0F, 0.1F, Color.WHITE);
            engine.removePlugin(this)
        }
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
        val FIRE_COLOR = Color(255, 71, 0, 200)
        val PUFF_COLOR = Color(50, 50, 50, 128)
    }
}