package data.shipsystems

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.DamagingProjectileAPI
import com.fs.starfarer.api.combat.MissileAPI
import com.fs.starfarer.api.combat.MutableShipStatsAPI
import com.fs.starfarer.api.combat.OnFireEffectPlugin
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.ShipSystemAPI
import com.fs.starfarer.api.combat.WeaponAPI
import com.fs.starfarer.api.combat.WeaponAPI.WeaponType
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript
import com.fs.starfarer.api.plugins.ShipSystemStatsScript
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import com.fs.starfarer.api.util.WeightedRandomPicker
import org.lazywizard.lazylib.MathUtils
import org.lazywizard.lazylib.VectorUtils
import java.awt.Color
import kotlin.math.max

class ApexBifurcator: BaseShipSystemScript() {
    var runOnce = false
    var shotTimer = IntervalUtil(0f, 1f)
    var beamShotTimer = IntervalUtil(0f, 1f)
    val shotPicker = WeightedRandomPicker<WeaponAPI>()
    var target: ShipAPI? = null
    var beamShots = 0
    var fakegun: WeaponAPI? = null

    companion object {
        const val DPS_DURATION = 3.0f
        const val FIRING_DURATION = 1.0f
        const val SPREAD = 10.0f
    }

    override fun isUsable(system: ShipSystemAPI, ship: ShipAPI): Boolean {
        return if (ship == Global.getCombatEngine().playerShip) {
            true
        } else {
            ship.shipTarget != null && ship.shipTarget.owner != ship.owner
        }
    }

    override fun apply(
        stats: MutableShipStatsAPI,
        id: String,
        state: ShipSystemStatsScript.State,
        effectLevel: Float
    ) {
        val ship = stats.entity as? ShipAPI ?: return
        if (fakegun == null)
            fakegun = Global.getCombatEngine().createFakeWeapon(ship, "apex_python_dummygun")
        if (!runOnce) {
            if (ship.shipTarget != null)
                target = ship.shipTarget

            runOnce = true

            var totalShots = 0f
            for (weapon in ship.allWeapons) {
                if (!weapon.isBeam) {
                    // use dps instead of ROF to account for LINKED weapon barrels
                    val shots = kotlin.math.ceil((DPS_DURATION * (weapon.derivedStats.dps)) / weapon.damage.baseDamage)
                    if (shotPicker.getWeight(weapon) != null) {
                        shotPicker.add(weapon, shots + shotPicker.getWeight(weapon))
                    } else {
                        shotPicker.add(weapon, shots)
                    }
                    totalShots += shots
                } else if (weapon.isBeam)
                {
                    beamShots += (kotlin.math.ceil(weapon.derivedStats.burstDamage / 300f)).toInt()
                }
            }
            shotTimer = IntervalUtil(FIRING_DURATION / totalShots * 0.66f, FIRING_DURATION / totalShots * 1.33f)
            beamShotTimer = IntervalUtil(FIRING_DURATION / beamShots * 0.66f, FIRING_DURATION / beamShots * 1.33f)
        }
        shotTimer.advance(Global.getCombatEngine().elapsedInLastFrame)
        beamShotTimer.advance(Global.getCombatEngine().elapsedInLastFrame)
        if (beamShotTimer.intervalElapsed() && beamShots > 0) {

            beamShots -= 1
            val firePoint = MathUtils.getRandomPointOnCircumference(ship.location, ship.collisionRadius * 1.2f)

            var angle: Float
            if (ship == Global.getCombatEngine().playerShip && target == null) {
                angle = VectorUtils.getAngle(firePoint, ship.mouseTarget) + Misc.random.nextFloat(-SPREAD, SPREAD)
            } else {
                angle = VectorUtils.getAngle(firePoint, target!!.location) + Misc.random.nextFloat(-SPREAD, SPREAD)
            }

            val proj: DamagingProjectileAPI = Global.getCombatEngine().spawnProjectile(
                ship,
                fakegun,
                "apex_python_dummygun",
                firePoint,
                angle,
                Misc.ZERO
            ) as DamagingProjectileAPI
            // poof!
            Global.getCombatEngine().addSwirlyNebulaParticle(
                firePoint,
                Misc.ZERO,
                6f,
                9f,
                0.1f,
                0.2f,
                0.33f,
                proj.projectileSpec?.fringeColor ?: Color.MAGENTA,
                true
            )
            // sfx
            if (proj.damage.baseDamage < 150)
                Global.getSoundPlayer().playSound("minipulser_fire", 1.0f, 1.0f, firePoint, Misc.ZERO)
            else
                Global.getSoundPlayer().playSound("voidblaster_fire", 1.0f, 1.0f, firePoint, Misc.ZERO)
        }
        if (shotTimer.intervalElapsed()) {
            if (!shotPicker.isEmpty()) {
                // pick random shot to fire
                val wep = shotPicker.pick()
                val firePoint = MathUtils.getRandomPointOnCircumference(ship.location, ship.collisionRadius*1.2f)

                var angle: Float
                if (ship == Global.getCombatEngine().playerShip && target == null) {
                    angle = VectorUtils.getAngle(firePoint, ship.mouseTarget) + Misc.random.nextFloat(-SPREAD, SPREAD)
                } else {
                    angle = VectorUtils.getAngle(firePoint, target!!.location) + Misc.random.nextFloat(-SPREAD, SPREAD)
                }

                val proj: DamagingProjectileAPI = Global.getCombatEngine().spawnProjectile(
                    ship,
                    wep,
                    wep.id,
                    firePoint,
                    angle,
                    Misc.ZERO
                ) as DamagingProjectileAPI

                if (proj !is MissileAPI) // it's damagingprojectileapi
                {
                    val onfire: OnFireEffectPlugin? = proj.projectileSpec.onFireEffect
                    onfire?.onFire(proj, wep, Global.getCombatEngine())
                }
                // poof!

                Global.getCombatEngine().addSwirlyNebulaParticle(
                    firePoint,
                    Misc.ZERO,
                    6f,
                    9f,
                    0.1f,
                    0.2f,
                    0.33f,
                    proj.projectileSpec?.fringeColor ?: Color.MAGENTA,
                    true
                )
                // sfx
                if (proj.damage.baseDamage < 150)
                    Global.getSoundPlayer().playSound("minipulser_fire", 1.0f, 1.0f, firePoint, Misc.ZERO)
                else
                    Global.getSoundPlayer().playSound("voidblaster_fire", 1.0f, 1.0f, firePoint, Misc.ZERO)
            }
        }
    }

    override fun unapply(stats: MutableShipStatsAPI?, id: String?) {
        target = null
        runOnce = false
        shotPicker.clear()
        beamShots = 0
    }
}