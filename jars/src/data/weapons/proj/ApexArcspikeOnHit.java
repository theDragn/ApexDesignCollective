package data.weapons.proj;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.util.Misc;
import utils.ApexUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public class ApexArcspikeOnHit implements OnHitEffectPlugin
{
    public static final float ARC_CHANCE = 0.25f;
    public static final float HARD_FLUX_FRACTION = 75/110f;

    @Override
    public void onHit(DamagingProjectileAPI proj, CombatEntityAPI target, Vector2f point, boolean shieldhit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine)
    {
        if (shieldhit && target instanceof ShipAPI)
        {
            float damage = HARD_FLUX_FRACTION * proj.getDamageAmount() * ApexUtils.getDamageTypeMult(proj.getSource(), (ShipAPI)target);
            ((ShipAPI) target).getFluxTracker().increaseFlux(damage, true);
            if (Misc.shouldShowDamageFloaty(proj.getSource(), (ShipAPI) target)) {
                engine.addFloatingDamageText(point, damage, Misc.FLOATY_SHIELD_DAMAGE_COLOR, target, proj.getSource());
            }
        } else if (!shieldhit && target instanceof ShipAPI && Misc.random.nextFloat() <= ARC_CHANCE)
        {
            engine.spawnEmpArc(proj.getSource(), point, target, target,
                    DamageType.ENERGY,
                    0f, // no damage, just emp
                    proj.getEmpAmount(), // emp
                    100000f, // max range
                    "tachyon_lance_emp_impact",
                    20f, // thickness
                    new Color(25,100,155,255),
                    new Color(255,255,255,255)
            );
        }
    }
}
