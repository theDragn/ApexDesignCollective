package data.weapons.proj;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.loading.ProjectileSpawnType;
import com.fs.starfarer.api.util.Misc;
import org.magiclib.plugins.MagicTrailPlugin;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import static org.lwjgl.opengl.GL11.GL_ONE;
import static org.lwjgl.opengl.GL11.GL_SRC_ALPHA;

// this is gonna be a long one
public class ApexUltrachromeTrail implements OnFireEffectPlugin, EveryFrameWeaponEffectPlugin
{

    // trail settings
    private static final String SPRITE_ID = "apex_trail_helix"; // adds "_reversed" for guns in right-hand slots
    private static final float FADE_IN_DURATION = 0f;
    private static final float MAIN_DURATION = 0.05f;
    private static final float FADE_OUT_DURATION = 1f;
    private static final float SIZE_IN = 40f; // trail width when it fades in
    private static final float SIZE_OUT = 80f; // trail width when it fades out
    // COLOR_IN is computed through projectile lifetime and an HSV to RGB conversion- see ~line 183
    private static final Color COLOR_OUT = new Color(108, 108, 108);
    private static final float OPACITY = 0.7f;
    private static final float TEXTURE_LENGTH = 300;
    private static final float TEXTURE_SCROLL = 20;
    private static final float DISTANCE = 0; // distance in front of projectile to start spawning trail
    private static final float DISPERSION = 0; // causes trail to spread out, iirc?
    private static final float DRIFT = 0;
    private static final boolean FADE_ON_FADE_OUT = true;
    private static final boolean ANGLE_ADJUSTMENT = false;
    private static final float VELOCITY_IN = 0;
    private static final float VELOCITY_OUT = 0f;
    private static final float ROTATION_IN = 0;
    private static final float ROTATION_OUT = 0;
    private static final float RANDOM_VELOCITY = 0;
    private static final boolean RANDOM_ROTATION = false;

    // please do not think about how these objects are handled
    // it works, trust me
    private final HashMap<DamagingProjectileAPI, Float> idMap = new HashMap<>();
    private final HashSet<DamagingProjectileAPI> projectiles = new HashSet<>();

    @Override
    public void onFire(DamagingProjectileAPI proj, WeaponAPI weapon, CombatEngineAPI engine)
    {
        // add projectile to the list of projectiles we need to manage trails for
        projectiles.add(proj);

        // this loop is just muzzle flash, not trails
        for (int i = 0; i < 5; i++)
        {
            Color muzzleFlashColor = new Color(Color.HSBtoRGB(Misc.random.nextFloat(), 1.0f, 1.0f));
            Color actualColor = new Color(muzzleFlashColor.getRed(), muzzleFlashColor.getGreen(), muzzleFlashColor.getBlue(), 100);
            engine.addNebulaParticle(
                    proj.getLocation(),
                    MathUtils.getPointOnCircumference(proj.getVelocity(), 200f, proj.getFacing() + 150 + Misc.random.nextFloat() * 60),
                    MathUtils.getRandomNumberInRange(40f, 60f),
                    1.2f,
                    0.1f,
                    0.3f,
                    MathUtils.getRandomNumberInRange(0.6f, 1.6f),
                    actualColor
            );
        }
    }

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon)
    {
        // just doing the trail segment every frame and cleaning the list
        ArrayList<DamagingProjectileAPI> toRemove = new ArrayList<>();
        for (DamagingProjectileAPI proj : projectiles)
        {
            if (proj.isExpired() || proj.didDamage() || !engine.isInPlay(proj))
                toRemove.add(proj);
            else
            {
                addTrailSegment(proj, (proj.getElapsed() / 2f) % 1f);
                // do sparkly trail
                engine.addSmoothParticle(
                        MathUtils.getRandomPointInCircle(proj.getLocation(), 20f),
                        MathUtils.getPointOnCircumference(proj.getVelocity(), 200f, proj.getFacing() + 150 + Misc.random.nextFloat() * 60),
                        15f,
                        0.5f,
                        0.66f,
                        new Color(Color.HSBtoRGB(Misc.random.nextFloat(), 1.0f, 1.0f)));
            }
        }
        for (DamagingProjectileAPI proj : toRemove)
        {
            projectiles.remove(proj);
        }
        //System.out.println(projectiles.size());
    }


    // spawns trail segment. should be called each frame on each projectile.
    // color changing is handled in here
    // almost entirely ripped from magiclib's trail plugin
    private void addTrailSegment(DamagingProjectileAPI proj, float scalar)
    {
        if (!idMap.containsKey(proj))
            idMap.put(proj, MagicTrailPlugin.getUniqueID());
        // reverse spiral direction if it's on the other gun
        String spriteID = proj.getWeapon().getSlot().getId().equals("WS0015") ? SPRITE_ID : SPRITE_ID + "_reversed";
        SpriteAPI sprite = Global.getSettings().getSprite("fx", spriteID);

        Vector2f projVel = new Vector2f(proj.getVelocity());
        //Fix for some first-frame error shenanigans
        if (projVel.length() < 0.1f && proj.getSource() != null)
        {
            projVel = new Vector2f(proj.getSource().getVelocity());
        }
        //If we use angle adjustment, do that here
        if (ANGLE_ADJUSTMENT && projVel.length() > 0.1f && !proj.getSpawnType().equals(ProjectileSpawnType.BALLISTIC_AS_BEAM))
        {
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

        if (RANDOM_ROTATION)
        {
            float rand = MathUtils.getRandomNumberInRange(-1f, 1f);
            rotationIn = rotationIn * rand;
            rotationOut = rotationOut * rand;
        }

        if (DISPERSION > 0)
        {
            Vector2f.add(
                    sidewayVel,
                    MathUtils.getRandomPointInCircle(null, DISPERSION),
                    sidewayVel);
        }

        if (RANDOM_VELOCITY > 0)
        {
            float rand = MathUtils.getRandomNumberInRange(-1f, 1f);
            velIn *= 1 + RANDOM_VELOCITY * rand;
            velOut *= 1 + RANDOM_VELOCITY * rand;
        }

        //Opacity adjustment for fade-out, if the projectile uses it
        float opacityMult = 1f;
        if (FADE_ON_FADE_OUT && proj.isFading())
        {
            opacityMult = Math.max(0, Math.min(1, proj.getDamageAmount() / proj.getBaseDamageAmount()));
        }

        //Then, actually spawn a trail
        MagicTrailPlugin.addTrailMemberAdvanced(
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
                new Color(Color.HSBtoRGB(scalar, 1f, 1f)), // would normally be COLOR_IN
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
