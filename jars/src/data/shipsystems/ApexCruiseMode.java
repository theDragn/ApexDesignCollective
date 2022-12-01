package data.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.EngineSlotAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipEngineControllerAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.plugins.ShipSystemStatsScript;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.lazylib.MathUtils;

import java.awt.*;

import static data.ApexUtils.text;

public class ApexCruiseMode extends BaseShipSystemScript
{
    boolean runOnce = false;
    public static final Color SMOKE_COLOR = new Color(255,255,255,128);

    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel)
    {
        ShipAPI ship = (ShipAPI)stats.getEntity();
        if (state == State.OUT)
        {
            stats.getMaxSpeed().unmodify(id); // to slow down ship to its regular top speed while powering drive down
            stats.getMaxTurnRate().unmodify(id);
        } else if (state == State.IN)
        {
            stats.getMaxSpeed().modifyMult(id, 0.25f);
            stats.getMaxTurnRate().modifyMult(id, 0.25f);
            ship.getEngineController().extendFlame(this, 0.25f * effectLevel, 0f * effectLevel, 0f * effectLevel);
        } else
        {
            stats.getMaxSpeed().modifyMult(id, 3f);
            stats.getAcceleration().modifyMult(id, 3f);
            stats.getMaxTurnRate().modifyMult(id, 0.25f);
            float extendMult = 1;//ship.getVelocity().length() / ship.getMaxSpeed();
            ship.getEngineController().extendFlame(this, 2f * extendMult * effectLevel, 0f * effectLevel, 0f * effectLevel);
            if (!runOnce)
            {
                runOnce = true;
                for (ShipEngineControllerAPI.ShipEngineAPI engine : ship.getEngineController().getShipEngines())
                {
                    for (int i = 0; i < 10; i++)
                    {
                        Global.getCombatEngine().addNebulaSmokeParticle(
                                engine.getLocation(),
                                MathUtils.getRandomPointInCircle(Misc.ZERO, 75f),
                                10f + Misc.random.nextFloat() * 10f,
                                1.5f,
                                0f,
                                0.33f,
                                1f,
                                SMOKE_COLOR
                        );
                    }
                }
            }
        }
        if (ship != null && ship.getShield() != null)
            ship.getShield().toggleOff();
    }

    public void unapply(MutableShipStatsAPI stats, String id)
    {
        runOnce = false;
        stats.getMaxSpeed().unmodify(id);
        stats.getAcceleration().unmodify(id);
        stats.getMaxTurnRate().unmodify(id);
    }

    public StatusData getStatusData(int index, State state, float effectLevel)
    {
        if (index == 0)
        {
            if (state == State.IN)
                return new StatusData(text("travel1"), false);
            if (state == State.OUT)
                return new StatusData(text("travel2"), false);
            if (state == State.ACTIVE)
                return new StatusData(text("travel3"), false);
        }
        return null;
    }
}


