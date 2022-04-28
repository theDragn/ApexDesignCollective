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
    public static final float MAXIMUM_TIMEFLOW_MULT = 4f; // maximum flux-based timeflow multiplier, read by phase script
    public static final float PHASE_COOLDOWN_REDUCTION = 67f; // also used for cloak activation cost
    public static final float MAXIMUM_SPEED_MULT = 2f;


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

            tooltip.addPara("\n• Reduces the timeflow bonus from phasing to %s.",
                    0,
                    Misc.getHighlightColor(),
                    (int) (BASE_TIMEFLOW_MULT) + "x");

            tooltip.addPara("• Reduces phase activation cost and cooldown duration by %s.",
                    0,
                    Misc.getHighlightColor(),
                    (int) (PHASE_COOLDOWN_REDUCTION) + "%");

            tooltip.addPara("• %s by phase coil stress.",
                    0,
                    Misc.getHighlightColor(),
                    "Unaffected");

            tooltip.addPara("• Phase timeflow increases with flux level, up to %s at %s flux.",
                    0f,
                    Misc.getHighlightColor(),
                    (int) (MAXIMUM_TIMEFLOW_MULT * BASE_TIMEFLOW_MULT) + "x", "100%");

            tooltip.addPara("• Speed increases with flux level, up to %s at %s flux.",
                    0f,
                    Misc.getHighlightColor(),
                    (int) (MAXIMUM_SPEED_MULT * 100f - 100f) + "%", "100%");
        }
    }

    @Override
    public void advanceInCombat(ShipAPI ship, float amount)
    {
        ship.getMutableStats().getMaxSpeed().modifyMult("excessioncoils", 1f + (MAXIMUM_SPEED_MULT - 1f) * ship.getFluxLevel());
    }
}
