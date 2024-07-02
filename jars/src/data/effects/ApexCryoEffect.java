package data.effects;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.Misc;
import data.hullmods.ApexCryoSystemHullmod;
import org.lazywizard.lazylib.CollisionUtils;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;
import utils.ApexUtils;

import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static utils.ApexUtils.text;

public class ApexCryoEffect extends BaseEveryFrameCombatPlugin
{
    public static final float POOL_FRACTION = 0.1f;
    public static final float MIN_EFFECT = 100f;
    public static final float SOFTCAP = 2000f;
    public static final Color PARTICLE_COLOR = new Color(0, 187, 255,200);
    private HullSize sourceSize;
    private boolean removeMapEntry;
    private float pool = 0f;
    private ShipAPI target;

    public static final Map<ShipAPI, ApexCryoEffect> effectMap = new HashMap<>();

    public ApexCryoEffect() {}

    public ApexCryoEffect(ShipAPI target, HullSize sourceSize)
    {
        this.removeMapEntry = true;
        this.target = target;
        this.sourceSize = sourceSize;
        this.pool = ApexCryoSystemHullmod.dissMap.get(sourceSize);
        // check to see if ship already has a regen effect going
        if (effectMap.containsKey(target))
        {
            // call the existing effect's combine function (this effect gets deleted, the original one stays with new numbers)
            effectMap.get(target).combineEffects(this);
            // don't remove the map entry, since it points to the existing effect plugin (remember, a new value for an existing key overwrites it)
            removeMapEntry = false;
            this.pool = 0f;
            // can't remove the plugin in the constructor, so we'll do it when the next frame hits
        } else
        {
            effectMap.put(target, this);
        }
    }

    public void combineEffects(ApexCryoEffect newEffect)
    {
        pool = diminish(newEffect.pool, this.pool);
    }

    @Override
    public void advance(float amount, List<InputEventAPI> events)
    {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine.isPaused()) return;
        if (!target.isAlive() || pool <= 0)
        {
            engine.removePlugin(this);
            // this might cause it to drop the buff for one frame when effects combine but it shouldn't be noticeable
            unmodify(target);
            if (removeMapEntry)
                effectMap.remove(target);
            return;
        }

        float effect = Math.max(POOL_FRACTION * pool, MIN_EFFECT);
        pool -= effect * amount * target.getMutableStats().getTimeMult().getMult();
        target.getMutableStats().getFluxDissipation().modifyFlat("apexCryo", effect);

        if (engine.getPlayerShip() == target && pool > 0f)
        {
            engine.maintainStatusForPlayerShip("apex_cryo", "graphics/icons/buffs/apex_cryo.png", "+" + (int)(effect) + " " + text("cryo1") , text("cryo2") + ": " + Misc.getRoundedValue(pool), false);
        }

        // pick a random spot somewhere inside the ship's collision bounds
        // similar frequency to the repair vfx, which peaks at about 30 sprites for a normal-looking patch of damaged armor
        // this is 0.2 * 60 * 1 = 12 sprites at any given time
        if (Misc.random.nextFloat() < 0.2f)
        {
            // pick random point and make sure it's inside ship bounds
            Vector2f point = MathUtils.getRandomPointInCircle(target.getLocation(), target.getCollisionRadius());
            if (!CollisionUtils.isPointWithinBounds(point, target)) return;
            float size = target.getCollisionRadius() * 0.05f;
            engine.addSwirlyNebulaParticle(point, target.getVelocity(), ApexUtils.randBetween(size, size*2.5f), 2.5f, 0f, 0.5f, 1f,
                    PARTICLE_COLOR,
                    false);
        }
    }

    private void unmodify(ShipAPI target)
    {
        target.getMutableStats().getFluxDissipation().unmodify("apexCryo");
    }

    /**
     * square-root soft cap for dissipation pool
     * @param addedAmount dissipation to add to pool
     * @param currentPool current pool total
     * @return new pool amount
     */
    private float diminish(float addedAmount, float currentPool)
    {
        // this is linear scaling until soft cap
        // after cap, it's sqrt(2000x)
        if (currentPool + addedAmount < SOFTCAP) return currentPool + addedAmount;
        if (currentPool < SOFTCAP) // gotta get the portion to add linearly
        {
            // add linearly up to softcap
            // linear increase and our curve intersect at soft cap point at x=400, y=400
            addedAmount -= (SOFTCAP - currentPool);
        }
        // we do the inverse of the curve function to figure out the x-pos of the current total, then add the remaining to it
        float x = currentPool * currentPool * 0.0005f + addedAmount;
        // then we run it back through the curve to get the diminished amount
        return (float)(44.72135955 * Math.sqrt(x));
    }
}
