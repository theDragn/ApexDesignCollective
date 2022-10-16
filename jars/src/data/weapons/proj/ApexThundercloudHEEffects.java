package data.weapons.proj;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.util.Misc;
import data.weapons.proj.ApexDartgunOnHit;
import data.weapons.proj.ApexDartgunPlugin;
import org.lwjgl.util.vector.Vector2f;

import java.util.HashSet;

public class ApexThundercloudHEEffects implements OnHitEffectPlugin
{
    private HashSet<CombatEntityAPI> hitTargets = new HashSet<>();

    @Override
    public void onHit(DamagingProjectileAPI proj, CombatEntityAPI target, Vector2f point, boolean shieldHit, ApplyDamageResultAPI damage, CombatEngineAPI engine)
    {
        if (hitTargets.contains(target))
            return;
        else
            hitTargets.add(target);
        if (!(target instanceof ShipAPI))
            return;
        ShipAPI ship = (ShipAPI) target;
        if (ship.getShield() != null && ship.getShield().isOn() && ship.getShield().isWithinArc(point))
        {
            // do nothing
        } else {
            engine.addLayeredRenderingPlugin(new ApexDartgunPlugin(ship));
            engine.addHitParticle(point, Misc.ZERO, 50f, 1f, 0.2f, ApexDartgunOnHit.GLOW_COLOR);
        }
    }
}
