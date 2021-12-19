package data.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;

public class ApexPhaseCoils extends BaseHullMod
{
    public static final float PHASE_COST_MULT = 0.5f;
    public static final float PHASE_COOLDOWN_REDUCTION = 67f;
    // now handled through custom phase cloak stats
    //public static final float PHASE_TIMEFLOW_MULT = 0.5f;

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id)
    {
        //stats.getDynamic().getStat(Stats.PHASE_TIME_BONUS_MULT).modifyMult(id, PHASE_TIMEFLOW_MULT);
        stats.getPhaseCloakActivationCostBonus().modifyMult(id, PHASE_COOLDOWN_REDUCTION / 100f);
        stats.getPhaseCloakCooldownBonus().modifyMult(id, 1f - PHASE_COOLDOWN_REDUCTION / 100f);
    }

    @Override
    public String getDescriptionParam(int index, ShipAPI.HullSize hullSize)
    {
        if (index == 1)
            return  (int)PHASE_COOLDOWN_REDUCTION + "%";
        if (index == 0)
            return  "2x";
        if (index == 2)
            return "50%";
        return null;
    }
}
