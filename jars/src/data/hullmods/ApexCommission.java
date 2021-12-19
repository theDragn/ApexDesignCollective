package data.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;

public class ApexCommission extends BaseHullMod
{
    public static final float INSTA_REPAIR = 15f;
    public static final float ARMOR_BONUS = 10f;

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id)
    {
        stats.getEffectiveArmorBonus().modifyPercent(id, ARMOR_BONUS);
        stats.getDynamic().getStat(Stats.INSTA_REPAIR_FRACTION).modifyFlat(id, INSTA_REPAIR/100f);
    }

    @Override
    public String getDescriptionParam(int index, ShipAPI.HullSize hullSize)
    {
        if (index == 0)
            return (int)ARMOR_BONUS + "%";
        if (index == 1)
            return (int)INSTA_REPAIR + "%";
        return null;
    }
}
