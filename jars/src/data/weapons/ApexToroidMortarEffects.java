package data.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.loading.DamagingExplosionSpec;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import data.ApexUtils;
import data.scripts.util.MagicRender;
import org.lazywizard.lazylib.CollisionUtils;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static plugins.ApexModPlugin.POTATO_MODE;

public class ApexToroidMortarEffects implements OnFireEffectPlugin, OnHitEffectPlugin, EveryFrameWeaponEffectPlugin
{
    public static final float MIN_ARC_DAMAGE = 0.1f;
    public static final float MAX_ARC_DAMAGE = 0.2f;
    public static final float ARC_RANGE = 200f;
    public static final float ARC_MIN_INTERVAL = 0.33f;
    public static final float ARC_MAX_INTERVAL = 0.5f;
    public static final float EXP_RANGE = 100f;

    private HashMap<DamagingProjectileAPI, IntervalUtil> projMap = new HashMap<>();
    // this is the hit glow sprite. don't ask.
    private SpriteAPI glowSprite = Global.getSettings().getSprite("campaignEntities", "fusion_lamp_glow");
    private static final Vector2f spriteSize = new Vector2f(150f, 150f);

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon)
    {
        List<DamagingProjectileAPI> toRemove = new ArrayList<>();

        for (DamagingProjectileAPI proj : projMap.keySet())
        {
            projMap.get(proj).advance(amount);
            glowSprite.setAlphaMult(proj.getBrightness() * 0.33f);
            Vector2f adjustedPos = VectorUtils.rotate(new Vector2f(18f, 0f), proj.getFacing());
            Vector2f.add(adjustedPos, proj.getLocation(), adjustedPos);
            // rendering a glow sprite here, because the large projectile sprite makes it weirdly offset if I use a normal one
            MagicRender.singleframe(
                    glowSprite,
                    adjustedPos,
                    spriteSize,
                    0,
                    proj.getProjectileSpec().getFringeColor(),
                    true
            );
            // arc if the arc timer is up
            if (projMap.get(proj).intervalElapsed())
                arc(proj, engine);
            if (proj.isFading() || proj.isExpired() || !engine.isInPlay(proj))
            {
                toRemove.add(proj);
                if (!proj.didDamage())
                    explode(proj, proj.getLocation(), engine, null);
                engine.removeEntity(proj);
            }
        }
        for (DamagingProjectileAPI proj : toRemove)
            projMap.remove(proj);
    }

    @Override
    public void onFire(DamagingProjectileAPI projectile, WeaponAPI weapon, CombatEngineAPI engine)
    {
        projMap.put(projectile, new IntervalUtil(ARC_MIN_INTERVAL, ARC_MAX_INTERVAL));
    }

    @Override
    public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target, Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine)
    {
        if (target instanceof MissileAPI)
            return;
        explode(projectile, point, engine, target);
    }

    private static void explode(DamagingProjectileAPI projectile, Vector2f point, CombatEngineAPI engine, CombatEntityAPI target)
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

        // damaging targets
        for (MissileAPI missile : engine.getMissiles())
        {
            if (missile.getOwner() == projectile.getOwner())
                continue;
            if (MathUtils.getDistanceSquared(projectile.getLocation(), missile.getLocation()) < EXP_RANGE * EXP_RANGE)
                engine.applyDamage(
                        missile,
                        missile.getLocation(),
                        projectile.getDamageAmount(),
                        projectile.getDamageType(),
                        projectile.getEmpAmount(),
                        true,
                        false,
                        projectile.getSource(),
                        false
                );
        }
        for (ShipAPI ship : CombatUtils.getShipsWithinRange(projectile.getLocation(), EXP_RANGE * 3))
        {
            if (ship.getOwner() == projectile.getOwner() || target == ship)
                continue;
            Vector2f collisionPoint = CollisionUtils.getCollisionPoint(projectile.getLocation(), ship.getLocation(), ship);
            if (collisionPoint != null && MathUtils.getDistanceSquared(projectile.getLocation(), collisionPoint) < EXP_RANGE * EXP_RANGE)
                engine.applyDamage(
                        ship,
                        collisionPoint,
                        projectile.getDamageAmount(),
                        projectile.getDamageType(),
                        projectile.getEmpAmount(),
                        false,
                        false,
                        projectile.getSource(),
                        false
                );
        }
    }

    private static void arc(DamagingProjectileAPI projectile, CombatEngineAPI engine)
    {
        // damaging arc targeting logic
        WeightedRandomPicker<CombatEntityAPI> targets = new WeightedRandomPicker<>();
        for (CombatEntityAPI possibleTarget : CombatUtils.getEntitiesWithinRange(projectile.getLocation(), ARC_RANGE))
        {
            if (possibleTarget.getOwner() == projectile.getOwner())
                continue;
            if (possibleTarget instanceof ShipAPI && ((ShipAPI) possibleTarget).isPhased())
                continue;
            if (possibleTarget instanceof MissileAPI)
                targets.add(possibleTarget, 0.5f);
            if (possibleTarget instanceof ShipAPI)
                targets.add(possibleTarget, 1f);
        }


        if (targets.isEmpty())
            return;

        float damageMult = Misc.random.nextFloat() * (MAX_ARC_DAMAGE - MIN_ARC_DAMAGE) + MIN_ARC_DAMAGE;
        CombatEntityAPI arcTarget = targets.pick();
        Vector2f adjustedPos = VectorUtils.rotate(new Vector2f(18f, 0f), projectile.getFacing());
        Vector2f.add(adjustedPos, projectile.getLocation(), adjustedPos);
        engine.spawnEmpArc(
                projectile.getSource(),
                adjustedPos,
                projectile,
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
