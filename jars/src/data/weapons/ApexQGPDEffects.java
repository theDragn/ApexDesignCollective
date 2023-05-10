package data.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.Misc;
import org.magiclib.plugins.MagicTrailPlugin;
import data.weapons.proj.ApexQGPDHomingScript;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public class ApexQGPDEffects implements OnFireEffectPlugin, EveryFrameWeaponEffectPlugin
{

    // hullmod bonus effect
    public static final float NEBULA_SIZE = 3f;
    public static final float NEBULA_SIZE_MULT = 9f;
    public static final float NEBULA_DUR = 0.5f;
    public static final float NEBULA_RAMPUP = 0.1f;
    public static final Color PARTICLE_COLOR = new Color(82,206,198,255);
    public static final float[] angles = {0f};

    @Override
    public void onFire(DamagingProjectileAPI proj, WeaponAPI weapon, CombatEngineAPI engine)
    {
        if (weapon.getShip().getVariant().hasHullMod("apex_coherency_amplifier"))
        {
            Vector2f shipVel = weapon.getShip().getVelocity();
            Vector2f loc = proj.getLocation();
            float size = NEBULA_SIZE * (0.75f + Misc.random.nextFloat() * 0.5f); // 0.75x to 1.25x
            // do visual fx
            engine.addSwirlyNebulaParticle(loc,
                    shipVel,
                    NEBULA_SIZE,
                    NEBULA_SIZE_MULT,
                    NEBULA_RAMPUP,
                    0.2f,
                    NEBULA_DUR,
                    PARTICLE_COLOR,
                    true
            );
            engine.addSmoothParticle(loc,
                    shipVel,
                    NEBULA_SIZE * 2,
                    0.75f,
                    NEBULA_RAMPUP,
                    NEBULA_DUR / 2,
                    PARTICLE_COLOR
            );
        }
        // weapon normally fires flak shot, so if it's in an energy slot, swap it out for non-flak and apply homing script
        if (weapon.getSlot().getWeaponType().equals(WeaponAPI.WeaponType.ENERGY))
        {
            DamagingProjectileAPI newProj;
            if (weapon.getSize() == WeaponAPI.WeaponSize.SMALL)
                newProj = (DamagingProjectileAPI) engine.spawnProjectile(proj.getSource(), weapon, "apex_qgpd_s_nonflak", proj.getLocation(), proj.getFacing(), null);
            else
                newProj = (DamagingProjectileAPI) engine.spawnProjectile(proj.getSource(), weapon, "apex_qgpd_m_nonflak", proj.getLocation(), proj.getFacing(), null);

            addHomingPlugin(newProj, weapon);
            // needs a little bit of extra velocity to account for the curvature
            newProj.getVelocity().scale(1.33f);
            engine.removeEntity(proj);
        } else if (weapon.getSlot().getWeaponType().equals(WeaponAPI.WeaponType.UNIVERSAL))
        {
            addHomingPlugin(proj, weapon);
            proj.getVelocity().scale(1.33f);
        }

    }

    private void addHomingPlugin(DamagingProjectileAPI proj, WeaponAPI weapon)
    {
        ShipAPI target = null;
        ShipAPI source = proj.getSource();
        if (source.getWeaponGroupFor(weapon) != null)
        {
            if (source.getWeaponGroupFor(weapon).isAutofiring()
                    && source.getSelectedGroupAPI() != source.getWeaponGroupFor(weapon))
            {
                target = source.getWeaponGroupFor(weapon).getAutofirePlugin(weapon).getTargetShip();
            } else
            {
                target = source.getShipTarget();
            }
        }
        Global.getCombatEngine().addPlugin(new ApexQGPDHomingScript(proj, target));
    }

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon)
    {
    }
}
