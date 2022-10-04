package data.hullmods;

import com.fs.starfarer.api.combat.*;

public class ApexSonoraUplink extends BaseHullMod
{
    public static final float RANGE_BONUS = 75f;
    public static final float ENGAGEMENT_RANGE_PENALTY_MULT = -55f/75f; // engagement range capped at 55%
    public static final String ID = "apex_sonora_uplink";

    @Override
    public void advanceInCombat(ShipAPI ship, float amount)
    {
        if (ship.getAllWings().isEmpty())
            return;
        // only run the computation once
        if (!ship.getCustomData().containsKey(ID))
            ship.setCustomData(ID, getBonus(ship));

        float bonus = (float)ship.getCustomData().get(ID);
        //System.out.println(bonus);
        // reduce engagement range, if it's getting penalized
        if (bonus > 0f)
            ship.getMutableStats().getFighterWingRange().modifyPercent(ID, ENGAGEMENT_RANGE_PENALTY_MULT * RANGE_BONUS);
        // increase/decrease fighter range as necessary
        for (FighterWingAPI wing : ship.getAllWings())
        {
            for (ShipAPI fighter : wing.getWingMembers())
            {
                MutableShipStatsAPI stats = fighter.getMutableStats();
                stats.getMissileWeaponRangeBonus().modifyPercent(ID, bonus);
                //stats.getMissileMaxSpeedBonus().modifyPercent(ID, RANGE_BONUS);
                stats.getBallisticWeaponRangeBonus().modifyPercent(ID, bonus);
                stats.getEnergyWeaponRangeBonus().modifyPercent(ID, bonus);
            }
        }
    }

    public String getDescriptionParam(int index, ShipAPI.HullSize hullSize)
    {
        if (index == 0) return "" + (int) (RANGE_BONUS) + "%";
        if (index == 1) return "" + (int) (-ENGAGEMENT_RANGE_PENALTY_MULT * RANGE_BONUS) + "%";
        return null;
    }

    public static float getBonus(ShipAPI ship)
    {
        float max = 0f;
        for (WeaponAPI wep : ship.getAllWeapons())
        {
            float bonus = wep.getRange() / wep.getSpec().getMaxRange();
            if (bonus > max)
                max = bonus;
        }
        max = max * 100f - 100f;
        return Math.min(max, RANGE_BONUS);
    }
}
