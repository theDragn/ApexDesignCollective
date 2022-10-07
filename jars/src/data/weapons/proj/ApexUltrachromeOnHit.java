package data.weapons.proj;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.HashSet;
import java.util.List;

public class ApexUltrachromeOnHit implements OnHitEffectPlugin
{
    public static final float HARD_FLUX_FRAC = 0.04f/1000f; // percentage hardflux per damage
    public static final float HARD_FLUX_FLAT = 1f; // ratio of projectile damage : hardflux
    public static final Color ARC_COLOR = new Color(128, 128, 255);

    @Override
    public void onHit(DamagingProjectileAPI proj, CombatEntityAPI target, Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine)
    {
        if (!(target instanceof ShipAPI))
            return;
        if (!shieldHit)
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
                float flux = Math.min(proj.getDamageAmount() * HARD_FLUX_FRAC * dude.getFluxTracker().getMaxFlux(), proj.getDamageAmount() * HARD_FLUX_FLAT);
                dude.getFluxTracker().increaseFlux(flux, true);
                // this loop is just muzzle flash, not trails
                for (int i = 0; i < 5; i++)
                {
                    Color color = new Color(Color.HSBtoRGB(Misc.random.nextFloat(), 1.0f, 1.0f));
                    Color actualColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), 100);
                    engine.addNebulaParticle(
                            dude.getLocation(),
                            MathUtils.getRandomPointInCircle(Misc.ZERO, 100f),
                            MathUtils.getRandomNumberInRange(40f, 60f),
                            2.2f,
                            0.3f,
                            0.3f,
                            MathUtils.getRandomNumberInRange(0.6f, 1.6f),
                            actualColor
                    );
                }
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
