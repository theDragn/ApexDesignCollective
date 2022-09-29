package data.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.FighterWingAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;

public class ApexSonoraUplink extends BaseHullMod
{
    public static final float RANGE_BONUS = 50f;
    public static final float ENGAGEMENT_RANGE_PENALTY = -55f;
    public static final String ID = "apex_sonora_uplink";

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id)
    {
        stats.getFighterWingRange().modifyPercent(ID, ENGAGEMENT_RANGE_PENALTY);
    }

    @Override
    public void advanceInCombat(ShipAPI ship, float amount)
    {
        if (ship.getAllWings().isEmpty())
            return;
        for (FighterWingAPI wing : ship.getAllWings())
        {
            for (ShipAPI fighter : wing.getWingMembers())
            {
                MutableShipStatsAPI stats = fighter.getMutableStats();
                stats.getMissileWeaponRangeBonus().modifyPercent(ID, RANGE_BONUS);
                stats.getMissileMaxSpeedBonus().modifyPercent(ID, RANGE_BONUS);
                stats.getBallisticWeaponRangeBonus().modifyPercent(ID, RANGE_BONUS);
                stats.getEnergyWeaponRangeBonus().modifyPercent(ID, RANGE_BONUS);
            }
        }
    }

    public String getDescriptionParam(int index, ShipAPI.HullSize hullSize)
    {
        if (index == 0) return "" + (int) (RANGE_BONUS) + "%";
        if (index == 1) return "" + (int) (-ENGAGEMENT_RANGE_PENALTY) + "%";
        return null;
    }
}
