package data.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lazywizard.lazylib.combat.CombatUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static data.shipsystems.ApexDefenseUplink.*;

public class ApexDefenseUplinkPlugin implements EveryFrameCombatPlugin
{
    public HashMap<ShipAPI, Float> targets = new HashMap<>();
    private CombatEngineAPI engine;

    public ApexDefenseUplinkPlugin()
    {

    }

    @Override
    public void init(CombatEngineAPI engine)
    {
        this.engine = engine;
    }

    @Override
    public void advance(float amount, List<InputEventAPI> events)
    {
        // committing O(n^2) sins
        for (ShipAPI ship : engine.getShips())
        {
            if (ship.getHullSize().equals(ShipAPI.HullSize.FIGHTER))
                continue;
            // we've got a ship with a buffer system
            // apply buffs to ships in range
            if (ship.getSystem() != null && ship.getSystem().getSpecAPI().getId().equals("apex_uplink") && ship.getSystem().isOn())
            {
                float range = ship.getMutableStats().getSystemRangeBonus().computeEffective(ApexDefenseUplink.RANGE);
                for (ShipAPI shipToBuff : CombatUtils.getShipsWithinRange(ship.getLocation(), range))
                {
                    if (shipToBuff.getHullSize().equals(ShipAPI.HullSize.FIGHTER) || !shipToBuff.isAlive() || ship.getOwner() != shipToBuff.getOwner())
                        continue;
                    targets.put(shipToBuff, 1f);
                }
            }
        }
        List<ShipAPI> toRemove = new ArrayList<>();
        for (ShipAPI ship : targets.keySet())
        {
            float lifetime = Math.max(targets.get(ship) - amount, 0);
            targets.put(ship, lifetime);
            if (lifetime > 0)
            {
                if (Global.getCombatEngine().getPlayerShip().equals(ship))
                    Global.getCombatEngine().maintainStatusForPlayerShip(BUFF_ID,
                            Global.getSettings().getShipSystemSpec("apex_uplink").getIconSpriteName(),
                            Global.getSettings().getShipSystemSpec("apex_uplink").getName(),
                            "Armor and shield performance improved",
                            false
                    );
                ship.setJitterUnder(BUFF_ID, JITTER_COLOR, 1f * lifetime, 3, 2f * lifetime, 3f * lifetime);
                ship.setJitterShields(false);
                ship.getMutableStats().getEffectiveArmorBonus().modifyMult(BUFF_ID, ARMOR_EFFECTIVE_MULT);
                ship.getMutableStats().getShieldDamageTakenMult().modifyMult(BUFF_ID, SHIELD_EFFICIENCY_MULT);
            } else {
                ship.getMutableStats().getEffectiveArmorBonus().unmodify(BUFF_ID);
                ship.getMutableStats().getShieldDamageTakenMult().unmodify(BUFF_ID);
                toRemove.add(ship);
            }
        }
        for (ShipAPI ship : toRemove)
            targets.remove(ship);
    }

    @Override
    public void renderInWorldCoords(ViewportAPI viewport)
    {

    }

    @Override
    public void renderInUICoords(ViewportAPI viewport)
    {

    }

    @Override
    public void processInputPreCoreControls(float amount, List<InputEventAPI> events)
    {

    }
}
