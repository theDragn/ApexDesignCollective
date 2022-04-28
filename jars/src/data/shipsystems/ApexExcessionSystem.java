package data.shipsystems;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;

import static data.hullmods.ApexExcessionReactor.MAX_SYSTEM_CHARGE;
import static data.hullmods.ApexExcessionReactor.damageMap;

public class ApexExcessionSystem extends BaseShipSystemScript
{
    private static final float MAXIMUM_DAMAGE_BOOST = 1f;

    private boolean runOnce = false;
    private float damageMult = 0f;


    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel)
    {
        if (!runOnce && stats.getEntity() != null)
        {
            ShipAPI ship = (ShipAPI)stats.getEntity();
            float fluxLevel = ship.getFluxLevel();
            damageMult = 1f + fluxLevel * MAXIMUM_DAMAGE_BOOST;
            stats.getEnergyWeaponDamageMult().modifyMult(id, damageMult);
            runOnce = true;

            float charge = MAX_SYSTEM_CHARGE * fluxLevel;
            damageMap.put(ship, Math.min(damageMap.get(ship) + charge, MAX_SYSTEM_CHARGE));
            ship.getFluxTracker().decreaseFlux(Math.min(ship.getFluxTracker().getCurrFlux() * 0.25f, 3000f));
        }

    }

    @Override
    public void unapply(MutableShipStatsAPI stats, String id)
    {
        runOnce = false;
        stats.getEnergyWeaponDamageMult().unmodify(id);
    }

    @Override
    public StatusData getStatusData(int index, State state, float effectLevel)
    {
        if (index == 0)
        {
            return new StatusData("+ " + (int)(damageMult * 100f - 100f) + "% energy weapon damage", false);
        }
        return null;
    }
}
