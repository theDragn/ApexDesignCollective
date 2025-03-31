package data.weapons.proj;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.util.Misc;
import data.hullmods.ApexQGAmplifier;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.HashSet;

public class ApexQGOnHit implements OnHitEffectPlugin
{
    private HashSet<CombatEntityAPI> hitTargets = new HashSet<>();


    // why the fuck did I put like five different guns in one plugin
    @Override
    public void onHit(DamagingProjectileAPI proj, CombatEntityAPI target, Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine)
    {
        if (proj.getSource() != null
                && (proj.getSource().getVariant().hasHullMod("apex_coherency_amplifier"))
                && target instanceof ShipAPI
                && !hitTargets.contains(target))
        {
            // prevents the damage being dealt more than once (a problem for the explosions)
            hitTargets.add(target);
            engine.addHitParticle(point, target.getVelocity(), 100f, 1.0F, 0.1F, Color.BLUE);
            // if it's the flak (flak aoe will have null for weapon)
            if (proj.getWeapon() == null || proj.getProjectileSpecId().startsWith("apex_flak"))
            {
                engine.applyDamage(target, point, ApexQGAmplifier.QGPD_EXTRA_ENERGY, DamageType.ENERGY, 0f, false, false, proj.getSource());
            }
            // if it's the pulser
            else
            {
                //engine.applyDamage(target, point, 1f, DamageType.ENERGY, proj.getDamageAmount() * ApexQGAmplifier.QGP_EMP_FRACTION, false, false, proj.getSource());
            }
        }
        // Gecko projectiles
        else if (proj.getWeapon() != null && proj.getWeapon().getId().contains("apex_qgp_fighter") && target instanceof ShipAPI)
        {
            engine.applyDamage(target, point, 25f, DamageType.ENERGY, 0f, false, false, proj.getSource());
        }
    }
}
