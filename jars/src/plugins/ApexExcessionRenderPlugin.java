package plugins;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import data.hullmods.ApexExcessionReactor;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.EnumSet;

import static org.lazywizard.lazylib.opengl.ColorUtils.glColor;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA;

// renders Excession's particle effects and indicator ring
public class ApexExcessionRenderPlugin extends BaseCombatLayeredRenderingPlugin
{
    public static final int NUM_INDICATOR_SEGMENTS = 8;
    public static final float INDICATOR_OPACITY = 1f;
    public static final float LINE_WIDTH = 3f;
    public static final Color LINE_COLOR = new Color(0, 158, 0, 255);

    private ShipAPI ship;

    public ApexExcessionRenderPlugin(ShipAPI ship)
    {
        this.ship = ship;
    }

    public ApexExcessionRenderPlugin()
    {
    }

    @Override
    public void render(CombatEngineLayers layer, ViewportAPI viewport)
    {

        // render indicator ring
        if (Global.getCombatEngine().getPlayerShip() != ship || ship == null)
            return;
        if (ship.isHulk() || !ship.isAlive())
            return;
        if (!Global.getCombatEngine().isUIShowingHUD())
            return;
        Vector2f ringCenter = ship.getLocation();
        // 1.0 at 100% width
        ApexExcessionReactor.ExcessionData data = (ApexExcessionReactor.ExcessionData)ship.getCustomData().get(ApexExcessionReactor.KEY);
        float arcWidth = data.getEntropy() / ApexExcessionReactor.MAX_ENTROPY;
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
                Vector2f pointLoc = MathUtils.getPointOnCircumference(ringCenter, 400f, pointAngle);
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
