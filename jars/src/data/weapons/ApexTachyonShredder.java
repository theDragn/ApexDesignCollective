package data.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.loading.ProjectileSpawnType;
import data.ApexUtils;
import data.scripts.plugins.MagicAutoTrails.trailData;
import data.scripts.plugins.MagicTrailPlugin;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;

import static org.lwjgl.opengl.GL11.GL_ONE;
import static org.lwjgl.opengl.GL11.GL_SRC_ALPHA;

// handles damage boost from flux and trail graphics
public class ApexTachyonShredder implements EveryFrameWeaponEffectPlugin, OnFireEffectPlugin
{
    public static final float DAMAGE_BOOST = 1f; // this much extra damage at maximum flux

    private static final String SPRITE_ID = "base_trail_smooth";
    private static final float FADE_IN_DURATION = 0f;
    private static final float MAIN_DURATION = 0.05f;
    private static final float FADE_OUT_DURATION = 0.5f;
    private static final float SIZE_IN = 14f; // trail width when it fades in
    private static final float SIZE_OUT = 0f; // trail width when it fades out
    private static final Color COLOR_IN = new Color(12, 16, 226);
    private static final Color COLOR_IN_FADE = new Color(192, 48, 238);
    private static final Color COLOR_OUT = new Color(12, 16, 226);
    private static final float OPACITY = 0.7f;
    private static final float TEXTURE_LENGTH = 300;
    private static final float TEXTURE_SCROLL = 400;
    private static final float DISTANCE = 20; // distance in front of projectile to start spawning trail
    private static final float DISPERSION = 0; // causes trail to spread out, iirc?
    private static final float DRIFT = 1;
    private static final boolean FADE_ON_FADE_OUT = true;
    private static final boolean ANGLE_ADJUSTMENT = false;
    private static final float VELOCITY_IN = 0;
    private static final float VELOCITY_OUT = 0f;
    private static final float ROTATION_IN = 0;
    private static final float ROTATION_OUT = 0;
    private static final float RANDOM_VELOCITY = 0;
    private static final boolean RANDOM_ROTATION = false;


