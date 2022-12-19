package data.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.loading.MissileSpecAPI;
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
    public static final float MAX_SPLIT_ANGLE = 25f;
    public static final float SPLIT_CHANCE_PER_INTERVAL = 0.075f; // 50% chance per second to split at least once
    public static final float MAX_SPLIT_RANGE = 1350 * 1350; // stops splitting projectiles if they're this far from the ship

    // turns projectile spec ID's into appropriate weapon ID's, if something isn't present, it's fine
    private static final HashMap<String, String> projToWep = new HashMap<>();


    // why the fuck did I make this thing
    // "what if I make a system that turns every gun into the templar fractal laser"
    // fucking idiot
    // and yet, here we are
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
        projToWep.put("edshipyard_Fragmine_m", "edshipyard_Fragbomb");
        projToWep.put("edshipyard_sc_p", "edshipyard_SC0");
        projToWep.put("AL_spiderpulse_shard", "AL_fake_spidershard");
        projToWep.put("AL_fake_spiderlaser_shot", "AL_fake_spiderlaser");
        projToWep.put("FM_Blade_ac_shell", "FM_Blade_ac");
        projToWep.put("FM_ice_shell", "FM_ice_weapon_m");
        projToWep.put("FM_star_shot_s", "FM_star_weapon");
        // god damn amazigh, got enough of these things?
        projToWep.put("kyeltziv_killcloud_frag","kyeltziv_lightKillCloud_Scatter");
        projToWep.put("kyeltziv_killcloud_warhead_f","kyeltziv_lightKillCloud_Burst");
        projToWep.put("kyeltziv_killcloud_warhead","kyeltziv_heavyKillCloud_Burst");
        projToWep.put("kyeltziv_lorentz_frag","kyeltziv_lorentz_fragment");
        projToWep.put("kyeltziv_fougasse_pellet","kyeltziv_shockFougasse_pellet");
        projToWep.put("kyeltziv_shrap_driver_pellet","kyeltziv_driver_shrap_pellet");
        projToWep.put("prv_massdriver_1_shot","prv_massdriver_1");
        projToWep.put("prv_jursla_1_shot", "prv_jursla_1");
        // because this exists indefinitely, allowing duplication would permit storing up an infinite amount of damage
        projToWep.put("sun_ice_mobiusray_hack", null);
        projToWep.put("sun_ice_mobiusray", null);
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
        wepToWep.put("kyeltziv_discharger_lrg","kyeltziv_heavy_discharger_jet_3");
        wepToWep.put("kyeltziv_discharger_sml","kyeltziv_light_discharger_jet_2");
        wepToWep.put("neutrino_unstable_photon","neutrino_unstable_photon3");
    }

    private IntervalUtil splitTimer = new IntervalUtil(0.075f, 0.125f); // 0.1s average


    @Override
    public void apply(MutableShipStatsAPI stats, String id, ShipSystemStatsScript.State state, float effectLevel)
    {

        CombatEngineAPI engine = Global.getCombatEngine();
        float amount = engine.getElapsedInLastFrame();
        if (((ShipAPI) stats.getEntity()).isPhased() || amount == 0)
            return;
        splitTimer.advance(amount / stats.getTimeMult().getModifiedValue());
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
                float moveSpeed = proj.getVelocity().length();
                float exp = (moveSpeed / 700f * SPLIT_CHANCE_PER_INTERVAL)/2f + 1f;
                if (Misc.random.nextFloat() < Math.pow(1 + SPLIT_CHANCE_PER_INTERVAL * moveSpeed / 700f, exp) - 1f)
                {
                    if (MathUtils.getDistanceSquared(proj.getLocation(), proj.getSource().getLocation()) > MAX_SPLIT_RANGE)
                        continue;
                    spawnSplitProj(proj);
                }
            }
        }
    }

    private void spawnSplitProj(DamagingProjectileAPI proj)
    {
        // unfuck weapon ID
        String weaponID = proj.getWeapon().getId();//getUnfuckedWeaponID(proj);
        String spec = proj.getProjectileSpecId();
        boolean didSwap = false;
        if (projToWep.containsKey(spec))
        {
            weaponID = projToWep.get(spec);
            didSwap = true;
        } else if (wepToWep.containsKey(weaponID))
        {
            weaponID = wepToWep.get(weaponID);
            didSwap = true;
        }
        if (weaponID == null)
            return;
        float facing = proj.getFacing() + Misc.random.nextFloat() * MAX_SPLIT_ANGLE - MAX_SPLIT_ANGLE / 2f;
        //float facing = (Misc.random.nextBoolean() ? -1 : 1) * MAX_SPLIT_ANGLE + proj.getFacing();
        DamagingProjectileAPI newProj = (DamagingProjectileAPI) Global.getCombatEngine().spawnProjectile(
                proj.getSource(),
                proj.getWeapon(),
                weaponID,
                proj.getLocation(),
                facing,
                Misc.ZERO);
        newProj.setDamageAmount(proj.getDamageAmount());

        // don't do on-fire if we had to unfuck the gun
        if (didSwap)
            return;
        OnFireEffectPlugin onFire;
        System.out.println(newProj.getProjectileSpecId());

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
        if (proj instanceof MissileAPI)
        {
            onFire = ((MissileAPI)proj).getSpec().getOnFireEffect();
        } else {
            onFire = proj.getProjectileSpec().getOnFireEffect();
        }
        if (onFire != null)
            onFire.onFire(newProj, proj.getWeapon(), Global.getCombatEngine());

        if (color == null)
            return;
        // one particle for flash
        Global.getCombatEngine().addHitParticle(proj.getLocation(), Misc.ZERO, size * 3f, 1f, 0.5f, color);
        // a few particles for splitty bits
        for (int i = 0; i < size / 4f; i++)
        {
            Vector2f randVel = MathUtils.getRandomPointInCircle(Misc.ZERO, 100f);
            Global.getCombatEngine().addHitParticle(proj.getLocation(), randVel, size, 1f, 0.5f, color);
        }

    }
}
