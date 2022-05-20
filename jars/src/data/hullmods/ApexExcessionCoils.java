package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

import java.awt.*;

public class ApexExcessionCoils extends BaseHullMod
{
    public static final float BASE_TIMEFLOW_MULT = 2f; // read by phase script
    public static final float MAXIMUM_TIMEFLOW_MULT = 5f; // maximum flux-based timeflow multiplier, read by phase script
    public static final float PHASE_COOLDOWN_REDUCTION = 66.66f; // also used for cloak activation cost


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
        return null;
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec)
    {
        if (ship != null)
        {
            float pad = 10f;
            tooltip.addSectionHeading("Details", Alignment.MID, pad);

            tooltip.addPara("\n• Reduces the base timeflow bonus from phasing to %s.",
                    0,
                    Misc.getHighlightColor(),
                    (int) (BASE_TIMEFLOW_MULT) + "x");

            tooltip.addPara("• %s less phase activation cost and cooldown duration.",
                    0,
                    Misc.getHighlightColor(),
                    (int) (PHASE_COOLDOWN_REDUCTION) + "%");

            tooltip.addPara("• Phase timeflow increases with flux level, up to %s at %s flux.",
                    0f,
                    Misc.getHighlightColor(),
                    (int) (MAXIMUM_TIMEFLOW_MULT * BASE_TIMEFLOW_MULT) + "x", "100%");

            tooltip.addPara("• %s by phase coil stress.",
                    0f,
                    Misc.getHighlightColor(),
                    "Unaffected");
        }
    }
}
