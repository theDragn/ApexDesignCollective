package data.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.CombatUtils;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashSet;

public class ApexUplink extends BaseShipSystemScript
{
    public static final float RANGE = 2000f;
    public static final float SHIELD_EFFICIENCY_MULT = 0.8f;
    public static final float ARMOR_EFFECTIVE_MULT = 1.2f;
    public static final String BUFF_ID = "apex_uplink_sys";
    public static final Color JITTER_COLOR = new Color(0, 190, 0);

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

    private class ApexUplinkBuff
    {
        private float lifetime;
        private ShipAPI target;
        private ShipAPI source;

        public ApexUplinkBuff(ShipAPI source, ShipAPI target)
        {
            this.target = target;
            this.source = source;
            this.lifetime = 0.2f;
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
            ((ShipAPI)stats.getEntity()).setJitterUnder(BUFF_ID, JITTER_COLOR, 3f * lifetime * 5f, 3, 0f, 25f * lifetime);
        }
    }
}
