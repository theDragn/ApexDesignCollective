package data.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.util.Misc;
import org.dark.shaders.distortion.DistortionShader;
import org.dark.shaders.distortion.RippleDistortion;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

import static data.hullmods.ApexExcessionReactor.*;

public class ApexExcessionSystem extends BaseShipSystemScript
{
    public static final float INSTANT_DISS = 3f; // yo momma so fat, she can instantly dissipate this many seconds of flux dissipation
    public static final float CHARGE_MULT = 2f;
    public static Color JITTER_COLOR = new Color(255,50,50,75);

    public static Object KEY_SHIP = new Object();

    private boolean runOnce = false;
    private int doEffects = 0;

    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel)
    {
        ShipAPI ship = (ShipAPI)stats.getEntity();
        ship.setJitter(KEY_SHIP, JITTER_COLOR, effectLevel, 4, 0f, effectLevel * 50f);
        if (!runOnce && stats.getEntity() != null)
        {
            runOnce = true;
            float diss = stats.getFluxDissipation().getModifiedValue();
            ship.getFluxTracker().decreaseFlux(diss * INSTANT_DISS);
        }

        // prevent phase cloaking
        ship.getPhaseCloak().setCooldownRemaining(0.1f);

        // I wrote this code, I can steal it back
        float shipRadius = ship.getCollisionRadius();
        if (doEffects < 10) {
            if (doEffects == 0) {
                RippleDistortion ripple = new RippleDistortion(ship.getLocation(), ship.getVelocity());
                ripple.setSize(300f + shipRadius * 1.75f);
                ripple.setIntensity(shipRadius);
                ripple.setFrameRate(60f);
                ripple.fadeInSize(0.75f);
                ripple.fadeOutIntensity(0.5f);
                DistortionShader.addDistortion(ripple);
            }
            for (int i = 0; i < 2; i++) {
                Vector2f particleLoc = MathUtils.getRandomPointInCircle(ship.getLocation(), (shipRadius + 300) * 2f);
                Vector2f particleVel = MathUtils.getPointOnCircumference(Misc.ZERO, MathUtils.getDistance(particleLoc, ship.getLocation()), VectorUtils.getAngle(particleLoc, ship.getLocation()));
                Global.getCombatEngine().addSmoothParticle(particleLoc, particleVel, (float)Math.random() * 15f + 5f, 0.5f, 0.75f, Color.white);
            }
            doEffects++;
        }

        // extra charge rate is from reactor hullmod now
        // three frames of full core charge - enough to wipe out whatever is within interception radius, but no more
        /*if (doEffects < 3)
        {
            damageMap.put(ship, MAX_STORED_CHARGE); // hullmod script will make sure it doesn't go over 100%
        }*/
        //stats.getFluxDissipation().modifyMult(id, DISSIPATION_BOOST);

        //float charge = BASE_CHARGE_RATE * CORE_BASE_GEN_MULT * Global.getCombatEngine().getElapsedInLastFrame();



    }

    @Override
    public void unapply(MutableShipStatsAPI stats, String id)
    {
        runOnce = false;
        doEffects = 0;
        stats.getFluxDissipation().unmodify(id);
    }

    @Override
    public StatusData getStatusData(int index, State state, float effectLevel)
    {
        return null;
    }
}
