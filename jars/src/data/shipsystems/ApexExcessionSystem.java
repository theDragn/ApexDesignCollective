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


    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel)
    {
        if (!runOnce && stats.getEntity() != null)
        {
            ShipAPI ship = (ShipAPI)stats.getEntity();
            float fluxLevel = ship.getFluxLevel();
            runOnce = true;

            float charge = MAX_SYSTEM_CHARGE * fluxLevel;
            damageMap.put(ship, Math.min(damageMap.get(ship) + charge, MAX_SYSTEM_CHARGE));
            ship.getFluxTracker().decreaseFlux(Math.min(ship.getFluxTracker().getCurrFlux() * 0.25f, 3000f));

            doGraphics(ship);
        }

    }

    // TODO
    private void doGraphics(ShipAPI ship)
    {
    }

    @Override
    public void unapply(MutableShipStatsAPI stats, String id)
    {
        runOnce = false;
    }

    @Override
    public StatusData getStatusData(int index, State state, float effectLevel)
    {
        return null;
    }
}
