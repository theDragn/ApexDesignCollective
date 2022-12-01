package data.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.ShipAPI;

import static data.ApexUtils.text;

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
            return text("vw2");
        if (index == 1)
            return text("vlrm1");
        if (index == 2)
            return text("vw3");
        return null;
    }
}
