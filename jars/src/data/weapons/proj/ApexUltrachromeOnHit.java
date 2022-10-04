package data.weapons.proj;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.HashSet;
import java.util.List;

public class ApexUltrachromeOnHit implements OnHitEffectPlugin
{
    public static final float HARD_FLUX_FRAC = 1.0f;
    public static final Color ARC_COLOR = new Color(128, 128, 255);

    @Override
    public void onHit(DamagingProjectileAPI proj, CombatEntityAPI target, Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine)
    {
        if (!(target instanceof ShipAPI))
            return;
        ShipAPI ship = (ShipAPI) target;
        // first, find the station root
        if (ship.getParentStation() != null)
            ship = ship.getParentStation();
        // then, find child modules and fuck em up
        if (ship.isShipWithModules() || ship.isStation())
        {
            HashSet<ShipAPI> dudesToFuck = getAllModules(ship);
            for (ShipAPI dude : dudesToFuck)
            {
                dude.getFluxTracker().increaseFlux(proj.getDamageAmount() * HARD_FLUX_FRAC, true);
                engine.addSwirlyNebulaParticle(dude.getLocation(),
                        dude.getVelocity(),
                        dude.getCollisionRadius() * 0.05f,
                        5f, // end size mult
                        0.1f,
                        0.2f,
                        0.4f,
                        ARC_COLOR,
                        true
                );
            }
        }
    }
    public static HashSet<ShipAPI> getAllModules(ShipAPI ship)
    {
        HashSet<ShipAPI> modules = new HashSet<>();
        List<ShipAPI> children = ship.getChildModulesCopy();
        modules.add(ship);
        modules.addAll(children);
        return modules;
    }
}
