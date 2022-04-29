package data.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.loading.ProjectileSpawnType;
import data.scripts.plugins.MagicAutoTrails.trailData;
import data.scripts.plugins.MagicTrailPlugin;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.HashMap;

// handles damage boost from flux and trail graphics
public class ApexTachyonShredder implements EveryFrameWeaponEffectPlugin, OnFireEffectPlugin
{
    public static final float DAMAGE_BOOST = 0.5f; // this much extra damage at maximum flux
    // trail properties
    trailData trailSpec = new trailData();

    // tracks projectiles + flux level at firing to do graphics
    private final HashMap<DamagingProjectileAPI, Float> projMap = new HashMap<>();

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
    }

    // long so I'm hiding it down here
    private void addTrail(DamagingProjectileAPI proj, float scalar)
    {
        SpriteAPI spriteToUse = Global.getSettings().getSprite("fx", "sprite goes here");

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
        sidewayVel = (Vector2f) VectorUtils.rotate(sidewayVel, proj.getFacing()).scale(trailSpec.drift);

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
         id,
            sprite,
        spawnPosition,
        float startSpeed,
        float endSpeed,
        float angle,
        float startAngularVelocity,
        float endAngularVelocity,
        float startSize,
        float endSize,
        java.awt.Color startColor,
        java.awt.Color endColor,
        float opacity,
        float inDuration,
        float mainDuration,
        float outDuration,
        boolean additive,
        float textureLoopLength,
        float textureScrollSpeed,
        float textureOffset,
        @Nullable
        org.lwjgl.util.vector.Vector2f offsetVelocity,
        @Nullable
        java.util.Map<java.lang.String,java.lang.Object> advancedOptions,
        @Nullable
        com.fs.starfarer.api.combat.CombatEngineLayers layerToRenderOn,
        float frameOffsetMult)
        );
    }

}
