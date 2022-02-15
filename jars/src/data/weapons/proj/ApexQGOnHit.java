package data.weapons.proj;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.util.Misc;
import data.hullmods.ApexQGAmplifier;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public class ApexQGOnHit implements OnHitEffectPlugin
{
    private boolean didDamage = false;
    @Override
    public void onHit(DamagingProjectileAPI proj, CombatEntityAPI target, Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine)
    {
        if (proj.getSource() != null
                && proj.getSource().getVariant().hasHullMod("apex_coherency_amplifier")
                && target instanceof ShipAPI
                && !didDamage)
        {
            // prevents the explosion from dealing damage each frame it exists
            didDamage = true;
            engine.addHitParticle(point, target.getVelocity(), 100f, 1.0F, 0.1F, Color.BLUE);
            // if it's the flak (flak aoe will have null for weapon)
            if (proj.getWeapon() == null || proj.getProjectileSpecId().startsWith("apex_flak"))
            {
                engine.applyDamage(target, point, ApexQGAmplifier.QGPD_EXTRA_ENERGY, DamageType.ENERGY, 0f, false, false, proj.getSource());
            }
            // if it's the pulser
            else
            {
                engine.applyDamage(target, point, 1f, DamageType.ENERGY, proj.getDamageAmount() * ApexQGAmplifier.QGP_EMP_FRACTION, false, false, proj.getSource());
            }
        }
    }
}
