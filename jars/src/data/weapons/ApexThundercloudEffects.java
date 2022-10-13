package data.weapons;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

public class ApexThundercloudEffects implements EveryFrameWeaponEffectPlugin
{
    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon)
    {
        if (!weapon.getShip().hasListenerOfClass(ApexThundercloudListener.class))
            weapon.getShip().addListener(new ApexThundercloudListener());
    }

    public static class ApexThundercloudListener implements DamageDealtModifier
    {
        @Override
        public String modifyDamageDealt(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit)
        {
            //System.out.println(param.getClass()); // DamagingExplosion, instance of DamagingProjectileAPI but returns null for fucking everything
            // so instead: the explosion does a very specific amount of damage, and it detects that
            // jank city 2: jank harder
            if (param instanceof DamagingProjectileAPI && ((DamagingProjectileAPI) param).getBaseDamageAmount() == 251)
            {
                //System.out.println("found you, bitch");
                if (target instanceof ShipAPI && (((ShipAPI)target).isStationModule() || ((ShipAPI)target).isStation()))
                    return null;
                float mult = MathUtils.clamp(target.getCollisionRadius() / 150f, 0.25f, 1f);
                damage.getModifier().modifyMult("apex_tcloud", mult);
                return "apex_tcloud";
            } else
                return null;
        }
    }
}
