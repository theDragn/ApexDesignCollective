package data.effects;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import data.hullmods.ApexArmor;
import data.hullmods.ApexArmorRepairHullmod;
import data.hullmods.ApexCryoArmor;
import org.lazywizard.lazylib.CollisionUtils;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;
import org.magiclib.util.MagicRender;
import plugins.ApexModPlugin;

import java.awt.*;
import java.util.*;
import java.util.List;

import static utils.ApexUtils.text;

// this creates the glow and armor regen effects
public class ApexRegenEffect extends BaseEveryFrameCombatPlugin
{
    public static final float SOFTCAP = 400f;
    public static final float MIN_RATE = 20f;
    public static final float POOL_FRACTION = 0.1f;

    // magic number :3
    public static final float sqrt3 = 1.732051f;
    public static final Color[] ARMOR_COLORS = {
            new Color(224, 255, 0),
            new Color(100, 255, 0),
            new Color(255, 251, 0),
            new Color(255, 205, 0)
    };

    public ApexRegenEffect()
    {
    }
    private ShipAPI target;
    private ShipAPI source;
    private float pool = 0f;
    private float regenMult = 1f;
    private boolean removeMapEntry = true;

    public static final Map<ShipAPI, ApexRegenEffect> effectMap = new HashMap<>();

    public ApexRegenEffect(ShipAPI target, DamagingProjectileAPI proj)
    {
        removeMapEntry = true; // this just makes this plugin get deleted from the list of effects when it finishes
        this.target = target;
        this.source = proj.getSource();
        this.pool = ApexArmorRepairHullmod.regenMap.get(source.getHullSize());
        if (target.getVariant().hasHullMod("apex_armor"))
            this.regenMult *= ApexArmor.REGEN_MULT;
        if (target.getVariant().hasHullMod("apex_cryo_armor"))
            this.regenMult *= ApexCryoArmor.REGEN_MULT;

        // check to see if ship already has a regen effect going
        if (effectMap.containsKey(target))
        {
            // call the existing effect's combine function (this effect gets deleted, the original one stays with new numbers)
            effectMap.get(target).combineEffects(this);
            // don't remove the map entry, since it goes to the existing effect plugin
            removeMapEntry = false;
            // can't remove the plugin in the constructor, so we'll make our own cleanup code do it by zeroing out this plugin
            pool = 0;
        } else
        {
            effectMap.put(target, this);
        }
    }

    public void combineEffects(ApexRegenEffect newEffect)
    {
        // stacking effects gives you (1/1.5n) of the weakest effect plus 100% of the strongest effect, and resets the duration
        pool = diminish(newEffect.pool, pool);
    }

    @Override
    public void advance(float amount, List<InputEventAPI> events)
    {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine.isPaused()) return;
        if (!target.isAlive() || pool <= 0f)
        {
            engine.removePlugin(this);
            if (removeMapEntry)
                effectMap.remove(target);
            return;
        }

        doRegen(amount);

