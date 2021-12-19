package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;

public class ApexDummyCivHull extends BaseHullMod
{
    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id)
    {
        ShipVariantAPI var = stats.getVariant();
        String hullid = var.getHullSpec().getBaseHullId();
        // in case this hullmod ends up on something else, somehow
        if (hullid.contains("apex_spectrum"))
        {
            if (var.hasHullMod("apex_spectrum_cargo") || var.hasHullMod("apex_spectrum_fuel"))
            {
                var.setHullSpecAPI(Global.getSettings().getHullSpec("apex_spectrum_refitdummy"));
            } else
            {
                var.setHullSpecAPI(Global.getSettings().getHullSpec("apex_spectrum"));
            }
        } else if (hullid.equals("apex_backscatter"))
        {
            if (var.hasHullMod("apex_spectrum_cargo") || var.hasHullMod("apex_spectrum_fuel"))
            {
                var.setHullSpecAPI(Global.getSettings().getHullSpec("apex_backscatter_refitdummy"));
            } else
            {
                var.setHullSpecAPI(Global.getSettings().getHullSpec("apex_backscatter"));
            }
        }
    }
}
