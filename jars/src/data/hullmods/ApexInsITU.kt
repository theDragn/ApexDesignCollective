package data.hullmods

import com.fs.starfarer.api.combat.BaseHullMod
import com.fs.starfarer.api.combat.MutableShipStatsAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.WeaponAPI
import com.fs.starfarer.api.combat.listeners.WeaponBaseRangeModifier

class ApexInsITU: BaseHullMod()
{
    companion object
    {
        const val ENERGY_FLUX_COST_MULT = 0.67f
        const val ENERGY_RANGE_BONUS = 300f
        const val ALL_RANGE_BONUS = 100f
    }
    override fun applyEffectsBeforeShipCreation(hullSize: ShipAPI.HullSize, stats: MutableShipStatsAPI, id: String)
    {
        stats.energyWeaponFluxCostMod.modifyMult(id, ENERGY_FLUX_COST_MULT)
    }

    override fun applyEffectsAfterShipCreation(ship: ShipAPI, id: String)
    {
        ship.addListener(ApexInsITURangeMod())
    }

    override fun getDescriptionParam(index: Int, hullSize: ShipAPI.HullSize): String
    {
        return when (index)
        {
            0 -> "" + ALL_RANGE_BONUS.toInt()
            1 -> "" + ENERGY_RANGE_BONUS.toInt()
            2 -> "" + (100f - (100f * ENERGY_FLUX_COST_MULT)).toInt() + "%"
            else -> ""
        }
    }

    class ApexInsITURangeMod: WeaponBaseRangeModifier
    {
        override fun getWeaponBaseRangePercentMod(ship: ShipAPI?, weapon: WeaponAPI?): Float
        {
            return 0f
        }

        override fun getWeaponBaseRangeMultMod(ship: ShipAPI?, weapon: WeaponAPI?): Float
        {
            return 1f
        }

        override fun getWeaponBaseRangeFlatMod(ship: ShipAPI, weapon: WeaponAPI): Float
        {
            if (weapon.type == WeaponAPI.WeaponType.ENERGY)
            {
                return ALL_RANGE_BONUS + ENERGY_RANGE_BONUS;
            } else if (weapon.type == WeaponAPI.WeaponType.BALLISTIC) {
                return ALL_RANGE_BONUS
            }
            return 0f
        }
    }
}