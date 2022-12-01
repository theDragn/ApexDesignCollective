package data.shipsystems.ai;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lazywizard.lazylib.CollisionUtils;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.HashMap;
import java.util.Map;

public class ApexThornsAI implements ShipSystemAIScript
{
    private ShipAPI ship;
    private ShipSystemAPI system;
    private CombatEngineAPI engine;
    private ShipwideAIFlags flags;
    private float evaluationRadiusSquared;
    private IntervalUtil timer = new IntervalUtil(0.25f, 0.5f);
    private static Map<DamageType, Float> armorDamageTypeMult = new HashMap<>();
    static {
        armorDamageTypeMult.put(DamageType.HIGH_EXPLOSIVE, 2f);
        armorDamageTypeMult.put(DamageType.ENERGY, 1f);
        armorDamageTypeMult.put(DamageType.KINETIC, 0.5f);
        armorDamageTypeMult.put(DamageType.FRAGMENTATION, 0.25f);
        armorDamageTypeMult.put(DamageType.OTHER, 1f);
    }
    private static Map<DamageType, Float> shieldDamageTypeMult = new HashMap<>();
    static {
        shieldDamageTypeMult.put(DamageType.HIGH_EXPLOSIVE, 0.5f);
        shieldDamageTypeMult.put(DamageType.ENERGY, 1f);
        shieldDamageTypeMult.put(DamageType.KINETIC, 2f);
        shieldDamageTypeMult.put(DamageType.FRAGMENTATION, 0.25f);
        shieldDamageTypeMult.put(DamageType.OTHER, 1f);
    }

    @Override
    public void init(ShipAPI ship, ShipSystemAPI system, ShipwideAIFlags flags, CombatEngineAPI engine)
    {
        this.engine = engine;
        this.system = system;
        this.ship = ship;
        this.flags = flags;
        evaluationRadiusSquared = (ship.getCollisionRadius() + 750f) * (ship.getCollisionRadius() + 750f);
    }

    @Override
    public void advance(float amount, Vector2f missileDangerDir, Vector2f collisionDangerDir, ShipAPI target)
    {
        // sanity checks
        if (ship == null || engine.isPaused() || !ship.isAlive())
            return;
        // more sanity checks
        if (system.isCoolingDown() || system.isActive() || system.isOutOfAmmo())
            return;
        timer.advance(amount);
        if (timer.intervalElapsed() && shouldActivate(ship, evaluationRadiusSquared))
            ship.useSystem();
    }

    public boolean shouldActivate(ShipAPI ship, float evaluationRadiusSquared)
    {
        // if we're about to overload from incoming shit, hit da bricks
        if (ship.getFluxLevel() > 0.9f && ship.getAIFlags().hasFlag(ShipwideAIFlags.AIFlags.HAS_INCOMING_DAMAGE))
            return true;
        // if our flux is low, don't bother using the system, since we're out of danger and there's not much sense in wasting it on a target that isn't threatening us
        if (ship.getFluxLevel() < 0.2f && !(ship.getShield() == null || ship.getShield().getType().equals(ShieldAPI.ShieldType.NONE)))
            return false;
        // damage with armor damage below this number can be safely tanked with damper
        float dangerThreshold = ship.getArmorGrid().getArmorRating()*0.33f;

        float totalIncomingShieldDamage = 0f;
        float totalIncomingArmorDamage = 0f;
        float totalIncomingDangerArmorDamage = 0f;
        for (DamagingProjectileAPI proj : engine.getProjectiles())
        {
            // if projectile is on our team, skip
            if (proj.getOwner() == ship.getOwner())
                continue;
            // if projectile is outside of evaluation range, skip
            // comparing squared distance so that we can avoid a square root
            if (MathUtils.getDistanceSquared(proj.getLocation(), ship.getLocation()) > evaluationRadiusSquared)
                continue;
            // if projectile isn't going to hit us within 7.5 seconds, skip
            Vector2f dest = new Vector2f(proj.getVelocity());
            dest.scale(7.5f);
            if (!(CollisionUtils.getCollides(proj.getLocation(), Vector2f.add(proj.getLocation(), dest, new Vector2f()), ship.getLocation(), ship.getCollisionRadius())))
                continue;
            // okay, projectile will probably hit us. Can we safely tank damage with damper field?
            // if we can, add it to irrelevant damage
            float armorDamage = proj.getDamageAmount() * armorDamageTypeMult.get(proj.getDamageType());
            totalIncomingArmorDamage += armorDamage;
            if (armorDamage > dangerThreshold)
                totalIncomingDangerArmorDamage += armorDamage;
            // figure out how much damage we'd take if it hit shields
            totalIncomingShieldDamage += proj.getDamageAmount() * shieldDamageTypeMult.get(proj.getDamageType());
        }
        // be generous about activation on shieldless ships
        if ((ship.getShield() == null || ship.getShield().getType().equals(ShieldAPI.ShieldType.NONE)) && totalIncomingArmorDamage > 300)
            return true;

        // if the incoming damage that we can't safely take on armor (with the system) would overload us, activate system anyway- better to take damage than overload
        if (totalIncomingShieldDamage * ship.getMutableStats().getShieldDamageTakenMult().computeMultMod() > ship.getMaxFlux() * (1f - ship.getFluxLevel()))
            return true;
        // if our HP is low, we've got incoming damage, and we're close to overload
        if (totalIncomingArmorDamage + totalIncomingShieldDamage > 100 && ship.getHullLevel() < 0.25f && ship.getFluxLevel() > 0.9f)
            return true;
        // if there's incoming damage that we can't safely tank, don't activate damper
        if (totalIncomingDangerArmorDamage > 0f)
            return false;
        // if there's a good bit of damage we could take on armor, use it
        if (totalIncomingArmorDamage < totalIncomingShieldDamage)
            return true;
        // well, we got here without triggering any conditions, so don't use it
        return false;
    }
}
