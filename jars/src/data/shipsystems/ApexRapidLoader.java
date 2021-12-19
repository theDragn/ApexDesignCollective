package data.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.plugins.ShipSystemStatsScript;

public class ApexRapidLoader extends BaseShipSystemScript
{
    private boolean runOnce = false;

    @Override
    public void apply(MutableShipStatsAPI stats, String id, ShipSystemStatsScript.State state, float effectLevel)
    {
        if (stats.getEntity() == null)
            return;
        if (!runOnce)
        {
            runOnce = true;
            ShipAPI ship = (ShipAPI)stats.getEntity();

            for (WeaponAPI weapon : ship.getAllWeapons())
            {
                if (weapon.getAmmoPerSecond() > 0)
                    weapon.setAmmo((int)Math.min(weapon.getMaxAmmo(), weapon.getAmmo() + weapon.getAmmoPerSecond() * 20f));
            }
        }
    }

    @Override
    public void unapply(MutableShipStatsAPI stats, String id)
    {
        runOnce = false;
    }
}
