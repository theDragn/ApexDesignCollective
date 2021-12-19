package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;

import java.util.HashMap;
import java.util.Map;

// controls ammo swap
public class ApexVariableWarheads extends BaseHullMod
{
    public static final String LEFT_SLOT = "WS0015";
    public static final String RIGHT_SLOT = "WS0016";
    public static final String WEAPON_PREFIX_LEFT = "apex_vls_left_";
    public static final String WEAPON_PREFIX_RIGHT = "apex_vls_right_";

    // points to the next weapon/hullmod suffix
    public static final Map<String, String> LOADOUT_CYCLE = new HashMap<>();

    static
    {
        LOADOUT_CYCLE.put("acid", "kin");
        LOADOUT_CYCLE.put("kin", "tachyon");
        LOADOUT_CYCLE.put("tachyon", "acid");
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
            if (stats.getVariant().getHullMods().contains("apex_warheads_" + hullmod))
            {
                switchLoadout = false;
                break;
            }
        }

        if (switchLoadout)
        {

            // default to nanoacid if there's no weapons
            String newWeawpon = "acid";
            for (String key : LOADOUT_CYCLE.keySet())
            {
                // cycle to whatever the next weapon is, based on the weapon currently in the left slot
                if (stats.getVariant().getWeaponId(LEFT_SLOT) != null && stats.getVariant().getWeaponId(LEFT_SLOT).contains(key))
                {
                    newWeawpon = LOADOUT_CYCLE.get(key);
                }
            }

            // add hullmod to match new weapons
            stats.getVariant().addMod("apex_warheads_" + newWeawpon);

            // clear slots
            stats.getVariant().clearSlot(LEFT_SLOT);
            stats.getVariant().clearSlot(RIGHT_SLOT);
            // add guns
            stats.getVariant().addWeapon(LEFT_SLOT, WEAPON_PREFIX_LEFT + newWeawpon);
            stats.getVariant().addWeapon(RIGHT_SLOT, WEAPON_PREFIX_RIGHT + newWeawpon);
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
            if (stack.isWeaponStack() && stack.getWeaponSpecIfWeapon().getWeaponId().contains("apex_vls"))
                Global.getSector().getPlayerFleet().getCargo().removeStack(stack);
        }
    }

    @Override
    public String getDescriptionParam(int index, ShipAPI.HullSize hullSize)
    {
        return "without the normal CR penalty";
    }
}
