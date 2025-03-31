package data.hullmods

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.BaseHullMod
import com.fs.starfarer.api.combat.BeamAPI
import com.fs.starfarer.api.combat.CombatEntityAPI
import com.fs.starfarer.api.combat.DamageAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.listeners.AdvanceableListener
import com.fs.starfarer.api.combat.listeners.DamageTakenModifier
import org.lwjgl.util.vector.Vector2f
import kotlin.math.max

class ApexInsArmor2: BaseHullMod()
{
    companion object
    {
        const val DRAIN_FRAC_PER_SEC = 0.15f
        const val MIN_DRAIN_PER_SEC = 100f
        const val MIN_DAMAGE_TAKEN_MULT = 0.2f
        const val KEY = "apex_ins_armor2_pool"
    }

    override fun applyEffectsAfterShipCreation(ship: ShipAPI, id: String)
    {
        ship.addListener(ApexInsArmor2Listener(ship))
    }

    override fun advanceInCombat(ship: ShipAPI, amount: Float)
    {
        var pool = if (ship.customData.containsKey(KEY)) ship.customData[KEY] as Float else 0f
        val mult = poolToDamageTakenMult(pool)
        ship.mutableStats.armorDamageTakenMult.modifyMult(KEY, mult)
        if (ship == Global.getCombatEngine().playerShip && mult < 1f)
        {
            Global.getCombatEngine().maintainStatusForPlayerShip(
                KEY,
                "graphics/icons/hullsys/damper_field.png",
                "adaptive armor",
                (100f - mult * 100f).toInt().toString() + "% less damage taken",
                false
            )
        }
        pool -= max(pool * DRAIN_FRAC_PER_SEC * amount, MIN_DRAIN_PER_SEC * amount)
        pool = max(pool, 0f)
        ship.setCustomData(KEY, pool)
    }

    override fun getDescriptionParam(index: Int, hullSize: ShipAPI.HullSize?): String
    {
        return when (index) {
            0 -> (100f - 100f * MIN_DAMAGE_TAKEN_MULT).toInt().toString() + "%"
            else -> ""
        }
    }

    fun poolToDamageTakenMult(pool: Float): Float
    {
        return max(1f - (-1f / ( (1f/3000f) * (0.6f*pool + 3000f) ) + 1f), MIN_DAMAGE_TAKEN_MULT)
    }

    class ApexInsArmor2Listener(val ship: ShipAPI): DamageTakenModifier
    {
        override fun modifyDamageTaken(
            param: Any?,
            target: CombatEntityAPI?,
            damage: DamageAPI?,
            point: Vector2f?,
            shieldHit: Boolean
        ): String?
        {
            if (shieldHit || !ship.isAlive) return null
            damage ?: return null
            var poolAdd = 0f
            if (damage.isDps)
            {
                poolAdd += damage.computeDamageDealt(Global.getCombatEngine().elapsedInLastFrame) * damage.type.armorMult
            } else {
                poolAdd += damage.damage * damage.type.armorMult
            }
            val pool = if (ship.customData.containsKey(KEY)) ship.customData[KEY] as Float else 0f
            ship.setCustomData(KEY, pool + poolAdd)
            return null
        }
    }
}