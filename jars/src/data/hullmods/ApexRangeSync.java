package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.listeners.WeaponBaseRangeModifier;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import org.magiclib.util.MagicIncompatibleHullmods;

import java.awt.*;
import java.util.HashSet;
import java.util.Set;

import static utils.ApexUtils.text;

public class ApexRangeSync extends BaseHullMod
{
    // averages weapon ranges for each weapon size class
    // janky as fuck

    private static final float MAX_RANGE_BOOST = 1.33f;
    private static final float SMOD_MAX_RANGE_BOOST = 1.66f;

    private static final Set<String> BLOCKED_HULLMODS = new HashSet<>();

    static
    {
        BLOCKED_HULLMODS.add("apex_super_range_sync");
        // TODO: any others that need to be blocked?
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
                        "apex_range_sync",
                        hullmod
                );
                return;
            }
        }
        ApexSyncListener listener = new ApexSyncListener();
        if (ship.getVariant().getSMods().contains("apex_range_sync"))
            listener.maxBoost = SMOD_MAX_RANGE_BOOST;
        ship.addListener(listener);
    }

    @Override
    public String getDescriptionParam(int index, ShipAPI.HullSize hullSize)
    {
        if (index == 0)
            return (int) (MAX_RANGE_BOOST * 100f - 100f) + "%";
        return null;
    }

    public boolean isApplicableToShip(ShipAPI ship)
    {
        if (ship == null)
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
                return text("hmerror1") + " " + Global.getSettings().getHullModSpec(hullmod).getDisplayName() + ".";
            }
        }
        return null;
    }

    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec)
    {
        if (ship == null)
            return;
        Color incompatTextColor = Misc.getTextColor();
        for (String hullmod : BLOCKED_HULLMODS)
        {
            if (ship.getVariant().getHullMods().contains(hullmod))
            {
                incompatTextColor = Misc.getNegativeHighlightColor();
            }
        }
        tooltip.addPara("\n%s", 0, incompatTextColor, text("rngsync1"));
        /*if (ship.getVariant().getSMods().contains("apex_range_sync"))
        {
            tooltip.addPara(text("rngsync2"), 10f, Misc.getPositiveHighlightColor(), Misc.getHighlightColor(), (int) (SMOD_MAX_RANGE_BOOST * 100f - 100f) + "%");
        } else
        {
            tooltip.addPara(text("rngsync3"), 10f, Misc.getPositiveHighlightColor(), Misc.getHighlightColor(), (int) (SMOD_MAX_RANGE_BOOST * 100f - 100f) + "%");
        }*/
    }

    @Override
    public String getSModDescriptionParam(int index, ShipAPI.HullSize hullSize) {
        if (index == 0) return (int)(SMOD_MAX_RANGE_BOOST * 100f - 100f) + "%";
        return null;
    }

    @Override
    public boolean hasSModEffect() {
        return true;
    }

    // they call me Janky Kang
    public class ApexSyncListener implements WeaponBaseRangeModifier
    {
        private float average = 0f;

        public float maxBoost = MAX_RANGE_BOOST;

        // no clue how computationally expensive this is, but it's O(n) to get the average, so O(n^2) to get the average if everything is firing
        // fortunately we only need to compute it once, so we do that the first time any gun on the ship fires and save it
        private boolean didAve = false;

        @Override
        public float getWeaponBaseRangeFlatMod(ShipAPI ship, WeaponAPI weapon)
        {
            if (weapon.getType() == WeaponAPI.WeaponType.MISSILE
                    || weapon.getSpec().getAIHints().contains(WeaponAPI.AIHints.PD)
                    || weapon.getSlot() == null
                    || weapon.getSlot().isBuiltIn()
                    || weapon.isDecorative()
                    || weapon.getSpec().getMaxRange() == 0
                    || weapon.getSlot().isSystemSlot()
                    || weapon.getSpec().getAIHints().contains(WeaponAPI.AIHints.SYSTEM))
                return 0f;
            if (weapon.getId().contains("apex_repair") || weapon.getId().contains("apex_cryo"))
                return 0f;
            if (weapon.getSpec().getMaxRange() == 0f)
                return 1f;
            if (!didAve)
            {
                average = getAverageRange(ship);
                didAve = true;
            }
            if (average == 0)
                return 0;
            float adjustedRange = getAdjustedBaseRange(ship, weapon);
            return Math.min(average - adjustedRange, adjustedRange * (maxBoost - 1f));
        }

        @Override
        public float getWeaponBaseRangePercentMod(ShipAPI shipAPI, WeaponAPI weaponAPI)
        {
            return 0;
        }

        @Override
        public float getWeaponBaseRangeMultMod(ShipAPI shipAPI, WeaponAPI weaponAPI)
        {
            return 1;
        }

        private float getAverageRange(ShipAPI ship)
        {
            int numWeps = 0;
            float total = 0f;
            for (int i = 0; i < ship.getAllWeapons().size(); i++)
            {
                WeaponAPI wep = ship.getAllWeapons().get(i);
                if (wep.getSpec().getAIHints().contains(WeaponAPI.AIHints.PD) || wep.getType() == WeaponAPI.WeaponType.MISSILE || wep.getSlot().isBuiltIn() || wep.isDecorative() || wep.getSpec().getMaxRange() == 0
                        || wep.getSlot().isSystemSlot()
                        || wep.getSpec().getAIHints().contains(WeaponAPI.AIHints.SYSTEM))
                    continue;
                numWeps++;
                total += getAdjustedBaseRange(ship, wep);
            }
            if (numWeps == 0)
                return 0f;
            return total / (float) numWeps;
        }

        private float getAdjustedBaseRange(ShipAPI ship, WeaponAPI weapon)
        {
            float range = weapon.getSpec().getMaxRange();
            if (ship.hasListenerOfClass(WeaponBaseRangeModifier.class))
            {
                for (WeaponBaseRangeModifier listener : ship.getListeners(WeaponBaseRangeModifier.class))
                {
                    if (listener == this)
                        continue;
                    range += listener.getWeaponBaseRangeFlatMod(ship, weapon);
                }
            }
            return range;
        }
    }
}
