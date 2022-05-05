package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.listeners.WeaponBaseRangeModifier;
import com.fs.starfarer.api.combat.listeners.WeaponRangeModifier;
import data.scripts.util.MagicIncompatibleHullmods;
import data.weapons.ApexQGeffects;

import java.util.HashSet;
import java.util.Set;

public class ApexSuperRangeSync extends BaseHullMod
{
    // averages weapon ranges for each weapon size class, ignoring

    private static final float MAX_RANGE_BOOST = 2f;

    private static final Set<String> BLOCKED_HULLMODS = new HashSet<>();

    static
    {
        BLOCKED_HULLMODS.add("apex_range_sync");
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
                        hullmod,
                        "apex_super_range_sync"
                );
                return;
            }
        }
        ship.addListener(new ApexSuperSyncListener());
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
                return "Incompatible with " + Global.getSettings().getHullModSpec(hullmod).getDisplayName();
            }
        }
        return null;
    }

    // they call me Janky Kang
    public static class ApexSuperSyncListener implements WeaponBaseRangeModifier
    {
        private float average = 0;
        boolean didAve = false;

        public float maxBoost = MAX_RANGE_BOOST;

        // no clue how computationally expensive this is, but it's O(n) to get the average, so O(n^2) to get the average if everything is firing
        // so we should be conservative on computing the average

        @Override
        public float getWeaponBaseRangeFlatMod(ShipAPI ship, WeaponAPI weapon)
        {
            if (weapon.isBeam() || weapon.getType() == WeaponAPI.WeaponType.MISSILE || weapon.getSpec().getAIHints().contains(WeaponAPI.AIHints.PD) || weapon.getSlot() == null)
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
            return Math.min(average - adjustedRange, adjustedRange);
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
                if (wep.isBeam() || wep.getSpec().getAIHints().contains(WeaponAPI.AIHints.PD) || wep.getType() == WeaponAPI.WeaponType.MISSILE || wep.getSlot().isBuiltIn())
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