        if (engine.getPlayerShip() == target && pool > 0f)
        {
            engine.maintainStatusForPlayerShip("apex_regen", "graphics/icons/buffs/apex_regen.png", text("regen1"), text("regen2") + ": " + (int) pool, false);
        }
    }

    protected void doRegen(float amount)
    {
        //System.out.println("did effect tick");
        CombatEngineAPI engine = Global.getCombatEngine();

        if (pool <= 0)
            return;

        float repairThisFrame = amount * Math.max(POOL_FRACTION * pool, MIN_RATE);
        repairThisFrame *= regenMult;
        repairThisFrame *= target.getMutableStats().getTimeMult().getMult();
        pool -= repairThisFrame; // pool always decreases regardless of whether armor is missing or not

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
        float repairPerCell = repairThisFrame / (float)numCellsToRepair;
        float repairDoneThisTick = 0f;
        for (int x = 0; x < gridWidth; x++)
        {
            for (int y = 0; y < gridHeight; y++)
            {
                if (grid.getArmorValue(x, y) < maxArmorInCell)
                {
                    repairDoneThisTick += Math.min(repairPerCell, maxArmorInCell - grid.getArmorValue(x, y));
                    grid.setArmorValue(x, y, Math.min(grid.getArmorValue(x, y) + repairPerCell, maxArmorInCell));
                    doVisual(target, x, y);
                }
            }
        }

        if (repairDoneThisTick > 0 && ApexModPlugin.REPAIR_FLOATY && Misc.shouldShowDamageFloaty(target, target))
        {

            engine.addFloatingDamageText(target.getLocation(), repairDoneThisTick, Color.GREEN, target, target);
            target.syncWithArmorGridState();
        }
    }

    // it would probably be a lot more efficient to run this as some sort of shader
    // but this isn't awfully bad and it looks good
    // on average, this results in 0.026 * 60 * 2 = 3.12 sprites per armor grid square at any one point (ignoring startup time)
    // realistically, it is probably about half this, because half the armor cells aren't within ship bounds
    // so about 1.6 sprites per damaged armor grid square... or about 24 for a normally-damaged patch of armor.
    void doVisual(ShipAPI ship, int x, int y) {
        if (ApexModPlugin.POTATO_MODE) return;

        // do screenspace check first, nobody will notice that the vfx only start to appear once a ship is on the screen
        if (!MagicRender.screenCheck(500f, ship.getLocation())) return;

        float[][] grid = ship.getArmorGrid().getGrid();
        float cellsize = ship.getArmorGrid().getCellSize();
        //big glowy grid
        if (Misc.random.nextFloat() < 0.01f) {

            // these next couple lines are mostly taken from xaiier's implementation of a similar effect for Xhan's Gramada
            // no sense in reinventing the wheel
            // flip axes the right way around
            Vector2f offset = new Vector2f(y, -x);
            // scale to armor grid size
            offset.scale(cellsize);
            // armor grid 0,0 is bottom left corner; this shifts 0,0 to the center of the ship
            Vector2f.add(offset, new Vector2f(-grid[x].length / 2f * cellsize, grid.length / 2f * cellsize), offset);
            // get random point in the grid cell so that the triangle grid is evenly covered
            offset.x += Misc.random.nextFloat() * cellsize;
            offset.y += Misc.random.nextFloat() * cellsize;
            // take resulting point and snap it to the triangle grid
            Vector3f snapped = snapToTriGrid(offset, cellsize * 0.5f);
            offset.x = snapped.x;
            offset.y = snapped.y;

            // convert offset to world coords for bounds check
            Vector2f test = new Vector2f(offset);
            VectorUtils.rotate(test, ship.getFacing(), test);
            Vector2f.add(test, ship.getLocation(), test);

            if (CollisionUtils.isPointWithinBounds(test, ship)) {
                // finally we actually render the fucker
                MagicRender.objectspace(
                        Global.getSettings().getSprite("fx", "apex_tri_big"),
                        ship,
                        offset,
                        new Vector2f(), // vel
                        new Vector2f(cellsize, cellsize),
                        new Vector2f(), // growth
                        Misc.random.nextInt(3) * 120f + snapped.z + 90f - 60f,
                        0f,
                        true,
                        ARMOR_COLORS[Misc.random.nextInt(4)],
                        true,
                        0f,
                        0f,
                        0.6f,
                        1.0f,
                        0.048f,
                        0.1f,
                        2f,
                        0.1f,
                        true,
                        CombatEngineLayers.ABOVE_SHIPS_LAYER
                );
            }
        }
        if (Misc.random.nextFloat() < 0.016f && cellsize > 21f) {
            // same thing, but for the small triangles
            // we don't bother with this unless the grid size is already fairly large since it won't be visible

            // this vector math is mostly taken from xaiier because there is no reason to rediscover the jank here
            // flip axes the right way around
            Vector2f offset = new Vector2f(y, -x);
            // scale to armor grid size
            offset.scale(cellsize);
            // armor grid 0,0 is bottom left corner; this shifts 0,0 to the center of the ship
            Vector2f.add(offset, new Vector2f(-grid[x].length / 2f * cellsize, grid.length / 2f * cellsize), offset);
            // get random point in the grid cell so that the triangle grid is evenly covered
            offset.x += Misc.random.nextFloat() * cellsize;
            offset.y += Misc.random.nextFloat() * cellsize;
            // take resulting point and snap it to the triangle grid
            Vector3f snapped = snapToTriGrid(offset, cellsize * 0.25f);
            offset.x = snapped.x;
            offset.y = snapped.y;

            // convert offset to world coords for bounds check
            Vector2f test = new Vector2f(offset);
            VectorUtils.rotate(test, ship.getFacing(), test);
            Vector2f.add(test, ship.getLocation(), test);

            if (CollisionUtils.isPointWithinBounds(test, ship)) {
                // finally we actually render the fucker
                MagicRender.objectspace(
                        Global.getSettings().getSprite("fx", "apex_tri_big"),
                        ship,
                        offset,
                        new Vector2f(), // vel
                        new Vector2f(cellsize*0.5f, cellsize*0.5f),
                        new Vector2f(), // growth
                        Misc.random.nextInt(3) * 120f + snapped.z + 90f - 60f,
                        0f,
                        true,
                        ARMOR_COLORS[Misc.random.nextInt(4)],
                        true,
                        0f,
                        0f,
                        0.6f,
                        1.0f,
                        0.048f,
                        0.1f,
                        2f,
                        0.1f,
                        true,
                        CombatEngineLayers.ABOVE_SHIPS_LAYER
                );
            }
        }
    }

    /**
     * asymptotic soft cap for repair pool
     * @param addedAmount repair to add to pool
     * @param currentPool current pool total
     * @return new pool amount
     */
    float diminish(float addedAmount, float currentPool)
    {
        // this is basically linear scaling until soft cap
        // after cap, it's 800x/(400+x)
        // needs 400 actual repair to get 400 pool
        // needs 650 actual repair to get 500 pool
        // needs 1200 actual repair to get 600 pool
        // needs 2800 actual repair to get 700 pool
        if (currentPool + addedAmount < SOFTCAP) return currentPool + addedAmount;
        if (currentPool < SOFTCAP) // gotta get the portion to add linearly
        {
            // add linearly up to softcap
            // linear increase and our curve intersect at soft cap point at x=400, y=400
            addedAmount -= (SOFTCAP - currentPool);
        }
        // we do the inverse of the curve function to figure out the x-pos of the current total, then add the remaining to it
        float x = -400f*currentPool/(currentPool - 800f) + addedAmount;
        // then we run it back through the curve to get the diminished amount
        return 800f*x/(400f+x);
    }

    /**
     * Snaps input cartesian coordinates to a vertex in an equilateral triangle grid with the given side length
     * Assumes that the +y direction is up on the grid, the +x direction is right, and 0,0 is the origin.
     * @param vec input vector in cartesian coordinates
     * @param side side length of a triangle
     * @return Cartesian coordinates for the center of the triangle that vec is in. z is rotation angle for the triangle, if it should be inverted
     */
    Vector3f snapToTriGrid(Vector2f vec, float side)
    {
        // I do NOT want to talk about how long this goddamn method took me to write
        // we use a triplet of coordinates to identify each triangle

        int a = (int)Math.ceil((vec.x - sqrt3 / 3f * vec.y) / side);
        int b = (int)Math.floor((sqrt3 * 2f / 3f * vec.y) / side) + 1;
        int c = (int)Math.ceil((-1f * vec.x - sqrt3 / 3f * vec.y) / side);

        float z = 0f;
        // calculate number of odd coordinates
        // triangles with 2 or 0 odd coords point up, everything else points down
        int parity = (a&0x1) + (b&0x1) + (c&0x1);
        if (parity == 1 || parity == 3) z = 180f;

        // tri tells us what triangle a point is in
        // now, we compute the center of that triangle in cartesian coords
        // our z value is just the angle to rotate the fucker if it should be upside down
        return new Vector3f(
                (0.5f * a + -0.5f * c) * side,
                (-sqrt3 / 6f * a + sqrt3 / 3f * b - sqrt3 / 6f * c) * side,
                z
        );
    }
}
