package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.scripts.util.MagicIncompatibleHullmods;


import java.awt.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ApexArmor extends BaseHullMod
{
    private static final Map<HullSize, Float> bonusMap = new HashMap<HullSize, Float>();
    static {
        bonusMap.put(HullSize.FIGHTER, 0f);
        bonusMap.put(HullSize.FRIGATE, 100f);
        bonusMap.put(HullSize.DESTROYER, 200f);
        bonusMap.put(HullSize.CRUISER, 250f);
        bonusMap.put(HullSize.CAPITAL_SHIP, 300f);
    }

    private static final Set<String> BLOCKED_HULLMODS = new HashSet<>();
    static {
        BLOCKED_HULLMODS.add("heavyarmor");
        BLOCKED_HULLMODS.add("ugh_spongearmor");
        BLOCKED_HULLMODS.add("apex_cryo_armor");
        // complain to DME guy about this one, not me
        BLOCKED_HULLMODS.add("istl_monobloc");
        // needed to be done
        BLOCKED_HULLMODS.add("monjeau_armour");
        BLOCKED_HULLMODS.add("apex_excession_armor");
        // TODO: any others that need to be blocked?
    }
    public static final float REDUCTION_BONUS = 0.05f; // flat bonus to max armor damage reduction
    public static final float MIN_ARMOR_FRACTION_BONUS = 0.05f;
    public static final float REGEN_MULT = 1.33f;



    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id)
    {
        stats.getMaxArmorDamageReduction().modifyFlat(id, REDUCTION_BONUS);
        stats.getMinArmorFraction().modifyFlat(id, MIN_ARMOR_FRACTION_BONUS);
        stats.getArmorBonus().modifyFlat(id, (Float) bonusMap.get(hullSize));
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id)
    {
        for (String hullmod : BLOCKED_HULLMODS)
        {
            if (ship.getVariant().getHullMods().contains(hullmod))
            {
                MagicIncompatibleHullmods.removeHullmodWithWarning(
                        ship.getVariant(),
                        hullmod,
                        "apex_armor"
                );
            }
        }
    }

    public boolean isApplicableToShip(ShipAPI ship)
    {
        if(ship == null)
            return false;
        for (String hullmod : BLOCKED_HULLMODS)
        {
            if (ship.getVariant().getHullMods().contains(hullmod))
                return false;
        }
        return true;
    }

    public String getUnapplicableReason(ShipAPI ship)
    {
        if (ship == null)
        {
            return "Ship does not exist, what the fuck";
        }
        for (String hullmod : BLOCKED_HULLMODS)
        {
            if (ship.getVariant().getHullMods().contains(hullmod))
            {
                return "Incompatible with " + Global.getSettings().getHullModSpec(hullmod).getDisplayName() + ".";
            }
        }
        return null;
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec)
    {
        if (ship != null)
        {
            Color incompatTextColor = Misc.getTextColor();
            for (String hullmod : BLOCKED_HULLMODS)
            {
                if (ship.getVariant().getHullMods().contains(hullmod))
                {
                    incompatTextColor = Misc.getNegativeHighlightColor();
                }
            }
            tooltip.addPara("\n%s", 0, incompatTextColor, "Incompatible with Cryocooled Armor Lattice, Heavy Armor, and other hullmods that provide significant bonuses to armor rating.");
        }
    }

    @Override
    public String getDescriptionParam(int index, HullSize hullSize)
    {
        if (index == 0)
            return bonusMap.get(HullSize.FRIGATE).intValue() + "";
        if (index == 1)
            return bonusMap.get(HullSize.DESTROYER).intValue() + "";
        if (index == 2)
            return bonusMap.get(HullSize.CRUISER).intValue() + "";
        if (index == 3)
            return bonusMap.get(HullSize.CAPITAL_SHIP).intValue() + "";
        if (index == 4)
            return (int)(MIN_ARMOR_FRACTION_BONUS * 100f) +"%";
        if (index == 5)
            return (int)(REDUCTION_BONUS * 100f) + "%";
        if (index == 6)
            return (int)(100f*(REGEN_MULT - 1f)) + "%";
        return null;
    }
}
