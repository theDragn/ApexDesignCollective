package data.shipsystems.ai;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import data.shipsystems.ApexDefenseUplink;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

public class ApexDefenseUplinkAI implements ShipSystemAIScript
{
    private ShipAPI ship;
    private ShipSystemAPI system;
    private CombatEngineAPI engine;
    private IntervalUtil timer = new IntervalUtil(0.5f, 1f);

    @Override
    public void init(ShipAPI ship, ShipSystemAPI system, ShipwideAIFlags flags, CombatEngineAPI engine)
    {
        this.ship = ship;
        this.system = system;
        this.engine = engine;
    }

    @Override
    public void advance(float amount, Vector2f missileDangerDir, Vector2f collisionDangerDir, ShipAPI target)
    {
        timer.advance(amount);
        if (!timer.intervalElapsed())
            return;
        if (system.isChargedown() || system.isChargeup())
            return;
        boolean isOn = system.isOn();
        boolean shouldBeOn = shouldBeOn();

        if (isOn && shouldBeOn)
            return;
        if (!isOn && !shouldBeOn)
            return;
        ship.useSystem();
    }

    private boolean shouldBeOn()
    {
        // always on if we don't have a shield, since we won't have any hard flux
        if (ship.getShield() == null)
            return true;
        // turn it off if the shield is off and there's no soft flux, since the AI is trying to dissipate hard flux
        if (ship.getShield().isOff() && ship.getFluxTracker().getHardFlux() > 0 && ship.getFluxTracker().getCurrFlux() - ship.getFluxTracker().getHardFlux() == 0)
            return false;
        // otherwise, turn it on if there are any nearby allies to buff
        for (ShipAPI target : engine.getShips())
        {
            if (target.getHullSize().equals(ShipAPI.HullSize.FIGHTER))
                continue;
            if (target.getOwner() != ship.getOwner())
                continue;
            if (MathUtils.getDistanceSquared(ship.getLocation(), target.getLocation()) < ApexDefenseUplink.RANGE * ApexDefenseUplink.RANGE * 2f)
                return true;
        }
        return false;
    }
}
