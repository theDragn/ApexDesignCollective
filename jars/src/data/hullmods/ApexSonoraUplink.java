package data.hullmods;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.WeaponBaseRangeModifier;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

import java.awt.*;

public class ApexSonoraUplink extends BaseHullMod
{
    public static final float RANGE_BONUS = 75f;
    public static final float ENGAGEMENT_RANGE_PENALTY_MULT = -55f/75f; // engagement range capped at 55%
    public static final float RANGE_PER_OP = 15f;
    public static final float RANGE_BOOST_SMALL = 50f;
    public static final float RANGE_BOOST_MED = 25f;
    public static final String ID = "apex_sonora_uplink";

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id)
    {
        ship.addListener(new ApexSonoraRangeBuff(getOp(ship)));
    }

    @Override
    public void advanceInCombat(ShipAPI ship, float amount)
    {
        if (ship.getAllWings().isEmpty())
            return;
        // only run the computation once
        if (!ship.getCustomData().containsKey(ID))
            ship.setCustomData(ID, getBonus(ship));

        float bonus = (float)ship.getCustomData().get(ID);
        //System.out.println(bonus);
        // reduce engagement range, if it's getting penalized
        if (bonus > 0f)
            ship.getMutableStats().getFighterWingRange().modifyPercent(ID, ENGAGEMENT_RANGE_PENALTY_MULT * RANGE_BONUS);
        // increase/decrease fighter range as necessary
        for (FighterWingAPI wing : ship.getAllWings())
        {
            for (ShipAPI fighter : wing.getWingMembers())
            {
                MutableShipStatsAPI stats = fighter.getMutableStats();
                stats.getMissileWeaponRangeBonus().modifyPercent(ID, bonus);
                stats.getBallisticWeaponRangeBonus().modifyPercent(ID, bonus);
                stats.getEnergyWeaponRangeBonus().modifyPercent(ID, bonus);
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
                return op / RANGE_PER_OP * RANGE_BOOST_SMALL;
            if (weapon.getSize().equals(WeaponAPI.WeaponSize.MEDIUM))
                return op / RANGE_PER_OP * RANGE_BOOST_MED;
            return 0;
        }
    }

    public String getDescriptionParam(int index, ShipAPI.HullSize hullSize)
    {
        if (index == 0) return "" + (int) (RANGE_BONUS) + "%";
        if (index == 1) return "" + (int) (-ENGAGEMENT_RANGE_PENALTY_MULT * RANGE_BONUS) + "%";
        if (index == 2) return "" + (int) RANGE_PER_OP + " OP";
        if (index == 3) return "" + (int) RANGE_BOOST_SMALL + "su";
        if (index == 4) return "" + (int) RANGE_BOOST_MED + "su";
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
            tooltip.addPara("Fighter weapon range: %s", 0f, Misc.getPositiveHighlightColor(), "+" + (int)bonus);
            tooltip.addPara("Fighter engagement range: %s", 0f, Misc.getNegativeHighlightColor(), "-" + (int)Math.abs(bonus * ENGAGEMENT_RANGE_PENALTY_MULT));
        } else {
            Color highlight = bonus == 0 ? Misc.getHighlightColor() : Misc.getNegativeHighlightColor();
            String prefix = bonus == 0 ? "" : "-";
            tooltip.addPara("Fighter weapon range: %s", 0f, highlight, prefix + (int)bonus);
            tooltip.addPara("Fighter engagement range: %s", 0f, Misc.getHighlightColor(), "+0%");
        }
        int op = getOp(ship);
        Color highlight = op == 0 ? Misc.getHighlightColor() : Misc.getPositiveHighlightColor();

        tooltip.addPara("Small weapon base range: %s", 0f, highlight, "+" + (int)(op * RANGE_BOOST_SMALL) + "su");
        tooltip.addPara("Medium weapon base range: %s", 0f, highlight, "+" + (int)(op * RANGE_BOOST_MED) + "su");
    }

    public static float getBonus(ShipAPI ship)
    {
        float max = 0f;
        for (WeaponAPI wep : ship.getAllWeapons())
        {
            float bonus = wep.getRange() / wep.getSpec().getMaxRange();
            if (bonus > max)
                max = bonus;
        }
        max = max * 100f - 100f;
        return Math.min(max, RANGE_BONUS);
    }

    public static int getOp(ShipAPI ship)
    {
        int op = 0;
        for (FighterWingAPI wing : ship.getAllWings())
        {
            op += wing.getSpec().getOpCost(ship.getMutableStats());
        }
        return op;
    }
}
