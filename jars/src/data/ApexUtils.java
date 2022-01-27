package data;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.Misc;
import org.dark.shaders.distortion.DistortionShader;
import org.dark.shaders.distortion.WaveDistortion;
import org.dark.shaders.light.LightShader;
import org.dark.shaders.light.StandardLight;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;

public class ApexUtils
{
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
    // code provided by tomatopaste

    /**
     *
     * @param entity
     * @param center center point of arc
     * @param centerAngle angle from center point to center of the edge
     * @param arcDeviation angle to either side of center point (ie, 45 here would make a 90 degree arc)
     * @return
     */
    public static boolean isEntityInArc(CombatEntityAPI entity, Vector2f center, float centerAngle, float arcDeviation)
    {
        if (entity instanceof ShipAPI)
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
            Vector2f closest = ship.getLocation();
            float distSquared = 0f;
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
}
