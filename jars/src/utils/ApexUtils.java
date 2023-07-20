package utils;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.loading.ProjectileSpawnType;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import com.fs.starfarer.api.util.Misc;
import data.hullmods.ApexFastNozzles;
import data.hullmods.ApexSlowNozzles;
import org.magiclib.plugins.MagicTrailPlugin;
import org.dark.shaders.distortion.DistortionShader;
import org.dark.shaders.distortion.WaveDistortion;
import org.dark.shaders.light.LightShader;
import org.dark.shaders.light.StandardLight;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;

import static org.lwjgl.opengl.GL11.GL_ONE;
import static org.lwjgl.opengl.GL11.GL_SRC_ALPHA;

public class ApexUtils
{
    /**
     * Used for externalizing strings for translation
     * @param id text id
     * @return string
     */
    public static String text(String id)
    {
        return Global.getSettings().getString("apex", id);
    }


    /**
     * Draws an arc explosion thingy.
     *
     * @param source projectile to draw arcs from
     */
    public static void plasmaEffects(CombatEntityAPI source, Color color, float expRadius)
    {
        final int NUM_ARCS = 4;
        Vector2f from = MathUtils.getRandomPointInCircle(source.getLocation(), 20f);
        CombatEngineAPI engine = Global.getCombatEngine();
        for (int i = 0; i < NUM_ARCS; i++)
        {
            engine.spawnEmpArcVisual(from, null, MathUtils.getRandomPointInCircle(from, expRadius * 0.66f), null,
                    10f, color, Color.white);
        }
        if (expRadius > 0f)
        {
            engine.addSmoothParticle(source.getLocation(), Misc.ZERO, expRadius, 1.3f, 1.2f, color);
            engine.addSmoothParticle(source.getLocation(), Misc.ZERO, expRadius * 0.5f, 1.3f, 0.5f, Color.white);
        }
    }

    public static void addWaveDistortion(Vector2f location, float intensity, float size, float duration)
    {
        WaveDistortion wave = new WaveDistortion();
        wave.setLocation(location);
        wave.setIntensity(intensity);
        wave.fadeInSize(0.1F);
        wave.fadeOutIntensity(0.33F);
        wave.setSize(size);
        wave.setLifetime(duration);
        wave.setAutoFadeIntensityTime(0.67F);
        DistortionShader.addDistortion(wave);
    }

    public static void addLight(Vector2f location, float intensity, float size, float duration, Color color)
    {
        StandardLight light = new StandardLight();
        light.setLocation(location);
        light.setIntensity(intensity);
        light.setSize(size);
        light.setLifetime(duration);
        light.setColor(color);
        light.setAutoFadeOutTime(0.67f);
        LightShader.addLight(light);
    }

    /**
     * Used for figuring out damage mults for things that don't apply damage (onhit hardflux and armor damage, for example)
     * @param source
     * @param target
     * @return
     */
    public static float getDamageTypeMult(ShipAPI source, ShipAPI target)
    {
        if (source == null || target == null) return 1f;

        float damageTypeMult = 1f;
        switch (target.getHullSize())
        {
            case CAPITAL_SHIP:
                damageTypeMult *= source.getMutableStats().getDamageToCapital().getModifiedValue();
                break;
            case CRUISER:
                damageTypeMult *= source.getMutableStats().getDamageToCruisers().getModifiedValue();
                break;
            case DESTROYER:
                damageTypeMult *= source.getMutableStats().getDamageToDestroyers().getModifiedValue();
                break;
            case FRIGATE:
                damageTypeMult *= source.getMutableStats().getDamageToFrigates().getModifiedValue();
                break;
            case FIGHTER:
                damageTypeMult *= source.getMutableStats().getDamageToFighters().getModifiedValue();
                break;
        }
        return damageTypeMult;
    }

    /**
     * needs fixing, ship bounds aren't actually updated every frame, as far as I can tell
     * @param entity
     * @param center center point of arc
     * @param centerAngle angle from center point to center of the edge
     * @param arcDeviation angle to either side of center point (ie, 45 here would make a 90 degree arc)
     * @return
     */
    public static boolean isEntityInArc(CombatEntityAPI entity, Vector2f center, float centerAngle, float arcDeviation)
    {
        if (false) //entity instanceof ShipAPI)
        {
            Vector2f point = getNearestPointOnShipBounds((ShipAPI) entity, center);
            return Misc.isInArc(centerAngle, arcDeviation * 2f, center, point);
        } else
        {
            return Misc.isInArc(centerAngle, arcDeviation * 2f, center,
                    getNearestPointOnCollisionRadius(center, entity));
        }
    }

