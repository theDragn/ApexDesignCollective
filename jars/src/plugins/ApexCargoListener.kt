package plugins

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.BaseCampaignEventListener
import com.fs.starfarer.api.campaign.CargoAPI
import com.fs.starfarer.api.campaign.FleetEncounterContextPlugin

class ApexCargoListener(permaRegister: Boolean) : BaseCampaignEventListener(permaRegister)
{

    override fun reportEncounterLootGenerated(plugin: FleetEncounterContextPlugin?, loot: CargoAPI?)
    {
        plugin ?: return; loot ?: return
        val toRemove = ArrayList<Pair<String, Int>>()
        for (wep in loot.weapons)
        {
            val spec = Global.getSettings().getWeaponSpec(wep.item)
            spec ?: continue
            if (spec.tags.contains("SYSTEM"))
                toRemove.add(Pair(wep.item, wep.count))
        }
        for (thing in toRemove)
        {
            loot.removeWeapons(thing.first, thing.second)
        }
    }
}