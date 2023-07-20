package data.weapons.proj;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.util.Misc;
import utils.ApexUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.HashSet;

// this is actually kinetic but I'm not changing a dozen instances of "emp" to "kin"
public class ApexThundercloudEmpEffects implements OnHitEffectPlugin
{
    public static final float HARDFLUX_FRAC = 100f/250f;
    public static final float SOFTFLUX_FRAC = 100f/250f;
    private HashSet<CombatEntityAPI> hitTargets = new HashSet<>();

    @Override
    public void onHit(DamagingProjectileAPI proj, CombatEntityAPI target, Vector2f point, boolean shieldHit, ApplyDamageResultAPI damage, CombatEngineAPI engine)
    {
        if (!(target instanceof ShipAPI))
            return;
        if (hitTargets.contains(target))
            return;
        else
            hitTargets.add(target);
        ShipAPI ship = (ShipAPI) target;
        if (ship.getShield() != null && ship.getShield().isOn() && ship.getShield().isWithinArc(point))
        {
            float dam = HARDFLUX_FRAC * proj.getDamageAmount() * ApexUtils.getDamageTypeMult(proj.getSource(), (ShipAPI)target);
            ((ShipAPI) target).getFluxTracker().increaseFlux(dam, true);
            if (Misc.shouldShowDamageFloaty(proj.getSource(), (ShipAPI) target)) {
                engine.addFloatingDamageText(point, dam, Misc.FLOATY_SHIELD_DAMAGE_COLOR, target, proj.getSource());
            }
        }
        else
        {
            float dam = SOFTFLUX_FRAC * proj.getDamageAmount() * ApexUtils.getDamageTypeMult(proj.getSource(), (ShipAPI)target);
            ((ShipAPI) target).getFluxTracker().increaseFlux(dam, false);
            if (Misc.shouldShowDamageFloaty(proj.getSource(), (ShipAPI) target)) {
                engine.addFloatingDamageText(point, dam, Misc.FLOATY_SHIELD_DAMAGE_COLOR, target, proj.getSource());
            }
        }
    }
}
