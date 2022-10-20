package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.WeaponBaseRangeModifier;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import data.scripts.util.MagicIncompatibleHullmods;
import org.lazywizard.lazylib.MathUtils;

import java.awt.*;
import java.util.HashSet;
import java.util.Set;

public class ApexNetworkTargeter extends BaseHullMod
{
    public static final float FTR_RANGE_PERCENT_MAX = 75f;
    public static final float ENGAGEMENT_RANGE_PENALTY_MULT = -55f/75f; // engagement range capped at 55%
    public static final float FTR_RANGE_CAP = 2000f;

    public static final float RANGE_PER_OP = 15f;
    public static final float RANGE_BOOST_SMALL = 50f;
    public static final float RANGE_BOOST_MED = 25f;
    public static final float MAX_RANGE_AFTER_BOOST = 900f;

    public static final String ID = "apex_net_target";

    private static final Set<String> BLOCKED_HULLMODS = new HashSet<>();

    static
    {
        BLOCKED_HULLMODS.add("ballistic_rangefinder");
        // TODO: any others that need to be blocked?
    }

    private IntervalUtil update = new IntervalUtil(0.1f, 0.2f);

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
                        "apex_network_targeter"
                );
            }
        }
        ship.addListener(new ApexSonoraRangeBuff(getOp(ship)));
    }

    @Override
    public void advanceInCombat(ShipAPI ship, float amount)
    {
        update.advance(amount);
        if (!update.intervalElapsed())
            return;
        if (ship.getAllWings().isEmpty())
            return;
        // only run the computation once
        if (!ship.getCustomData().containsKey(ID))
            ship.setCustomData(ID, getBonus(ship));

        float bonus = (float)ship.getCustomData().get(ID);
        //System.out.println(bonus);
        // reduce engagement range, if it's getting penalized
        if (bonus > 0f)
            ship.getMutableStats().getFighterWingRange().modifyPercent(ID, ENGAGEMENT_RANGE_PENALTY_MULT * FTR_RANGE_PERCENT_MAX);
        // increase/decrease fighter range as necessary
        for (FighterWingAPI wing : ship.getAllWings())
        {
            for (ShipAPI fighter : wing.getWingMembers())
            {
                MutableShipStatsAPI stats = fighter.getMutableStats();
                stats.getBallisticWeaponRangeBonus().modifyPercent(ID, bonus);
                stats.getEnergyWeaponRangeBonus().modifyPercent(ID, bonus);
                stats.getWeaponRangeMultPastThreshold().modifyMult(ID, 0f);
                stats.getWeaponRangeThreshold().modifyFlat(ID, FTR_RANGE_CAP);

                stats.getMissileWeaponRangeBonus().modifyPercent(ID, bonus);
                float maxRange = 0;
                for (WeaponAPI wep : fighter.getAllWeapons())
                {
                    if (!wep.getType().equals(WeaponAPI.WeaponType.MISSILE))
                        continue;
                    float range = wep.getRange();
                    if (range > maxRange)
                        maxRange = range;
                }
                if (maxRange > FTR_RANGE_CAP)
                {
                    stats.getMissileWeaponRangeBonus().modifyMult(ID+"_cap", FTR_RANGE_CAP / maxRange);
                }
            }
        }
    }

    public static class ApexSonoraRangeBuff implements WeaponBaseRangeModifier
    {
        private int op = 0;
        public ApexSonoraRangeBuff(int op)
        {
            this.op = op;
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

        @Override
        public float getWeaponBaseRangeFlatMod(ShipAPI ship, WeaponAPI weapon)
        {
            if (weapon.getSize().equals(WeaponAPI.WeaponSize.SMALL))
                return MathUtils.clamp(op / RANGE_PER_OP * RANGE_BOOST_SMALL, 0, MAX_RANGE_AFTER_BOOST - weapon.getSpec().getMaxRange());
            if (weapon.getSize().equals(WeaponAPI.WeaponSize.MEDIUM))
                return MathUtils.clamp(op / RANGE_PER_OP * RANGE_BOOST_MED, 0, MAX_RANGE_AFTER_BOOST - weapon.getSpec().getMaxRange());;
            return 0;
        }
    }

    public String getDescriptionParam(int index, ShipAPI.HullSize hullSize)
    {
        if (index == 0) return "" + (int) (FTR_RANGE_PERCENT_MAX) + "%";
        if (index == 1) return "" + (int) (FTR_RANGE_CAP);
        if (index == 2) return "" + (int) (-ENGAGEMENT_RANGE_PENALTY_MULT * FTR_RANGE_PERCENT_MAX) + "%";
        if (index == 3) return "" + (int) RANGE_PER_OP + " OP";
        if (index == 4) return "" + (int) RANGE_BOOST_SMALL + "";
        if (index == 5) return "" + (int) RANGE_BOOST_MED + "";
        if (index == 6) return "" + (int) MAX_RANGE_AFTER_BOOST + "";
        return null;
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec)
    {
        if (ship == null)
            return;
        float pad = 10f;
        tooltip.addSectionHeading("Current Effects", Alignment.MID, pad);
        int bonus = (int)getBonus(ship);
        if (bonus > 0)
        {
            tooltip.addPara("Fighter weapon range: %s", 0f, Misc.getPositiveHighlightColor(), "+" + (int)bonus + "%");
            tooltip.addPara("Fighter engagement range: %s", 0f, Misc.getNegativeHighlightColor(), "-" + (int)Math.abs(bonus * ENGAGEMENT_RANGE_PENALTY_MULT) + "%");
        } else {
            Color highlight = bonus == 0 ? Misc.getHighlightColor() : Misc.getNegativeHighlightColor();
            String prefix = bonus == 0 ? "+" : "-";
            tooltip.addPara("Fighter weapon range: %s", 0f, highlight, prefix + (int)Math.abs(bonus) + "%");
            tooltip.addPara("Fighter engagement range: %s", 0f, Misc.getHighlightColor(), "+0%");
        }
        int op = getOp(ship);
        float bonusRangeMult = op / RANGE_PER_OP;
        Color highlight = op == 0 ? Misc.getHighlightColor() : Misc.getPositiveHighlightColor();

        tooltip.addPara("Small weapon base range: %s", 0f, highlight, "+" + (int)(bonusRangeMult * RANGE_BOOST_SMALL) + "");
        tooltip.addPara("Medium weapon base range: %s", 0f, highlight, "+" + (int)(bonusRangeMult * RANGE_BOOST_MED) + "");
    }

    public static float getBonus(ShipAPI ship)
    {
        float min = Float.MAX_VALUE;
        for (WeaponAPI wep : ship.getAllWeapons())
        {
            float bonus = wep.getRange() / wep.getSpec().getMaxRange();
            //System.out.println(bonus);
            if (bonus < min && bonus != 1)
                min = bonus;
        }
        if (min == Float.MAX_VALUE)
            min = 1.0f;
        min = min * 100f - 100f;
        //System.out.println(min);
        return Math.min(min, FTR_RANGE_PERCENT_MAX);
    }

    public static int getOp(ShipAPI ship)
    {
        int op = 0;
        for (String wing : ship.getVariant().getNonBuiltInWings())
        {
            op += Global.getSettings().getFighterWingSpec(wing).getOpCost(ship.getMutableStats());
        }
        return op;
    }

    @Override
    public boolean isApplicableToShip(ShipAPI ship)
    {
        if (ship == null || ship.getNumFighterBays() == 0)
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
        if (ship.getNumFighterBays() == 0)
            return "Ship must have fighter bays.";
        for (String hullmod : BLOCKED_HULLMODS)
        {
            if (ship.getVariant().getHullMods().contains(hullmod))
            {
                return "Incompatible with " + Global.getSettings().getHullModSpec(hullmod).getDisplayName() + ".";
            }
        }

        return null;
    }
}
