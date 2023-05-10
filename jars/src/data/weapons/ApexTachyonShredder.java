package data.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.loading.ProjectileSpawnType;
import com.fs.starfarer.api.util.FaderUtil;
import com.fs.starfarer.api.util.Misc;
import data.ApexUtils;
import org.magiclib.plugins.MagicTrailPlugin;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.*;
import java.util.List;

import static org.lwjgl.opengl.GL11.GL_ONE;
import static org.lwjgl.opengl.GL11.GL_SRC_ALPHA;

// handles damage boost from flux and trail graphics
// woe unto ye who enter this code, it does too many things
public class ApexTachyonShredder extends BaseCombatLayeredRenderingPlugin implements OnFireEffectPlugin, EveryFrameWeaponEffectPlugin
{
    public static final float DAMAGE_BOOST = 1f; // this much extra damage at maximum flux
    public static final Color START_COLOR = new Color(48, 48, 255);
    public static final Color FINAL_COLOR = new Color(255, 48, 144);

    // trail settings
    private static final String SPRITE_ID = "base_trail_smooth";
    private static final float FADE_IN_DURATION = 0f;
    private static final float MAIN_DURATION = 0.05f;
    private static final float FADE_OUT_DURATION = 0.5f;
    private static final float SIZE_IN = 20f; // trail width when it fades in
    private static final float SIZE_OUT = 5f; // trail width when it fades out
    private static final Color COLOR_IN = new Color(0, 0, 255);
    private static final Color COLOR_IN_FADE = new Color(255, 0, 123);
    private static final Color COLOR_OUT = new Color(12, 16, 226);
    private static final float OPACITY = 0.7f;
    private static final float TEXTURE_LENGTH = 300;
    private static final float TEXTURE_SCROLL = 400;
    private static final float DISTANCE = 20; // distance in front of projectile to start spawning trail
    private static final float DISPERSION = 0; // causes trail to spread out, iirc?
    private static final float DRIFT = 1;
    private static final boolean FADE_ON_FADE_OUT = true;
    private static final boolean ANGLE_ADJUSTMENT = false;
    private static final float VELOCITY_IN = 0;
    private static final float VELOCITY_OUT = 0f;
    private static final float ROTATION_IN = 0;
    private static final float ROTATION_OUT = 0;
    private static final float RANDOM_VELOCITY = 0;
    private static final boolean RANDOM_ROTATION = false;

    private final HashMap<DamagingProjectileAPI, Float> idMap = new HashMap<>();
    private final HashSet<DamagingProjectileAPI> projectiles = new HashSet<>();

    // needs this to not crash
    public ApexTachyonShredder()
    {
    }

