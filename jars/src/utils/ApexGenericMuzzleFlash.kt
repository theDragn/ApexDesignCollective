package utils

import com.fs.starfarer.api.combat.CombatEngineAPI
import com.fs.starfarer.api.combat.DamagingProjectileAPI
import com.fs.starfarer.api.combat.OnFireEffectPlugin
import com.fs.starfarer.api.combat.WeaponAPI
import com.fs.starfarer.api.util.Misc
import org.lazywizard.lazylib.MathUtils
import org.lazywizard.lazylib.ext.plus
import org.lazywizard.lazylib.ext.rotate
import org.lwjgl.util.vector.Vector2f
import java.awt.Color

// an adaptation of Nicke's muzzle flash script, as an extensible kotlin class
// can call this for whatever you want, really
open class ApexGenericMuzzleFlash
{
    fun spawnSmoke(proj: DamagingProjectileAPI?, weapon: WeaponAPI?, engine: CombatEngineAPI?)
    {
        proj ?: return; weapon ?: return; engine ?: return; proj.source ?: return
        for (ID in USED_IDS)
        {
            val cullDist = PARTICLE_CULL_DISTANCE[ID] ?: 600f
            if (!engine.viewport.isNearViewport(weapon.location, cullDist)) return
            val spawnOffset = SPAWN_OFFSET[ID] ?: Vector2f(0f, 0f)
            val type = PARTICLE_TYPE[ID] ?: "SMOKE"
            val color = PARTICLE_COLOR[ID] ?: Color(128,128,128,128)
            val sizeMin = PARTICLE_SIZE_MIN[ID] ?: 5f
            val sizeMax = PARTICLE_SIZE_MAX[ID] ?: 20f
            val velMin = PARTICLE_VELOCITY_MIN[ID] ?: 0f
            val velMax = PARTICLE_VELOCITY_MAX[ID] ?: 40f
            val arc = PARTICLE_ARC[ID] ?: 0f
            val durMin = PARTICLE_DURATION_MIN[ID] ?: 1.5f
            val durMax = PARTICLE_DURATION_MAX[ID] ?: 2f
            val offsetMin = PARTICLE_OFFSET_MIN[ID] ?: 0f
            val offsetMax = PARTICLE_OFFSET_MAX[ID] ?: 40f
            val numParticles = PARTICLE_COUNT[ID] ?: 15

            val startLoc = spawnOffset.rotate(proj.facing) + proj.location
            val startFacing = proj.facing

            for (i in 1..numParticles)
            {
                val facing = startFacing + MathUtils.getRandomNumberInRange(-arc/2f, arc/2f)
                val extraDist = MathUtils.getRandomNumberInRange(offsetMin, offsetMax)
                val spawnLoc = MathUtils.getPointOnCircumference(startLoc, extraDist, facing)
                val randVel = MathUtils.getRandomNumberInRange(velMin, velMax)
                val velocity = MathUtils.getPointOnCircumference(Misc.ZERO, randVel, facing) + proj.source.velocity
                val size = MathUtils.getRandomNumberInRange(sizeMin, sizeMax)
                val duration = MathUtils.getRandomNumberInRange(durMin, durMax)
                when (type)
                {
                    "SMOOTH" -> engine.addSmoothParticle(spawnLoc, velocity, size, 1f, duration, color)
                    "HIT" -> engine.addHitParticle(spawnLoc, velocity, size, 1f, duration, color)
                    "SMOKE" -> engine.addSmokeParticle(spawnLoc, velocity, size, 1f, duration, color)
                    "NEBULA" -> engine.addNebulaParticle(spawnLoc, velocity, size, 1f, 1f, 1f, duration, color)
                    "SWIRLY_NEBULA" -> engine.addSwirlyNebulaParticle(spawnLoc, velocity, size, 1f, 1f, 1f, duration, color, false)
                    "NEGATIVE_NEBULA" -> engine.addNegativeNebulaParticle(spawnLoc, velocity, size, 1f, 1f, 1f, duration, color)
                    "NEGATIVE_SWIRLY_NEBULA" -> engine.addNegativeSwirlyNebulaParticle(spawnLoc, velocity, size, 1f, 1f, 1f, duration, color)
                }
            }

        }
    }
    // for when you want the muzzle flash without a proj (or weird spawn conditions)
    fun spawnSmoke(loc: Vector2f?, facing: Float?, engine: CombatEngineAPI?, baseVel: Vector2f?)
    {
        loc ?: return; facing ?: return; engine ?: return

        for (ID in USED_IDS)
        {
            val cullDist = PARTICLE_CULL_DISTANCE[ID] ?: 600f
            if (!engine.viewport.isNearViewport(loc, cullDist)) return
            val spawnOffset = SPAWN_OFFSET[ID] ?: Vector2f(0f, 0f)
            val type = PARTICLE_TYPE[ID] ?: "SMOKE"
            val color = PARTICLE_COLOR[ID] ?: Color(128,128,128,128)
            val sizeMin = PARTICLE_SIZE_MIN[ID] ?: 5f
            val sizeMax = PARTICLE_SIZE_MAX[ID] ?: 20f
            val velMin = PARTICLE_VELOCITY_MIN[ID] ?: 0f
            val velMax = PARTICLE_VELOCITY_MAX[ID] ?: 40f
            val arc = PARTICLE_ARC[ID] ?: 0f
            val durMin = PARTICLE_DURATION_MIN[ID] ?: 1.5f
            val durMax = PARTICLE_DURATION_MAX[ID] ?: 2f
            val offsetMin = PARTICLE_OFFSET_MIN[ID] ?: 0f
            val offsetMax = PARTICLE_OFFSET_MAX[ID] ?: 40f
            val numParticles = PARTICLE_COUNT[ID] ?: 15

            val startLoc = spawnOffset.rotate(facing) + loc
            val startFacing = facing

            for (i in 1..numParticles)
            {
                val facing = startFacing + MathUtils.getRandomNumberInRange(-arc/2f, arc/2f)
                val extraDist = MathUtils.getRandomNumberInRange(offsetMin, offsetMax)
                val spawnLoc = MathUtils.getPointOnCircumference(startLoc, extraDist, facing)
                val randVel = MathUtils.getRandomNumberInRange(velMin, velMax)
                val velocity = MathUtils.getPointOnCircumference(Misc.ZERO, randVel, facing) + (baseVel ?: Misc.ZERO)
                val size = MathUtils.getRandomNumberInRange(sizeMin, sizeMax)
                val duration = MathUtils.getRandomNumberInRange(durMin, durMax)
                when (type)
                {
                    "SMOOTH" -> engine.addSmoothParticle(spawnLoc, velocity, size, 1f, duration, color)
                    "HIT" -> engine.addHitParticle(spawnLoc, velocity, size, 1f, duration, color)
                    "SMOKE" -> engine.addSmokeParticle(spawnLoc, velocity, size, 1f, duration, color)
                    "NEBULA" -> engine.addNebulaParticle(spawnLoc, velocity, size, 1f, 1f, 1f, duration, color)
                    "SWIRLY_NEBULA" -> engine.addSwirlyNebulaParticle(spawnLoc, velocity, size, 1f, 1f, 1f, duration, color, false)
                    "NEGATIVE_NEBULA" -> engine.addNegativeNebulaParticle(spawnLoc, velocity, size, 1f, 1f, 1f, duration, color)
                    "NEGATIVE_SWIRLY_NEBULA" -> engine.addNegativeSwirlyNebulaParticle(spawnLoc, velocity, size, 1f, 1f, 1f, duration, color)
                }
            }

        }
    }

