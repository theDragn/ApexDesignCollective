package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.listeners.WeaponBaseRangeModifier;
import com.fs.starfarer.api.combat.listeners.WeaponRangeModifier;
import com.fs.starfarer.api.loading.WeaponSpecAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.scripts.util.MagicIncompatibleHullmods;
import data.weapons.ApexQGeffects;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.HashSet;
import java.util.Set;

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
                return "Incompatible with " + Global.getSettings().getHullModSpec(hullmod).getDisplayName() + ".";
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
        tooltip.addPara("\n%s", 0, incompatTextColor, "Incompatible with hullmods that provide variable range bonuses.");
        if (ship.getVariant().getSMods().contains("apex_range_sync"))
        {
            tooltip.addPara("S-mod Bonus: The maximum range bonus that can be given by this hullmod is increased to %s.", 10f, Misc.getPositiveHighlightColor(), Misc.getHighlightColor(), (int) (SMOD_MAX_RANGE_BOOST * 100f - 100f) + "%");
        } else
        {
            tooltip.addPara("If this hullmod is built in, the maximum range bonus that can be given by this hullmod will be increased to %s.", 10f, Misc.getPositiveHighlightColor(), Misc.getHighlightColor(), (int) (SMOD_MAX_RANGE_BOOST * 100f - 100f) + "%");
        }
    }

    // they call me Janky Kang
    public static class ApexSyncListener implements WeaponBaseRangeModifier
    {
        private float sAve = 0f;
        private float mAve = 0f;
        private float lAve = 0f;

        public float maxBoost = MAX_RANGE_BOOST;

        // no clue how computationally expensive this is, but it's O(n) to get the average, so O(n^2) to get the average if everything is firing
        // so we should be conservative on computing the average
        private boolean didSmallAverage = false;
        private boolean didMedAverage = false;
        private boolean didLargeAverage = false;

        @Override
        public float getWeaponBaseRangeFlatMod(ShipAPI ship, WeaponAPI weapon)
        {
            if (weapon.isBeam() || weapon.getType() == WeaponAPI.WeaponType.MISSILE || weapon.getSpec().getAIHints().contains(WeaponAPI.AIHints.PD) || weapon.getSlot() == null)
                return 0f;
            if (weapon.getId().contains("apex_repair") || weapon.getId().contains("apex_cryo"))
                return 0f;
            if (weapon.getSpec().getMaxRange() == 0f)
                return 1f;
            float average = getAverageForWeaponSize(ship, weapon);
            if (average == 0)
                return 0;
            if (ship.getVariant().getSMods().contains("apex_range_sync"))
                maxBoost = SMOD_MAX_RANGE_BOOST;
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

        private float getAverageRange(ShipAPI ship, WeaponAPI.WeaponSize size)
        {
            int numWeps = 0;
            float total = 0f;
            for (int i = 0; i < ship.getAllWeapons().size(); i++)
            {
                WeaponAPI wep = ship.getAllWeapons().get(i);
                if (wep.isBeam() || wep.getSpec().getAIHints().contains(WeaponAPI.AIHints.PD) || wep.getType() == WeaponAPI.WeaponType.MISSILE || wep.getSize() != size || wep.getSlot().isBuiltIn())
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

        private float getAverageForWeaponSize(ShipAPI ship, WeaponAPI weapon)
        {
            switch (weapon.getSize())
            {
                case SMALL:
                    if (!didSmallAverage)
                    {
                        didSmallAverage = true;
                        sAve = getAverageRange(ship, WeaponAPI.WeaponSize.SMALL);
                    }
                    return sAve;
                case MEDIUM:
                    if (!didMedAverage)
                    {
                        didMedAverage = true;
                        mAve = getAverageRange(ship, WeaponAPI.WeaponSize.MEDIUM);
                    }
                    return mAve;
                case LARGE:
                    if (!didLargeAverage)
                    {
                        didLargeAverage = true;
                        lAve = getAverageRange(ship, WeaponAPI.WeaponSize.LARGE);
                    }
                    return lAve;
                default:
                    return sAve;
            }
        }
    }
}
