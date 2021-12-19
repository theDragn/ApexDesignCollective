package data.weapons.proj;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.util.Misc;
import data.ApexUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;


public class ApexSledgehammerOnHit implements OnHitEffectPlugin
{
    @Override
    public void onHit(DamagingProjectileAPI proj, CombatEntityAPI target, Vector2f point, boolean shieldHit, ApplyDamageResultAPI applyDamageResult, CombatEngineAPI engine)
    {
            engine.addSmoothParticle(point, Misc.ZERO, 300f, 1.0F, 0.05F, Color.WHITE);
            engine.addSmoothParticle(point, Misc.ZERO, 300f, 1.0F, 0.1F, Color.WHITE);
    }
}
