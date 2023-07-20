package data.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.util.Misc;
import utils.ApexUtils;
import org.magiclib.util.MagicRender;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

// this is a combined script holding both muzzle flash and on-hit effects
public class ApexDisruptorEffects implements OnHitEffectPlugin, OnFireEffectPlugin, EveryFrameWeaponEffectPlugin
{
    // onhit damage
    public static final float DAMAGE_FRACTION_SHIELD = 1f/12f;
    public static final float DAMAGE_FRACTION_ARMOR = 1f/24f;

    // smoke vents
    public static final float PARTICLE_SIZE_MIN = 5f;
    public static final float PARTICLE_SIZE_MAX = 20f;
    public static final float PARTICLE_DURATION = 0.33f;
    public static final float PARTICLE_SPREAD = 30f;
    public static final float PARTICLE_VEL_MIN = 5f;
    public static final float PARTICLE_VEL_MAX = 60f;
    public static final float PARTICLE_MIN_DISTANCE = 16f;
    public static final float PARTICLE_MAX_DISTANCE = 20f;
    public static final int PARTICLE_NUM = 10;
    public static final Color PARTICLE_COLOR = new Color(158,41,246,255);
    public static final float[] angles = {-132f,132f};

    // onhit graphical effect
    private static final Color CORE_EXPLOSION_COLOR = new Color(158,41,246, 255);
    private static final Color OUTER_EXPLOSION_COLOR = new Color(255, 0, 244, 25);
    private static final Color GLOW_COLOR = new Color(158,41,246, 150);
    private static final float CORE_EXP_RADIUS = 100f;
    private static final float CORE_EXP_DUR = 1f;
    private static final float OUTER_EXP_RADIUS = 150f;
    private static final float OUTER_EXP_DUR = 0.2f;
    private static final float GLOW_RADIUS = 200f;
    private static final float GLOW_DUR = 0.2f;


    @Override
    public void onHit(DamagingProjectileAPI proj, CombatEntityAPI target, Vector2f point, boolean shieldHit, ApplyDamageResultAPI damage, CombatEngineAPI engine)
    {
        // base damage ratio * damage * damage multiplier against target ship size
        if (target instanceof ShipAPI && shieldHit)
        {
            float damageAmount = DAMAGE_FRACTION_SHIELD * proj.getDamageAmount() * ApexUtils.getDamageTypeMult(proj.getSource(), (ShipAPI) target);
            ((ShipAPI) target).getFluxTracker().increaseFlux(damageAmount, true);
            if (Misc.shouldShowDamageFloaty(proj.getSource(), (ShipAPI) target)) {
                engine.addFloatingDamageText(point, damageAmount, Misc.FLOATY_SHIELD_DAMAGE_COLOR, target, proj.getSource());
            }
        }
        else if (target instanceof ShipAPI)
        {
            dealArmorDamage(proj, (ShipAPI)target, point);
        }
        if (!(target instanceof MissileAPI))
        {
            // blatantly inspired by (and totally not stolen from) the scalartech ruffle
            engine.spawnExplosion(point, Misc.ZERO, CORE_EXPLOSION_COLOR, CORE_EXP_RADIUS, CORE_EXP_DUR);
            engine.spawnExplosion(point, Misc.ZERO, OUTER_EXPLOSION_COLOR, OUTER_EXP_RADIUS, OUTER_EXP_DUR);
            engine.addHitParticle(point, Misc.ZERO, GLOW_RADIUS, 1f, GLOW_DUR, GLOW_COLOR);

            MagicRender.battlespace(
                    Global.getSettings().getSprite("graphics/fx/explosion_ring0.png"),
                    point,
                    Misc.ZERO,
                    new Vector2f(80,80),
                    new Vector2f(240,240),
                    MathUtils.getRandomNumberInRange(0,360),
                    0,
                    GLOW_COLOR,
                    true,
                    0.125f,
                    0.0f,
                    0.125f
            );
            MagicRender.battlespace(
                    Global.getSettings().getSprite("graphics/fx/explosion_ring0.png"),
                    point,
                    Misc.ZERO,
                    new Vector2f(120,120),
                    new Vector2f(100,100),
                    MathUtils.getRandomNumberInRange(0,360),
                    0,
                    CORE_EXPLOSION_COLOR,
                    true,
                    0.2f,
                    0.0f,
                    0.2f
            );
        }
    }

