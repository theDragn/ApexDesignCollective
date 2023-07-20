package data.weapons.proj;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.util.Misc;
import utils.ApexUtils;
import org.lwjgl.util.vector.Vector2f;

public class ApexKinVLSOnHit implements OnHitEffectPlugin
{

    public static final float SHIELD_HIT_FLUX_FRAC = 0.50f;
    public static final float ARMOR_HIT_FLUX_FRAC = 0.50f;

    @Override
    public void onHit(DamagingProjectileAPI proj, CombatEntityAPI target, Vector2f point, boolean shieldhit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine)
    {
        if (shieldhit && target instanceof ShipAPI)
        {
            float damage = SHIELD_HIT_FLUX_FRAC * proj.getDamageAmount() * ApexUtils.getDamageTypeMult(proj.getSource(), (ShipAPI)target);
            ((ShipAPI) target).getFluxTracker().increaseFlux(damage, true);
            if (Misc.shouldShowDamageFloaty(proj.getSource(), (ShipAPI) target)) {
                engine.addFloatingDamageText(point, damage, Misc.FLOATY_SHIELD_DAMAGE_COLOR, target, proj.getSource());
            }
        } else if (!shieldhit && target instanceof ShipAPI)
        {
            float damage = ARMOR_HIT_FLUX_FRAC * proj.getDamageAmount() * ApexUtils.getDamageTypeMult(proj.getSource(), (ShipAPI)target);
            ((ShipAPI) target).getFluxTracker().increaseFlux(damage, false);
            if (Misc.shouldShowDamageFloaty(proj.getSource(), (ShipAPI) target)) {
                engine.addFloatingDamageText(point, damage, Misc.FLOATY_SHIELD_DAMAGE_COLOR, target, proj.getSource());
            }
        }
    }
}
