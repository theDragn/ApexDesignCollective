package data.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.ShipAPI;

public class ApexVariableLrmEmp extends BaseHullMod
{
    @Override
    public int getDisplaySortOrder()
    {
        return 100;
    }

    @Override
    public int getDisplayCategoryIndex()
    {
        return 3;
    }

    @Override
    public String getDescriptionParam(int index, ShipAPI.HullSize hullSize)
    {
        if (index == 0)
            return "kinetic";
        if (index == 1)
            return "high-explosive";
        if (index == 2)
            return "no CR penalty";
        return null;
    }
}
