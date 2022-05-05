package data.effects;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.util.FaderUtil;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.opengl.GL14;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import static com.fs.starfarer.api.impl.combat.DisintegratorEffect.getDamageTypeMult;
import static plugins.ApexModPlugin.POTATO_MODE;

public class ApexNanoacidEffect extends BaseCombatLayeredRenderingPlugin
{
    // each tick is on average .33 seconds
    private int maxTicks = 0;
    private float damageRemaining = 0;
    protected float sizeMult;
    private boolean degradeArmor;
    private boolean damagehull;
    private Color cloudColor;
    protected List<ParticleData> particles = new ArrayList<ParticleData>();
    protected DamagingProjectileAPI proj;
    protected ShipAPI target;
    protected Vector2f offset;
    protected int ticks = 0;
    protected IntervalUtil interval;
    protected FaderUtil fader = new FaderUtil(1f, 0.5f, 0.5f);

    public ApexNanoacidEffect()
    {
    }

    public ApexNanoacidEffect(DamagingProjectileAPI proj, ShipAPI target, Vector2f offset, float sizeMult, int alpha, float damage, float duration, boolean degradeArmor, boolean damageHull)
    {
        this.proj = proj;
        this.target = target;
        this.offset = offset;
        this.maxTicks = (int) (duration * 2f);
        this.damageRemaining = damage;
        this.sizeMult = sizeMult;
        this.degradeArmor = degradeArmor;
        this.damagehull = damageHull;
        this.cloudColor = new Color(100, 60, 120, alpha);

        interval = new IntervalUtil(0.4f, 0.6f);
        interval.forceIntervalElapsed();
    }

    public static class ParticleData
    {
        public SpriteAPI sprite;
        public Vector2f offset = new Vector2f();
        public Vector2f vel = new Vector2f();
        public float scale = 1f;
        public float scaleIncreaseRate = 1f;
        public float turnDir = 1f;
        public float angle = 1f;

        public float maxDur;
        public FaderUtil fader;
        public float elapsed = 0f;
        public float baseSize;

        public ParticleData(float baseSize, float maxDur, float endSizeMult)
        {
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

            this.maxDur = maxDur;
            scaleIncreaseRate = endSizeMult / maxDur;
            if (endSizeMult < 1f)
            {
                scaleIncreaseRate = -1f * endSizeMult;
            }
            scale = 1f;

            this.baseSize = baseSize;
            turnDir = Math.signum((float) Math.random() - 0.5f) * 20f * (float) Math.random();
            //turnDir = 0f;

            float driftDir = (float) Math.random() * 360f;
            vel = Misc.getUnitVectorAtDegreeAngle(driftDir);
            //vel.scale(proj.getProjectileSpec().getLength() / maxDur * (0f + (float) Math.random() * 3f));
            vel.scale(0.25f * baseSize / maxDur * (1f + (float) Math.random() * 1f));

            fader = new FaderUtil(0f, 0.5f, 0.5f);
            fader.forceOut();
            fader.fadeIn();
        }

        public void advance(float amount)
        {
            scale += scaleIncreaseRate * amount;

            offset.x += vel.x * amount;
            offset.y += vel.y * amount;

            angle += turnDir * amount;

            elapsed += amount;
            if (maxDur - elapsed <= fader.getDurationOut() + 0.1f)
            {
                fader.fadeOut();
            }
            fader.advance(amount);
        }
    }

    @Override
    public float getRenderRadius()
    {
        return 500f;
    }

    protected EnumSet<CombatEngineLayers> layers = EnumSet.of(CombatEngineLayers.BELOW_INDICATORS_LAYER);

    @Override
    public EnumSet<CombatEngineLayers> getActiveLayers()
    {
        return layers;
    }

    @Override
    public void init(CombatEntityAPI entity)
    {
        super.init(entity);
    }

    @Override
    public void advance(float amount)
    {
        if (Global.getCombatEngine().isPaused()) return;

        if (degradeArmor)
            target.getMutableStats().getMinArmorFraction().modifyMult("apexAcid", 0.5f);
        Vector2f loc = new Vector2f(offset);
        loc = Misc.rotateAroundOrigin(loc, target.getFacing());
        Vector2f.add(target.getLocation(), loc, loc);
        entity.getLocation().set(loc);

        List<ParticleData> remove = new ArrayList<ParticleData>();
        for (ParticleData p : particles)
        {
            p.advance(amount);
            if (p.elapsed >= p.maxDur)
            {
                remove.add(p);
            }
        }
        particles.removeAll(remove);

        float volume = 1f;
        if (ticks >= maxTicks || !target.isAlive() || !Global.getCombatEngine().isEntityInPlay(target))
        {
            fader.fadeOut();
            fader.advance(amount);
            volume = fader.getBrightness();
            if (degradeArmor)
                target.getMutableStats().getMinArmorFraction().unmodify("apexAcid");
        }
        Global.getSoundPlayer().playLoop("disintegrator_loop", target, 1f, volume, loc, target.getVelocity());


        interval.advance(amount);
        if (interval.intervalElapsed() && ticks < maxTicks)
        {
            dealDamage();
            ticks++;
        }
    }


