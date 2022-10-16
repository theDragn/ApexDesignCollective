package data.weapons.proj.ai;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.Misc;
import data.scripts.util.MagicTargeting;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

// lots of this is stolen from magiclib's ai script
public class ApexThundercloudAI implements MissileAIPlugin, GuidedMissileAI
{
    private static final String SPLIT_SOUND = "amsrm_fire";
    private static final float DAMPING = 0.1f;
    private static final float DETONATION_RANGE = 150f * 150f;
    private static final float AI_DELAY = 0.25f;
    private static final float OVERSTEER_ANGLE = 45f; // turns off engine to turn if angle is >this

    public static final Color FRAG_COLOR = new Color(75, 255, 84, 150);
    public static final Color EMP_COLOR = new Color(75, 192, 255, 150);
    public static final Color HE_COLOR = new Color(255, 75, 75, 150);

    private CombatEngineAPI engine;
    private final MissileAPI missile;
    private CombatEntityAPI target;
    private String subWeapon = "_frag";
    private Color subColor = FRAG_COLOR;

    private float undershoot = 0.5f;

    public ApexThundercloudAI(MissileAPI missile, ShipAPI launchingShip)
    {
        this.engine = Global.getCombatEngine();
        this.missile = missile;
        if (missile.getSource().getVariant().getHullMods().contains("eccm"))
        {
            undershoot = 0.75f;
        }
        if (missile.getProjectileSpecId().equals("apex_thundercloud_missile_emp"))
        {
            subWeapon = "_emp";
            subColor = EMP_COLOR;
        }
        else if (missile.getProjectileSpecId().equals("apex_thundercloud_missile_he"))
        {
            subWeapon = "_he";
            subColor = HE_COLOR;
        }
    }

    @Override
    public void advance(float amount)
    {
        if (engine.isPaused() || missile.isFading() || missile.isFizzling())
        {
            return;
        }
        // don't immediately turn AI on so we can bully people for using it at short range
        if (missile.getElapsed() < AI_DELAY)
            return;
        // pick target
        if (target == null
                || target.getOwner() == missile.getOwner()
                || !engine.isEntityInPlay(target)
        )
        {
            setTarget(MagicTargeting.pickMissileTarget(
                    missile,
                    MagicTargeting.targetSeeking.NO_RANDOM,
                    10000,
                    360,
                    0,
                    1,
                    3,
                    4,
                    5)
            );
           return;
        }

        float dist = MathUtils.getDistanceSquared(missile, target);
        // in detonation range, trigger separation
        if (dist < DETONATION_RANGE)
        {
            mirv();
            return;
        } else if (missile.getElapsed() > 2
                && dist < 250000
                && Math.abs(MathUtils.getShortestRotation(VectorUtils.getFacing(missile.getVelocity()), VectorUtils.getAngle(missile.getLocation(), target.getLocation()))) > 90)
        {
            mirv();
            return;
        }


        Vector2f interceptLoc = AIUtils.getBestInterceptPoint(
                missile.getLocation(),
                missile.getMaxSpeed() * undershoot,
                target.getLocation(),
                target.getVelocity()
        );
        if (interceptLoc == null)
        {
            interceptLoc = target.getLocation();
        }

        // what direction we want to be going in
        float targetAngle = VectorUtils.getAngle(
                missile.getLocation(),
                interceptLoc
        );

        float aimAngle = MathUtils.getShortestRotation(missile.getFacing(), targetAngle);

        if (Math.abs(aimAngle) < OVERSTEER_ANGLE)
            missile.giveCommand(ShipCommand.ACCELERATE);
        if (aimAngle < 0)
            missile.giveCommand(ShipCommand.TURN_RIGHT);
        else
            missile.giveCommand(ShipCommand.TURN_LEFT);

        // angular damping
        // every missile AI uses this so that the missile doesn't zigzag left and right as it gets close to its target angle
        if (Math.abs(aimAngle) < Math.abs(missile.getAngularVelocity()) * DAMPING)
        {
            missile.setAngularVelocity(aimAngle / DAMPING);
        }
    }

    private void mirv()
    {
        // no damage, just visual
        engine.spawnExplosion(
                missile.getLocation(),
                new Vector2f(),
                subColor,
                300,
                Misc.random.nextFloat() + 3f
        );
        engine.addHitParticle(
                missile.getLocation(),
                new Vector2f(),
                300,
                0.5f,
                0.25f,
                Color.WHITE
        );
        // still visuals
        for (int i = 0; i < 30; i++)
        {

            Vector2f loc = new Vector2f(missile.getLocation());
            Vector2f vel = MathUtils.getRandomPointInCircle(Misc.ZERO, 150);
            Vector2f.add(loc, vel, loc);
            float rand = Misc.random.nextFloat();
            engine.addHitParticle(
                    loc,
                    vel,
                    5f + Misc.random.nextFloat() * 5,
                    0.5f,
                    1f + Misc.random.nextFloat(),
                    new Color(1, 0.25f * rand, 0.7f * rand)
            );
        }
        // now we're spawning mines
        for (int i = 0; i < missile.getMirvNumWarheads(); i++)
        {
            engine.spawnProjectile(
                    missile.getSource(),
                    missile.getWeapon(),
                    "apex_thundercloud_mine" + subWeapon,
                    missile.getLocation(),
                    Misc.random.nextFloat() * 360,
                    MathUtils.getRandomPointInCone(new Vector2f(), 400, missile.getFacing() - 70f, missile.getFacing() + 70f) // random starting velocity to spice things up
            );
        }

        //sound effect
        Global.getSoundPlayer().playSound(
                SPLIT_SOUND,
                1,
                1,
                missile.getLocation(),
                new Vector2f()
        );

        // make it go away
        engine.applyDamage(
                missile,
                missile.getLocation(),
                missile.getHitpoints(),
                DamageType.FRAGMENTATION,
                0,
                true,
                false,
                missile,
                false
        );
    }

    @Override
    public CombatEntityAPI getTarget()
    {
        return target;
    }

    @Override
    public void setTarget(CombatEntityAPI target)
    {
        this.target = target;
    }
}
