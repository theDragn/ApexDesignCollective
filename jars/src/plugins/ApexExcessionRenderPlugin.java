package plugins;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.util.Misc;
import data.hullmods.ApexExcessionReactor;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;

import static org.lazywizard.lazylib.opengl.ColorUtils.glColor;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA;

// renders Excession's particle effects and indicator ring
public class ApexExcessionRenderPlugin extends BaseCombatLayeredRenderingPlugin
{
    private static final String SPRITE_ID = "graphics/fx/particlealpha32sq.png";
    public static final float MAX_SPEED = 200f;
    public static final float ACCEL = 500f;
    public static final float PARTICLE_DURATION = 1f;
    public static final float MIN_SIZE = 10f;
    public static final float MAX_SIZE = 20f;
    public static final int NUM_INDICATOR_SEGMENTS = 8;
    public static final float INDICATOR_OPACITY = 1f;
    public static final float LINE_WIDTH = 3f;
    public static final Color LINE_COLOR = new Color(0, 158, 0, 255);

    private ShipAPI ship;
    private final HashSet<TargetedParticle> particles = new HashSet<>();

    public ApexExcessionRenderPlugin(ShipAPI ship)
    {
        this.ship = ship;
    }

    private static class TargetedParticle
    {
        public Vector2f pos;
        public Vector2f vel;
        public CombatEntityAPI destEntity;
        public Vector2f destVector;
        public Color color;
        public SpriteAPI sprite;
        public float angle;
        public float elapsed = 0f;
        public float size;

        public TargetedParticle(Vector2f pos, Vector2f vel, CombatEntityAPI destEntity, Vector2f destVector, Color color)
        {
            sprite = Global.getSettings().getSprite(SPRITE_ID);
            this.pos = pos;
            this.vel = vel;
            this.destEntity = destEntity;
            this.destVector = destVector;
            this.color = color;
            this.angle = VectorUtils.getAngle(Misc.ZERO, vel);
            this.size = MathUtils.getRandomNumberInRange(MIN_SIZE, MAX_SIZE);
        }

        public void advance(float amount)
        {
            // compute endpoint
            Vector2f actualDest = new Vector2f(destVector);
            VectorUtils.rotate(actualDest, destEntity.getFacing(), actualDest);
            Vector2f.add(destEntity.getLocation(), actualDest, actualDest);
            // rotate sprite towards target\
            angle = VectorUtils.getAngle(Misc.ZERO, vel);
            // accelerate towards endpoint
            Vector2f accelThisFrame = MathUtils.getPointOnCircumference(Misc.ZERO, ACCEL * amount, VectorUtils.getAngle(pos, actualDest));
            Vector2f.add(vel, accelThisFrame, vel);
            VectorUtils.clampLength(vel, MAX_SPEED, vel);
            Vector2f.add(pos, new Vector2f(vel.x * amount, vel.y * amount), pos);
            elapsed += amount;
        }
    }

    public ApexExcessionRenderPlugin()
    {
    }

    public void addTargetedParticle(Vector2f origin, Vector2f startVel, CombatEntityAPI destEntity, Vector2f destVector, Color particleColor)
    {
        TargetedParticle particle = new TargetedParticle(origin, startVel, destEntity, destVector, particleColor);
        particles.add(particle);
    }

    @Override
    public void render(CombatEngineLayers layer, ViewportAPI viewport)
    {
        // render particles
        if (!particles.isEmpty())
        {
            float b = viewport.getAlphaMult();
            for (TargetedParticle p : particles)
            {
                p.sprite.setAngle(p.angle);
                p.sprite.setSize(p.size, p.size);
                float brightness = 1f;
                if (p.elapsed > PARTICLE_DURATION / 2f)
                    brightness = 2f - 2f * p.elapsed;
                p.sprite.setAlphaMult(b * brightness);
                p.sprite.setColor(p.color);
                p.sprite.renderAtCenter(p.pos.x, p.pos.y);
            }
        }

        // render indicator ring
        if (Global.getCombatEngine().getPlayerShip() != ship || ship == null)
            return;
        if (ship.isHulk() || !ship.isAlive())
            return;
        if (!Global.getCombatEngine().isUIShowingHUD())
            return;
        Vector2f ringCenter = ship.getLocation();
        // 1.0 at 100% width
        float arcWidth = ApexExcessionReactor.chargeMap.get(ship) / ApexExcessionReactor.MAX_STORED_CHARGE;
        // draw segments



        float separation = 360f / (float) NUM_INDICATOR_SEGMENTS;
        for (int i = 0; i < NUM_INDICATOR_SEGMENTS; i++)
        {
            glDisable(GL_TEXTURE_2D);
            glEnable(GL_BLEND);
            glEnable(GL_LINE_SMOOTH);
            glHint(GL_LINE_SMOOTH_HINT, GL_NICEST);
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
            glLineWidth(LINE_WIDTH / viewport.getViewMult());
            glBegin(GL_LINE_STRIP);
            glColor(LINE_COLOR, INDICATOR_OPACITY, true);
            for (int j = 0; j < 13; j++) // 100 points for the whole circle is enough to look nice
            {
                float pointAngle = (j - 6) * (separation * arcWidth / 13f) + i * separation;
                Vector2f pointLoc = MathUtils.getPointOnCircumference(ringCenter, ApexExcessionReactor.ARC_RANGE, pointAngle);
                glVertex2f(pointLoc.x, pointLoc.y);
            }
            glEnd();
            glLineWidth(1f);
            glDisable(GL_BLEND);
            glPopAttrib();
        }
    }

    @Override
    public void advance(float amount)
    {
        ArrayList<TargetedParticle> toRemove = new ArrayList<>();
        for (TargetedParticle p : particles)
        {
            p.advance(amount);
            if (p.elapsed > PARTICLE_DURATION)
                toRemove.add(p);
        }
        if (!toRemove.isEmpty())
        {
            for (TargetedParticle p : toRemove)
                particles.remove(p);
        }
    }

    @Override
    public float getRenderRadius()
    {
        return 999999f;
    }

    @Override
    public boolean isExpired()
    {
        return (false);
    }

    protected EnumSet<CombatEngineLayers> layers = EnumSet.of(CombatEngineLayers.FF_INDICATORS_LAYER);

    @Override
    public EnumSet<CombatEngineLayers> getActiveLayers()
    {
        return layers;
    }

}
