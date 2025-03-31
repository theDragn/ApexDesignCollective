package data.weapons

import com.fs.starfarer.api.combat.*
import com.fs.starfarer.api.graphics.SpriteAPI
import java.awt.Color

class ApexPlasmaRifleEffects: WeaponEffectPluginWithInit, EveryFrameWeaponEffectPlugin, OnFireEffectPlugin
{
    // the idea for the sprite reversing comes from Tecrys; this code is my own
    // but honestly it's not that different, there's only a few ways to skin a cat

    override fun advance(amount: Float, engine: CombatEngineAPI, weapon: WeaponAPI)
    {
    }

    override fun init(weapon: WeaponAPI)
    {
        val isfront = !(weapon.slot.angle > 90.1 || weapon.slot.angle > 90.1)
        if ((weapon.location.y > 0f && isfront) || (weapon.location.y < 0f && !isfront))
        {
            mirror(weapon.sprite)
            mirror(weapon.glowSpriteAPI)
        }
    }

    fun mirror(spr: SpriteAPI?)
    {
        spr ?: return
        spr.width = -spr.width
        spr.setCenter(-spr.centerX, spr.centerY)
    }

    override fun onFire(proj: DamagingProjectileAPI, weapon: WeaponAPI, engine: CombatEngineAPI)
    {
        // do visual fx
        engine.addSwirlyNebulaParticle(
            proj.location,
            proj.source.velocity,
            6f,
            9f,
            0.1f,
            0.2f,
            0.33f,
            Color(154,10,255),
            true
        )
        engine.addSmoothParticle(
            proj.location,
            proj.source.velocity,
            12f,
            0.75f,
            0.1f,
            0.33f / 2,
            Color(154,10,255)
        )
    }
}