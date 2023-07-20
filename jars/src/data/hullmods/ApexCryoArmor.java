package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import org.magiclib.util.MagicIncompatibleHullmods;

import java.awt.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static utils.ApexUtils.text;

public class ApexCryoArmor extends BaseHullMod
{

    private static final Set<String> BLOCKED_HULLMODS = new HashSet<>();
    static {
        BLOCKED_HULLMODS.add("heavyarmor");
        BLOCKED_HULLMODS.add("ugh_spongearmor");
        BLOCKED_HULLMODS.add("apex_armor");
        // complain to DME guy about this one, not me
        BLOCKED_HULLMODS.add("istl_monobloc");
        // needed to be done
        BLOCKED_HULLMODS.add("monjeau_armour");
        BLOCKED_HULLMODS.add("apex_excession_armor");
        BLOCKED_HULLMODS.add("eis_damperhull");
        // TODO: any others that need to be blocked?
    }

    private static final Map<ShipAPI.HullSize, Float> bonusMap = new HashMap<ShipAPI.HullSize, Float>();
    static {
        bonusMap.put(ShipAPI.HullSize.FIGHTER, 0f);
        bonusMap.put(ShipAPI.HullSize.FRIGATE, 100f);
        bonusMap.put(ShipAPI.HullSize.DESTROYER, 150f);
        bonusMap.put(ShipAPI.HullSize.CRUISER, 200f);
        bonusMap.put(ShipAPI.HullSize.CAPITAL_SHIP, 250f);
    }

    public static final float BEAM_DAMAGE_MULT = 0.7f;
    public static final float MANEUVERING_MULT = 0.9f;
    public static final float REPAIR_TIME_MULT = 1.5f;



    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id)
    {
        stats.getBeamDamageTakenMult().modifyMult(id, BEAM_DAMAGE_MULT);
        stats.getTurnAcceleration().modifyMult(id, MANEUVERING_MULT);
        stats.getAcceleration().modifyMult(id, MANEUVERING_MULT);
        stats.getArmorBonus().modifyFlat(id, bonusMap.get(hullSize));
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
        //if (!(ship.getShield() == null || ship.getShield().getType().equals(ShieldAPI.ShieldType.NONE) || ship.getShield().getType().equals(ShieldAPI.ShieldType.PHASE)))
        //    return false;
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
        //if (!(ship.getShield() == null || ship.getShield().getType().equals(ShieldAPI.ShieldType.NONE) || ship.getShield().getType().equals(ShieldAPI.ShieldType.PHASE)))
        //    return "Ship has a shield.";
        for (String hullmod : BLOCKED_HULLMODS)
        {
            if (ship.getVariant().getHullMods().contains(hullmod))
            {
                return text("hmerror1") + " " + Global.getSettings().getHullModSpec(hullmod).getDisplayName() + ".";
            }
        }
        return null;
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec)
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
            tooltip.addPara("\n%s", 0, incompatTextColor,text("cryoarmor1"));
        }
    }

    @Override
    public String getDescriptionParam(int index, ShipAPI.HullSize hullSize)
    {
        if (index == 0)
            return bonusMap.get(ShipAPI.HullSize.FRIGATE).intValue() + "";
        if (index == 1)
            return bonusMap.get(ShipAPI.HullSize.DESTROYER).intValue() + "";
        if (index == 2)
            return bonusMap.get(ShipAPI.HullSize.CRUISER).intValue() + "";
        if (index == 3)
            return bonusMap.get(ShipAPI.HullSize.CAPITAL_SHIP).intValue() + "";
        if (index == 4)
            return Misc.getRoundedValue(100f - BEAM_DAMAGE_MULT * 100f) + "%";
        if (index == 5)
            return Misc.getRoundedValue(100f - MANEUVERING_MULT * 100f) + "%";
        if (index == 6)
            return Misc.getRoundedValue(REPAIR_TIME_MULT * 100f - 100f) + "%";
        return null;
    }
}
