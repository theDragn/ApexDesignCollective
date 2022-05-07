package data.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.util.FaderUtil;
import com.fs.starfarer.api.util.Misc;
import data.ApexUtils;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.*;
import java.util.List;

// handles damage boost from flux and trail graphics
public class ApexTachyonShredder extends BaseCombatLayeredRenderingPlugin implements OnFireEffectPlugin
{
    public static final float DAMAGE_BOOST = 1f; // this much extra damage at maximum flux
    public static final Color START_COLOR = new Color(0, 0, 255);
    public static final Color FINAL_COLOR = new Color(255, 0, 123);

    // needs this to not crash
    public ApexTachyonShredder() {}

    @Override
    public void onFire(DamagingProjectileAPI proj, WeaponAPI weapon, CombatEngineAPI engine)
    {
        float extraDamage = weapon.getShip().getFluxLevel() * DAMAGE_BOOST;
        Color effectColor = ApexUtils.blendColors(START_COLOR, FINAL_COLOR, extraDamage);
        proj.getProjectileSpec().setFringeColor(effectColor);
        ApexTachyonShredder trail = new ApexTachyonShredder(proj);

        CombatEntityAPI effect = engine.addLayeredRenderingPlugin(trail);
        effect.getLocation().set(proj.getLocation());

        proj.getDamage().getModifier().modifyMult("shredder_bonus", 1f + extraDamage);
        Color effectCol = new Color(
                proj.getProjectileSpec().getFringeColor().getRed(),
                proj.getProjectileSpec().getFringeColor().getGreen(),
                proj.getProjectileSpec().getFringeColor().getBlue(),
                100
        );

        for (int i = 0; i < 5 * proj.getSource().getFluxLevel(); i++)
        {
            engine.addNebulaParticle(
                    proj.getLocation(),
                    weapon.getShip().getVelocity(),
                    MathUtils.getRandomNumberInRange(40f, 60f),
                    1.2f,
                    0.1f,
                    0.3f,
                    MathUtils.getRandomNumberInRange(0.6f, 1.6f),
                    effectCol
            );
        }

        engine.spawnExplosion(
                proj.getLocation(),
                weapon.getShip().getVelocity(),
                proj.getProjectileSpec().getFringeColor(),
                60f,
                0.15f
        );
    }

    /*@Override
    public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target, Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine)
    {

    }*/

    public static class ParticleData
    {
        public SpriteAPI sprite;
        public Vector2f offset = new Vector2f();
        public Vector2f vel = new Vector2f();
        public float scale = 1f;
        public DamagingProjectileAPI proj;
        public float scaleIncreaseRate = 1f;
        public float turnDir = 1f;
        public float angle = 1f;

        public float maxDur;
        public Vector2f origVel;
        public FaderUtil fader;
        public Vector2f dirVelChange;

        public ParticleData(DamagingProjectileAPI proj)
        {
            this.proj = proj;
            sprite = Global.getSettings().getSprite("misc", "nebula_particles");
            float i = Misc.random.nextInt(4);
            float j = Misc.random.nextInt(4);
            sprite.setTexWidth(0.25f);
            sprite.setTexHeight(0.25f);
            sprite.setTexX(i * 0.25f);
            sprite.setTexY(j * 0.25f);
            sprite.setAdditiveBlend();

            angle = (float) Math.random() * 360f;

            maxDur = proj.getWeapon().getRange() / proj.getWeapon().getProjectileSpeed();
            scaleIncreaseRate = 1.5f / maxDur;
            scale = 1f;

            turnDir = Math.signum((float) Math.random() - 0.5f) * 30f * (float) Math.random();

            float driftDir = proj.getFacing() + 180f + ((float) Math.random() * 30f - 15f);
            vel = Misc.getUnitVectorAtDegreeAngle(driftDir);
            vel.scale(80f / maxDur * (0f + (float) Math.random() * 3f));

            origVel = new Vector2f(vel);
            dirVelChange = Misc.getUnitVectorAtDegreeAngle(proj.getFacing() + 180f);

            fader = new FaderUtil(0f, 0.25f, 0.05f);
            fader.fadeIn();
        }

