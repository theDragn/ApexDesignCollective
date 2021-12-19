package data.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

import java.awt.*;

public class ApexCrampedMags extends BaseHullMod
{
    public static final float PENALTY = 0.8f;

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id)
    {
        float penalty = getPenalty(ship);
        ship.getMutableStats().getMissileRoFMult().modifyMult(id, penalty);
    }

    @Override
    public String getDescriptionParam(int index, ShipAPI.HullSize hullSize)
    {
        if (index == 0)
            return "prevents the installation of a full missile suite without compromises";
        if (index == 1)
            return (int)(100f - 100f * PENALTY)+ "%";
        return null;
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec)
    {
        if (ship != null)
        {
            Color incompatTextColor = Misc.getPositiveHighlightColor();
            float penalty = getPenalty(ship);
            if (penalty < 1f)
                incompatTextColor = Misc.getNegativeHighlightColor();
            tooltip.addPara("\nThe current penalty is %s.", 0, incompatTextColor,  (int)(100f - 100f * getPenalty(ship)) + "%");
        }
    }

    private float getPenalty(ShipAPI ship)
    {
        int largeMissiles = 0;
        int mediumMissiles = 0;
        for (WeaponAPI wep : ship.getAllWeapons())
        {
            if (wep.getType().equals(WeaponAPI.WeaponType.MISSILE))
            {
                if (wep.getSize().equals(WeaponAPI.WeaponSize.MEDIUM))
                    mediumMissiles++;
                if (wep.getSize().equals(WeaponAPI.WeaponSize.LARGE))
                    largeMissiles++;
            }
        }

        int totalPenalty = 0;
        totalPenalty = Math.max(largeMissiles - 1, mediumMissiles + largeMissiles - 2);
        if (totalPenalty < 0)
            totalPenalty = 0;
        return (float)Math.pow(PENALTY, totalPenalty);
    }
}
