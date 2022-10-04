package data.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import org.lazywizard.lazylib.combat.CombatUtils;

import java.util.ArrayList;

public class ApexUplink extends BaseShipSystemScript
{
    public static final float RANGE = 2000f;
    public static final float SHIELD_EFFICIENCY_MULT = 0.8f;
    public static final float ARMOR_EFFECTIVE_MULT = 1.2f;
    public static final String BUFF_ID = "apex_uplink_sys";

    private ArrayList<ApexUplinkBuff> buffs = new ArrayList<>();
    

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
                buffs.add(buff);
                Global.getCombatEngine().addPlugin(buff);
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
        super.unapply(stats, id);
    }

    private class ApexUplinkBuff extends BaseEveryFrameCombatPlugin
    {
        private float lifetime;
        private ShipAPI target;

        public ApexUplinkBuff(ShipAPI target)
        {
            this.target = target;
            this.lifetime = 0.2f;
        }

        // advance()
        public boolean update(float amount)
        {
            // should probably do graphical stuff in here too
            lifetime -= amount;
            MutableShipStatsAPI stats = target.getMutableStats();
            if (lifetime > 0)
            {
                stats.getEffectiveArmorBonus().modifyMult(BUFF_ID, ARMOR_EFFECTIVE_MULT);
                stats.getShieldDamageTakenMult().modifyMult(BUFF_ID, SHIELD_EFFICIENCY_MULT);
                return true;
            } else
            {
                stats.getEffectiveArmorBonus().unmodify(BUFF_ID);
                stats.getShieldDamageTakenMult().unmodify(BUFF_ID);
                return false;
            }
        }
    }
}
