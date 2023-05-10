package data.weapons.proj;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.loading.DamagingExplosionSpec;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import org.magiclib.util.MagicRender;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class ApexArcMineScript extends BaseEveryFrameCombatPlugin
{
    // arc stats
    public static final int MAX_ARCS = 3;
    public static final float ARC_DAMAGE = 100f;
    public static final float ARC_EMP = 200f;
    public static final float ARC_RANGE = 300f;
    public static final float ARMING_TIME = 0.66f;
    public static final Color ARC_COLOR = new Color(128, 128, 255);


    private DamagingProjectileAPI mine;
    private CombatEngineAPI engine;
    private ShipAPI source;
    private float aliveTime;
    private int arcsDone;

    private IntervalUtil arcInterval = new IntervalUtil(0.5f, 0.75f);
    private IntervalUtil decorativeArcInterval = new IntervalUtil(0.2f, 0.33f);

    public ApexArcMineScript(ShipAPI source, DamagingProjectileAPI mine)
    {
        this.source = source;
        this.mine = mine;
        arcsDone = 0;
        aliveTime = 0;
        engine = Global.getCombatEngine();
        ((MissileAPI)mine).setNoMineFFConcerns(true);
    }

    @Override
    public void advance(float amount, List<InputEventAPI> events)
    {
        if (engine.isPaused())
            return;
        aliveTime += amount;
        if (mine == null || !Global.getCombatEngine().isInPlay(mine) || aliveTime > 10f)
        {
            engine.removePlugin(this);
            return;
        }
        mine.setCollisionClass(CollisionClass.MISSILE_NO_FF);
        doArcShit(amount);


    }

    private void doArcShit(float amount)
    {
        arcInterval.advance(amount);
        decorativeArcInterval.advance(amount);
        if (aliveTime < ARMING_TIME || arcsDone >= MAX_ARCS || aliveTime > 5f)
            return;
        if (arcInterval.intervalElapsed())
        {
            List<CombatEntityAPI> targets = getTargets();
            if (targets.isEmpty())
                return;
            CombatEntityAPI target = targets.get(MathUtils.getRandomNumberInRange(0, targets.size() - 1));
            engine.spawnEmpArc(source,
                    mine.getLocation(),
                    mine,
                    target,
                    DamageType.ENERGY,
                    ARC_DAMAGE,
                    ARC_EMP,
                    10000f,
                    "tachyon_lance_emp_arc_impact",
                    20f,
                    ARC_COLOR,
                    Color.WHITE);
            Global.getSoundPlayer().playSound("system_emp_emitter_impact", 1f, 1f, mine.getLocation(), Misc.ZERO);
            arcsDone++;
        }
        if (decorativeArcInterval.intervalElapsed())
        {
            engine.spawnEmpArcVisual(mine.getLocation(), mine, MathUtils.getRandomPointInCircle(mine.getLocation(), 33f), null,
                    10f, ARC_COLOR, Color.white);
        }
    }

    private List<CombatEntityAPI> getTargets()
    {
        List<CombatEntityAPI> targets = new ArrayList<>();
        for (CombatEntityAPI entity : CombatUtils.getEntitiesWithinRange(mine.getLocation(), source.getMutableStats().getSystemRangeBonus().computeEffective(ARC_RANGE)))
        {
            if ((entity instanceof MissileAPI || entity instanceof ShipAPI) && entity.getOwner() != source.getOwner())
            {
                // don't arc to wrecks or phased ships
                if (entity instanceof ShipAPI && (!((ShipAPI) entity).isAlive() || ((ShipAPI) entity).isPhased()))
                {
                    continue;
                }
                targets.add(entity);
            }
        }
        return targets;
    }

}
