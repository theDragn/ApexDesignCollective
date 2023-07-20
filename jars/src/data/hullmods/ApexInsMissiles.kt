package data.hullmods

import com.fs.starfarer.api.combat.BaseHullMod
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.WeaponAPI.WeaponType

class ApexInsMissiles : BaseHullMod()
{

    val RELOAD_PER_MIN = 0.1f

    override fun getDescriptionParam(index: Int, hullSize: ShipAPI.HullSize?): String
    {
        when (index)
        {
            0 -> return (RELOAD_PER_MIN * 100f).toInt().toString() + "%"
        }
        return ""
    }

    override fun applyEffectsAfterShipCreation(ship: ShipAPI?, id: String?)
    {
        for (wep in ship!!.allWeapons)
        {
            if (wep.type == WeaponType.MISSILE)
            {
                wep.ammoTracker.reloadSize = wep.spec.burstSize.toFloat()
                wep.ammoTracker.ammoPerSecond = wep.ammoPerSecond + wep.maxAmmo * RELOAD_PER_MIN / 60f
            }
        }
    }
}