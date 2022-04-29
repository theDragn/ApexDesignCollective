package data.weapons.proj;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.loading.DamagingExplosionSpec;
import com.fs.starfarer.api.util.Misc;
import data.scripts.util.MagicRender;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public class ApexTachyonShredderOnHit implements OnHitEffectPlugin
{
    private static final float BASE_EXP_RADIUS = 100f;
    private static final float RADIUS_BONUS_MULT = 0.75f; // 175 radius at maximum flux
    private static final float FRAG_FRACTION = 0.5f; // proj damage * this = frag exp damage

    // onhit graphical effect
    private static final Color CORE_EXPLOSION_COLOR = new Color(96,41,246, 255);
    private static final Color OUTER_EXPLOSION_COLOR = new Color(85, 0, 255, 25);
    private static final Color GLOW_COLOR = new Color(109,41,246, 150);
    private static final float CORE_EXP_RADIUS = 100f;
    private static final float CORE_EXP_DUR = 1f;
    private static final float OUTER_EXP_RADIUS = 150f;
    private static final float OUTER_EXP_DUR = 0.2f;
    private static final float GLOW_RADIUS = 200f;
    private static final float GLOW_DUR = 0.2f;
    private static final float VEL_MULT = 4f;

    @Override
    public void onHit(DamagingProjectileAPI proj, CombatEntityAPI target, Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine)
    {
        if (!(target instanceof MissileAPI))
        {
            // damaging explosion
            float radius = BASE_EXP_RADIUS + RADIUS_BONUS_MULT * proj.getSource().getFluxLevel();
            DamagingExplosionSpec spec = new DamagingExplosionSpec(0.1f,
                    radius,
                    radius * 0.75f,
                    proj.getDamageAmount() * FRAG_FRACTION,
                    proj.getDamageAmount() * FRAG_FRACTION * 0.75f,
                    CollisionClass.PROJECTILE_NO_FF,
                    CollisionClass.PROJECTILE_FIGHTER,
                    1f,
                    10f,
                    0.2f,
                    20,
                    Color.WHITE,
                    null);
            spec.setDamageType(DamageType.FRAGMENTATION);
            spec.setShowGraphic(false);
            engine.spawnDamagingExplosion(spec, proj.getSource(), point, false);

            // graphics
            // blatantly inspired by (and totally not stolen from) the scalartech ruffle
            engine.spawnExplosion(point, Misc.ZERO, CORE_EXPLOSION_COLOR, CORE_EXP_RADIUS, CORE_EXP_DUR / VEL_MULT);
            engine.spawnExplosion(point, Misc.ZERO, OUTER_EXPLOSION_COLOR, OUTER_EXP_RADIUS, OUTER_EXP_DUR / VEL_MULT);
            engine.addHitParticle(point, Misc.ZERO, GLOW_RADIUS, 1f, GLOW_DUR / VEL_MULT, GLOW_COLOR);

            MagicRender.battlespace(
                    Global.getSettings().getSprite("graphics/fx/explosion_ring0.png"),
                    point,
                    Misc.ZERO,
                    new Vector2f(80, 80),
                    new Vector2f(240 * VEL_MULT, 240 * VEL_MULT),
                    MathUtils.getRandomNumberInRange(0, 360),
                    0,
                    GLOW_COLOR,
                    true,
                    0.125f / VEL_MULT,
                    0.0f,
                    0.125f / VEL_MULT
            );
            MagicRender.battlespace(
                    Global.getSettings().getSprite("graphics/fx/explosion_ring0.png"),
                    point,
                    Misc.ZERO,
                    new Vector2f(120, 120),
                    new Vector2f(100 * VEL_MULT, 100 * VEL_MULT),
                    MathUtils.getRandomNumberInRange(0, 360),
                    0,
                    CORE_EXPLOSION_COLOR,
                    true,
                    0.2f / VEL_MULT,
                    0.0f,
                    0.2f / VEL_MULT
            );
        }
    }
}
