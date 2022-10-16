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
import data.ApexUtils;
import data.scripts.util.MagicIncompatibleHullmods;
import data.subsystems.ApexCryoSubsystem;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


public class ApexCryoSystemHullmod extends BaseHullMod
{

    public static final float CRYO_GENERATION_MULT = 1.3f;
    public static final float CRYO_BUFF_DURATION = 10f;
    public static final float CRYO_BUFF_EFFECTIVENESS_VS_LARGER = 0.5f;
    public static final float MAX_COOLANT_LOCKON_RANGE = 1500f; // distance at which a repair target is considered in range
    public static final float BASE_COOLDOWN = 30f;

    // TODO: when 0.95.1 hits, make this increase DP too
    public static final Map<HullSize, Float> supplyCostMap = new HashMap<HullSize, Float>();
    static {
        supplyCostMap.put(HullSize.DEFAULT, 0f);
        supplyCostMap.put(HullSize.FRIGATE, 1f);
        supplyCostMap.put(HullSize.DESTROYER, 1f);
        supplyCostMap.put(HullSize.CRUISER, 2f);
        supplyCostMap.put(HullSize.CAPITAL_SHIP, 3f);
    }

    public static final Map<HullSize, Float> smodCostMap = new HashMap<HullSize, Float>();
    static {
        smodCostMap.put(HullSize.DEFAULT, 0f);
        smodCostMap.put(HullSize.FRIGATE, 0f);
        smodCostMap.put(HullSize.DESTROYER, 0f);
        smodCostMap.put(HullSize.CRUISER, 1f);
        smodCostMap.put(HullSize.CAPITAL_SHIP, 2f);
    }


    public static final String line1 = "\n• Fires magnetically-guided blobs of cryogenic coolant that increase flux dissipation. " +
            "\n• Has a " + (int)BASE_COOLDOWN + " second cooldown and generates soft flux on use. " +
            "\n• Targets the selected ally, if in range. If no allied target is selected, targets allies with non-zero flux within range.";
    public static final String line2 = "• Projectiles increase flux dissipation by %s for %s seconds.";
    public static final String line3 = "• Hitting an ally with multiple projectiles increases the duration of the buff, with diminishing returns.";
    public static final String line4 = "• Projectiles from smaller ships are %s less effective on larger ships.";
    public static final String[] line2sub = {
            (int)(100f*CRYO_GENERATION_MULT - 100f) + "%",
            (int) CRYO_BUFF_DURATION + ""
    };
    public static final String[] line4sub = {
            (int)(100f- CRYO_BUFF_EFFECTIVENESS_VS_LARGER *100f) + "%"
    };

    private static final Set<String> BLOCKED_HULLMODS = new HashSet<>();
    static
    {
        BLOCKED_HULLMODS.add("apex_flare_system");
        BLOCKED_HULLMODS.add("apex_armor_repairer");
    }

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id)
    {
        if (stats.getVariant().getSMods().contains("apex_cryo_projector"))
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
                        "apex_cryo_projector"
                );
            }
        }
        ApexSubsystemUtils.queueSubsystemForShip(ship, ApexCryoSubsystem.class);
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
            tooltip.addPara(line3, 0f);
            tooltip.addPara(line4, 0f, Misc.getHighlightColor(), line4sub);
            TooltipMakerAPI text = tooltip.beginImageWithText("graphics/hullmods/apex_nozzle.png", 40);
            text.addPara("The number of projectiles fired by the system depends on the number of nozzles built into the hull. This hull has " + nozzles + " nozzles, and the system fires one projectile per nozzle.", 0, Misc.getHighlightColor(), nozzles + "", "one");
            tooltip.addImageWithText(pad);
            if (ship.getVariant().getSMods().contains("apex_cryo_projector"))
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
        int nozzles = ApexUtils.getNumNozzles(ship);
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
        ApexUtils.getNumNozzles(ship);
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
