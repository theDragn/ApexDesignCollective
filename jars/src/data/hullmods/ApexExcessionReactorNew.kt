package data.hullmods

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.*
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier
import com.fs.starfarer.api.impl.campaign.ids.MemFlags
import com.fs.starfarer.api.ui.Alignment
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import org.lwjgl.util.vector.Vector2f
import org.magiclib.util.MagicUI
import plugins.ApexModPlugin.Companion.xd
import utils.ApexUtils.text
import java.awt.Color
import kotlin.math.max
import kotlin.math.min

class ApexExcessionReactorNew: BaseHullMod()
{

    class ExcessionData
    {
        var entropy = 0f
        var ppt_time = 0f
    }

    companion object
    {
        const val BASE_CHARGE_RATE = 100f
        const val DAMAGE_CHARGE_MULT = 1.5f // from system being active
        const val MAX_ENTROPY = 3000f
        const val REPAIR_RATE = 15f
        const val KEY = "apex_ex"
        val CHARGE_COLOR = Color(89, 170, 255);
    }

    override fun getDisplayCategoryIndex(): Int
    {
        return -1
    }

    override fun applyEffectsAfterShipCreation(ship: ShipAPI, id: String)
    {
        ship.addListener(ApexExcessionChargeListener())
        ship.customData[KEY] = ExcessionData()
    }

    override fun advanceInCombat(ship: ShipAPI, amount: Float)
    {
        if (!ship.isAlive || ship.isHulk) return
        val data = ship.customData[KEY] as ExcessionData

        MagicUI.drawInterfaceStatusBar(
            ship,
            data.entropy / MAX_ENTROPY,
            null,
            null,
            0f,
            "entropy",
            data.entropy.toInt())
        data.entropy = min(data.entropy + BASE_CHARGE_RATE * amount, MAX_ENTROPY)

        if (data.ppt_time < ship.timeDeployedForCRReduction)
        {
            data.ppt_time += amount / ship.mutableStats.timeMult.modifiedValue
            ship.setTimeDeployed(data.ppt_time)
        }

        if (ship.isPhased)
            doRepair(ship, amount)

        // trigger killswitch, if necessary
        // no, they're not giving you a supership without some precautions
        // triggers if enemy fleet is apex faction, and will not give a rep penalty on death

        // trigger killswitch, if necessary
        // no, they're not giving you a supership without some precautions
        // triggers if enemy fleet is apex faction, and will not give a rep penalty on death
        val context = Global.getCombatEngine().context
        if (!Global.getCombatEngine().isSimulation && context != null && context.otherFleet != null && context.otherFleet.faction != null)
        {
            // checks to see if no rep impact flag is set
            val mem = context.otherFleet.memoryWithoutUpdate
            if (mem.contains(MemFlags.MEMORY_KEY_NO_REP_IMPACT) && mem[MemFlags.MEMORY_KEY_NO_REP_IMPACT] is Boolean && mem[MemFlags.MEMORY_KEY_NO_REP_IMPACT] as Boolean) return
            if (context.otherFleet.faction.id == "apex_design")
            {
                Global.getCombatEngine().addFloatingText(
                    ship.location,
                    xd("S2lsbHN3aXRjaCBBY3RpdmF0ZWQh"),  // hiding text for funsies
                    40f,
                    Color.RED,
                    ship,
                    0.5f,
                    6f
                )
                // you fool, you utter buffoon
                // did you really think they'd give you a ship that you could use against them
                ship.mutableStats.maxSpeed.modifyMult("get owned", 0.33f)
                ship.mutableStats.fluxDissipation.modifyMult("you idiot", 0.25f)
                Global.getCombatEngine().applyDamage(
                    ship,
                    ship.location,
                    3000f,
                    DamageType.ENERGY,
                    10000f,
                    true,
                    false,
                    ship
                )
            }
        }
    }

