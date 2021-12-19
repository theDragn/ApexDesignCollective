package data.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.plugins.ShipSystemStatsScript;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.HashSet;

public class ApexBifurcator extends BaseShipSystemScript
{
    public static final float MAX_SPLIT_ANGLE = 20f;
    public static final float SPLIT_CHANCE_PER_INTERVAL = 0.075f; // 50% chance per second to split at least once

    private HashSet<DamagingProjectileAPI> splitProjs = new HashSet<>();
    private IntervalUtil splitTimer = new IntervalUtil(0.075f,0.125f); // 0.1s average


    @Override
    public void apply(MutableShipStatsAPI stats, String id, ShipSystemStatsScript.State state, float effectLevel)
    {

        CombatEngineAPI engine = Global.getCombatEngine();
        float amount = engine.getElapsedInLastFrame();
        if (((ShipAPI)stats.getEntity()).isPhased() || amount == 0)
            return;
        splitTimer.advance(amount);
        if (splitTimer.intervalElapsed())
        {
            for (DamagingProjectileAPI proj : engine.getProjectiles())
            {
                if (proj.getSource() != stats.getEntity())
                    continue;
                if (proj.didDamage() || proj.isExpired() || !engine.isInPlay(proj))
                    continue;
                if (proj.getWeapon() == null)
                    continue;
                if (proj.getWeapon().getType().equals(WeaponAPI.WeaponType.MISSILE))
                    continue;
                if (splitProjs.contains(proj))
                    continue;
                if (Misc.random.nextFloat() < Math.min(SPLIT_CHANCE_PER_INTERVAL, SPLIT_CHANCE_PER_INTERVAL * proj.getMoveSpeed() / 600f))
                {
                    float distFraction = MathUtils.getDistance(proj.getLocation(), proj.getSource().getLocation()) / proj.getWeapon().getRange();
                    // if projectile is outside of weapon range, run another rng check
                    if (distFraction > 1f)
                    {
                        if (Misc.random.nextFloat() > 0.5f/distFraction)
                            continue;
                    }
                    // missiles don't have these traits and I can't be bothered to look them up rn
                    float size = 0;
                    Color color = Color.LIGHT_GRAY;
                    if (proj instanceof MissileAPI)
                    {
                        MissileAPI missile = (MissileAPI)proj;
                        size = missile.getCollisionRadius();
                        color = missile.getSpec().getExplosionColor();
                        if (color == null)
                            color = missile.getSpec().getGlowColor();
                    } else
                    {
                        size = Math.max(10, proj.getProjectileSpec().getWidth());
                        color = proj.getProjectileSpec().getFringeColor();

                    }
                    float velMult = 1f;//1f - distFraction;
                    spawnSplitProj(proj, Math.max(0.5f, velMult));

                    if (color == null)
                        return;
                    // one particle for flash
                    engine.addHitParticle(proj.getLocation(), Misc.ZERO, size * 3f, 1f, 0.5f, color);
                    // a few particles for splitty bits
                    for (int i = 0; i < size / 4f; i++)
                    {
                        Vector2f randVel = MathUtils.getRandomPointInCircle(Misc.ZERO, 100f);
                        engine.addHitParticle(proj.getLocation(), randVel, size, 1f, 0.5f, color);
                    }
                }
            }
        }
    }

    private void spawnSplitProj(DamagingProjectileAPI proj, float velMult)
    {
        String weaponID = getUnfuckedWeaponID(proj);
        if (weaponID == null)
            return;
        float facing = proj.getFacing() + Misc.random.nextFloat() * MAX_SPLIT_ANGLE - MAX_SPLIT_ANGLE/2f;
        DamagingProjectileAPI newProj = (DamagingProjectileAPI)Global.getCombatEngine().spawnProjectile(proj.getSource(), proj.getWeapon(),weaponID, proj.getLocation(), facing, Misc.ZERO);
        newProj.getVelocity().scale(velMult);
        newProj.setDamageAmount(proj.getDamageAmount());
        //splitProjs.add(newProj);
    }

    private String getUnfuckedWeaponID(DamagingProjectileAPI proj)
    {
        String weaponID = proj.getWeapon().getId();
        // hard-coded exceptions for guns that use a dummy weapon to do a projectile swap
        String spec = proj.getProjectileSpecId();
        switch (spec)
        {
            case "istl_scatterlaser_sub_shot":
                return "istl_scatterlaser_sub";
            case "istl_scatterlaser_core_shot":
                return "istl_scatterlaser_core";
            case "istl_scatterlaser_submicro":
                return "istl_scatterlaser_submicro_shot";
            case "magellan_beehive_core_shot":
                return "magellan_beehive_core";
            case "magellan_beehive_sub_shot":
                return "magellan_beehive_sub";
            case "magellan_bonecracker_core_shot":
                return "magellan_bonecracker_core";
            case "magellan_bonecracker_sub_shot":
                return "magellan_bonecracker_sub";
            case "eis_ghb_shot_splinter":
                return "eis_infernal_star_dummy";
            case "vayra_shockweb_flechette":
                return "vayra_shockweb_copy";
            case "vayra_light_flechette_shot":
                return "vayra_canister_gun_copy";
            case "vayra_biorifle_goo_copy":
                return null; // sorry folks, it's just busted. You can still split the original shots, but the child shots stick around too long.
            case "prv_spattergun_1_shot":
            case "prv_spattergun_2_shot":
                return "prv_spattergun_1";
            case "ora_invokedS":
                return "ora_invoked";
        }

        switch (weaponID)
        {
            // these have a shitload of dummy weapons so we're just gonna pick one (it sets the damage to match the source proj anyway so it doesn't matter)
            case "al_pelletdriver":
                return "al_pelletdriver_3";
            case "al_pelletgun_f":
            case "al_pelletgun":
                return "al_pelletgun_4";
            case "al_pelletcannon":
                return "al_pelletcannon_5";
        }

        // if we don't need to swap, leave weaponID alone
        return weaponID;
    }
    @Override
    public void unapply(MutableShipStatsAPI stats, String id)
    {
        //splitProjs.clear();
    }
}
