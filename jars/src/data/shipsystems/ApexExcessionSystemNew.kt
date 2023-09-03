package data.shipsystems

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.MutableShipStatsAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.WeaponAPI
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript
import com.fs.starfarer.api.loading.WeaponSlotAPI
import com.fs.starfarer.api.plugins.ShipSystemStatsScript
import com.fs.starfarer.api.util.Misc
import com.fs.starfarer.api.util.WeightedRandomPicker
import data.hullmods.ApexExcessionReactor
import org.dark.shaders.distortion.DistortionShader
import org.dark.shaders.distortion.RippleDistortion
import org.lazywizard.lazylib.MathUtils
import org.lazywizard.lazylib.VectorUtils
import utils.ApexUtils
import utils.ApexUtils.lerp
import java.awt.Color

class ApexExcessionSystemNew: BaseShipSystemScript()
{
    var runOnce = false
    var entropy_spent = 0f
    var doEffects = 0
    var dummy_wep: WeaponAPI? = null

    companion object
    {
        const val ENTROPY_PER_SHOT = 250f
        const val MAX_FLUX_REDUCTION = 0.33f
        const val ENTROPY_TO_FLUX_CONVERSION_RATIO = 0.2f // ie, eating 3000 entropy will convert 1500 hard flux to soft
        val JITTER_COLOR = Color(255, 50, 50, 75)
        var KEY_SHIP = Any()
    }

    override fun apply(
        stats: MutableShipStatsAPI,
        id: String,
        state: ShipSystemStatsScript.State,
        effectLevel: Float
    )
    {
        stats.entity ?: return
        if (!(stats.entity is ShipAPI)) return
        val ship = stats.entity as ShipAPI


        if (!runOnce)
        {
            runOnce = true
            val data = ship.customData[ApexExcessionReactor.KEY] as ApexExcessionReactor.ExcessionData
            entropy_spent = data.entropy
            data.entropy = 0f

            ship.fluxTracker.hardFlux = (ship.fluxTracker.hardFlux - entropy_spent * ENTROPY_TO_FLUX_CONVERSION_RATIO).coerceAtLeast(0f)

            val slot_picker = WeightedRandomPicker<WeaponSlotAPI>()
            for (slot in ship.hullSpec.allWeaponSlotsCopy)
            {
                if (slot.isSystemSlot) slot_picker.add(slot)
            }

            val to_fire = (entropy_spent / ENTROPY_PER_SHOT).toInt()
            dummy_wep = dummy_wep ?: Global.getCombatEngine().createFakeWeapon(ship, "apex_excession_missile")
            for (i in 1..to_fire)
            {
                val slot_to_use = slot_picker.pick()
                val loc = slot_to_use.computePosition(ship)
                val angle = slot_to_use.computeMidArcAngle(ship) + ApexUtils.randBetween(-20f, 20f);
                Global.getCombatEngine().spawnProjectile(ship, dummy_wep, "apex_excession_missile", loc, angle, ship.velocity)
            }


        }

        val flux_mult = lerp(1f, MAX_FLUX_REDUCTION, entropy_spent / ApexExcessionReactor.MAX_ENTROPY)
        stats.energyWeaponFluxCostMod.modifyMult(id, flux_mult)
        stats.missileWeaponFluxCostMod.modifyMult(id, flux_mult)
        stats.ballisticWeaponFluxCostMod.modifyMult(id, flux_mult)
        // vfx
        ship.setJitter(
            KEY_SHIP,
            JITTER_COLOR,
            effectLevel,
            4,
            0f,
            effectLevel * 50f
        )

        // I wrote this code, I can steal it back
        val shipRadius = ship.collisionRadius
        if (doEffects < 10)
        {
            if (doEffects == 0)
            {
                val ripple = RippleDistortion(ship.location, ship.velocity)
                ripple.size = 300f + shipRadius * 1.75f
                ripple.intensity = shipRadius
                ripple.frameRate = 60f
                ripple.fadeInSize(0.75f)
                ripple.fadeOutIntensity(0.5f)
                DistortionShader.addDistortion(ripple)
            }
            for (i in 0..1)
            {
                val particleLoc = MathUtils.getRandomPointInCircle(ship.location, (shipRadius + 300) * 2f)
                val particleVel = MathUtils.getPointOnCircumference(
                    Misc.ZERO,
                    MathUtils.getDistance(particleLoc, ship.location),
                    VectorUtils.getAngle(particleLoc, ship.location)
                )
                Global.getCombatEngine().addSmoothParticle(
                    particleLoc,
                    particleVel,
                    Math.random().toFloat() * 15f + 5f,
                    0.5f,
                    0.75f,
                    Color.white
                )
            }
            doEffects++
        }

        // prevent phasing
        ship.phaseCloak.cooldownRemaining = 0.1f

    }

    override fun unapply(stats: MutableShipStatsAPI, id: String)
    {
        runOnce = false
        doEffects = 0
        stats.energyWeaponFluxCostMod.unmodify(id)
        stats.missileWeaponFluxCostMod.unmodify(id)
        stats.ballisticWeaponFluxCostMod.unmodify(id)
    }

}