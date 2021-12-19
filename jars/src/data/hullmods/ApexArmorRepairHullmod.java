package data.hullmods;

import apexsubs.ApexSubsystemUtils;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.scripts.util.MagicIncompatibleHullmods;
import data.subsystems.ApexArmorRepairSubsystem;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


public class ApexArmorRepairHullmod extends BaseHullMod
{
    public static final Map<HullSize, Float> regenMap = new HashMap<HullSize, Float>();
    static {
        regenMap.put(HullSize.DEFAULT, 0f);
        regenMap.put(HullSize.FRIGATE, 50f);
        regenMap.put(HullSize.DESTROYER, 100f);
        regenMap.put(HullSize.CRUISER, 175f);
        regenMap.put(HullSize.CAPITAL_SHIP, 300f);
    }

    // TODO: when 0.95.1 hits, make this increase DP too
    public static final Map<HullSize, Float> supplyCostMap = new HashMap<HullSize, Float>();
    static {
        supplyCostMap.put(HullSize.DEFAULT, 0f);
        supplyCostMap.put(HullSize.FRIGATE, 2f);
        supplyCostMap.put(HullSize.DESTROYER, 3f);
        supplyCostMap.put(HullSize.CRUISER, 4f);
        supplyCostMap.put(HullSize.CAPITAL_SHIP, 5f);
    }

    public static final Map<HullSize, Float> smodCostMap = new HashMap<HullSize, Float>();
    static {
        smodCostMap.put(HullSize.DEFAULT, 0f);
        smodCostMap.put(HullSize.FRIGATE, 1f);
        smodCostMap.put(HullSize.DESTROYER, 2f);
        smodCostMap.put(HullSize.CRUISER, 2f);
        smodCostMap.put(HullSize.CAPITAL_SHIP, 3f);
    }

    public static final float BASE_REGEN_DURATION = 10f; // time in seconds for regen to be applied
    public static final float MAX_REGEN_LOCKON_RANGE = 1500f; // distance at which a repair target is considered in range
    public static final float MAX_REGEN_FRACTION = 0.75f; // can't regen armor to more than this fraction of the base amount
    public static final float BASE_COOLDOWN = 30f; // cooldown time

    public static final String line1 = "\n• Fires magnetically-guided blobs of molten armor material that can repair allies. " +
            "\n• Has a " + (int)BASE_COOLDOWN + " second cooldown and generates soft flux on use. " +
            "\n• Targets the selected ally, if they are in range and can be repaired. If no allied target is selected, targets repairable allies within range.";
    public static final String line2 = "• Projectiles repair %s/%s/%s/%s armor over %s seconds, depending on the size of the source ship.";
    public static final String line3 = "• Cannot repair above %s of initial armor strength.\n• Multiple repair effects stack with diminishing returns.";
    public static final String line4 = "• Cannot repair %s without Cryocooled Armor Lattice, as shields are used to cool the molten material.";
    public static final String[] line2sub = {
            regenMap.get(HullSize.FRIGATE).intValue()+ "",
            regenMap.get(HullSize.DESTROYER).intValue() + "",
            regenMap.get(HullSize.CRUISER).intValue() + "",
            regenMap.get(HullSize.CAPITAL_SHIP).intValue() + "",
            (int)BASE_REGEN_DURATION + ""
    };
    public static final String[] line3sub = {
            (int)(MAX_REGEN_FRACTION * 100f) + "%"
    };
    public static final String[] line4sub = {
            "shieldless targets"
    };

    private static final Set<String> BLOCKED_HULLMODS = new HashSet<>();
    static
    {
        BLOCKED_HULLMODS.add("apex_flare_system");
        BLOCKED_HULLMODS.add("apex_cryo_projector");
    }

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id)
    {
        if (stats.getVariant().getSMods().contains("apex_armor_repairer"))
        {
            stats.getSuppliesPerMonth().modifyFlat(id, smodCostMap.get(hullSize));
            stats.getSuppliesToRecover().modifyFlat(id, smodCostMap.get(hullSize));
            stats.getDynamic().getMod(Stats.DEPLOYMENT_POINTS_MOD).modifyFlat(id, smodCostMap.get(hullSize));
        } else
        {
            stats.getSuppliesPerMonth().modifyFlat(id, supplyCostMap.get(hullSize));
            stats.getSuppliesToRecover().modifyFlat(id, supplyCostMap.get(hullSize));
            stats.getDynamic().getMod(Stats.DEPLOYMENT_POINTS_MOD).modifyFlat(id, supplyCostMap.get(hullSize));
        }
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
                        "apex_armor_repairer"
                );
            }
        }
        ApexSubsystemUtils.queueSubsystemForShip(ship, ApexArmorRepairSubsystem.class);
    }


    @Override
    public String getDescriptionParam(int index, HullSize hullSize)
    {
        if (index == 0)
            return supplyCostMap.get(HullSize.FRIGATE).intValue() + "";
        if (index == 1)
            return supplyCostMap.get(HullSize.DESTROYER).intValue() + "";
        if (index == 2)
            return supplyCostMap.get(HullSize.CRUISER).intValue() + "";
        if (index == 3)
            return supplyCostMap.get(HullSize.CAPITAL_SHIP).intValue() + "";
        return null;
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
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
            tooltip.addPara(line2, 0f, Misc.getHighlightColor(), line2sub);
            tooltip.addPara(line3, 0f, Misc.getHighlightColor(), line3sub);
            tooltip.addPara(line4, 0f, Misc.getHighlightColor(), line4sub);
            TooltipMakerAPI text = tooltip.beginImageWithText("graphics/hullmods/apex_nozzle.png", 40);
            text.addPara("The number of projectiles fired by the system depends on the number of nozzles built into the hull. This hull has " + nozzles + " nozzles, and the system fires one projectile per nozzle.", 0, Misc.getHighlightColor(), nozzles + "", "one");
            tooltip.addImageWithText(pad);
            if (ship.getVariant().getSMods().contains("apex_armor_repairer"))
            {
                tooltip.addPara("S-mod Bonus: The deployment and maintenance cost increase is reduced to %s.", 10f, Misc.getPositiveHighlightColor(), Misc.getHighlightColor(), smodCostMap.get(hullSize).intValue() + "");
            } else
            {
                tooltip.addPara("If this hullmod is built in, the deployment and maintenance cost increase will be reduced to %s.",10f, Misc.getPositiveHighlightColor(), Misc.getHighlightColor(), smodCostMap.get(hullSize).intValue() + "");
            }
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
