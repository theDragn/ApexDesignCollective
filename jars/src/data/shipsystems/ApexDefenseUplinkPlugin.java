package data.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lazywizard.lazylib.MathUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static data.shipsystems.ApexDefenseUplink.*;

public class ApexDefenseUplinkPlugin implements EveryFrameCombatPlugin
{
    public HashMap<ShipAPI, Float> targets = new HashMap<>();
    private CombatEngineAPI engine;
    private IntervalUtil update = new IntervalUtil(0.25f, 0.75f);
    private List<ShipAPI> shipsWithSystem = new ArrayList<>();


    public ApexDefenseUplinkPlugin()
    {

    }

    @Override
    public void init(CombatEngineAPI engine)
    {
        this.engine = engine;
        for (ShipAPI ship : engine.getShips())
        {
            if (ship.getSystem() != null && ship.getSystem().getSpecAPI().getId().equals("apex_uplink"))
                shipsWithSystem.add(ship);
        }
    }

    @Override
    public void advance(float amount, List<InputEventAPI> events)
    {
        // check list of ships that we know have the system every frame
        // this is still O(n^2) but it's a lot better than checking all ships for the system presence/activation every frame
        update.advance(amount);
        for (ShipAPI bufferShip : shipsWithSystem)
        {
            if (bufferShip.getHullSize().equals(ShipAPI.HullSize.FIGHTER))
                continue;
            // we've got a ship with a buffer system
            // apply buffs to ships in range
            if (bufferShip.getSystem() != null && bufferShip.getSystem().isOn())
            {
                float range = bufferShip.getMutableStats().getSystemRangeBonus().computeEffective(ApexDefenseUplink.RANGE);
                range = range * range;
                for (ShipAPI shipToBuff : engine.getShips())
                {
                    if (shipToBuff.getHullSize().equals(ShipAPI.HullSize.FIGHTER)
                            || MathUtils.getDistanceSquared(shipToBuff.getLocation(), bufferShip.getLocation()) > range
                            || !shipToBuff.isAlive()
                            || bufferShip.getOwner() != shipToBuff.getOwner())
                        continue;
                    targets.put(shipToBuff, 1f);
                }
            }
        }
        // update buffs every frame
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

        // update list of ships with system every ~0.5 sec.
        if (!update.intervalElapsed())
            return;
        shipsWithSystem.clear();
        for (ShipAPI ship : engine.getShips())
        {
            if (ship.getSystem() != null && ship.getSystem().getSpecAPI().getId().equals("apex_uplink"))
                shipsWithSystem.add(ship);
        }
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
