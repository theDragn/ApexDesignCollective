package data.shipsystems.ai

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.*
import com.fs.starfarer.api.combat.ShipAPI.HullSize
import com.fs.starfarer.api.util.IntervalUtil
import org.lazywizard.lazylib.CollisionUtils
import org.lazywizard.lazylib.MathUtils
import org.lwjgl.util.vector.Vector2f
import utils.ApexUtils

class ApexInsDamperAI: ShipSystemAIScript
{
    var engine: CombatEngineAPI? = null
    var system: ShipSystemAPI? = null
    var ship: ShipAPI? = null
    var flags: ShipwideAIFlags? = null
    var evaluationRadiusSquared = 0f
    val timer = IntervalUtil(0.25f, 0.5f)

    override fun init(ship: ShipAPI, system: ShipSystemAPI, flags: ShipwideAIFlags, engine: CombatEngineAPI)
    {
        this.engine = engine
        this.system = system
        this.ship = ship
        this.flags = flags
        evaluationRadiusSquared = (ship.collisionRadius + 750f) * (ship.collisionRadius + 750f)
    }

    override fun advance(amount: Float, missileDangerDir: Vector2f?, collisionDangerDir: Vector2f?, target: ShipAPI?)
    {
        if (ship == null || engine!!.isPaused || !ship!!.isAlive) return
        if (system!!.isCoolingDown || system!!.isActive || system!!.isOutOfAmmo) return

        timer.advance(amount)
        if (timer.intervalElapsed() && shouldActivate(ship!!, evaluationRadiusSquared)) ship!!.useSystem()
    }

    private fun shouldActivate(ship: ShipAPI, evaluationRadiusSquared: Float): Boolean
    {
        var missingArmor = 0f
        val grid = ship.armorGrid
        val gW = grid.grid.size
        val gH = grid.grid[0].size
        val maxArmor = grid.maxArmorInCell
        for (x in 0 until gW)
            for (y in 0 until gH)
                missingArmor += maxArmor - grid.getArmorValue(x, y)
        if (!ship.hullSize.equals(HullSize.CAPITAL_SHIP))
            missingArmor *= 2
        // this isn't exact, just a ballpark to determine if we should activate damper
        var incomingArmorDamage = 0f
        // expensive, but there shouldn't be very many of these ships and this check doesn't run that often
        for (proj in Global.getCombatEngine().projectiles)
        {
            if (proj.owner != ship.owner) continue
            if (MathUtils.getDistanceSquared(proj.location, ship.location) > evaluationRadiusSquared) continue
            // collision check
            // extend velocity vector 4 seconds in current direction and see if that intersects with the ship
            val dest = Vector2f(proj.velocity)
            dest.scale(4f)
            if (!CollisionUtils.getCollides(
                    proj.location,
                    Vector2f.add(proj.location, dest, Vector2f()),
                    ship.location,
                    ship.collisionRadius
                )
            ) continue
            // okay, projectile will hit us
            // do some bootleg math to represent how dangerous high-hit-strength shots are
            // and how un-dangerous low-power shots are
            var projDam = proj.damageType.armorMult * proj.damageAmount
            // account for special armor hullmod's reduction
            if (projDam > 750) projDam = 750 + (projDam - 750) * 0.2f
            if (projDam < 100) projDam *= 0.66f
            if (projDam > 400) projDam *= 1.5f
            incomingArmorDamage += projDam
        }
        // same but for beams
        for (beam in Global.getCombatEngine().beams)
        {
            // skip beams that aren't enemy
            if (beam.source is ShipAPI && beam.source.owner == ship.owner) continue
            // skip beams that are outside eval range
            if (MathUtils.getDistanceSquared(beam.to, ship.location) > evaluationRadiusSquared) continue
            // skip beams that aren't going to touch us
            if (!CollisionUtils.isPointWithinCollisionCircle(beam.to, ship)) continue
            // if an enemy beam's endpoint is within our collision circle, it's almost certainly hitting us
            var beamDam = beam.damage.damage * beam.damage.type.armorMult
            if (beamDam > 750) beamDam = 750 + (beamDam -750) * 0.2f
            if (beamDam < 100) beamDam *= 0.66f
            if (beamDam > 400) beamDam *= 1.5f
            incomingArmorDamage += beamDam
        }
        //print("incoming armor damage = $incomingArmorDamage")
        // if we're missing a bunch of armor, incoming shots will do more damage
        // also encourages use for mid-fight armor regen
        if (missingArmor > 2000) incomingArmorDamage *= 1.5f
        if (missingArmor > 4000) incomingArmorDamage *= 1.5f
        // okay, actual evaluation time now
        // simple, activate if we're in a good amount of danger.
        // for a benchmark- one hellbore shot (750 HE) evaluates to 1350, one gauss evaluates to 350
        if (incomingArmorDamage > 1300) return true
        // if on full charges, consider using system just to repair armor
        if (ship.system.ammo == ship.system.maxAmmo)
        {
            if (missingArmor > 2500) return true
            // be more aggressive about use for repair if we're low on hull
            if (ship.hullLevel < 0.5 && missingArmor > 1500) return true
        }
        // if we're in a fight and full on flux, use it to dump flux
        if (incomingArmorDamage > 300 && ship.fluxLevel > 0.95) return true
        // if we're missing a ton of armor and are on full charges, don't bother reserving them, just repair
        if (missingArmor > 4000 && ship.system.ammo == ship.system.maxAmmo) return true
        // if hull is low, be more aggressive about use
        if (ship.hullLevel < 0.5 && incomingArmorDamage > 1000) return true
        // if hull is *very* low and we're under any significant threat, use it
        if (ship.hullLevel < 0.25 && incomingArmorDamage > 500) return true
        if (ship.hullLevel < 0.1 && incomingArmorDamage > 0) return true
        // okay, if we're here we don't have any reason to use it
        return false
    }
}