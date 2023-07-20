package data.hullmods;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.Misc;

import static utils.ApexUtils.text;

public class ApexExpandedSquadrons extends BaseHullMod
{

    public static final float REFIT_TIME_MULT = 0.65f;
    public static final float CREW_PER_DECK = 50f;

    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id)
    {
        stats.getFighterRefitTimeMult().modifyMult(id, REFIT_TIME_MULT);
        if (stats.getVariant() != null && !stats.getVariant().hasHullMod("apex_spectrum_cargo") && !stats.getVariant().hasHullMod("apex_spectrum_fuel"))
            stats.getMinCrewMod().modifyFlat(id, (stats.getNumFighterBays().getBaseValue() * CREW_PER_DECK));
    }

    public void advanceInCombat(ShipAPI ship, float amount)
    {
        if (ship == null || !ship.isAlive())
            return;
        for (FighterLaunchBayAPI bay : ship.getLaunchBaysCopy())
        {
            if (bay.getWing() == null)
                continue;
            // instant extra fighters when the ship is first deployed
            if (ship.getFullTimeDeployed() < 2f)
                bay.setFastReplacements(6);
            int addForWing = Math.min(bay.getWing().getSpec().getNumFighters(), 6 - bay.getWing().getSpec().getNumFighters());
            int maxTotal = bay.getWing().getSpec().getNumFighters() + addForWing;
            int actualAdd = maxTotal - bay.getWing().getWingMembers().size();
            if (actualAdd > 0)
            {
                bay.setExtraDeployments(actualAdd);
                bay.setExtraDeploymentLimit(maxTotal);
                bay.setExtraDuration(1000000f);
            }
            // if some of our extra fighters are destroyed
            if (bay.getWing().getWingMembers().size() < bay.getWing().getSpec().getNumFighters() + addForWing && bay.getWing().getWingMembers().size() >= bay.getWing().getSpec().getNumFighters())
            {
                // haha magic number go brrrr
                // seriously I just guessed at this shit
                bay.setCurrRate(bay.getCurrRate() - amount * 0.017f);
            }
        }
    }

    @Override
    public String getDescriptionParam(int index, ShipAPI.HullSize hullSize)
    {
        if (index == 0)
            return text("exps1");
        if (index == 1)
            return Misc.getRoundedValue(100f - REFIT_TIME_MULT * 100f) + "%";
        if (index == 2)
            return text("exps2");
        if (index == 3)
            return (int) CREW_PER_DECK + "";
        return null;
    }
}
