package data.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.ArrayList;

import static data.ApexUtils.text;

// unused
// tested it and didn't like it
public class ApexTemporalSiphon extends BaseShipSystemScript
{
    public static final float RANGE = 1500f;
    public static final float SIPHON_AMOUNT = 0.15f; // takes this much timeflow per target

    public static Object KEY_SIPHON = new Object();
    private IntervalUtil arcTimer = new IntervalUtil(0.1f, 0.2f);
    private ArrayList<ShipAPI> targets = new ArrayList<>();
    private boolean runOnce = false;
    private float timeMult = 0;


    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel)
    {
        ShipAPI ship = (ShipAPI)stats.getEntity();
        float range = ship.getMutableStats().getSystemRangeBonus().computeEffective(RANGE);
        // while charging: decorative arcs to targets in range
        if (state == State.IN)
        {
            arcTimer.advance(Global.getCombatEngine().getElapsedInLastFrame());
            if (arcTimer.intervalElapsed())
            {
                WeightedRandomPicker<ShipAPI> targetsToArc = new WeightedRandomPicker<>();
                for (ShipAPI target : CombatUtils.getShipsWithinRange(ship.getLocation(), range))
                {
                    if (target.getOwner() == ship.getOwner())
                        continue;
                    targetsToArc.add(target);
                }
                if (!targetsToArc.isEmpty())
                {
                    ShipAPI target = targetsToArc.pick();
                    // from point on edge of collision radius, towards target, to random point within target collision radius
                    Vector2f from = MathUtils.getPointOnCircumference(ship.getLocation(), ship.getCollisionRadius(), VectorUtils.getAngle(ship.getLocation(), target.getLocation()));
                    Vector2f to = MathUtils.getRandomPointInCircle(target.getLocation(), target.getCollisionRadius() * 0.5f);
                    Global.getCombatEngine().spawnEmpArcVisual(from, ship, to, target, 10f, Color.white, Color.white);
                }
            }
        } else if (state == State.ACTIVE && !runOnce)
        {
            // record targets
            runOnce = true;
            for (ShipAPI target : CombatUtils.getShipsWithinRange(ship.getLocation(), range))
            {
                if (target.getOwner() == ship.getOwner())
                    continue;
                targets.add(target);
                if (!target.isFighter())
                    target.getFluxTracker().showOverloadFloatyIfNeeded(text("timesiphon1"), new Color(255, 0, 170, 255), 2f, true);
            }
            if (targets.isEmpty())
            {
                ship.getSystem().forceState(ShipSystemAPI.SystemState.COOLDOWN, 0f);
                ship.getSystem().setCooldownRemaining(0.5f);
            }
            timeMult = 1f + SIPHON_AMOUNT * targets.size();
        }
        if (state == State.ACTIVE && runOnce)
        {
            stats.getTimeMult().modifyMult(id, timeMult);
            if (ship == Global.getCombatEngine().getPlayerShip())
                Global.getCombatEngine().getTimeMult().modifyMult(id, 1f / timeMult);
            else
                Global.getCombatEngine().getTimeMult().unmodify(id);

            for (ShipAPI target : targets)
            {
                target.getMutableStats().getTimeMult().modifyMult(id, 1f - SIPHON_AMOUNT);
                target.setJitter(KEY_SIPHON, new Color(255, 0, 170, 84), effectLevel, 3, 4f, 15f);
                if (target == Global.getCombatEngine().getPlayerShip())
                {
                    Global.getCombatEngine().maintainStatusForPlayerShip(KEY_SIPHON,
                            ship.getSystem().getSpecAPI().getIconSpriteName(), ship.getSystem().getDisplayName(), "15% " + text("timesiphon2"), true);
                }
            }
        }
    }

    @Override
    public void unapply(MutableShipStatsAPI stats, String id)
    {
        // iterate over list, remove debuffs
        if (!targets.isEmpty())
        {
            for (ShipAPI target : targets)
            {
                target.getMutableStats().getTimeMult().unmodify(id);
            }
        }
        stats.getTimeMult().unmodify(id);
        Global.getCombatEngine().getTimeMult().unmodify(id);
        // remove buff
        targets.clear();
        timeMult = 0;
        runOnce = false;
    }
}
