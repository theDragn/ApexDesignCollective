package data.weapons.proj;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.loading.DamagingExplosionSpec;
import com.fs.starfarer.api.util.Misc;
import data.scripts.util.MagicRender;
import org.lazywizard.lazylib.CollisionUtils;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.List;

public class ApexThundercloudMinePlugin implements EveryFrameCombatPlugin
{
    public static final float EXP_RANGE = 400f;

    private MissileAPI mine;
    private CombatEngineAPI engine;

    public ApexThundercloudMinePlugin(MissileAPI mine)
    {
        this.mine = mine;
        mine.setUntilMineExplosion(1.5f);
    }

    @Override
    public void processInputPreCoreControls(float amount, List<InputEventAPI> events)
    {

    }

    @Override
    public void advance(float amount, List<InputEventAPI> events)
    {
        if ((!engine.isInPlay(mine) && mine.getHitpoints() > 0) || mine.didDamage())
        {
            explode();
            engine.removePlugin(this);
        }
        if (mine == null || !Global.getCombatEngine().isInPlay(mine))
        {
            engine.removePlugin(this);
            return;
        }
    }

    private void explode()
    {
        engine.addSwirlyNebulaParticle(
                mine.getLocation(),
                Misc.ZERO,
                75f,
                1.5f,
                0.1f,
                0.2f,
                1f,
                new Color(90, 128, 248, 120),
                false
        );
        MagicRender.battlespace(
                Global.getSettings().getSprite("fx", "apex_shockwave"),
                mine.getLocation(),
                new Vector2f(0.0f, 0.0f),
                new Vector2f(25f, 25f),
                new Vector2f(2500f, 2500.0f),
                Misc.random.nextFloat() * 360f,
                0.0f,
                new Color(90, 128, 248, 120),
                true,
                0.0f,
                0.1f,
                0.55f);
        /*
        float damage = mine.getMirvWarheadDamage();
        for (CombatEntityAPI target : engine.getShips())
        {
            // skip sqrt if possible
            if (MathUtils.getDistanceSquared(target.getLocation(), mine.getLocation()) > 4000000)
                continue;
            float range = MathUtils.getDistance(target, mine);
            if (range > EXP_RANGE)
                continue;
            int numHits = (int)range % 100;
            float hitDamage = damage / (float)numHits;
            Vector2f hitLoc = CollisionUtils.getCollisionPoint(mine.getLocation(), target.getLocation(), target);
            for (int i = 0; i < numHits; i++)
            {
                engine.applyDamage(target, hitLoc, hitDamage, mine.getDamageType(), mine.getEmpAmount(), false, false, mine.getSource());
            }
        }*/
    }

    @Override
    public void renderInWorldCoords(ViewportAPI viewport) {}

    @Override
    public void renderInUICoords(ViewportAPI viewport) {}

    @Override
    public void init(CombatEngineAPI engine)
    {
        this.engine = engine;
        //mine.setDamageAmount(0);
    }
}