    // tracks projectiles + flux level at firing to do graphics
    private final HashMap<DamagingProjectileAPI, Float> projMap = new HashMap<>();
    private final HashMap<DamagingProjectileAPI, Float> idMap = new HashMap<>();

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon)
    {
        ArrayList<DamagingProjectileAPI> toRemove = new ArrayList<>();
        for (DamagingProjectileAPI proj : projMap.keySet())
        {
            if (proj.didDamage() || proj.isExpired() || !engine.isEntityInPlay(proj))
            {
                toRemove.add(proj);
                continue;
            }
            float scalar = projMap.get(proj);
            addTrail(proj, scalar);
        }

        for (DamagingProjectileAPI proj : toRemove)
        {
            projMap.remove(proj);
        }
    }

    @Override
    public void onFire(DamagingProjectileAPI proj, WeaponAPI weapon, CombatEngineAPI engine)
    {
        projMap.put(proj, weapon.getShip().getFluxLevel());
        proj.getDamage().getModifier().modifyMult("shredder_bonus", 1f + weapon.getShip().getFluxLevel() * DAMAGE_BOOST);
        Color effectCol = new Color(
                proj.getProjectileSpec().getFringeColor().getRed(),
                proj.getProjectileSpec().getFringeColor().getGreen(),
                proj.getProjectileSpec().getFringeColor().getBlue(),
                100
        );

        for (int i = 0; i < 5 * proj.getSource().getFluxLevel(); i++) {
            engine.addNegativeNebulaParticle(
                    proj.getLocation(),
                    weapon.getShip().getVelocity(),
                    MathUtils.getRandomNumberInRange(40f, 60f),
                    1.2f,
                    0.1f,
                    0.3f,
                    MathUtils.getRandomNumberInRange(0.6f, 1.6f),
                    effectCol
            );
        }

        engine.spawnExplosion(
                proj.getLocation(),
                weapon.getShip().getVelocity(),
                proj.getProjectileSpec().getFringeColor(),
                60f,
                0.15f
        );
    }

    // long so I'm hiding it down here
    private void addTrail(DamagingProjectileAPI proj, float scalar)
    {
        if (!idMap.containsKey(proj))
            idMap.put(proj, MagicTrailPlugin.getUniqueID());

        SpriteAPI sprite = Global.getSettings().getSprite("fx", SPRITE_ID);

        Vector2f projVel = new Vector2f(proj.getVelocity());
        //Fix for some first-frame error shenanigans
        if (projVel.length() < 0.1f && proj.getSource() != null) {
            projVel = new Vector2f(proj.getSource().getVelocity());
        }
        //If we use angle adjustment, do that here
        if (ANGLE_ADJUSTMENT && projVel.length() > 0.1f && !proj.getSpawnType().equals(ProjectileSpawnType.BALLISTIC_AS_BEAM)) {
            proj.setFacing(VectorUtils.getFacing(projVel));
        }

        //Gets a custom "offset" position, so we can slightly alter the spawn location to account for "natural fade-in", and add that to our spawn position
//            Vector2f offsetPoint = new Vector2f((float) Math.cos(Math.toRadians(proj.getFacing())) * trailData.distance, (float) Math.sin(Math.toRadians(proj.getFacing())) * trailData.distance);
//            Vector2f spawnPosition = new Vector2f(offsetPoint.x + proj.getLocation().x, offsetPoint.y + proj.getLocation().y);
        Vector2f spawnPosition = MathUtils.getPointOnCircumference(
                proj.getLocation(), DISTANCE, proj.getFacing());

        //Sideway offset velocity, for projectiles that use it
        Vector2f projBodyVel = new Vector2f(projVel);
        projBodyVel = VectorUtils.rotate(projBodyVel, -proj.getFacing());
        Vector2f projLateralBodyVel = new Vector2f(0f, projBodyVel.getY());
        Vector2f sidewayVel = new Vector2f(projLateralBodyVel);
        sidewayVel = (Vector2f) VectorUtils.rotate(sidewayVel, proj.getFacing()).scale(DRIFT);

        //random dispersion of the segments if necessary
        float rotationIn = ROTATION_IN;
        float rotationOut = ROTATION_OUT;

        float velIn = VELOCITY_IN;
        float velOut = VELOCITY_OUT;

        if (RANDOM_ROTATION) {
            float rand = MathUtils.getRandomNumberInRange(-1f, 1f);
            rotationIn = rotationIn * rand;
            rotationOut = rotationOut * rand;
        }

        if (DISPERSION > 0) {
            Vector2f.add(
                    sidewayVel,
                    MathUtils.getRandomPointInCircle(null, DISPERSION),
                    sidewayVel);
        }

        if (RANDOM_VELOCITY > 0) {
            float rand = MathUtils.getRandomNumberInRange(-1f, 1f);
            velIn *= 1 + RANDOM_VELOCITY * rand;
            velOut *= 1 + RANDOM_VELOCITY * rand;
        }

        //Opacity adjustment for fade-out, if the projectile uses it
        float opacityMult = 1f;
        if (FADE_ON_FADE_OUT && proj.isFading()) {
            opacityMult = Math.max(0, Math.min(1, proj.getDamageAmount() / proj.getBaseDamageAmount()));
        }

        //Then, actually spawn a trail
        MagicTrailPlugin.AddTrailMemberAdvanced(
                proj,
                idMap.get(proj),
                sprite,
                spawnPosition,
                velIn,
                velOut,
                proj.getFacing() - 180f,
                rotationIn,
                rotationOut,
                SIZE_IN,
                SIZE_OUT,
                ApexUtils.blendColors(COLOR_IN, COLOR_IN_FADE, scalar),
                COLOR_OUT,
                OPACITY * opacityMult,
                FADE_IN_DURATION,
                MAIN_DURATION,
                FADE_OUT_DURATION,
                GL_SRC_ALPHA,
                GL_ONE,
                TEXTURE_LENGTH,
                TEXTURE_SCROLL,
                0f,
                sidewayVel,
                null,
                CombatEngineLayers.BELOW_INDICATORS_LAYER,
                1f
        );
    }

}
