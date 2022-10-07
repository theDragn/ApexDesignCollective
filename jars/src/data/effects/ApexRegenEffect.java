package data.effects;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.FaderUtil;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import data.hullmods.ApexArmor;
import data.hullmods.ApexArmorRepairHullmod;
import data.hullmods.ApexCryoArmor;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.opengl.GL14;
import org.lwjgl.util.vector.Vector2f;
import plugins.ApexModPlugin;

import java.awt.*;
import java.util.*;
import java.util.List;

// this creates the glow and armor regen effects
public class ApexRegenEffect extends BaseEveryFrameCombatPlugin
{
    public ApexRegenEffect()
    {
    }
    private ShipAPI target;
    private ShipAPI source;
    protected IntervalUtil interval;
    private int maxTicks = 0;
    private int numCombinations = 0;
    private float regenAmount = 0f;
    private int ticks = 0;
    private boolean removeMapEntry = true;

    public static final Map<ShipAPI, ApexRegenEffect> effectMap = new HashMap<>();

    public ApexRegenEffect(ShipAPI target, DamagingProjectileAPI proj)
    {
        //System.out.println("Applied regen to " + target.getName() + " from " + proj.getSource().getName());
        removeMapEntry = true;
        this.target = target;
        this.source = proj.getSource();
        this.regenAmount = ApexArmorRepairHullmod.regenMap.get(source.getHullSize());
        this.maxTicks = (int) (ApexArmorRepairHullmod.BASE_REGEN_DURATION * 2f);
        if (target.getVariant().hasHullMod("apex_armor"))
            this.regenAmount *= ApexArmor.REGEN_MULT;
        if (target.getVariant().hasHullMod("apex_cryo_armor"))
            this.maxTicks *= ApexCryoArmor.REPAIR_TIME_MULT;
        interval = new IntervalUtil(0.45f, 0.55f);
        interval.forceIntervalElapsed();

        // check to see if ship already has a regen effect going
        if (effectMap.containsKey(target))
        {
            // call the existing effect's combine function (this effect gets deleted, the original one stays with new numbers)
            effectMap.get(target).combineEffects(this);
            // don't remove the map entry, since it goes to the existing effect plugin
            removeMapEntry = false;
            // can't remove the plugin in the constructor, so we'll do it when the next frame hits
            ticks = maxTicks + 1;
        } else
        {
            effectMap.put(target, this);
        }
    }

    public void combineEffects(ApexRegenEffect newEffect)
    {
        numCombinations++;
        // stacking effects gives you (1/1.5n) of the weakest effect plus 100% of the strongest effect, and resets the duration
        regenAmount = Math.max(newEffect.regenAmount/(1.5f*numCombinations) + regenAmount, regenAmount/(1.5f*numCombinations) + newEffect.regenAmount);
        ticks = 0;
    }

    @Override
    public void advance(float amount, List<InputEventAPI> events)
    {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine.isPaused()) return;
        if (!target.isAlive() || ticks > maxTicks || regenAmount <= 1f)
        {
            engine.removePlugin(this);
            if (removeMapEntry)
                effectMap.remove(target);
            return;
        }

        interval.advance(amount);
        if (interval.intervalElapsed() && ticks < maxTicks)
        {
            doRegen();
            ticks++;
        }
        if (engine.getPlayerShip() == target && regenAmount > 0f && ticks < maxTicks)
        {
            engine.maintainStatusForPlayerShip("apex_regen", "graphics/icons/buffs/apex_regen.png", "Applying Remote Armor Patch", "Remaining repair strength: " + (int) regenAmount, false);
        }
    }

    protected void doRegen()
    {
        //System.out.println("did effect tick");
        CombatEngineAPI engine = Global.getCombatEngine();

        if (regenAmount <= 0)
            return;

        float repairThisTick = regenAmount / (float) (maxTicks - ticks);

        ArmorGridAPI grid = target.getArmorGrid();
        if (grid == null) return;
        int gridWidth = grid.getGrid().length;
        int gridHeight = grid.getGrid()[0].length;
        float maxArmorInCell = grid.getMaxArmorInCell() * ApexArmorRepairHullmod.MAX_REGEN_FRACTION;

        // first, get number of cells missing armor
        int numCellsToRepair = 0;
        for (int x = 0; x < gridWidth; x++)
        {
            for (int y = 0; y < gridHeight; y++)
            {
                if (grid.getArmorValue(x, y) < maxArmorInCell)
                    numCellsToRepair++;
            }
        }
        if (numCellsToRepair == 0)
            return;

        // then, repair the cells

        float repairPerCell = repairThisTick / numCellsToRepair;
        float repairDoneThisTick = 0f;
        for (int x = 0; x < gridWidth; x++)
        {
            for (int y = 0; y < gridHeight; y++)
            {
                if (grid.getArmorValue(x, y) < maxArmorInCell)
                {
                    repairDoneThisTick += Math.min(repairPerCell, maxArmorInCell - grid.getArmorValue(x, y));
                    grid.setArmorValue(x, y, Math.min(grid.getArmorValue(x, y) + repairPerCell, maxArmorInCell));
                    if (!ApexModPlugin.POTATO_MODE)
                    {
                        Global.getCombatEngine().addSmokeParticle(
                                Vector2f.add(grid.getLocation(x, y), MathUtils.getRandomPointInCircle(Misc.ZERO, 12f), new Vector2f()),
                                Vector2f.add(target.getVelocity(),MathUtils.getRandomPointInCircle(Misc.ZERO, 15f), new Vector2f()),
                                Misc.random.nextFloat() * 5f,
                                0.75f,
                                0.66f,
                                Color.GREEN
                        );
                    }
                }
            }
        }

        if (repairDoneThisTick > 0)
        {
            if (Misc.shouldShowDamageFloaty(target, target))
            {
                engine.addFloatingDamageText(target.getLocation(), repairDoneThisTick, Color.GREEN, target, target);
            }
            regenAmount -= repairDoneThisTick;
            target.syncWithArmorGridState();
        } else // no armor left to repair, so end the effect
        {
            ticks = maxTicks + 1;
        }
    }
}