    /**
     *
     * @param ship
     * @param point
     * @return
     */
    public static Vector2f getNearestPointOnShipBounds(ShipAPI ship, Vector2f point)
    {
        BoundsAPI bounds = ship.getExactBounds();
        if (bounds == null)
        {
            return getNearestPointOnCollisionRadius(point, ship);
        } else
        {
            Vector2f closest = new Vector2f(ship.getLocation());
            float distSquared = 99999f;
            for (BoundsAPI.SegmentAPI segment : bounds.getSegments())
            {
                Vector2f tmpcp = MathUtils.getNearestPointOnLine(point, segment.getP1(), segment.getP2());
                float distSquaredTemp = MathUtils.getDistanceSquared(tmpcp, point);
                if (distSquaredTemp < distSquared)
                {
                    distSquared = distSquaredTemp;
                    closest = tmpcp;
                }
            }
            return closest;
        }
    }

    public static Vector2f getNearestPointOnCollisionRadius(Vector2f point, CombatEntityAPI entity)
    {
        return MathUtils.getPointOnCircumference(entity.getLocation(), entity.getCollisionRadius(),
                VectorUtils.getAngle(entity.getLocation(), point));
    }

    /**
     * Blends two colors
     *
     * @param c1    color 1
     * @param c2    color 2
     * @param ratio what percent of color 1 the result should have
     * @return blended color
     */
    public static Color blendColors(Color c1, Color c2, float ratio)
    {

        float iRatio = 1.0f - ratio;

        int a1 = c1.getAlpha();
        int r1 = c1.getRed();
        int g1 = c1.getGreen();
        int b1 = c1.getBlue();

        int a2 = c2.getAlpha();
        int r2 = c2.getRed();
        int g2 = c2.getGreen();
        int b2 = c2.getBlue();

        int a = (int) ((a1 * iRatio) + (a2 * ratio));
        int r = (int) ((r1 * iRatio) + (r2 * ratio));
        int g = (int) ((g1 * iRatio) + (g2 * ratio));
        int b = (int) ((b1 * iRatio) + (b2 * ratio));

        return new Color(r, g, b, a);
    }

    // unused but perhaps useful in the future
    // oh my god thank you, past me
    /**
     * Returns true if there is no more armor to remove at an impact point
     * @param target
     * @param point
     * @return
     */
    public static boolean isArmorStripped(ShipAPI target, Vector2f point)
    {
        ArmorGridAPI grid = target.getArmorGrid();
        int[] cell = grid.getCellAtLocation(point);
        if (cell == null) return true;

        int gridWidth = grid.getGrid().length;
        int gridHeight = grid.getGrid()[0].length;

        int cellsWithArmor = 0;
        for (int i = -2; i <= 2; i++)
        {
            for (int j = -2; j <= 2; j++)
            {
                if ((i == 2 || i == -2) && (j == 2 || j == -2)) continue; // skip corners

                int cx = cell[0] + i;
                int cy = cell[1] + j;

                if (cx < 0 || cx >= gridWidth || cy < 0 || cy >= gridHeight) continue;


                if (grid.getArmorValue(cx, cy) > 0)
                    return false;
            }
        }
        return true;
    }

    public static float getNozzleCooldownMult(ShipAPI ship)
    {
        float mult = 1f;
        if (ship.getVariant().hasHullMod("apex_fast_nozzles"))
            mult *= ApexFastNozzles.NOZZLE_COOLDOWN_MULT;
        if (ship.getVariant().hasHullMod("apex_slow_nozzles"))
            mult *= ApexSlowNozzles.NOZZLE_COOLDOWN_MULT;
        if (ship.getVariant().hasHullMod("apex_gantry"))
            mult *= ApexSlowNozzles.NOZZLE_COOLDOWN_MULT;
        return mult;
    }

    public static int getNumNozzles(ShipAPI ship)
    {
        int nozzles = 0;
        for (WeaponSlotAPI slot : ship.getHullSpec().getAllWeaponSlotsCopy())
        {
            if (slot.isSystemSlot() && !slot.getId().contains("MINE"))
                nozzles++;
        }
        return nozzles;
    }

    public static Vector2f getRandomPointOnShipBounds(ShipAPI ship)
    {
        BoundsAPI bounds = ship.getExactBounds();
        int segment = Misc.random.nextInt(bounds.getSegments().size());
        return MathUtils.getRandomPointOnLine(
                bounds.getSegments().get(segment).getP1(),
                bounds.getSegments().get(segment).getP2()
        );
    }

    public static float lerp(float a, float b, float amount)
    {
        return a*(1f-amount) + b*amount;
    }

    public static Vector2f lerp(Vector2f a, Vector2f b, float amount)
    {
        return new Vector2f(lerp(a.x, b.x, amount), lerp(a.y, b.y, amount));
    }
}
