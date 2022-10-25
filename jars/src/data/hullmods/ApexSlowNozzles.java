package data.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;

public class ApexSlowNozzles extends BaseHullMod
{
    public static final float NOZZLE_COOLDOWN_MULT = 1.15f;

    // effects are handled by the nozzle subsystems

    @Override
    public String getDescriptionParam(int index, ShipAPI.HullSize hullSize)
    {
        if (index == 0)
            return  (int)(100f * NOZZLE_COOLDOWN_MULT - 100f) + "%";
        return null;
    }
}