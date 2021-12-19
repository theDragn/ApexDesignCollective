package data.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipEngineControllerAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.plugins.ShipSystemStatsScript;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

import static plugins.ApexModPlugin.POTATO_MODE;

public class ApexInertialSkimmer extends BaseShipSystemScript
{
    public static float SPEED_BONUS = 300f;
    public static float TURN_BONUS = 100f;
    private static final Color AFTERIMAGE_COLOR = new Color(133, 133, 200, 60);
    private IntervalUtil afterImageTimer = new IntervalUtil(0.01f, 0.01f);

    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel)
    {
        if (!(stats.getEntity() instanceof ShipAPI))
            return;
        ShipAPI ship = (ShipAPI) stats.getEntity();

        stats.getMaxSpeed().modifyFlat(id, SPEED_BONUS);
        stats.getTurnAcceleration().modifyFlat(id, TURN_BONUS);
        stats.getTurnAcceleration().modifyPercent(id, TURN_BONUS * 4f);
        stats.getMaxTurnRate().modifyFlat(id, 60f);
        stats.getMaxTurnRate().modifyPercent(id, 100f);

        afterImageTimer.advance(Global.getCombatEngine().getElapsedInLastFrame());
        if (afterImageTimer.intervalElapsed() && !POTATO_MODE)
        {
            ship.addAfterimage(AFTERIMAGE_COLOR,
                    0f,
                    0f,
                    ship.getVelocity().x * -0.8f,
                    ship.getVelocity().y * -0.8f,
                    0,
                    0f,
                    0f,
                    0.5f,
                    true,
                    true,
                    false);
        }

        if (state == State.IN) {
            stats.getAcceleration().modifyMult(id, 50f);
            stats.getDeceleration().modifyMult(id, 50f);
        }
        if (state == State.ACTIVE) {
            if (ship.getVelocity().length() < ship.getMaxSpeed())
            {
                ship.getVelocity().scale(100f);
                VectorUtils.clampLength(ship.getVelocity(), ship.getMaxSpeed(), ship.getVelocity());
            }
            stats.getAcceleration().modifyMult(id, 0f);
            stats.getDeceleration().modifyMult(id, 0f);
        }
        if (state == State.OUT) {
            stats.getMaxSpeed().unmodify(id);
            stats.getAcceleration().modifyMult(id, 50f);
            stats.getDeceleration().modifyMult(id, 50f);
            stats.getMaxTurnRate().unmodify(id);
        }
    }

    @Override
    public void unapply(MutableShipStatsAPI stats, String id)
    {
        stats.getMaxSpeed().unmodify(id);
        stats.getMaxTurnRate().unmodify(id);
        stats.getTurnAcceleration().unmodify(id);
        stats.getAcceleration().unmodify(id);
        stats.getDeceleration().unmodify(id);
    }
}