    override fun addPostDescriptionSection(
        tooltip: TooltipMakerAPI,
        hullSize: ShipAPI.HullSize?,
        ship: ShipAPI?,
        width: Float,
        isForModSpec: Boolean
    )
    {
        ship ?: return
        val pad = 10f
        tooltip.addSectionHeading(text("Details"), Alignment.MID, pad)
        tooltip.addPara("\n• " + text("excb1"), 0f, CHARGE_COLOR, text("excb2"))
        tooltip.addPara("• " + text("excb3"), 0f, CHARGE_COLOR, text("excb2"))
        tooltip.addPara("• " + text("excb5"), 0f, CHARGE_COLOR, text("excb6"))
        val colors = arrayOf(CHARGE_COLOR, Misc.getHighlightColor())
        tooltip.addPara("• " + text("excb7"), 0f, colors, text("excb2"), REPAIR_RATE.toInt().toString())
        tooltip.addPara("• " + text("excb8"), 0f, Misc.getHighlightColor(), text("excb9"))
        tooltip.addPara("• " + text("excb10"), 0f, Misc.getHighlightColor(), text("excb11"))
    }

    class ApexExcessionChargeListener : DamageDealtModifier
    {
        override fun modifyDamageDealt(
            param: Any,
            target: CombatEntityAPI,
            damage: DamageAPI,
            point: Vector2f,
            shieldHit: Boolean
        ): String?
        {
            if (target is ShipAPI && target.isHulk) return null
            if (param is DamagingProjectileAPI)
            {
                var chargeAmount = damage.damage
                val proj = param
                if (proj.source.system.isActive) chargeAmount *= DAMAGE_CHARGE_MULT
                addCharge(proj.source, chargeAmount)
                if (Misc.shouldShowDamageFloaty(
                        proj.source,
                        proj.source
                    ) && proj.source.system.isActive && proj.weapon != null
                )
                {
                    Global.getCombatEngine().addFloatingDamageText(
                        proj.weapon.location,
                        chargeAmount * ApexExcessionReactor.DAMAGE_CHARGE_MULT,
                        Color.MAGENTA,
                        proj.source,
                        proj.source
                    )
                }
            }
            return null
        }

        private fun addCharge(source: ShipAPI, charge: Float)
        {
            val data = source.customData[KEY] as ExcessionData
            data.entropy = min(MAX_ENTROPY, data.entropy + charge)
        }
    }

    private fun doRepair(ship: ShipAPI, amount: Float)
    {
        //System.out.println("did effect tick");
        var amount = amount
        val engine = Global.getCombatEngine()
        val timeMult = ship.mutableStats.timeMult.modifiedValue
        amount *= timeMult // first one is to bring it back to "normal" timeflow, second is to multiply it by timeflow.
        val repairThisFrame = min(
            REPAIR_RATE,
            (ship.customData[KEY] as ExcessionData).entropy
        )
        if (repairThisFrame <= 0) return
        val grid = ship.armorGrid ?: return
        val gridWidth = grid.grid.size
        val gridHeight = grid.grid[0].size
        val maxArmorInCell = grid.maxArmorInCell

        // first, get number of cells missing armor
        var numCellsToRepair = 0
        for (x in 0 until gridWidth)
        {
            for (y in 0 until gridHeight)
            {
                if (grid.getArmorValue(x, y) < maxArmorInCell) numCellsToRepair++
            }
        }
        if (numCellsToRepair == 0) return

        // then, repair the cells
        val repairPerCell = repairThisFrame / numCellsToRepair.toFloat()
        var repairDoneThisFrame = 0f
        for (x in 0 until gridWidth)
        {
            for (y in 0 until gridHeight)
            {
                if (grid.getArmorValue(x, y) < maxArmorInCell)
                {
                    repairDoneThisFrame += min(repairPerCell, maxArmorInCell - grid.getArmorValue(x, y))
                    grid.setArmorValue(x, y, min(grid.getArmorValue(x, y) + repairPerCell, maxArmorInCell))
                }
            }
        }
        if (repairDoneThisFrame > 0)
        {
            // only show rapid regen while phased, normal regen is pretty damn slow
            if (Misc.shouldShowDamageFloaty(ship, ship) && ship.isPhased)
            {
                engine.addFloatingDamageText(ship.location, repairDoneThisFrame, Color.GREEN, ship, ship)
            }
            ApexExcessionReactor.repairMap[ship] = max(ApexExcessionReactor.repairMap[ship]!! - repairDoneThisFrame, 0f)
            ship.syncWithArmorGridState()
        }
    }

}