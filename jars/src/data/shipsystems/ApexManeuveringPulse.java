package data.shipsystems;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.plugins.ShipSystemStatsScript;

import static utils.ApexUtils.text;

public class ApexManeuveringPulse extends BaseShipSystemScript
{

    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel)
    {
        if (state == ShipSystemStatsScript.State.OUT)
        {
            stats.getMaxSpeed().unmodify(id); // to slow down ship to its regular top speed while powering drive down
            stats.getMaxTurnRate().unmodify(id);
        } else
        {
            stats.getMaxSpeed().modifyFlat(id, 400f);
            stats.getAcceleration().modifyFlat(id, 2000f);
            stats.getMaxTurnRate().modifyMult(id, 6f);
            stats.getTurnAcceleration().modifyMult(id, 20f);
        }
    }

    public void unapply(MutableShipStatsAPI stats, String id)
    {
        stats.getMaxSpeed().unmodify(id);
        stats.getAcceleration().unmodify(id);
        stats.getMaxTurnRate().unmodify(id);
        stats.getTurnAcceleration().unmodify(id);
    }

    public StatusData getStatusData(int index, State state, float effectLevel)
    {
        if (index == 0)
        {
            return new StatusData(text("moveburst"), false);
        }
        return null;
    }
}