        public void advance(float amount)
        {
            scale += scaleIncreaseRate * amount;
            offset.x += vel.x * amount;
            offset.y += vel.y * amount;

            if (!proj.didDamage())
            {
                float speed = vel.length();
                if (speed > 0)
                {
                    float speedIncrease = proj.getMoveSpeed() / maxDur * 0.5f;
                    Vector2f dir = new Vector2f(dirVelChange);
                    dir.scale(speedIncrease * amount);
                    Vector2f.add(vel, dir, vel);
                }
            }

            angle += turnDir * amount;
            fader.advance(amount);
        }
    }

    protected List<ParticleData> particles = new ArrayList<ParticleData>();

    protected DamagingProjectileAPI proj;
    protected Vector2f projVel;
    protected Vector2f projLoc;

    public ApexTachyonShredder(DamagingProjectileAPI proj)
    {
        this.proj = proj;

        projVel = new Vector2f(proj.getVelocity());
        projLoc = new Vector2f(proj.getLocation());

        int num = 30;
        for (int i = 0; i < num; i++)
        {
            particles.add(new ParticleData(proj));
        }

        for (ParticleData p : particles)
        {
            p.offset = Misc.getPointWithinRadius(p.offset, 20f);
        }
    }

    public float getRenderRadius()
    {
        return 700f;
    }


    protected EnumSet<CombatEngineLayers> layers = EnumSet.of(CombatEngineLayers.ABOVE_SHIPS_AND_MISSILES_LAYER);

    @Override
    public EnumSet<CombatEngineLayers> getActiveLayers()
    {
        return layers;
    }

    public void init(CombatEntityAPI entity)
    {
        super.init(entity);
    }

    protected boolean resetTrailSpeed = false;

    public void advance(float amount)
    {
        if (Global.getCombatEngine().isPaused()) return;

        entity.getLocation().set(proj.getLocation());

        float max = 0f;
        for (ParticleData p : particles)
        {
            p.advance(amount);
            max = Math.max(max, p.offset.lengthSquared());
        }

        // BALLISTIC_AS_BEAM don't get some stuff set right away, catch it in the first few frames
        // but after that the particles move independently
        if (proj.getElapsed() < 0.1f)
        {
            projVel.set(proj.getVelocity());
            projLoc.set(proj.getLocation());
        } else
        {
            projLoc.x += projVel.x * amount;
            projLoc.y += projVel.y * amount;

            if (proj.didDamage())
            {
                if (!resetTrailSpeed)
                {
                    for (ParticleData p : particles)
                    {
                        Vector2f.add(p.vel, projVel, p.vel);
                    }
                    projVel.scale(0f);
                    resetTrailSpeed = true;
                }
                for (ParticleData p : particles)
                {
                    float dist = p.offset.length();
                    p.vel.scale(Math.min(1f, dist / 100f));
                }
            }
        }
    }


    public boolean isExpired()
    {
        return proj.isExpired() || !Global.getCombatEngine().isEntityInPlay(proj);
    }

    public void render(CombatEngineLayers layer, ViewportAPI viewport)
    {
        float x = projLoc.x;
        float y = projLoc.y;

        Color color = proj.getProjectileSpec().getFringeColor();
        color = Misc.setAlpha(color, 30);
        float b = proj.getBrightness();
        b *= viewport.getAlphaMult();

        for (ParticleData p : particles)
        {
            float size = 25f;
            size *= p.scale;

            Vector2f loc = new Vector2f(x + p.offset.x, y + p.offset.y);

            p.sprite.setAngle(p.angle);
            p.sprite.setSize(size, size);
            p.sprite.setAlphaMult(b * p.fader.getBrightness());
            p.sprite.setColor(color);
            p.sprite.renderAtCenter(loc.x, loc.y);
        }
    }

}
