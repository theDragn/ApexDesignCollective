package data.hullmods;

import org.magiclib.subsystems.MagicSubsystemsManager;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import utils.ApexUtils;
import org.magiclib.util.MagicIncompatibleHullmods;
import data.activators.ApexFlareActivator;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static utils.ApexUtils.text;

public class ApexFlareSystemHullmod extends BaseHullMod
{
    public static final HashMap<ShipAPI.HullSize, Integer> NUM_FLARES = new HashMap<>();
    static
    {
        NUM_FLARES.put(ShipAPI.HullSize.FRIGATE, 2);
        NUM_FLARES.put(ShipAPI.HullSize.DESTROYER, 2);
        NUM_FLARES.put(ShipAPI.HullSize.CRUISER, 4);
        NUM_FLARES.put(ShipAPI.HullSize.CAPITAL_SHIP, 4);
    }

    public static final Map<ShipAPI.HullSize, Float> supplyCostMap = new HashMap<ShipAPI.HullSize, Float>();
    static {
        supplyCostMap.put(ShipAPI.HullSize.DEFAULT, 0f);
        supplyCostMap.put(ShipAPI.HullSize.FRIGATE, 1f);
        supplyCostMap.put(ShipAPI.HullSize.DESTROYER, 1f);
        supplyCostMap.put(ShipAPI.HullSize.CRUISER, 2f);
        supplyCostMap.put(ShipAPI.HullSize.CAPITAL_SHIP, 2f);
    }

    public static final Map<ShipAPI.HullSize, Float> smodCostMap = new HashMap<ShipAPI.HullSize, Float>();
    static {
        smodCostMap.put(ShipAPI.HullSize.DEFAULT, 0f);
        smodCostMap.put(ShipAPI.HullSize.FRIGATE, 0f);
        smodCostMap.put(ShipAPI.HullSize.DESTROYER, 0f);
        smodCostMap.put(ShipAPI.HullSize.CRUISER, 0f);
        smodCostMap.put(ShipAPI.HullSize.CAPITAL_SHIP, 0f);
    }
    public static final float BASE_COOLDOWN = 20f;

    private static final Set<String> BLOCKED_HULLMODS = new HashSet<>();
    static
    {
        BLOCKED_HULLMODS.add("apex_armor_repairer");
        BLOCKED_HULLMODS.add("apex_cryo_projector");
    }

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id)
    {
        if (stats.getVariant().getSMods().contains("apex_flare_system"))
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
                        "apex_flare_system"
                );
            }
        }
        MagicSubsystemsManager.addSubsystemToShip(ship, new ApexFlareActivator(ship));
    }


    @Override
    public String getDescriptionParam(int index, ShipAPI.HullSize hullSize)
    {
        if (index == 0)
            return supplyCostMap.get(ShipAPI.HullSize.FRIGATE).intValue() + "";
        if (index == 1)
            return supplyCostMap.get(ShipAPI.HullSize.DESTROYER).intValue() + "";
        if (index == 2)
            return supplyCostMap.get(ShipAPI.HullSize.CRUISER).intValue() + "";
        if (index == 3)
            return supplyCostMap.get(ShipAPI.HullSize.CAPITAL_SHIP).intValue() + "";
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
            /*if (ship.getVariant().getSMods().contains("apex_flare_system"))
            {
                tooltip.addPara(text("nozz3") + " %s.", 10f, Misc.getPositiveHighlightColor(), Misc.getHighlightColor(), smodCostMap.get(hullSize).intValue() + "");
            } else
            {
                tooltip.addPara(text("nozz4") + " %s.",10f, Misc.getPositiveHighlightColor(), Misc.getHighlightColor(), smodCostMap.get(hullSize).intValue() + "");
            }*/
        }
    }
    @Override
    public boolean hasSModEffect() {
        return true;
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
        return nozzles != 0 && ship.getHullSpec().getHullId().contains("apex_") && !ship.getHullSpec().getHullId().contains("apex_ins");
    }

    public String getUnapplicableReason(ShipAPI ship)
    {
        if (ship == null)
        {
            return "Ship does not exist, what the fuck";
        }

        if (!ship.getHullSpec().getHullId().contains("apex_") || ship.getHullSpec().getHullId().contains("apex_ins"))
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
