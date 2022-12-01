package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;

public class ApexQuasarBricker extends BaseHullMod
{

    private static final float WEAPON_MALFUNCTION_PROB = 0.1f;
    private static final float ENGINE_MALFUNCTION_PROB = 0.01f;

    @Override
    public void advanceInCombat(ShipAPI ship, float amount)
    {
        // if you got this far, just remove the hullmod from apex_quasar.ship
        if (ship.getOwner() == 0 && !Global.getCombatEngine().isSimulation())
        {
            MutableShipStatsAPI stats = ship.getMutableStats();
            stats.getCriticalMalfunctionChance().modifyFlat("lol", 0.5f);
            stats.getWeaponMalfunctionChance().modifyFlat("lol", WEAPON_MALFUNCTION_PROB);
            stats.getEngineMalfunctionChance().modifyFlat("lol", ENGINE_MALFUNCTION_PROB);
        }
    }

    public String getDescriptionParam(int index, ShipAPI.HullSize hullSize)
    {
        return null;
    }
}
