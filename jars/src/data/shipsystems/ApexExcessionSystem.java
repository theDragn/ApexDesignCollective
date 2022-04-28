package data.shipsystems;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;

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
            damageMult = (((ShipAPI)(stats.getEntity())).getFluxLevel() + 1f) * MAXIMUM_DAMAGE_BOOST ;
            stats.getEnergyWeaponDamageMult().modifyMult(id, damageMult);
            runOnce = true;
        }
        stats.getPhaseCloakUpkeepCostBonus().modifyMult(id, -1f);
        stats.getPhaseCloakActivationCostBonus().modifyMult(id, -1f);
        stats.getEnergyWeaponFluxCostMod().modifyMult(id, -1f);
        stats.getBallisticWeaponFluxCostMod().modifyMult(id, -1f);
        stats.getMissileWeaponFluxCostMod().modifyMult(id, -1f);
    }

    @Override
    public void unapply(MutableShipStatsAPI stats, String id)
    {
        runOnce = false;
        stats.getEnergyWeaponDamageMult().unmodify(id);
        stats.getPhaseCloakUpkeepCostBonus().unmodify(id);
        stats.getPhaseCloakActivationCostBonus().unmodify(id);
        stats.getEnergyWeaponFluxCostMod().unmodify(id);
        stats.getBallisticWeaponFluxCostMod().unmodify(id);
        stats.getMissileWeaponFluxCostMod().unmodify(id);
    }

    @Override
    public StatusData getStatusData(int index, State state, float effectLevel)
    {
        if (index == 0)
        {
            return new StatusData("Flux generation reversed", false);
        } else if (index == 2)
        {
            return new StatusData("+ " + (int)(damageMult * 100f - 100f) + "% energy weapon damage", false);
        }
        return null;
    }
}
