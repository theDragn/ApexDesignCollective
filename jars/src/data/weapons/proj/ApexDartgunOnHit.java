package data.weapons.proj;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public class ApexDartgunOnHit implements OnHitEffectPlugin
{
    public static final Color GLOW_COLOR = new Color(255,165,30,105);

    @Override
    public void onHit(DamagingProjectileAPI proj, CombatEntityAPI target, Vector2f point, boolean shieldHit, ApplyDamageResultAPI applyDamageResult, CombatEngineAPI engine)
    {
        if (target instanceof ShipAPI && !shieldHit)
        {
            ShipAPI ship = (ShipAPI)target;
            engine.addLayeredRenderingPlugin(new ApexDartgunPlugin(ship));
            engine.addHitParticle(point, Misc.ZERO, 50f, 1f, 0.2f, GLOW_COLOR);
        }
    }
}
