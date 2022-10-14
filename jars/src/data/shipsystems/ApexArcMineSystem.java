package data.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import com.fs.starfarer.api.util.Misc;
import data.weapons.proj.ApexArcMineScript;

public class ApexArcMineSystem extends BaseShipSystemScript
{
    boolean runOnce = false;

    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel)
    {
        if (runOnce || stats.getEntity() == null)
            return;
        runOnce = true;
        ShipAPI ship = (ShipAPI)stats.getEntity();
        for (WeaponSlotAPI slot : ship.getHullSpec().getAllWeaponSlotsCopy())
        {
            if ((ship.getHullSpec().getBaseHullId().contains("lacerta") || slot.getId().contains("MINE")) && slot.isSystemSlot())
            {
                MissileAPI mine = (MissileAPI) Global.getCombatEngine().spawnProjectile(ship, null, "apex_arc_mine", slot.computePosition(ship), slot.computeMidArcAngle(ship) + 30f * (Misc.random.nextFloat() - 0.5f), null);
                Global.getCombatEngine().addPlugin(new ApexArcMineScript(ship, mine));
            }

        }
    }

    @Override
    public void unapply(MutableShipStatsAPI stats, String id)
    {
        runOnce = false;
    }
}
