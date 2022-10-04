package data.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.loading.ProjectileSpecAPI;
import com.fs.starfarer.api.plugins.ShipSystemStatsScript;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.HashMap;
import java.util.HashSet;

public class ApexBifurcator extends BaseShipSystemScript
{
    public static final float MAX_SPLIT_ANGLE = 20f;
    public static final float SPLIT_CHANCE_PER_INTERVAL = 0.075f; // 50% chance per second to split at least once

    // turns projectile spec ID's into appropriate weapon ID's, if something isn't present, it's fine
    private static final HashMap<String, String> projToWep = new HashMap<>();

    static
    {
        projToWep.put("istl_scatterlaser_sub_shot", "istl_scatterlaser_sub");
        projToWep.put("istl_scatterlaser_core_shot", "istl_scatterlaser_core");
        projToWep.put("istl_scatterlaser_submicro", "istl_scatterlaser_submicro_shot");
        projToWep.put("magellan_beehive_core_shot", "magellan_beehive_core");
        projToWep.put("magellan_beehive_sub_shot", "magellan_beehive_sub");
        projToWep.put("magellan_bonecracker_core_shot", "magellan_bonecracker_core");
        projToWep.put("magellan_bonecracker_sub_shot", "magellan_bonecracker_sub");
        projToWep.put("eis_ghb_shot_splinter", "eis_infernal_star_dummy");
        projToWep.put("vayra_shockweb_flechette", "vayra_shockweb_copy");
        projToWep.put("vayra_light_flechette_shot", "vayra_canister_gun_copy");
        projToWep.put("vayra_biorifle_goo_copy", null); // sorry folks, it's just busted. You can still split the original shots, but the child shots stick around too long.
        projToWep.put("prv_spattergun_1_shot", "prv_spattergun_1");
        projToWep.put("prv_spattergun_2_shot", "prv_spattergun_1");
        projToWep.put("ora_invokedS", "ora_invoked");
        projToWep.put("armaa_flamer_shot3", "armaa_aleste_flamer_right_copy");
        projToWep.put("fed_ionprojector_shot_clone_proj", "fed_ionprojector_shot_clone");
        projToWep.put("fed_flak3_scrap_clone_proj", "fed_flak3_scrap_clone");
        projToWep.put("fed_flak4_scrap_clone_proj", "fed_flak4_scrap_clone");
        projToWep.put("fed_flak2_scrap_clone_proj", "fed_flak2_scrap_clone");
        projToWep.put("fed_flak1_scrap_clone_proj", "fed_flak1_scrap_clone");
        projToWep.put("KT_boomstick_proj2", "KT_blunderbuss_he");
        projToWep.put("KT_boomstick_proj", "KT_blunderbuss_ki");
        projToWep.put("KT_blunderbuss_debris1", null);
        projToWep.put("KT_blunderbuss_debris2", null);
        projToWep.put("tahlan_tousle_split_shot","tahlan_tousle_split");
    }
    // turns weapon spec ID's into appropriate weapon spec ID's. Used for guns with too many sub-weapons
    // not perfectly accurate but generally good enough that it's hard to tell that it's cheating
    private static final HashMap<String, String> wepToWep = new HashMap<>();

    static
    {
        wepToWep.put("al_pelletdriver", "al_pelletdriver_3");
        wepToWep.put("al_pelletgun_f", "al_pelletgun_4");
        wepToWep.put("al_pelletgun", "al_pelletgun_4");
        wepToWep.put("al_pelletcannon", "al_pelletcannon_5");
        wepToWep.put("KT_magmablaster", "KT_magma25");
        wepToWep.put("KT_magmahose", "KT_hose25");
        wepToWep.put("KT_bigrockchucker","KT_rockchucker_dummy1");
    }

    private IntervalUtil splitTimer = new IntervalUtil(0.075f, 0.125f); // 0.1s average


    @Override
    public void apply(MutableShipStatsAPI stats, String id, ShipSystemStatsScript.State state, float effectLevel)
    {

        CombatEngineAPI engine = Global.getCombatEngine();
        float amount = engine.getElapsedInLastFrame();
        if (((ShipAPI) stats.getEntity()).isPhased() || amount == 0)
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
                if (Misc.random.nextFloat() < Math.min(SPLIT_CHANCE_PER_INTERVAL, SPLIT_CHANCE_PER_INTERVAL * proj.getMoveSpeed() / 600f))
                {
                    float distFraction = MathUtils.getDistance(proj.getLocation(), proj.getSource().getLocation()) / proj.getWeapon().getRange();
                    // if projectile is outside of weapon range, run another rng check
                    if (distFraction > 1f)
                    {
                        if (Misc.random.nextFloat() > 0.5f / distFraction)
                            continue;
                    }
                    // missiles don't have these traits and I can't be bothered to look them up rn
                    float size = 0;
                    Color color = Color.LIGHT_GRAY;
                    if (proj instanceof MissileAPI)
                    {
                        MissileAPI missile = (MissileAPI) proj;
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
        float facing = proj.getFacing() + Misc.random.nextFloat() * MAX_SPLIT_ANGLE - MAX_SPLIT_ANGLE / 2f;
        DamagingProjectileAPI newProj = (DamagingProjectileAPI) Global.getCombatEngine().spawnProjectile(
                proj.getSource(),
                proj.getWeapon(),
                weaponID,
                proj.getLocation(),
                facing,
                Misc.ZERO);
        newProj.getVelocity().scale(velMult);
        newProj.setDamageAmount(proj.getDamageAmount());
    }

    private String getUnfuckedWeaponID(DamagingProjectileAPI proj)
    {
        String weaponID = proj.getWeapon().getId();
        String spec = proj.getProjectileSpecId();

        // hard-coded exceptions for guns that use a dummy weapon to do a projectile swap
        if (projToWep.containsKey(spec))
            return projToWep.get(spec);
        if (wepToWep.containsKey(weaponID))
            return wepToWep.get(weaponID);

        // if we don't need to swap, leave weaponID alone
        return weaponID;
    }

}
