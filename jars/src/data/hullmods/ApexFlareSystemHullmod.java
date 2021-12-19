package data.hullmods;

import apexsubs.ApexSubsystemUtils;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.scripts.util.MagicIncompatibleHullmods;
import data.subsystems.ApexFlareSubsystem;

import java.util.HashSet;
import java.util.Set;

public class ApexFlareSystemHullmod extends BaseHullMod
{

    public static final float NUM_FLARES = 3f; // time in seconds for regen to be applied
    public static final float BASE_COOLDOWN = 20f;

    public static final String line1 = "\n• Fires guided flares from the ship's nozzles, with a " + (int)BASE_COOLDOWN + " second cooldown.";

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
            int nozzles = 0;
            for (WeaponSlotAPI slot : ship.getHullSpec().getAllWeaponSlotsCopy())
            {
                if (slot.isSystemSlot())
                    nozzles++;
            }
            if (!ship.getHullSpec().getHullId().contains("apex_"))
                nozzles = 0;
            tooltip.addSectionHeading("Details", Alignment.MID, pad);
            tooltip.addPara(line1, 0f, Misc.getHighlightColor(), (int)BASE_COOLDOWN + " second");
            TooltipMakerAPI text = tooltip.beginImageWithText("graphics/hullmods/apex_nozzle.png", 40);
            text.addPara("The number of projectiles fired by the system depends on the number of nozzles built into the hull. This hull has " + nozzles + " nozzles, and the system fires three projectiles per nozzle.", 0, Misc.getHighlightColor(), nozzles + "", "three");
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
            return "Can only be installed on Apex Design Collective ships.";
        }
        int nozzles = 0;
        for (WeaponSlotAPI slot : ship.getHullSpec().getAllWeaponSlotsCopy())
        {
            if (slot.isSystemSlot())
                nozzles++;
        }
        if (nozzles == 0)
            return "Cannot be installed on ships without projector nozzles.";
        for (String hullmod : BLOCKED_HULLMODS)
        {
            if (ship.getVariant().getHullMods().contains(hullmod))
            {
                return "Incompatible with other nozzle-based subsystems.";
            }
        }
        return null;
    }
}
