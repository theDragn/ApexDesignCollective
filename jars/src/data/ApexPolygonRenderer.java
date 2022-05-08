package data;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEngineLayers;
import com.fs.starfarer.api.combat.ShipAPI;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lazywizard.lazylib.ui.FontException;
import org.lazywizard.lazylib.ui.LazyFont;
import org.lazywizard.lazylib.ui.LazyFont.DrawableString;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

import static org.lazywizard.lazylib.opengl.ColorUtils.glColor;
import static org.lwjgl.opengl.GL11.*;

public class ApexPolygonRenderer
{

    // FIGHTING POLYGON ~~TEAM~~ RENDERER
    // lots of code here provided by tomatopaste
    // not used by default, but I spent too long fiddling with it to delete it now

    private static DrawableString DRAW_STRING;

    static {
        try {
            LazyFont fontdraw = LazyFont.loadFont("graphics/fonts/victor14.fnt");
            DRAW_STRING = fontdraw.createText("testing one two three", Color.MAGENTA, 14f);

        } catch (FontException ex) {
        }
    }

    public static void drawText(Vector2f location, String text, Color textColor, float fontSize)
    {
        float viewMult = Global.getCombatEngine().getViewport().getViewMult();
        DRAW_STRING.setColor(Color.black);
        DRAW_STRING.draw(location.x + 2f * viewMult, location.y - 2f * viewMult);
        DRAW_STRING.setColor(textColor);
        DRAW_STRING.setText(text);
        DRAW_STRING.setFontSize(fontSize);
        DRAW_STRING.draw(location);
    }

    public static class ApexPrimitivePolygon
    {
        public float vertexRadius;
        public int points;
        public Color color;
        public Vector2f location;
        public CombatEngineLayers layer;
        public float alpha;
        public float lineWidth;
        public float rotation;
        public boolean fill;

        public ApexPrimitivePolygon(
                Vector2f location,
                Color color,
                CombatEngineLayers layer,
                float vertexRadius,
                float angle,
                int points,
                float alpha,
                float lineWidth,
                boolean fill
        )
        {
            this.location = new Vector2f(location);
            this.color = color;
            this.layer = layer;
            this.vertexRadius = vertexRadius;
            this.points = points;
            this.rotation = angle;
            this.fill = fill;
            this.alpha = alpha;
            this.lineWidth = lineWidth;
        }


        public void render(CombatEngineAPI engine)
        {
            glDisable(GL_TEXTURE_2D);
            glEnable(GL_BLEND);
            glEnable(GL_LINE_SMOOTH);
            glHint(GL_LINE_SMOOTH_HINT, GL_NICEST);
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

            if (fill)
            {
                glBegin(GL_POLYGON);

                glColor(color, alpha * 0.25f, true);

                for (int i = 0; i < points; i++)
                {
                    Vector2f vertex = getVertex(i);
                    glVertex2f(vertex.x, vertex.y);
                }

                glEnd();
            }
            glLineWidth(lineWidth / engine.getViewport().getViewMult());
            glBegin(GL_LINE_LOOP);

            glColor(color, alpha, true);

            for (int i = 0; i < points; i++)
            {
                Vector2f vertex = getVertex(i);
                glVertex2f(vertex.x, vertex.y);
            }

            glEnd();
            glLineWidth(1f);
            glDisable(GL_BLEND);
            glPopAttrib();
        }

        public Vector2f getVertex(int index)
        {
            float divisor = 360f / points;
            float angle = divisor * index;

            Vector2f up = (Vector2f) new Vector2f(0f, 1f).scale(vertexRadius);
            up = VectorUtils.rotate(up, angle);
            up = VectorUtils.rotate(up, rotation);

            return Vector2f.add(up, location, null);
        }

        public boolean shouldRender(CombatEngineAPI engine)
        {
            return engine.getViewport().isNearViewport(location, vertexRadius);
        }
    }
    private static void openGL11ForText() {
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT);
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glViewport(0, 0, Display.getWidth(), Display.getHeight());
        GL11.glOrtho(0.0, Display.getWidth(), 0.0, Display.getHeight(), -1.0, 1.0);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
    }
    private static void closeGL11ForText() {
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glPopMatrix();
        GL11.glPopAttrib();
    }
}
