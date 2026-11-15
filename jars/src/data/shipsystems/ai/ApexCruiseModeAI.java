package data.shipsystems.ai;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;

public class ApexCruiseModeAI implements ShipSystemAIScript
{
    private ShipAPI ship;
    private ShipSystemAPI system;
    private CombatEngineAPI engine;
    private ShipwideAIFlags flags;
    private IntervalUtil timer = new IntervalUtil(0.25f, 0.33f);

    @Override
    public void init(ShipAPI ship, ShipSystemAPI system, ShipwideAIFlags flags, CombatEngineAPI engine)
    {
        this.ship = ship;
        this.system = system;
        this.engine = engine;
        this.flags = flags;
    }

    @Override
    public void advance(float amount, Vector2f missileDangerDir, Vector2f collisionDangerDir, ShipAPI target)
    {
        if (!ship.isAlive() || engine.isPaused())
            return;
        timer.advance(amount);
        if (timer.intervalElapsed())
        {
            boolean hasNearbyEnemies = !AIUtils.getNearbyEnemies(ship, 2000f).isEmpty();
            if (system.getState().equals(ShipSystemAPI.SystemState.IDLE) && ship.getHardFluxLevel() == 0f && !hasNearbyEnemies)
            {
                ship.useSystem();

            }
            else if (system.getState().equals(ShipSystemAPI.SystemState.ACTIVE) && hasNearbyEnemies)
            {
                ship.useSystem();
            }
        }
    }
}