    @Override
    public void onFire(DamagingProjectileAPI proj, WeaponAPI weapon, CombatEngineAPI engine)
    {
        projectiles.add(proj);
        float extraDamage = weapon.getShip().getFluxLevel() * DAMAGE_BOOST;
        Color movingEffectColor = ApexUtils.blendColors(START_COLOR, FINAL_COLOR, weapon.getShip().getFluxLevel());
        //proj.getProjectileSpec().setFringeColor(effectColor);
        ApexTachyonShredder trail = new ApexTachyonShredder(proj, movingEffectColor);

        CombatEntityAPI effect = engine.addLayeredRenderingPlugin(trail);
        effect.getLocation().set(proj.getLocation());

        proj.getDamage().getModifier().modifyMult("shredder_bonus", 1f + extraDamage);
        Color muzzleFlashColor = new Color(movingEffectColor.getRed(), movingEffectColor.getGreen(), movingEffectColor.getBlue(), 100);

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
                    muzzleFlashColor
            );
        }

        engine.spawnExplosion(
                proj.getLocation(),
                weapon.getShip().getVelocity(),
                muzzleFlashColor,
                60f,
                0.15f
        );
    }


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
        public Color particleColor;

        public ParticleData(DamagingProjectileAPI proj, Color color)
        {
            this.proj = proj;
            sprite = Global.getSettings().getSprite("misc", "nebula_particles");
            float i = Misc.random.nextInt(4);
            float j = Misc.random.nextInt(4);
            particleColor = color;
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

    public ApexTachyonShredder(DamagingProjectileAPI proj, Color color)
    {
        this.proj = proj;

        projVel = new Vector2f(proj.getVelocity());
        projLoc = new Vector2f(proj.getLocation());

        int num = 30;
        for (int i = 0; i < num; i++)
        {
            particles.add(new ParticleData(proj, color));
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

    // render plugin method
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

    // render plugin method
    public boolean isExpired()
    {
        return proj.isExpired() || !Global.getCombatEngine().isEntityInPlay(proj);
    }

    // render plugin method
    public void render(CombatEngineLayers layer, ViewportAPI viewport)
    {
        float x = projLoc.x;
        float y = projLoc.y;


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
            Color color = p.particleColor;
            color = Misc.setAlpha(color, 30);
            p.sprite.setColor(color);

            p.sprite.renderAtCenter(loc.x, loc.y);

        }
    }

    // every-frame weapon plugin method
    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon)
    {
        ArrayList<DamagingProjectileAPI> toRemove = new ArrayList<>();
        for (DamagingProjectileAPI proj : projectiles)
        {
            if (proj.isExpired() || proj.didDamage() || !engine.isInPlay(proj))
                toRemove.add(proj);
            else
                addTrailSegment(proj, weapon.getShip().getFluxLevel());
        }
        for (DamagingProjectileAPI proj : toRemove)
        {
            projectiles.remove(proj);
        }
        //System.out.println(projectiles.size());
    }

    // spawns trail segment. should be called each frame on each projectile.
    private void addTrailSegment(DamagingProjectileAPI proj, float scalar)
    {
        if (!idMap.containsKey(proj))
            idMap.put(proj, MagicTrailPlugin.getUniqueID());

        SpriteAPI sprite = Global.getSettings().getSprite("fx", SPRITE_ID);

        Vector2f projVel = new Vector2f(proj.getVelocity());
        //Fix for some first-frame error shenanigans
        if (projVel.length() < 0.1f && proj.getSource() != null)
        {
            projVel = new Vector2f(proj.getSource().getVelocity());
        }
        //If we use angle adjustment, do that here
        if (ANGLE_ADJUSTMENT && projVel.length() > 0.1f && !proj.getSpawnType().equals(ProjectileSpawnType.BALLISTIC_AS_BEAM))
        {
            proj.setFacing(VectorUtils.getFacing(projVel));
        }

        //Gets a custom "offset" position, so we can slightly alter the spawn location to account for "natural fade-in", and add that to our spawn position
//            Vector2f offsetPoint = new Vector2f((float) Math.cos(Math.toRadians(proj.getFacing())) * trailData.distance, (float) Math.sin(Math.toRadians(proj.getFacing())) * trailData.distance);
//            Vector2f spawnPosition = new Vector2f(offsetPoint.x + proj.getLocation().x, offsetPoint.y + proj.getLocation().y);
        Vector2f spawnPosition = MathUtils.getPointOnCircumference(
                proj.getLocation(), DISTANCE, proj.getFacing());

        //Sideway offset velocity, for projectiles that use it
        Vector2f projBodyVel = new Vector2f(projVel);
        projBodyVel = VectorUtils.rotate(projBodyVel, -proj.getFacing());
        Vector2f projLateralBodyVel = new Vector2f(0f, projBodyVel.getY());
        Vector2f sidewayVel = new Vector2f(projLateralBodyVel);
        sidewayVel = (Vector2f) VectorUtils.rotate(sidewayVel, proj.getFacing()).scale(DRIFT);

        //random dispersion of the segments if necessary
        float rotationIn = ROTATION_IN;
        float rotationOut = ROTATION_OUT;

        float velIn = VELOCITY_IN;
        float velOut = VELOCITY_OUT;

        if (RANDOM_ROTATION)
        {
            float rand = MathUtils.getRandomNumberInRange(-1f, 1f);
            rotationIn = rotationIn * rand;
            rotationOut = rotationOut * rand;
        }

        if (DISPERSION > 0)
        {
            Vector2f.add(
                    sidewayVel,
                    MathUtils.getRandomPointInCircle(null, DISPERSION),
                    sidewayVel);
        }

        if (RANDOM_VELOCITY > 0)
        {
            float rand = MathUtils.getRandomNumberInRange(-1f, 1f);
            velIn *= 1 + RANDOM_VELOCITY * rand;
            velOut *= 1 + RANDOM_VELOCITY * rand;
        }

        //Opacity adjustment for fade-out, if the projectile uses it
        float opacityMult = 1f;
        if (FADE_ON_FADE_OUT && proj.isFading())
        {
            opacityMult = Math.max(0, Math.min(1, proj.getDamageAmount() / proj.getBaseDamageAmount()));
        }

        //Then, actually spawn a trail
        MagicTrailPlugin.addTrailMemberAdvanced(
                proj,
                idMap.get(proj),
                sprite,
                spawnPosition,
                velIn,
                velOut,
                proj.getFacing() - 180f,
                rotationIn,
                rotationOut,
                SIZE_IN,
                SIZE_OUT,
                ApexUtils.blendColors(COLOR_IN, COLOR_IN_FADE, scalar),
                COLOR_OUT,
                OPACITY * opacityMult,
                FADE_IN_DURATION,
                MAIN_DURATION,
                FADE_OUT_DURATION,
                GL_SRC_ALPHA,
                GL_ONE,
                TEXTURE_LENGTH,
                TEXTURE_SCROLL,
                0f,
                sidewayVel,
                null,
                CombatEngineLayers.BELOW_INDICATORS_LAYER,
                1f
        );
    }
}
