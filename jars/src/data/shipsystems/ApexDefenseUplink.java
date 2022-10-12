package data.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class ApexDefenseUplink extends BaseShipSystemScript
{
    public static final float RANGE = 1600f;
    public static final float SHIELD_EFFICIENCY_MULT = 0.8f;
    public static final float ARMOR_EFFECTIVE_MULT = 1.33f;
    public static final String BUFF_ID = "apex_uplink_sys";
    public static final Color JITTER_COLOR = new Color(0, 190, 0);
    private static final int RING_PARTICLE_COUNT = 20;

    private static boolean addedPlugin = false;
    private static int hashCode = 0;
    

    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel)
    {

        if (stats.getEntity() == null)
            return;
        // plugin handles the buffs
        if (hashCode != Global.getCombatEngine().hashCode())
        {
            Global.getCombatEngine().addPlugin(new ApexDefenseUplinkPlugin());
            hashCode = Global.getCombatEngine().hashCode();
        }
        ShipAPI ship = (ShipAPI) stats.getEntity();
        drawParticleRing(ship, effectLevel, ship.getMutableStats().getSystemRangeBonus().computeEffective(RANGE));
    }

    @Override
    public void unapply(MutableShipStatsAPI stats, String id)
    {
        if (hashCode != Global.getCombatEngine().hashCode())
        {
            Global.getCombatEngine().addPlugin(new ApexDefenseUplinkPlugin());
            hashCode = Global.getCombatEngine().hashCode();
        }
    }

    // thanks histi
    // Used to indicate the range, because I CBA to write a separate combat plugin for drawing
    public void drawParticleRing(ShipAPI ship, float effectMult, float distance) {
        // animates the ring
        float angleOffset = Global.getCombatEngine().getTotalElapsedTime(false)*10;

        float anglePerParticle = 360/RING_PARTICLE_COUNT;
        float size = 25 * effectMult;
        Vector2f origin = new Vector2f(ship.getLocation());
        Vector2f shipVel = ship.getVelocity();

        for (int i=0; i<RING_PARTICLE_COUNT; i++) {
            float angle = i * anglePerParticle + angleOffset;
            Vector2f pos = VectorUtils.rotate(new Vector2f(0, distance), angle);
            pos.x += origin.x;
            pos.y += origin.y;
            Vector2f vel = VectorUtils.rotate(new Vector2f(150, 0), angle);
            // remove effect of ship velocity on the particle movement
            vel.x += shipVel.x;
            vel.y += shipVel.y;

            Global.getCombatEngine().addSmoothParticle(pos, vel, size,
                    1,    // brightness
                    0.25f,    // duration
                    JITTER_COLOR);
        }
    }
}
