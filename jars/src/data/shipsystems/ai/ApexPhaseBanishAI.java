package data.shipsystems.ai;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import data.shipsystems.ApexPhaseBanish;
import org.lazywizard.lazylib.CollisionUtils;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.List;

public class ApexPhaseBanishAI implements ShipSystemAIScript
{
    private ShipAPI ship;
    private ShipSystemAPI system;
    private CombatEngineAPI engine;
    private ShipwideAIFlags flags;

    private IntervalUtil timer = new IntervalUtil(0.25f, 0.33f);

    @Override
    public void init(ShipAPI ship, ShipSystemAPI system, ShipwideAIFlags flags, CombatEngineAPI engine)
    {
        this.ship = ship;
        this.system = system;
        this.engine = engine;
        this.flags = flags;
    }

    @Override
    public void advance(float amount, Vector2f missileDangerDir, Vector2f collisionDangerDir, ShipAPI target)
    {
        if (!ship.isAlive() || engine.isPaused() || system.getAmmo() == 0 || system.isCoolingDown() || ship.isPhased())
            return;
        timer.advance(amount);
        if (timer.intervalElapsed())
        {
            MissileAPI nearestMissile = AIUtils.getNearestEnemyMissile(ship);
            if (nearestMissile != null && MathUtils.getDistanceSquared(nearestMissile.getLocation(), ship.getLocation()) < 500f * 500f)
            {
                ship.useSystem();
                return;
            }
            ShipAPI nearestEnemy = AIUtils.getNearestEnemy(ship);
            if (nearestEnemy != null && nearestEnemy.getHullSize().equals(ShipAPI.HullSize.FIGHTER) && MathUtils.getDistanceSquared(nearestEnemy.getLocation(), ship.getLocation()) < 500f * 500f)
            {
                ship.useSystem();
                return;
            }
            float totalDamage = 0;
            List<DamagingProjectileAPI> nearbyProjectiles = CombatUtils.getProjectilesWithinRange(ship.getLocation(), 500f);
            if (nearbyProjectiles.isEmpty())
                return;
            for (DamagingProjectileAPI proj : nearbyProjectiles)
            {
                if (proj.getOwner() == ship.getOwner())
                    continue;
                Vector2f dest = new Vector2f(proj.getVelocity());
                dest.scale(7.5f);
                if (!(CollisionUtils.getCollides(proj.getLocation(), Vector2f.add(proj.getLocation(), dest, new Vector2f()), ship.getLocation(), ship.getCollisionRadius())))
                    continue;
                if (proj.getDamageType().equals(DamageType.HIGH_EXPLOSIVE))
                    totalDamage += 2 * proj.getDamageAmount();
                else if (proj.getDamageType().equals(DamageType.KINETIC))
                    totalDamage += 0.5 * proj.getDamageAmount();
                else if (proj.getDamageType().equals(DamageType.ENERGY) || proj.getDamageAmount() >= 75f)
                    totalDamage += 0.5 * proj.getDamageAmount();
                else
                    totalDamage += 0.25 * proj.getDamageAmount();
            }
            if (totalDamage > 200f)
                ship.useSystem();
        }
    }
}
