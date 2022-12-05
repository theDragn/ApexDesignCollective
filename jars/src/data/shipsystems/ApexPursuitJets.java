package data.shipsystems;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.plugins.ShipSystemStatsScript;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;

import java.awt.*;
import java.util.EnumSet;

import static data.ApexUtils.text;

public class ApexPursuitJets extends BaseShipSystemScript
{
    public static final float MAX_BOOST_ARC = 45f;  // degrees to either side where boost = 100%
    public static final float BOOST_MIN = 120f;      // degrees to either side where boost = 0%
    public static final float MAX_SPEED_BONUS = 100f;
    public static final float ACCEL_MULT = 2f;
    public static final float TURN_MULT = 1.5f;
    public static final float DAMAGE_MULT = 1.33f;

    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel)
    {
        if (!(stats.getEntity() instanceof ShipAPI))
            return;
        ShipAPI ship = (ShipAPI) stats.getEntity();

        // angle of the ship's current velocity, from forwards
        float velAngle = Math.abs(MathUtils.getShortestRotation(ship.getFacing(), VectorUtils.getAngle(Misc.ZERO, ship.getVelocity())));
        float boostAmount = 0f;
        if (velAngle < MAX_BOOST_ARC)
            boostAmount = 1;
        else if (velAngle < BOOST_MIN)
            boostAmount = 1f - (velAngle - MAX_BOOST_ARC) / (BOOST_MIN - MAX_BOOST_ARC);

        stats.getMaxSpeed().modifyFlat(id, boostAmount * MAX_SPEED_BONUS * effectLevel);
        stats.getAcceleration().modifyMult(id, ACCEL_MULT);
        stats.getTurnAcceleration().modifyMult(id, TURN_MULT);
        stats.getMaxTurnRate().modifyMult(id, TURN_MULT);

        stats.getBallisticWeaponDamageMult().modifyMult(id, DAMAGE_MULT);
        stats.getEnergyWeaponDamageMult().modifyMult(id, DAMAGE_MULT);

        ship.setWeaponGlow(effectLevel * 0.5f, Color.MAGENTA, EnumSet.of(WeaponAPI.WeaponType.BALLISTIC, WeaponAPI.WeaponType.ENERGY));
    }

    public void unapply(MutableShipStatsAPI stats, String id)
    {
        stats.getMaxSpeed().unmodify(id);
        stats.getAcceleration().unmodify(id);
        stats.getMaxTurnRate().unmodify(id);
        stats.getTurnAcceleration().unmodify(id);
        stats.getBallisticWeaponDamageMult().unmodify(id);
        stats.getEnergyWeaponDamageMult().unmodify(id);
    }

    public StatusData getStatusData(int index, State state, float effectLevel)
    {
        if (index == 0)
        {
            return new StatusData(text("pursuitjets"), false);
        }
        return null;
    }
}

