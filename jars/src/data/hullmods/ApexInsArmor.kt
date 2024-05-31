package data.hullmods

import com.fs.starfarer.api.combat.*
import com.fs.starfarer.api.combat.listeners.DamageTakenModifier
import org.lwjgl.util.vector.Vector2f
import utils.ApexUtils
import java.util.EnumMap


class ApexInsArmor: BaseHullMod()
{
    val ARMOR_RATING_MULT = 1.5f
    val SMOD_RATING_MULT = 2.0f
    val ARMOR_EFF_MULT = 0.67f
    val SMOD_EFF_MULT = 0.5f


    companion object
    {
        const val DAMAGE_CAP = 750f
        const val EXTRA_DAM_MULT = 0.25f
        val MIN_ARMOR_TO_WORK = 0.15f
    }

    override fun getDescriptionParam(index: Int, hullSize: ShipAPI.HullSize?, ship: ShipAPI?): String
    {
        return when (index) {
            0 -> (ARMOR_RATING_MULT * 100f - 100f).toInt().toString() + "%"
            1 -> (100f - 100f * ARMOR_EFF_MULT).toInt().toString() + "%"
            2 -> DAMAGE_CAP.toInt().toString()
            3 -> DAMAGE_CAP.toInt().toString()
            4 -> DAMAGE_CAP.toInt().toString()
            5 -> (100f - 100f * EXTRA_DAM_MULT).toInt().toString() + "%"
            6 -> (100 * MIN_ARMOR_TO_WORK).toInt().toString() + "%"
            else -> ""
        }
    }

    @Override

    override fun applyEffectsBeforeShipCreation(hullSize: ShipAPI.HullSize, stats: MutableShipStatsAPI, id: String)
    {
        if (!isSMod(stats))
        {
            stats.armorBonus.modifyMult(id, ARMOR_RATING_MULT)
            stats.effectiveArmorBonus.modifyMult(id, ARMOR_EFF_MULT)
        } else {
            stats.armorBonus.modifyMult(id, SMOD_RATING_MULT)
            stats.effectiveArmorBonus.modifyMult(id, SMOD_EFF_MULT)
        }
        stats.engineHealthBonus.modifyMult(id, ARMOR_RATING_MULT)
        stats.weaponHealthBonus.modifyMult(id, ARMOR_RATING_MULT)

    }

    override fun hasSModEffect(): Boolean
    {
        return true
    }

    override fun getSModDescriptionParam(index: Int, hullSize: ShipAPI.HullSize?): String
    {
        return when (index)
        {
            0 -> (SMOD_RATING_MULT * 100f - 100f).toInt().toString() + "%"
            1 -> (100f - 100f * SMOD_EFF_MULT).toInt().toString() + "%"
            else -> ""
        }
    }

    override fun applyEffectsAfterShipCreation(ship: ShipAPI, id: String?)
    {
        ship.addListener(ApexInsArmorListener())
    }

    class ApexInsArmorListener: DamageTakenModifier
    {
        override fun modifyDamageTaken(
            param: Any?,
            target: CombatEntityAPI?,
            damage: DamageAPI?,
            point: Vector2f?,
            shieldHit: Boolean
        ): String?
        {
            if (shieldHit) return null
            if (target !is ShipAPI) return null
            point ?: return null
            if (ApexUtils.getArmorFraction(target, point) < MIN_ARMOR_TO_WORK) return null
            if (damage != null)
            {
                var initialDamage = damage.damage
                initialDamage *= damage.type.armorMult
                if (initialDamage > DAMAGE_CAP)
                {
                    val toReduce = initialDamage - DAMAGE_CAP
                    val finaldam = (initialDamage + toReduce * EXTRA_DAM_MULT) / damage.type.armorMult
                    damage.modifier.modifyMult("ins_armor", finaldam / initialDamage)
                    print("did stuff, initial = $initialDamage, modified = $finaldam\n")
                    return "ins_armor"
                }
            }
            return null
        }
    }

    override fun showInRefitScreenModPickerFor(ship: ShipAPI?): Boolean
    {
        if (ship != null && ship.id.contains("apex_ins")) return true
        return false
    }
}