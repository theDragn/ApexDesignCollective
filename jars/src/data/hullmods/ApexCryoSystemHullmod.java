package data.hullmods;

import activators.ActivatorManager;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.ApexUtils;
import org.magiclib.util.MagicIncompatibleHullmods;
import data.activators.ApexCryoActivator;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static data.ApexUtils.text;


public class ApexCryoSystemHullmod extends BaseHullMod
{

    public static final float CRYO_BUFF_DURATION = 10f;
    public static final float MAX_COOLANT_LOCKON_RANGE = 2000f; // distance at which a repair target is considered in range
    public static final float BASE_COOLDOWN = 30f;

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
        smodCostMap.put(HullSize.CRUISER, 0f);
        smodCostMap.put(HullSize.CAPITAL_SHIP, 0f);
    }

    public static final Map<HullSize, Float> dissMap = new HashMap<HullSize, Float>();
    static {
        dissMap.put(HullSize.DEFAULT, 0f);
        dissMap.put(HullSize.FRIGATE, 100f);
        dissMap.put(HullSize.DESTROYER, 150f);
        dissMap.put(HullSize.CRUISER, 200f);
        dissMap.put(HullSize.CAPITAL_SHIP, 300f);
    }

    public static final String line1 = "\n• " + text("cryop1")+
            "\n• " + text("repper2") +
            "\n• " + text("cryop2");
    public static final String line2 = "• " + text("cryop3");
    public static final String line3 = "• " + text("cryop4");
    public static final String[] line2sub = {
            dissMap.get(HullSize.FRIGATE).intValue()+ "",
            dissMap.get(HullSize.DESTROYER).intValue() + "",
            dissMap.get(HullSize.CRUISER).intValue() + "",
            dissMap.get(HullSize.CAPITAL_SHIP).intValue() + "",
            (int)CRYO_BUFF_DURATION + ""
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
        ActivatorManager.addActivator(ship, new ApexCryoActivator(ship));
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
            int nozzles = ApexUtils.getNumNozzles(ship);
            if (!ship.getHullSpec().getHullId().contains("apex_"))
                nozzles = 0;
            tooltip.addSectionHeading(text("Details"), Alignment.MID, pad);
            tooltip.addPara(line1, 0f, Misc.getHighlightColor(), (int)BASE_COOLDOWN + " " + text("nozz8"));
            tooltip.addPara(line2, 0f, Misc.getHighlightColor(), line2sub);
            tooltip.addPara(line3, 0f);
            TooltipMakerAPI text = tooltip.beginImageWithText("graphics/hullmods/apex_nozzle.png", 40);
            text.addPara(text("nozz1") + " " + nozzles + " " + text("nozz2"), 0, Misc.getHighlightColor(), nozzles + "", text("nozzOne"));
            tooltip.addImageWithText(pad);
            if (ship.getVariant().getSMods().contains("apex_cryo_projector"))
            {
                tooltip.addPara(text("nozz3") + " %s.", 10f, Misc.getPositiveHighlightColor(), Misc.getHighlightColor(), smodCostMap.get(hullSize).intValue() + "");
            } else
            {
                tooltip.addPara(text("nozz4") + " %s.",10f, Misc.getPositiveHighlightColor(), Misc.getHighlightColor(), smodCostMap.get(hullSize).intValue() + "");
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
            return text("nozz5");
        }
        int nozzles = 0;
        ApexUtils.getNumNozzles(ship);
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
