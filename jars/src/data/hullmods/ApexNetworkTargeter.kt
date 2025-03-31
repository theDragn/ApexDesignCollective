package data.hullmods

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.BaseHullMod
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.ShipAPI.HullSize
import com.fs.starfarer.api.combat.WeaponAPI
import com.fs.starfarer.api.combat.WeaponAPI.WeaponType
import com.fs.starfarer.api.combat.listeners.WeaponBaseRangeModifier
import com.fs.starfarer.api.ui.Alignment
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import org.lazywizard.lazylib.MathUtils
import org.magiclib.util.MagicIncompatibleHullmods
import utils.ApexUtils
import java.awt.Color
import kotlin.math.abs
import kotlin.math.min

class ApexNetworkTargeter : BaseHullMod()
{
    companion object
    {
        const val ENGAGEMENT_RANGE_PENALTY_MULT = 0.5f
        const val MAX_OP_FOR_BONUS = 120f
        const val MAX_WEP_FLAT_BONUS = 300f
        const val MAX_FIGHTER_FLAT_BONUS = 600f
        const val MAX_RANGE_AFTER_BOOST = 900f
        const val ID = "apex_net_target"
        private val BLOCKED_HULLMODS: MutableSet<String> = HashSet()

        init
        {
            BLOCKED_HULLMODS.add("ballistic_rangefinder")
        }

        fun getBonusMult(ship: ShipAPI): Float
        {
            var op = 0
            for (weapon in ship.allWeapons)
            {
                if (weapon.type.equals(WeaponType.BUILT_IN) || weapon.type.equals(WeaponType.SYSTEM))
                    continue
                // does the character stats ever come up for this??
                op += weapon.spec.getOrdnancePointCost(Global.getSector().playerStats, ship.mutableStats).toInt()
            }
            if (op > MAX_OP_FOR_BONUS) op = MAX_OP_FOR_BONUS.toInt();
            return op/120f
        }
    }

    private val update = IntervalUtil(0.1f, 0.2f)
    override fun applyEffectsAfterShipCreation(ship: ShipAPI, id: String)
    {
        for (hullmod in BLOCKED_HULLMODS)
        {
            if (ship.variant.hullMods.contains(hullmod))
            {
                MagicIncompatibleHullmods.removeHullmodWithWarning(
                    ship.variant,
                    hullmod,
                    "apex_network_targeter"
                )
            }
        }
        val bonusMult = getBonusMult(ship)
        ship.addListener(ApexUplinkRangeMod(MAX_WEP_FLAT_BONUS*bonusMult))
    }

    override fun advanceInCombat(ship: ShipAPI, amount: Float)
    {
        update.advance(amount)
        if (!update.intervalElapsed()) return
        if (ship.allWings.isEmpty()) return
        // only run the computation once
        if (!ship.customData.containsKey(ID)) ship.setCustomData(ID, getBonusMult(ship)* MAX_FIGHTER_FLAT_BONUS)
        val bonus = ship.customData[ID] as Float
        //System.out.println(bonus);
        // reduce engagement range, if it's getting penalized
        if (bonus > 0f) ship.mutableStats.fighterWingRange.modifyMult(
            ID,
            ENGAGEMENT_RANGE_PENALTY_MULT
        )
        // increase/decrease fighter range as necessary
        for (wing in ship.allWings)
        {
            for (fighter in wing.wingMembers)
            {
                val stats = fighter.mutableStats
                stats.ballisticWeaponRangeBonus.modifyFlat(ID, bonus)
                stats.energyWeaponRangeBonus.modifyFlat(ID, bonus)
                stats.missileWeaponRangeBonus.modifyFlat(ID, bonus)
            }
        }
    }

    class ApexUplinkRangeMod(private val flatMod: Float) : WeaponBaseRangeModifier
    {
        override fun getWeaponBaseRangePercentMod(shipAPI: ShipAPI, weaponAPI: WeaponAPI): Float
        {
            return 0f
        }

        override fun getWeaponBaseRangeMultMod(shipAPI: ShipAPI, weaponAPI: WeaponAPI): Float
        {
            return 1f
        }

        override fun getWeaponBaseRangeFlatMod(ship: ShipAPI, weapon: WeaponAPI): Float
        {
            return if (weapon.type != WeaponType.MISSILE)
            {
                MathUtils.clamp(
                    flatMod,
                    0f,
                    MAX_RANGE_AFTER_BOOST - weapon.spec.maxRange
                )
            } else
                0f
        }
    }

    override fun getDescriptionParam(index: Int, hullSize: HullSize): String?
    {
        if (index == 0) return "" + (ENGAGEMENT_RANGE_PENALTY_MULT * 100f).toInt() + "%"
        if (index == 1) return "" + MAX_WEP_FLAT_BONUS.toInt() + ""
        if (index == 2) return "" + MAX_RANGE_AFTER_BOOST.toInt() + ""
        if (index == 3) return "" + MAX_FIGHTER_FLAT_BONUS.toInt() + ""
        if (index == 4) return "" + MAX_OP_FOR_BONUS.toInt() + ""
        else return null
    }

    override fun addPostDescriptionSection(
        tooltip: TooltipMakerAPI,
        hullSize: HullSize,
        ship: ShipAPI,
        width: Float,
        isForModSpec: Boolean
    )
    {
        if (ship == null) return
        val pad = 10f
        tooltip.addSectionHeading(ApexUtils.text("nett1"), Alignment.MID, pad)
        val bonus = (getBonusMult(ship))
        val hlcolor = if (bonus > 0) Misc.getPositiveHighlightColor() else Misc.getHighlightColor()

        tooltip.addPara(ApexUtils.text("nett2"), 0f, hlcolor, "+" + (bonus * MAX_FIGHTER_FLAT_BONUS).toInt())
        tooltip.addPara(ApexUtils.text("nett4"), 0f, hlcolor, "+" + (bonus * MAX_WEP_FLAT_BONUS).toInt() + "")
    }

    override fun isApplicableToShip(ship: ShipAPI): Boolean
    {
        if (ship == null || ship.numFighterBays == 0) return false
        for (hullmod in BLOCKED_HULLMODS)
        {
            if (ship.variant.hullMods.contains(hullmod)) return false
        }
        return true
    }

    override fun getUnapplicableReason(ship: ShipAPI): String?
    {
        if (ship == null)
        {
            return "Ship does not exist, what the fuck"
        }
        if (ship.numFighterBays == 0) return ApexUtils.text("nett6")
        for (hullmod in BLOCKED_HULLMODS)
        {
            if (ship.variant.hullMods.contains(hullmod))
            {
                return ApexUtils.text("hmerror1") + " " + Global.getSettings().getHullModSpec(hullmod).displayName + "."
            }
        }
        return null
    }
}