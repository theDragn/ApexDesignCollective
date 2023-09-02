package data.shipsystems

import com.fs.starfarer.api.combat.MutableShipStatsAPI
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript
import com.fs.starfarer.api.plugins.ShipSystemStatsScript

class ApexExcessionSystemNew: BaseShipSystemScript()
{
    var runOnce = false

    override fun apply(
        stats: MutableShipStatsAPI,
        id: String,
        state: ShipSystemStatsScript.State,
        effectLevel: Float
    )
    {
        stats.entity ?: return
        stats.energyWeaponDamageMult.modifyMult(id, 1.5f)
        stats.ballisticWeaponDamageMult.modifyMult(id, 1.5f)

        if (!runOnce)
        {

        }
    }
}