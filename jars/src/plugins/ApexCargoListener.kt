package plugins

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.BaseCampaignEventListener
import com.fs.starfarer.api.campaign.CargoAPI
import com.fs.starfarer.api.campaign.FleetEncounterContextPlugin
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.combat.WeaponAPI

class ApexCargoListener(permaRegister: Boolean) : BaseCampaignEventListener(permaRegister)
{

    override fun reportEncounterLootGenerated(plugin: FleetEncounterContextPlugin?, loot: CargoAPI?)
    {
        loot?.stacksCopy ?: return
        for (stack in loot.stacksCopy)
        {
            if (stack.isWeaponStack && stack.weaponSpecIfWeapon.aiHints.contains(WeaponAPI.AIHints.SYSTEM))
                loot.removeStack(stack)
        }
    }

    override fun reportPlayerOpenedMarket(market: MarketAPI?)
    {
        Global.getSector()?.playerFleet?.cargo?.stacksCopy ?: return
        val cargo = Global.getSector().playerFleet.cargo
        for (stack in cargo.stacksCopy)
        {
            if (stack.isWeaponStack && stack.weaponSpecIfWeapon.aiHints.contains(WeaponAPI.AIHints.SYSTEM))
                cargo.removeStack(stack)
        }
    }
}