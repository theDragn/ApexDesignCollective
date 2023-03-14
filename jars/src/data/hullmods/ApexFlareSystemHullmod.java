package data.hullmods;

import apexsubs.ApexSubsystemUtils;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.ApexUtils;
import data.scripts.util.MagicIncompatibleHullmods;
import data.subsystems.ApexFlareSubsystem;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import static data.ApexUtils.text;

public class ApexFlareSystemHullmod extends BaseHullMod
{
    public static final HashMap<ShipAPI.HullSize, Integer> NUM_FLARES = new HashMap<>();
    static
    {
        NUM_FLARES.put(ShipAPI.HullSize.FRIGATE, 2);
        NUM_FLARES.put(ShipAPI.HullSize.DESTROYER, 2);
        NUM_FLARES.put(ShipAPI.HullSize.CRUISER, 3);
        NUM_FLARES.put(ShipAPI.HullSize.CAPITAL_SHIP, 4);
    }
    public static final float BASE_COOLDOWN = 20f;

    private static final Set<String> BLOCKED_HULLMODS = new HashSet<>();
    static
    {
        BLOCKED_HULLMODS.add("apex_armor_repairer");
        BLOCKED_HULLMODS.add("apex_cryo_projector");
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
                        "apex_flare_system"
                );
            }
        }
        ApexSubsystemUtils.queueSubsystemForShip(ship, ApexFlareSubsystem.class);
    }


    @Override
    public String getDescriptionParam(int index, ShipAPI.HullSize hullSize)
    {
        return null;
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
        if (ship != null)
        {
            float pad = 10f;
            int nozzles = ApexUtils.getNumNozzles(ship);
            if (!ship.getHullSpec().getHullId().contains("apex_"))
                nozzles = 0;
            tooltip.addSectionHeading(text("Details"), Alignment.MID, pad);
            tooltip.addPara(
                    "\n• " + text("flare1"),
                    0f,
                    Misc.getHighlightColor(),
                    (int)BASE_COOLDOWN + " " + text("nozz8"));
            TooltipMakerAPI text = tooltip.beginImageWithText("graphics/hullmods/apex_nozzle.png", 40);
            text.addPara(
                    text("flare2"),
                    0, Misc.getHighlightColor(),
                    nozzles + "", "" + NUM_FLARES.get(ship.getHullSize()));
            tooltip.addImageWithText(pad);
        }
    }

    public boolean isApplicableToShip(ShipAPI ship)
    {
        if(ship == null)
            return false;
        int nozzles = 0;
        for (WeaponSlotAPI slot : ship.getHullSpec().getAllWeaponSlotsCopy())
        {
            if (slot.isSystemSlot())
                nozzles++;
        }
        for (String hullmod : BLOCKED_HULLMODS)
        {
            if (ship.getVariant().getHullMods().contains(hullmod))
            {
                return false;
            }
        }
        return nozzles != 0 && ship.getHullSpec().getHullId().contains("apex_");
    }

    public String getUnapplicableReason(ShipAPI ship)
    {
        if (ship == null)
        {
            return "Ship does not exist, what the fuck";
        }

        if (!ship.getHullSpec().getHullId().contains("apex_"))
        {
            return text("nozz5");
        }
        int nozzles = 0;
        for (WeaponSlotAPI slot : ship.getHullSpec().getAllWeaponSlotsCopy())
        {
            if (slot.isSystemSlot())
                nozzles++;
        }
        if (nozzles == 0)
            return text("nozz6");
        for (String hullmod : BLOCKED_HULLMODS)
        {
            if (ship.getVariant().getHullMods().contains(hullmod))
            {
                return text("nozz7");
            }
        }
        return null;
    }
}
