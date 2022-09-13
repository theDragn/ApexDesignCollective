package data.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

import java.awt.*;
import java.util.HashMap;

public class ApexCrampedMags extends BaseHullMod
{
    public static final float PENALTY = 0.9f;
    public static final HashMap<ShipAPI.HullSize, Integer> CAP_MAP = new HashMap<>();
    static
    {
        CAP_MAP.put(ShipAPI.HullSize.FIGHTER, 0);
        CAP_MAP.put(ShipAPI.HullSize.FRIGATE, 2);
        CAP_MAP.put(ShipAPI.HullSize.DESTROYER, 4);
        CAP_MAP.put(ShipAPI.HullSize.CRUISER, 6);
        CAP_MAP.put(ShipAPI.HullSize.CAPITAL_SHIP, 8);
    }

    public static final HashMap<WeaponAPI.WeaponSize, Integer> COST_MAP = new HashMap<>();
    static
    {
        COST_MAP.put(WeaponAPI.WeaponSize.SMALL, 1);
        COST_MAP.put(WeaponAPI.WeaponSize.MEDIUM, 2);
        COST_MAP.put(WeaponAPI.WeaponSize.LARGE, 4);
    }

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
        // costs to mount
        if (index == 1)
            return "" + COST_MAP.get(WeaponAPI.WeaponSize.SMALL);
        if (index == 2)
            return "" + COST_MAP.get(WeaponAPI.WeaponSize.MEDIUM);
        if (index == 3)
            return "" + COST_MAP.get(WeaponAPI.WeaponSize.LARGE);
        // caps per hull
        if (index == 4)
            return "" + CAP_MAP.get(ShipAPI.HullSize.FRIGATE);
        if (index == 5)
            return "" + CAP_MAP.get(ShipAPI.HullSize.DESTROYER);
        if (index == 6)
            return "" + CAP_MAP.get(ShipAPI.HullSize.CRUISER);
        if (index == 7)
            return "" + CAP_MAP.get(ShipAPI.HullSize.CAPITAL_SHIP);
        if (index == 8)
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
        int totalPoints = 0;
        int cap = CAP_MAP.get(ship.getHullSize());
        for (WeaponAPI wep : ship.getAllWeapons())
        {
            if (wep.getType().equals(WeaponAPI.WeaponType.MISSILE))
            {
                totalPoints += COST_MAP.get(wep.getSize());
            }
        }

        int excess = totalPoints - cap;
        if (excess > 0)
            return (float)Math.pow(PENALTY, excess);
        else
            return 1f;
    }
}
