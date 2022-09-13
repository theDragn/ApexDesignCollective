package data.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.plugins.ShipSystemStatsScript;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import data.ApexUtils;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;

import static plugins.ApexModPlugin.POTATO_MODE;

public class ApexPhaseBanish extends BaseShipSystemScript
{
    boolean runOnce = false;
    public static final Color JITTER_COLOR = new Color(0,157,255,75);
    public static final Color JITTER_UNDER_COLOR = new Color(0,157,255,155);
    public static final float RANGE = 750f;
    public static final float MAX_DAMAGE = 1250f;

    public void apply(MutableShipStatsAPI stats, String id, ShipSystemStatsScript.State state, float effectLevel)
    {
        ShipAPI ship = null;
        CombatEngineAPI engine = Global.getCombatEngine();
        if (stats.getEntity() instanceof ShipAPI)
            ship = (ShipAPI)stats.getEntity();
        if (ship == null)
            return;
        if (!runOnce)
        {
            runOnce = true;
            float rangeSquared = ship.getMutableStats().getSystemRangeBonus().computeEffective(RANGE);
            rangeSquared = rangeSquared * rangeSquared;
            WeightedRandomPicker<CombatEntityAPI> banishTargets = new WeightedRandomPicker<>();
            for (MissileAPI missile : engine.getMissiles())
            {
                if (missile.getOwner() != ship.getOwner() && MathUtils.getDistanceSquared(missile.getLocation(), ship.getLocation()) < rangeSquared)
                {
                    banishTargets.add(missile, getWeight(missile, ship));
                }
            }
            for (DamagingProjectileAPI proj : engine.getProjectiles())
            {
                if (proj.getOwner() != ship.getOwner() && MathUtils.getDistanceSquared(proj.getLocation(), ship.getLocation()) < rangeSquared)
                {
                    banishTargets.add(proj, getWeight(proj, ship));
                }
            }
            for (ShipAPI otherShip : engine.getShips())
            {
                if (!otherShip.getHullSize().equals(ShipAPI.HullSize.FIGHTER) || otherShip.getOwner() == ship.getOwner())
                    continue;
                if (MathUtils.getDistanceSquared(otherShip.getLocation(), ship.getLocation()) < rangeSquared)
                    banishTargets.add(otherShip, 28000);
            }
            if (banishTargets.isEmpty())
                return;
            float remainingDamage = MAX_DAMAGE;
            while (remainingDamage > 0 && !banishTargets.isEmpty())
            {
                CombatEntityAPI target = banishTargets.pickAndRemove();
                if (target instanceof MissileAPI)
                {
                    doRemoveVFX(target);
                    remainingDamage -= target.getHitpoints();
                    engine.removeEntity(target);
                }
                else if (target instanceof DamagingProjectileAPI)
                {
                    doRemoveVFX(target);
                    remainingDamage -= ((DamagingProjectileAPI)target).getDamageAmount() * 0.66f;
                    engine.removeEntity(target);
                }
                else if (target instanceof ShipAPI)
                {
                    remainingDamage -= 250f;
                    doRemoveVFX(target);
                    engine.applyDamage(target, target.getLocation(), 500, DamageType.ENERGY, 100, false, false, ship);
                }
            }
        }

        ship.setJitterUnder(this, JITTER_UNDER_COLOR, effectLevel, 11, 0f, 3f + effectLevel * 25f);
        ship.setJitter(this, JITTER_COLOR, effectLevel, 4, 0f, 0 + effectLevel * 25f);
    }

    private static float getWeight(CombatEntityAPI target, CombatEntityAPI self)
    {
        float angleTo = VectorUtils.getAngle(target.getLocation(), self.getLocation());
        float delta = MathUtils.getShortestRotation(target.getFacing(), angleTo);
        return (180 - delta) * (180 - delta); // maximum weight is facing directly at ship, minimum weight is facing directly away from ship
    }

    private void doRemoveVFX(CombatEntityAPI entity)
    {
        // draw removal effects
        if (!POTATO_MODE)
        {
            ApexUtils.addWaveDistortion(entity.getLocation(), 30f, 30f, 0.1f);
            ApexUtils.plasmaEffects(entity, JITTER_UNDER_COLOR, Math.min(entity.getCollisionRadius() * 2f, 10f));
            Global.getCombatEngine().addHitParticle(entity.getLocation(), entity.getVelocity(), 100f, 1.0f, 0.1f, JITTER_UNDER_COLOR);
        }
    }

    public void unapply(MutableShipStatsAPI stats, String id)
    {
        runOnce = false;
    }
}
