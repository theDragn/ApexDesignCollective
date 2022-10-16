package data.weapons.proj;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.util.Misc;
import data.ApexUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.HashSet;
import java.util.List;

public class ApexThundercloudFragEffects implements OnHitEffectPlugin
{

    private static final float DAMAGE_FRACTION_SHIELD = 100f/600f;
    private static final float DAMAGE_FRACTION_ARMOR = 50f/600f;

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
            float mult = proj.getSource() == null ? 1f : ApexUtils.getDamageTypeMult(proj.getSource(), (ShipAPI) target);
            float damageAmount = DAMAGE_FRACTION_SHIELD * proj.getDamageAmount() * mult;
            ((ShipAPI) target).getFluxTracker().increaseFlux(damageAmount, true);
            if (Misc.shouldShowDamageFloaty(proj.getSource(), (ShipAPI) target)) {
                engine.addFloatingDamageText(point, damageAmount, Misc.FLOATY_SHIELD_DAMAGE_COLOR, target, proj.getSource());
            }
        }
        else
        {
            dealArmorDamage(proj, (ShipAPI)target, point);
        }
    }

    public static void dealArmorDamage(DamagingProjectileAPI projectile, ShipAPI target, Vector2f point) {
        CombatEngineAPI engine = Global.getCombatEngine();

        ArmorGridAPI grid = target.getArmorGrid();
        int[] cell = grid.getCellAtLocation(point);
        if (cell == null) return;

        int gridWidth = grid.getGrid().length;
        int gridHeight = grid.getGrid()[0].length;

        float mult = projectile.getSource() == null ? 1f : ApexUtils.getDamageTypeMult(projectile.getSource(), (ShipAPI) target);
        float damageTypeMult = mult * target.getMutableStats().getArmorDamageTakenMult().getModifiedValue();

        float damageDealt = 0f;
        for (int i = -2; i <= 2; i++) {
            for (int j = -2; j <= 2; j++) {
                if ((i == 2 || i == -2) && (j == 2 || j == -2)) continue; // skip corners

                int cx = cell[0] + i;
                int cy = cell[1] + j;

                if (cx < 0 || cx >= gridWidth || cy < 0 || cy >= gridHeight) continue;

                float damMult = 1/30f;
                if (i == 0 && j == 0) {
                    damMult = 1/15f;
                } else if (i <= 1 && i >= -1 && j <= 1 && j >= -1) { // S hits
                    damMult = 1/15f;
                } else { // T hits
                    damMult = 1/30f;
                }

                float armorInCell = grid.getArmorValue(cx, cy);
                float damage = DAMAGE_FRACTION_ARMOR * projectile.getDamageAmount() * damMult * damageTypeMult;
                damage = Math.min(damage, armorInCell);
                if (damage <= 0) continue;

                target.getArmorGrid().setArmorValue(cx, cy, Math.max(0, armorInCell - damage));
                damageDealt += damage;
            }
        }

        if (damageDealt > 0) {
            if (Misc.shouldShowDamageFloaty(projectile.getSource(), target)) {
                engine.addFloatingDamageText(point, damageDealt, Misc.FLOATY_ARMOR_DAMAGE_COLOR, target, projectile.getSource());
            }
            target.syncWithArmorGridState();
        }
    }
}