    /* specifies which IDs will be used
    - USED_IDS can be anything you want
    -
     */
    open val USED_IDS = listOf<String>()
    // how many of each particle to spawn when the projectile is created
    open val PARTICLE_COUNT = mapOf<String, Int>()
    // fixed offset for where to spawn particles
    // probably not needed for most guns; particles will spawn where the projectile is
    // could maybe use for side/rear vents?
    open val SPAWN_OFFSET = mapOf<String, Vector2f>()
    // options are SMOOTH, HIT, SMOKE, NEBULA, SWIRLY_NEBULA, NEGATIVE_NEBULA, NEGATIVE_SWIRLY_NEBULA
    open val PARTICLE_TYPE = mapOf<String, String>()
    open val PARTICLE_COLOR = mapOf<String, Color>()
    open val PARTICLE_SIZE_MIN = mapOf<String, Float>()
    open val PARTICLE_SIZE_MAX = mapOf<String, Float>()
    open val PARTICLE_VELOCITY_MIN = mapOf<String, Float>()
    open val PARTICLE_VELOCITY_MAX = mapOf<String, Float>()
    // randomizes velocity vector within a cone of this many degrees (ie, n/2 to the left and n/2 to the right)
    open val PARTICLE_ARC = mapOf<String, Float>()
    open val PARTICLE_DURATION_MIN = mapOf<String, Float>()
    open val PARTICLE_DURATION_MAX = mapOf<String, Float>()
    // linear offset controls - spawns particle along its velocity vector
    open val PARTICLE_OFFSET_MIN = mapOf<String, Float>()
    open val PARTICLE_OFFSET_MAX = mapOf<String, Float>()
    // how far off-screen does the camera have to be before it'll stop spawning particles
    open val PARTICLE_CULL_DISTANCE = mapOf<String, Float>()
}