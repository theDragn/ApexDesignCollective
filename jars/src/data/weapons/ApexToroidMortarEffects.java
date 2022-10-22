package data.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import data.ApexUtils;
import data.scripts.util.MagicRender;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static plugins.ApexModPlugin.POTATO_MODE;

public class ApexToroidMortarEffects implements OnFireEffectPlugin, OnHitEffectPlugin, EveryFrameWeaponEffectPlugin
{
    public static final int MIN_ARCS = 2;
    public static final int MAX_ARCS = 4;
    public static final float MIN_ARC_DAMAGE = 0.1f;
    public static final float MAX_ARC_DAMAGE = 0.2f;
    public static final float ARC_RANGE = 200f;

    private List<DamagingProjectileAPI> projs = new ArrayList<>();
    // this is the hit glow sprite. don't ask.
    private SpriteAPI glowSprite = Global.getSettings().getSprite("campaignEntities", "fusion_lamp_glow");
    private static final Vector2f spriteSize = new Vector2f(150f, 150f);

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon)
    {
        List<DamagingProjectileAPI> toRemove = new ArrayList<>();

        for (DamagingProjectileAPI proj : projs)
        {
            glowSprite.setAlphaMult(proj.getBrightness() * 0.33f);
            Vector2f adjustedPos = VectorUtils.rotate(new Vector2f(18f, 0f), proj.getFacing());
            Vector2f.add(adjustedPos, proj.getLocation(), adjustedPos);
            MagicRender.singleframe(
                    glowSprite,
                    adjustedPos,
                    spriteSize,
                    0,
                    proj.getProjectileSpec().getFringeColor(),
                    true
            );
            if (proj.isFading() || proj.isExpired() || !engine.isInPlay(proj))
            {
                toRemove.add(proj);
                if (!proj.didDamage())
                    explode(proj, proj.getLocation(), engine);
                engine.removeEntity(proj);
            }
        }
        for (DamagingProjectileAPI proj : toRemove)
            projs.remove(proj);
    }

    @Override
    public void onFire(DamagingProjectileAPI projectile, WeaponAPI weapon, CombatEngineAPI engine)
    {
        projs.add(projectile);
    }

    @Override
    public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target, Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine)
    {
        if (target instanceof MissileAPI)
            return;
        explode(projectile, point, engine);
    }

    private static void explode(DamagingProjectileAPI projectile, Vector2f point, CombatEngineAPI engine)
    {
        // decorative arcs
        ApexUtils.plasmaEffects(projectile, projectile.getProjectileSpec().getCoreColor(), 100f);
        // shockwave
        if (!POTATO_MODE)
        {
            ApexUtils.addWaveDistortion(projectile.getLocation(), 30f, 100f, 0.25f);
            engine.spawnExplosion(
                    projectile.getLocation(),
                    Misc.ZERO,
                    projectile.getProjectileSpec().getFringeColor(),
                    150,
                    1.0f
            );
        }
        Global.getSoundPlayer().playSound("hit_heavy", 1f, 1f, point, Misc.ZERO);

        // damaging arc targeting logic
        WeightedRandomPicker<CombatEntityAPI> targets = new WeightedRandomPicker<>();
        for (CombatEntityAPI possibleTarget : CombatUtils.getEntitiesWithinRange(projectile.getLocation(), ARC_RANGE))
        {
            if (possibleTarget instanceof MissileAPI)
                targets.add(possibleTarget, 0.5f);
            if (possibleTarget instanceof ShipAPI)
                targets.add(possibleTarget, 1f);
        }

        int numArcs = MIN_ARCS + Misc.random.nextInt(MAX_ARCS - MIN_ARCS + 1);
        if (targets.isEmpty())
            return;
        for (int i = 0; i < numArcs; i++)
        {
            float damageMult = Misc.random.nextFloat() * (MAX_ARC_DAMAGE - MIN_ARC_DAMAGE) + MIN_ARC_DAMAGE;
            CombatEntityAPI arcTarget = targets.pick();
            engine.spawnEmpArc(
                    projectile.getSource(),
                    projectile.getLocation(),
                    null,
                    arcTarget,
                    projectile.getDamageType(),
                    projectile.getDamageAmount() * damageMult,
                    projectile.getEmpAmount() * damageMult,
                    9999f,
                    "tachyon_lance_emp_arc_impact",
                    10f,
                    projectile.getProjectileSpec().getFringeColor(),
                    Color.WHITE
            );
        }
    }
}
