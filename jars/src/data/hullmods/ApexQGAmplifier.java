package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

import static utils.ApexUtils.text;

public class ApexQGAmplifier extends BaseHullMod
{
    public static final float QGP_EMP_FRACTION = 0.5f;
    public static final float QGPD_EXTRA_ENERGY = 50f;

    // all of the actual effects are done in ApexQGOnHit

    @Override
    public String getDescriptionParam(int index, ShipAPI.HullSize hullSize)
    {
        if (index == 0)
            return (int)(QGP_EMP_FRACTION * 100f) + "%";
        if (index == 1)
            return (int)(QGPD_EXTRA_ENERGY) + " " + text("coamp4");
        return null;
    }

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        boolean sMod = isSMod(stats);
        if (sMod) stats.getProjectileSpeedMult().modifyMult(id, 1.15f);
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        if (!ship.hasListenerOfClass(ApexQGAmplifier.class))
            ship.addListener(new ApexQGAmpListener());
    }

    @Override
    public boolean hasSModEffect() {
        return true;
    }

    @Override
    public String getSModDescriptionParam(int index, ShipAPI.HullSize hullSize) {
        if (index == 0) return "15%";
        return null;
    }

    static class ApexQGAmpListener implements DamageDealtModifier
    {
        @Override
        public String modifyDamageDealt(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
            if (param instanceof DamagingProjectileAPI && ((DamagingProjectileAPI) param).getWeapon() != null)
            {
                DamagingProjectileAPI proj = (DamagingProjectileAPI)param;
                if (proj.getDamageType() == DamageType.FRAGMENTATION && proj.getWeapon().getType() == WeaponAPI.WeaponType.ENERGY)
                {
                        Global.getCombatEngine().applyDamage(target, point, 1F, DamageType.FRAGMENTATION, proj.getDamageAmount() * QGP_EMP_FRACTION, false, false, proj.getSource());
                }
            }
            return null;
        }
    }
}
