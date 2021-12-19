package data.shipsystems.ai;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;

public class ApexArcMineDispenserAI implements ShipSystemAIScript
{
    private ShipAPI ship;
    private ShipSystemAPI system;
    private CombatEngineAPI engine;
    private ShipwideAIFlags flags;
    private float timeSinceActivation;

    private IntervalUtil timer = new IntervalUtil(0.25f, 0.33f);

    @Override
    public void init(ShipAPI ship, ShipSystemAPI system, ShipwideAIFlags flags, CombatEngineAPI engine)
    {
        this.ship = ship;
        this.system = system;
        this.engine = engine;
        this.flags = flags;
        timeSinceActivation = 0f;
    }

    @Override
    public void advance(float amount, Vector2f missileDangerDir, Vector2f collisionDangerDir, ShipAPI target)
    {
        if (!ship.isAlive() || engine.isPaused())
            return;
        timer.advance(amount);
        timeSinceActivation += amount;
        if (timer.intervalElapsed() && system.getAmmo() > 0)
        {
            if (!AIUtils.getNearbyEnemies(ship, 400).isEmpty())
            {
                ship.useSystem();
                timeSinceActivation = 0f;
            } else if (!AIUtils.getNearbyEnemyMissiles(ship, 400).isEmpty() && timeSinceActivation > 1.2f)
            {
                ship.useSystem();
                timeSinceActivation = 0f;
            }
        }
    }
}