    // muzzle flash
    @Override
    public void onFire(DamagingProjectileAPI proj, WeaponAPI weapon, CombatEngineAPI engine)
    {

        for (float baseAngle : angles)
        {
            for (int i = 0; i < PARTICLE_NUM; i++)
            {
                float actualFacing = weapon.getCurrAngle() + baseAngle;
                float arcPoint = MathUtils.getRandomNumberInRange(actualFacing-(PARTICLE_SPREAD/2f), actualFacing+(PARTICLE_SPREAD/2f));
                Vector2f velocity = MathUtils.getPointOnCircumference(weapon.getShip().getVelocity(), MathUtils.getRandomNumberInRange(PARTICLE_VEL_MIN, PARTICLE_VEL_MAX), arcPoint);

                //Gets a spawn location in the cone, depending on our offsetMin/Max
                Vector2f spawnLocation = MathUtils.getPointOnCircumference(weapon.getLocation(), MathUtils.getRandomNumberInRange(PARTICLE_MIN_DISTANCE, PARTICLE_MAX_DISTANCE), arcPoint);
                engine.addSmoothParticle(spawnLocation, velocity, MathUtils.getRandomNumberInRange(PARTICLE_SIZE_MIN, PARTICLE_SIZE_MAX), 1f, PARTICLE_DURATION, PARTICLE_COLOR);
            }
        }

    }

    public static void dealArmorDamage(DamagingProjectileAPI projectile, ShipAPI target, Vector2f point) {
        CombatEngineAPI engine = Global.getCombatEngine();

        ArmorGridAPI grid = target.getArmorGrid();
        int[] cell = grid.getCellAtLocation(point);
        if (cell == null) return;

        int gridWidth = grid.getGrid().length;
        int gridHeight = grid.getGrid()[0].length;

        float damageTypeMult = ApexUtils.getDamageTypeMult(projectile.getSource(), target) * target.getMutableStats().getArmorDamageTakenMult().getModifiedValue();

        float damageDealt = 0f;
        for (int i = -2; i <= 2; i++) {
            for (int j = -2; j <= 2; j++) {
                if ((i == 2 || i == -2) && (j == 2 || j == -2)) continue; // skip corners

                int cx = cell[0] + i;
                int cy = cell[1] + j;

                if (cx < 0 || cx >= gridWidth || cy < 0 || cy >= gridHeight) continue;

                float damMult = 1/30f;
                if (i == 0 && j == 0) {
                    damMult = 1/15f;
                } else if (i <= 1 && i >= -1 && j <= 1 && j >= -1) { // S hits
                    damMult = 1/15f;
                } else { // T hits
                    damMult = 1/30f;
                }

                float armorInCell = grid.getArmorValue(cx, cy);
                float damage = DAMAGE_FRACTION_ARMOR * projectile.getDamageAmount() * damMult * damageTypeMult;
                damage = Math.min(damage, armorInCell);
                if (damage <= 0) continue;

                target.getArmorGrid().setArmorValue(cx, cy, Math.max(0, armorInCell - damage));
                damageDealt += damage;
            }
        }

        if (damageDealt > 0) {
            if (Misc.shouldShowDamageFloaty(projectile.getSource(), target)) {
                engine.addFloatingDamageText(point, damageDealt, Misc.FLOATY_ARMOR_DAMAGE_COLOR, target, projectile.getSource());
            }
            target.syncWithArmorGridState();
        }
    }

    @Override
    public void advance(float v, CombatEngineAPI combatEngineAPI, WeaponAPI weaponAPI)
    {
        // does nothing
        // needs to be here because I can't give something just an onFireEffect
    }
}

