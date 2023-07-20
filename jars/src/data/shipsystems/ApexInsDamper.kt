package data.shipsystems

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.MutableShipStatsAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.ShipSystemAPI
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript
import com.fs.starfarer.api.plugins.ShipSystemStatsScript
import sun.audio.AudioPlayer.player
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt


class ApexInsDamper: BaseShipSystemScript()
{
    val STATUSKEY1 = Object()

    override fun apply(
        stats: MutableShipStatsAPI?,
        id: String?,
        state: ShipSystemStatsScript.State?,
        effectLevel: Float
    )
    {
        stats ?: return; stats.entity ?: return
        val grid = (stats.entity as ShipAPI).armorGrid
        val gridWidth: Int = grid.grid.size
        val gridHeight: Int = grid.grid[0].size
        for (x in 0..gridWidth)
        {
            for (y in 0..gridHeight)
            {
                val toRepair = (grid.maxArmorInCell - grid.getArmorValue(x, y)) * ARMOR_REGEN * Global.getCombatEngine().elapsedInLastFrame
                grid.setArmorValue(x, y, min(grid.maxArmorInCell, toRepair + grid.getArmorValue(x, y)))
            }
        }

        stats.hullDamageTakenMult.modifyMult(id, DAMAGE_MULT)
        stats.armorDamageTakenMult.modifyMult(id, DAMAGE_MULT)
        stats.empDamageTakenMult.modifyMult(id, DAMAGE_MULT)
        stats.turnAcceleration.modifyMult(id, MANEUVERING_MULT)
        stats.acceleration.modifyMult(id, MANEUVERING_MULT)
        stats.maxTurnRate.modifyMult(id, MANEUVERING_MULT)

        if (Global.getCombatEngine().playerShip == stats.entity)
        {
            val system = (stats.entity as ShipAPI).system
            if (system != null)
            {
                val percent = DAMAGE_MULT * 100
                Global.getCombatEngine().maintainStatusForPlayerShip(
                    STATUSKEY1,
                    system.specAPI.iconSpriteName, system.displayName,
                    percent.roundToInt().toString() + "% less damage taken", false
                )
            }
        }
    }

    override fun unapply(stats: MutableShipStatsAPI?, id: String?)
    {
        stats ?: return
        stats.hullDamageTakenMult.unmodify(id)
        stats.armorDamageTakenMult.unmodify(id)
        stats.empDamageTakenMult.unmodify(id)
        stats.turnAcceleration.unmodify(id)
        stats.acceleration.unmodify(id)
        stats.maxTurnRate.unmodify(id)
    }

    companion object
    {
        const val DAMAGE_MULT = 0.5f
        const val MANEUVERING_MULT = 2f
        const val ARMOR_REGEN = 0.0333f
    }
}