    protected void dealDamage()
    {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (!POTATO_MODE)
        {
            int num = 3;
            for (int i = 0; i < num; i++)
            {
                ParticleData p = new ParticleData(30f * sizeMult, 3f + (float) Math.random() * 2f, 2f);
                particles.add(p);
                p.offset = Misc.getPointWithinRadius(p.offset, 20f);
            }
        }


        Vector2f point = new Vector2f(entity.getLocation());

        // maximum armor in a cell is 1/15th of the ship's stated armor rating

        ArmorGridAPI grid = target.getArmorGrid();
        int[] cell = grid.getCellAtLocation(point);
        if (cell == null) return;

        int gridWidth = grid.getGrid().length;
        int gridHeight = grid.getGrid()[0].length;

        float damageTypeMult = getDamageTypeMult(proj.getSource(), target) * target.getMutableStats().getArmorDamageTakenMult().getModifiedValue();

        float damagePerTick = (float) damageRemaining / (float) (maxTicks - ticks);
        float damageDealt = 0f;
        float excessDamage = 0f;

        for (int i = -2; i <= 2; i++)
        {
            for (int j = -2; j <= 2; j++)
            {
                if ((i == 2 || i == -2) && (j == 2 || j == -2)) continue; // skip corners

                int cx = cell[0] + i;
                int cy = cell[1] + j;

                if (cx < 0 || cx >= gridWidth || cy < 0 || cy >= gridHeight) continue;

                float damMult = 1 / 30f;
                if (i == 0 && j == 0)
                {
                    damMult = 1 / 15f;
                } else if (i <= 1 && i >= -1 && j <= 1 && j >= -1)
                { // S hits
                    damMult = 1 / 15f;
                } else
                { // T hits
                    damMult = 1 / 30f;
                }

                float armorInCell = grid.getArmorValue(cx, cy);
                float damage = damagePerTick * damMult * damageTypeMult;
                if (damage > armorInCell)
                    excessDamage += damage - armorInCell;
                damage = Math.min(damage, armorInCell);
                if (damage <= 0) continue;

                target.getArmorGrid().setArmorValue(cx, cy, Math.max(0, armorInCell - damage));
                damageDealt += damage;
            }
        }

        if (damagehull)
        {
            target.setHitpoints(Math.max(target.getHitpoints() - excessDamage, 1));
            if (target.getHitpoints() == 1)
                engine.applyDamage(target, point, 50, DamageType.FRAGMENTATION, 0, true, false, proj.getSource(), false);
        }

        if (damageDealt > 0 || excessDamage > 0)
        {
            if (Misc.shouldShowDamageFloaty(proj.getSource(), target))
            {
                if (damageDealt > 0)
                    engine.addFloatingDamageText(point, damageDealt, Misc.FLOATY_ARMOR_DAMAGE_COLOR, target, proj.getSource());
                if (excessDamage > 0)
                    engine.addFloatingDamageText(point, excessDamage, Misc.FLOATY_HULL_DAMAGE_COLOR, target, proj.getSource());
            }
            damageRemaining -= damageDealt;
            damageRemaining -= excessDamage;
            target.syncWithArmorGridState();
        }

    }

    @Override
    public boolean isExpired()
    {
        return particles.isEmpty() &&
                (ticks >= maxTicks || !target.isAlive() || !Global.getCombatEngine().isEntityInPlay(target));
    }

    @Override
    public void render(CombatEngineLayers layer, ViewportAPI viewport)
    {
        float x = entity.getLocation().x;
        float y = entity.getLocation().y;

        float b = viewport.getAlphaMult();

        GL14.glBlendEquation(GL14.GL_FUNC_REVERSE_SUBTRACT);

        for (ParticleData p : particles)
        {
            //float size = proj.getProjectileSpec().getWidth() * 0.6f;
            float size = p.baseSize * p.scale;

            Vector2f loc = new Vector2f(x + p.offset.x, y + p.offset.y);

            float alphaMult = 1f;

            p.sprite.setAngle(p.angle);
            p.sprite.setSize(size, size);
            p.sprite.setAlphaMult(b * alphaMult * p.fader.getBrightness());
            p.sprite.setColor(cloudColor);
            p.sprite.renderAtCenter(loc.x, loc.y);
        }

        GL14.glBlendEquation(GL14.GL_FUNC_ADD);
    }



}

