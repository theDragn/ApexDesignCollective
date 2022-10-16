package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;

import java.util.HashMap;
import java.util.Map;

public class ApexVariableLRM extends BaseHullMod
{
    public static final String SLOT = "WS0005";
    public static final String WEAPON_PREFIX = "apex_thundercloud_"; // start of weapon name
    public static final String HULLMOD_PREFIX = "apex_lrm_";  // start of hullmod name

    // points to the next weapon/hullmod suffix
    public static final Map<String, String> LOADOUT_CYCLE = new HashMap<>();

    static
    {
        LOADOUT_CYCLE.put("frag", "emp");
        LOADOUT_CYCLE.put("emp", "he");
        LOADOUT_CYCLE.put("he", "frag");
    }

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id)
    {

        if (stats.getEntity() == null)
            return;

        //WEAPONS

        // trigger a weapon switch if none of the selector hullmods are present (because one was removed, or because the ship was just spawned without one)
        boolean switchLoadout = true;
        for (String hullmod : LOADOUT_CYCLE.values())
        {
            if (stats.getVariant().getHullMods().contains(HULLMOD_PREFIX + hullmod))
            {
                switchLoadout = false;
                break;
            }
        }

        if (switchLoadout)
        {

            // default to frag if there's no weapons
            String newWeawpon = "frag";
            for (String key : LOADOUT_CYCLE.keySet())
            {
                // cycle to whatever the next weapon is, based on the weapon currently in the left slot
                if (stats.getVariant().getWeaponId(SLOT) != null && stats.getVariant().getWeaponId(SLOT).contains(key))
                {
                    newWeawpon = LOADOUT_CYCLE.get(key);
                }
            }

            // add hullmod to match new weapons
            stats.getVariant().addMod(HULLMOD_PREFIX + newWeawpon);

            // clear slots
            stats.getVariant().clearSlot(SLOT);
            // add guns
            stats.getVariant().addWeapon(SLOT, WEAPON_PREFIX + newWeawpon);
        }
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id)
    {
        // how the fuck can ALL of these be null
        // alex pls
        if (Global.getSector() == null || Global.getSector().getPlayerFleet() == null || Global.getSector().getPlayerFleet().getCargo() == null)
            return;
        if (Global.getSector().getPlayerFleet().getCargo().getStacksCopy() == null || Global.getSector().getPlayerFleet().getCargo().getStacksCopy().isEmpty())
            return;
        for (CargoStackAPI stack : Global.getSector().getPlayerFleet().getCargo().getStacksCopy())
        {
            if (stack.isWeaponStack() && stack.getWeaponSpecIfWeapon().getWeaponId().contains("apex_thundercloud"))
                Global.getSector().getPlayerFleet().getCargo().removeStack(stack);
        }
    }

    @Override
    public String getDescriptionParam(int index, ShipAPI.HullSize hullSize)
    {
        return "without the normal CR penalty";
    }
}