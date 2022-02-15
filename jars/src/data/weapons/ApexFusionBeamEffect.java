package data.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.loading.DamagingExplosionSpec;
import com.fs.starfarer.api.util.IntervalUtil;
import data.ApexUtils;
import org.dark.shaders.light.LightAPI;
import org.dark.shaders.light.LightShader;
import org.lazywizard.lazylib.CollisionUtils;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.DefenseUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.HashMap;
import java.util.HashSet;

import static com.fs.starfarer.api.util.Misc.ZERO;
import static plugins.ApexModPlugin.POTATO_MODE;

public class ApexFusionBeamEffect implements BeamEffectPlugin
{
    private HashSet<CombatEntityAPI> hitTargets = new HashSet<>();

    public static final float BONUS_DAMAGE_FRACTION_SMALL = 0.05f;
    public static final float BONUS_DAMAGE_FRACTION_MEDIUM = 0.10f;
    public static final float BONUS_DAMAGE_FRACTION_LARGE = 0.20f;
    public static final float TIME_FOR_EXPLOSION = 0.5f;

    private IntervalUtil flashInterval = new IntervalUtil(0.1f, 0.2f);
    private float timeOnTarget = 0f;

    @Override
    public void advance(float amount, CombatEngineAPI engine, BeamAPI beam)
    {
        flashInterval.advance(engine.getElapsedInLastFrame());
        if (flashInterval.intervalElapsed() && beam.getDamageTarget() != null) {
            float size = beam.getWidth() * MathUtils.getRandomNumberInRange(2f, 2.2f);
            float dur = MathUtils.getRandomNumberInRange(0.2f,0.25f);
            engine.addHitParticle(beam.getTo(), beam.getSource().getVelocity(), size * 3f, 0.8f, dur, beam.getFringeColor());
        }

        CombatEntityAPI target = beam.getDamageTarget();
        if (beam.getBrightness() >= 1f && target instanceof ShipAPI && !(hitTargets.contains(target)))
        {
            ShipAPI ship = (ShipAPI) target;
            boolean hitShield = target.getShield() != null && target.getShield().isWithinArc(beam.getTo());
            if (!hitShield)
            {
                // collision check because the isWithinArc alone can sometimes return true if the beam is on the edge of the shield
                Vector2f actualTo = MathUtils.getPointOnCircumference(beam.getTo(), 30f, beam.getWeapon().getCurrAngle());
                if (CollisionUtils.getCollisionPoint(beam.getFrom(), actualTo, ship) == null)
                    return;

                timeOnTarget += amount;

                if (timeOnTarget > TIME_FOR_EXPLOSION)
                {
                    hitTargets.add(target);
                    Vector2f point = beam.getRayEndPrevFrame();
                    float bonusFraction;
                    switch (beam.getWeapon().getSize()) {
                        case LARGE:
                            bonusFraction = BONUS_DAMAGE_FRACTION_LARGE;
                            break;
                        case MEDIUM:
                            bonusFraction = BONUS_DAMAGE_FRACTION_MEDIUM;
                            break;
                        case SMALL:
                            bonusFraction = BONUS_DAMAGE_FRACTION_SMALL;
                            break;
                        default:
                            bonusFraction = 0f;
                    }
                    float damage = ship.getArmorGrid().getArmorRating() * bonusFraction * ship.getMutableStats().getBeamWeaponDamageMult().computeMultMod() * ship.getMutableStats().getEnergyWeaponDamageMult().computeMultMod();
                    //System.out.println(damage);
                    //engine.spawnExplosion(point, ZERO, Color.WHITE, damage * 0.66f, 1f);
                    engine.spawnExplosion(point, ZERO, Color.MAGENTA, damage * 0.66f, 1f);
                    float radius = Math.min(damage * 0.66f, 250);
                    DamagingExplosionSpec spec = new DamagingExplosionSpec(0.1f,
                            radius * 1.2f,
                            radius,
                            damage,
                            damage * 0.75f,
                            CollisionClass.PROJECTILE_FF,
                            CollisionClass.PROJECTILE_FIGHTER,
                            1f,
                            10f,
                            0.2f,
                            20,
                            Color.WHITE,
                            null);
                    spec.setDamageType(DamageType.HIGH_EXPLOSIVE);
                    spec.setShowGraphic(false);
                    engine.spawnDamagingExplosion(spec, beam.getSource(), point, false);
                    // maybe spawn some fusiony nebula effects?
                    Global.getSoundPlayer().playSound("hit_heavy", 1f, 1f, point, target.getVelocity());
                    if (!POTATO_MODE) {
                        damage = Math.min(damage, 375);
                        ApexUtils.addWaveDistortion(point, Math.min(damage * 0.25f, 150f), damage * 0.25f, 0.2f + damage * 0.002f);
                        engine.addSmoothParticle(point, new Vector2f(), radius, 1.0F, 0.05F, Color.WHITE);
                        engine.addSmoothParticle(point, new Vector2f(), radius, 1.0F, 0.1F, Color.WHITE);
                        //ApexUtils.addLight(point, damage*0.025f, damage*0.025f, 0.2f*0.002f, Color.RED);
                    }
                }
            } else
            {
                timeOnTarget = 0;
            }
        } else
        {
            timeOnTarget = 0;
        }
        if (beam.getBrightness() < 1f)
        {
            hitTargets.clear();
        }
    }
}
