package data.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.AsteroidAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.loading.ProjectileSpawnType;
import com.fs.starfarer.api.util.IntervalUtil;
import org.dark.shaders.distortion.DistortionShader;
import org.dark.shaders.distortion.RippleDistortion;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

import static data.ApexUtils.text;
import static plugins.ApexModPlugin.POTATO_MODE;

public class ApexGravityShield extends BaseShipSystemScript
{
    public static final float RANGE = 600f;
    public static final float BEAM_MULT = 0.67f;
    public static final float TARGET_VEL_MULT = 0.2f;
    public static final float TARGET_VEL_FRAMES = 40f; // reaches target velocity mult after this many frames; assumes 60fps
    public static final float VEL_MULT_PER_FRAME = (float)Math.pow(TARGET_VEL_MULT, 1f/TARGET_VEL_FRAMES);

    public static Object KEY_SHIP = new Object();
    public static IntervalUtil rippleTimer = new IntervalUtil(0.33f, 0.33f);
    public static float elapsed = 0;

    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel)
    {
        stats.getBeamDamageTakenMult().modifyMult(id, BEAM_MULT);

        ShipAPI ship = (ShipAPI)stats.getEntity();
        if (ship == null) return;
        if (ship == Global.getCombatEngine().getPlayerShip())
        {
            if (ship.getSystem() != null)
            {
                Global.getCombatEngine().maintainStatusForPlayerShip(
                        KEY_SHIP,
                        ship.getSystem().getSpecAPI().getIconSpriteName(),
                        ship.getSystem().getDisplayName(),
                        (int) Math.round((1f - BEAM_MULT) * effectLevel * 100) + text("gravityshield"),
                        false
                );
            }
        }
        // vfx
        float amount = Global.getCombatEngine().getElapsedInLastFrame();
        elapsed += amount;
        rippleTimer.advance(amount);
        if (rippleTimer.intervalElapsed() && elapsed < 1f && !POTATO_MODE)
        {
            RippleDistortion ripple = new RippleDistortion(ship.getLocation(), new Vector2f(0, 0));
            ripple.setCurrentFrame(20);
            ripple.setIntensity(90);

            //Ensure the effect fades out properly
            ripple.setLifetime(2);
            ripple.fadeOutIntensity(2);

            //The ripple need needs to grow over time, or there's not much of a ripple!
            //Also adds animation
            ripple.setSize(RANGE);
            ripple.fadeInIntensity(1);
            //ripple.fadeInSize(2);
            ripple.setFrameRate(40);
            DistortionShader.addDistortion(ripple);
        }
        ship.setJitterUnder(KEY_SHIP, new Color(160, 0, 255, 255), effectLevel, 15, 0f, 15f);

        // projectile divert
        // fudge factor for lower/higher framerates
        float scaleFactor = MathUtils.clamp(1f - (1f - VEL_MULT_PER_FRAME) * (elapsed / (1f/60f)), 0f, 1f);
        for (CombatEntityAPI thing : CombatUtils.getEntitiesWithinRange(ship.getLocation(), RANGE))
        {
            if (thing.getOwner() == ship.getOwner() || thing instanceof ShipAPI)
                continue;

            if (thing instanceof DamagingProjectileAPI && ((DamagingProjectileAPI) thing).getSpawnType() == ProjectileSpawnType.BALLISTIC_AS_BEAM)
            {
                float idealAngle = VectorUtils.getAngle(thing.getLocation(), ship.getLocation()) + 180f;
                if (idealAngle >= 360f)
                    idealAngle -= 360f;

                float rotationNeeded = MathUtils.getShortestRotation(thing.getFacing(), idealAngle);
                thing.setFacing(thing.getFacing() + (rotationNeeded * amount)); // rotates 180 in 1s
            } else if (!(thing instanceof AsteroidAPI))
                thing.getVelocity().scale(scaleFactor);
        }
    }

    @Override
    public void unapply(MutableShipStatsAPI stats, String id)
    {
        elapsed = 0;
        stats.getBeamDamageTakenMult().unmodify(id);
    }
}
