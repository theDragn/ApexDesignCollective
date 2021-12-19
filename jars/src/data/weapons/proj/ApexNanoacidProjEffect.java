package data.weapons.proj;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.util.Misc;
import data.effects.ApexNanoacidEffect;
import org.lwjgl.util.vector.Vector2f;

public class ApexNanoacidProjEffect implements OnHitEffectPlugin
{
    public static final float ACID_DAMAGE_TORP = 1f;
    public static final float ACID_DAMAGE_GUN = 0.33f;
    public static final float ACID_DURATION_TORP = 10f;
    public static final float ACID_DURATION_GUN = 5f;

    @Override
    public void onHit(DamagingProjectileAPI proj, CombatEntityAPI target, Vector2f point, boolean shieldHit, ApplyDamageResultAPI damage, CombatEngineAPI engine)
    {
        if (shieldHit) return;
        if (proj.isFading()) return;
        if (!(target instanceof ShipAPI)) return;

        Vector2f offset = Vector2f.sub(point, target.getLocation(), new Vector2f());
        offset = Misc.rotateAroundOrigin(offset, -target.getFacing());

        float acidDamage = 0f;
        float acidDuration = 0f;
        float sizeMult = 1f;
        int alpha = 20;
        boolean degradeArmor = false;
        boolean damageHull = false;
        switch (proj.getProjectileSpecId())
        {
            case "apex_nanogun_shot":
                acidDamage = ACID_DAMAGE_GUN * proj.getDamageAmount();
                acidDuration = ACID_DURATION_GUN;
                sizeMult = 0.8f;
                degradeArmor = false;
                damageHull = true;
                break;
            case "apex_nanoacid_torp":
                acidDamage = ACID_DAMAGE_TORP * proj.getDamageAmount();
                acidDuration = ACID_DURATION_TORP;
                sizeMult = 2.25f;
                degradeArmor = true;
                damageHull = true;
                alpha = 33;
                break;
            case "apex_nanoacid_torp_guided":
                acidDamage = ACID_DAMAGE_TORP * proj.getDamageAmount();
                acidDuration = ACID_DURATION_TORP;
                sizeMult = 2.25f;
                degradeArmor = true;
                damageHull = true;
                alpha = 33;
                break;
            case "apex_vls_acid_missile":
                acidDamage = 0.5f * proj.getDamageAmount();
                acidDuration = ACID_DURATION_TORP;
                sizeMult = 1f;
                degradeArmor = true;
                damageHull = true;
                break;
        }
        ApexNanoacidEffect effect = new ApexNanoacidEffect(proj, (ShipAPI) target, offset, sizeMult, alpha, acidDamage, acidDuration, degradeArmor, damageHull);
        CombatEntityAPI e = engine.addLayeredRenderingPlugin(effect);
        e.getLocation().set(proj.getLocation());

    }


}
