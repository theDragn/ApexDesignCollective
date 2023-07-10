package data.weapons

import com.fs.starfarer.api.combat.CombatEngineAPI
import com.fs.starfarer.api.combat.DamagingProjectileAPI
import com.fs.starfarer.api.combat.OnFireEffectPlugin
import com.fs.starfarer.api.combat.WeaponAPI
import utils.ApexGenericMuzzleFlash
import java.awt.Color

class ApexDartgunMuzzleFlash: ApexGenericMuzzleFlash(), OnFireEffectPlugin
{
    // commented stuff is
    //var lastProj: DamagingProjectileAPI? = null
    //var adjust = false
    override fun onFire(proj: DamagingProjectileAPI?, weapon: WeaponAPI?, engine: CombatEngineAPI?)
    {
        proj ?: return; weapon ?: return; engine ?: return
        super.spawnSmoke(proj, weapon, engine)

        // TODO: smoke vfx
    }

    override val USED_IDS = listOf(
        "smoke1",
        "smoke2",
        "smoke3",
        "flash",
        "flashcore"
    )
    override val PARTICLE_TYPE = mapOf(
        "flash" to "HIT",
        "flashcore" to "HIT"
    )
    override val PARTICLE_COUNT = mapOf(
        "smoke3" to 10,
        "flash" to 1,
        "flashcore" to 1
    )
    override val PARTICLE_COLOR = mapOf(
        "smoke1" to Color(140,130,120,128),
        "smoke2" to Color(90, 80, 80, 118),
        "flash" to Color(255, 170, 60, 255),
        "flashcore" to Color(255, 240, 230, 255)
    )
    override val PARTICLE_OFFSET_MAX = mapOf(
        "smoke1" to 30f,
        "smoke2" to 20f,
        "smoke3" to 5f,
        "flash" to 0f,
        "flashcore" to 0f
    )
    override val PARTICLE_VELOCITY_MAX = mapOf(
        "smoke3" to 20f,
        "flash" to 0f,
        "flashcore" to 0f
    )
    override val PARTICLE_DURATION_MIN = mapOf(
        "flash" to 0.1f,
        "flashcore" to 0.05f
    )
    override val PARTICLE_DURATION_MAX = mapOf(
        "flash" to 0.1f,
        "flashcore" to 0.05f
    )
    override val PARTICLE_SIZE_MIN = mapOf(
        "flash" to 70f,
        "flashcore" to 50f
    )
    override val PARTICLE_SIZE_MAX = mapOf(
        "flash" to 70f,
        "flashcore" to 50f
    )
    override val PARTICLE_ARC = mapOf(
        "smoke3" to 160f
    )
}