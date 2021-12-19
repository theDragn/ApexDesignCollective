package data.weapons;

import com.fs.starfarer.api.combat.*;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public class ApexDartgunMuzzleFlash implements OnFireEffectPlugin, EveryFrameWeaponEffectPlugin
{
    // smoke vents
    public static final float PARTICLE_SIZE_MIN = 10f;
    public static final float PARTICLE_SIZE_MAX = 30f;
    public static final float PARTICLE_DURATION = 0.33f;
    public static final float PARTICLE_SPREAD = 30f;
    public static final float PARTICLE_VEL_MIN = 25f;
    public static final float PARTICLE_VEL_MAX = 100f;
    public static final float PARTICLE_MIN_DISTANCE = 0f;
    public static final float PARTICLE_MAX_DISTANCE = 20f;
    public static final int PARTICLE_NUM = 20;
    public static final Color PARTICLE_COLOR = new Color(50,50,50,100);
    public static final float[] angles = {0};

    // muzzle flash
    @Override
    public void onFire(DamagingProjectileAPI proj, WeaponAPI weapon, CombatEngineAPI engine)
    {

        for (float baseAngle : angles)
        {
            for (int i = 0; i < PARTICLE_NUM; i++)
            {
                float actualFacing = weapon.getCurrAngle() + baseAngle;
                float arcPoint = MathUtils.getRandomNumberInRange(actualFacing-(PARTICLE_SPREAD/2f), actualFacing+(PARTICLE_SPREAD/2f));
                Vector2f velocity = MathUtils.getPointOnCircumference(weapon.getShip().getVelocity(), MathUtils.getRandomNumberInRange(PARTICLE_VEL_MIN, PARTICLE_VEL_MAX), arcPoint);

                //Gets a spawn location in the cone, depending on our offsetMin/Max
                Vector2f spawnLocation = MathUtils.getPointOnCircumference(proj.getLocation(), MathUtils.getRandomNumberInRange(PARTICLE_MIN_DISTANCE, PARTICLE_MAX_DISTANCE), arcPoint);
                engine.addSmoothParticle(spawnLocation, velocity, MathUtils.getRandomNumberInRange(PARTICLE_SIZE_MIN, PARTICLE_SIZE_MAX), 1f, PARTICLE_DURATION, PARTICLE_COLOR);
            }
        }
    }

    @Override
    public void advance(float v, CombatEngineAPI combatEngineAPI, WeaponAPI weaponAPI)
    {
        // do nothing
    }
}
