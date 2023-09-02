package data.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.util.FaderUtil;
import com.fs.starfarer.api.util.Misc;
import data.effects.ApexNanoacidEffect;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.ArrayList;
import java.util.EnumSet;

// basically just a copy of cryoblaster script with small tweaks to improve visuals for a rapid-fire gun
public class ApexFoamerVFX extends BaseCombatLayeredRenderingPlugin implements OnFireEffectPlugin,
        OnHitEffectPlugin {

    public ApexFoamerVFX() {
    }

    public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target, Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {
        Color color = projectile.getProjectileSpec().getFringeColor();
        color = Misc.setAlpha(color, 100);

        Vector2f vel = new Vector2f();
        if (target instanceof ShipAPI) {
            vel.set(target.getVelocity());
        }

        float sizeMult = Misc.getHitGlowSize(100f, projectile.getDamage().getBaseDamage(), damageResult) / 100f;

        for (int i = 0; i < 2; i++) {
            //float size = projectile.getProjectileSpec().getWidth() * (0.75f + (float) Math.random() * 0.5f);
            float size = 20f * (0.75f + (float) Math.random() * 0.5f);

            float dur = 1f;
            //dur = 0.25f;
            float rampUp = 0f;
            Color c = Misc.scaleAlpha(color, projectile.getBrightness() * 0.5f);
            engine.addNebulaParticle(point, vel, size, 5f + 3f * sizeMult,
                    rampUp, 0f, dur, c, true);
        }

        if (!shieldHit && target instanceof ShipAPI)
        {
            Vector2f offset = Vector2f.sub(point, target.getLocation(), new Vector2f());
            offset = Misc.rotateAroundOrigin(offset, -target.getFacing());
            ApexNanoacidEffect acid = new ApexNanoacidEffect(projectile, (ShipAPI)target, offset, 1f, 15, projectile.getDamageAmount() * 2f, 3f, false, true);
            Global.getCombatEngine().addLayeredRenderingPlugin(acid);
        }
    }

    public void onFire(DamagingProjectileAPI projectile, WeaponAPI weapon, CombatEngineAPI engine) {
        ApexFoamerVFX trail = new ApexFoamerVFX(projectile);
        CombatEntityAPI e = engine.addLayeredRenderingPlugin(trail);
        e.getLocation().set(projectile.getLocation());
    }


    public static class ParticleData {
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

        public ParticleData(DamagingProjectileAPI proj) {
            this.proj = proj;
            sprite = Global.getSettings().getSprite("misc", "nebula_particles");
            //sprite = Global.getSettings().getSprite("misc", "dust_particles");
            float i = Misc.random.nextInt(4);
            float j = Misc.random.nextInt(4);
            sprite.setTexWidth(0.25f);
            sprite.setTexHeight(0.25f);
            sprite.setTexX(i * 0.25f);
            sprite.setTexY(j * 0.25f);
            sprite.setAdditiveBlend();

            angle = (float) Math.random() * 360f;

            maxDur = proj.getWeapon().getRange() / proj.getWeapon().getProjectileSpeed();
            scaleIncreaseRate = 2.5f / maxDur;
            scale = 1f;

            turnDir = Math.signum((float) Math.random() - 0.5f) * 30f * (float) Math.random();
            //turnDir = 0f;

            float driftDir = proj.getFacing() + 180f + ((float) Math.random() * 30f - 15f);
            vel = Misc.getUnitVectorAtDegreeAngle(driftDir);
            //vel.scale(proj.getProjectileSpec().getLength() / maxDur * (0f + (float) Math.random() * 3f));
            vel.scale(80f / maxDur * (0f + (float) Math.random() * 3f));

            origVel = new Vector2f(vel);
            dirVelChange = Misc.getUnitVectorAtDegreeAngle(proj.getFacing() + 180f);

//			offset.x += vel.x * 1f;
//			offset.y += vel.y * 1f;
            fader = new FaderUtil(0f, 0.25f, 0.05f);
            fader.fadeIn();
        }

        public void advance(float amount) {
            scale += scaleIncreaseRate * amount;

//			if (proj.didDamage()) {
//				vel.set(origVel);
//				//fader.fadeOut();
//			}

            offset.x += vel.x * amount;
            offset.y += vel.y * amount;

            if (!proj.didDamage()) {
                float speed = vel.length();
                if (speed > 0) {
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

    protected ArrayList<ParticleData> particles = new ArrayList<>();

    protected DamagingProjectileAPI proj;
    protected Vector2f projVel;
    protected Vector2f projLoc;
    public ApexFoamerVFX(DamagingProjectileAPI proj) {
        this.proj = proj;

        projVel = new Vector2f(proj.getVelocity());
        projLoc = new Vector2f(proj.getLocation());

        int num = 30;
        for (int i = 0; i < num; i++) {
            particles.add(new ParticleData(proj));
        }

        float index = 0;
        for (ParticleData p : particles) {
            //p.offset = Misc.getPointWithinRadius(p.offset, width * 0.5f);
            p.offset = Misc.getPointWithinRadius(p.offset, 20f);
            index++;
        }
    }

    public float getRenderRadius() {
        return 700f;
    }


    protected EnumSet<CombatEngineLayers> layers = EnumSet.of(CombatEngineLayers.ABOVE_SHIPS_AND_MISSILES_LAYER);
    @Override
    public EnumSet<CombatEngineLayers> getActiveLayers() {
        return layers;
    }

    public void init(CombatEntityAPI entity) {
        super.init(entity);
    }

    protected boolean resetTrailSpeed = false;
    public void advance(float amount) {
        if (Global.getCombatEngine().isPaused()) return;

        entity.getLocation().set(proj.getLocation());

        float max = 0f;
        for (ParticleData p : particles) {
            p.advance(amount);
            max = Math.max(max, p.offset.lengthSquared());
        }

        // BALLISTIC_AS_BEAM don't get some stuff set right away, catch it in the first few frames
        // but after that the particles move independently
        if (proj.getElapsed() < 0.1f) {
            projVel.set(proj.getVelocity());
            projLoc.set(proj.getLocation());
        } else {
            projLoc.x += projVel.x * amount;
            projLoc.y += projVel.y * amount;

            if (proj.didDamage()) {
                if (!resetTrailSpeed) {
                    for (ParticleData p : particles) {
                        Vector2f.add(p.vel, projVel, p.vel);
                    }
                    projVel.scale(0f);
                    resetTrailSpeed = true;
                }
                for (ParticleData p : particles) {
                    float dist = p.offset.length();
                    p.vel.scale(Math.min(1f, dist / 100f));
                }
            }
        }
    }


    public boolean isExpired() {
        return proj.isExpired() || !Global.getCombatEngine().isEntityInPlay(proj);
    }

    public void render(CombatEngineLayers layer, ViewportAPI viewport) {
//		float x = entity.getLocation().x;
//		float y = entity.getLocation().y;
        float x = projLoc.x;
        float y = projLoc.y;

        Color color = proj.getProjectileSpec().getFringeColor();
        color = Misc.setAlpha(color, 30);
        float b = proj.getBrightness();
        b *= viewport.getAlphaMult();

//		Vector2f farAhead = Misc.getUnitVectorAtDegreeAngle(proj.getFacing());
//		farAhead.scale(10000f);
//		Vector2f.add(proj.getLocation(), farAhead, farAhead);

        for (ParticleData p : particles) {
            //float size = proj.getProjectileSpec().getWidth() * 0.6f;
            float size = 25f;
            size *= p.scale;

            Vector2f loc = new Vector2f(x + p.offset.x, y + p.offset.y);

            float alphaMult = 0.5f;
            //float dParticle = Misc.getDistance(farAhead, loc);

            float a = alphaMult;

            p.sprite.setAngle(p.angle);
            p.sprite.setSize(size, size);
            p.sprite.setAlphaMult(b * a * p.fader.getBrightness());
            p.sprite.setColor(color);
            p.sprite.renderAtCenter(loc.x, loc.y);
        }
    }

}