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

public class ApexUplink extends BaseShipSystemScript
{
    public static final float RANGE = 1600f;
    public static final float SHIELD_EFFICIENCY_MULT = 0.8f;
    public static final float ARMOR_EFFECTIVE_MULT = 1.2f;
    public static final String BUFF_ID = "apex_uplink_sys";
    public static final Color JITTER_COLOR = new Color(0, 190, 0);
    private static final int RING_PARTICLE_COUNT = 20;

    private ArrayList<ApexUplinkBuff> buffs = new ArrayList<>();
    protected HashSet<ShipAPI> targets = new HashSet<>();
    

    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel)
    {
        if (stats.getEntity() == null)
            return;
        ShipAPI ship = (ShipAPI) stats.getEntity();
        float range = ship.getMutableStats().getSystemRangeBonus().computeEffective(RANGE);
        for (ShipAPI target : CombatUtils.getShipsWithinRange(ship.getLocation(), range))
        {
            if (target.isAlive() && target.getOwner() == ship.getOwner())
            {
                ApexUplinkBuff buff = new ApexUplinkBuff(ship, target);
                targets.add(target);
                buffs.add(buff);
            }

        }
        // advance buffs
        float amount = Global.getCombatEngine().getElapsedInLastFrame();
        for (ApexUplinkBuff buff : buffs)
        {
            buff.lifetime += amount;
            buff.update(amount);
        }
        drawParticleRing(ship, effectLevel, range);
    }

    @Override
    public void unapply(MutableShipStatsAPI stats, String id)
    {
        if (stats.getEntity() == null)
            return;
        float amount = Global.getCombatEngine().getElapsedInLastFrame();
        for (ApexUplinkBuff buff : buffs)
        {
            buff.update(amount);
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

    private class ApexUplinkBuff
    {
        private float lifetime;
        private ShipAPI target;
        private ShipAPI source;

        public ApexUplinkBuff(ShipAPI source, ShipAPI target)
        {
            this.target = target;
            this.source = source;
            this.lifetime = 1f;
        }

        // advance()
        public void update(float amount)
        {
            // should probably do graphical stuff in here too
            MutableShipStatsAPI stats = target.getMutableStats();
            if (!target.isAlive() || !source.isAlive() || MathUtils.getDistanceSquared(target.getLocation(), source.getLocation()) > RANGE * RANGE)
            {
                stats.getEffectiveArmorBonus().modifyMult(BUFF_ID, ARMOR_EFFECTIVE_MULT);
                stats.getShieldDamageTakenMult().modifyMult(BUFF_ID, SHIELD_EFFICIENCY_MULT);
            } else
            {
                stats.getEffectiveArmorBonus().unmodify(BUFF_ID);
                stats.getShieldDamageTakenMult().unmodify(BUFF_ID);
                lifetime -= amount;
                if (lifetime == 0)
                {
                    targets.remove(target);
                    buffs.remove(this);
                }
            }
            ((ShipAPI)stats.getEntity()).setJitterUnder(BUFF_ID, JITTER_COLOR, 2f * lifetime, 3, 0f, 4f * lifetime);
            ((ShipAPI)stats.getEntity()).setJitterShields(false);
        }
    }
}
