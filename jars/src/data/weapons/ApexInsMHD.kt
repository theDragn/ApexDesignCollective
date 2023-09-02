package data.weapons

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.*
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier
import com.fs.starfarer.api.util.Misc
import org.lwjgl.util.vector.Vector2f

class ApexInsMHD: EveryFrameWeaponEffectPlugin
{
    override fun advance(amount: Float, engine: CombatEngineAPI?, weapon: WeaponAPI?)
    {
        weapon?.ship ?: return
        if (!weapon.ship.hasListenerOfClass(ApexMHDListener::class.java))
        {
            weapon.ship.addListener(ApexMHDListener())
        }

    }
    class ApexMHDListener: DamageDealtModifier
    {
        override fun modifyDamageDealt(
            param: Any?,
            target: CombatEntityAPI?,
            damage: DamageAPI?,
            point: Vector2f?,
            shieldHit: Boolean
        ): String?
        {
            if (shieldHit && param is DamagingProjectileAPI && param.projectileSpecId != null && damage != null && target is ShipAPI && param.source != null)
            {
                if (param.projectileSpecId.equals("apex_ins_mhd_shot"))
                {
                    target.fluxTracker.increaseFlux(damage.damage, true)
                    Global.getCombatEngine().addFloatingDamageText(point, damage.damage, Misc.FLOATY_SHIELD_DAMAGE_COLOR, target, param.source)
                    damage.modifier.modifyMult("apexMHD", 0.5f)
                    return "apexMHD"
                }
            }
            return null
        }
    }
}

