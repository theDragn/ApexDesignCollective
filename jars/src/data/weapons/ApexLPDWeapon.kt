package data.weapons

import com.fs.starfarer.api.combat.*
import com.fs.starfarer.api.loading.DamagingExplosionSpec
import data.weapons.proj.ai.ApexLPDLoiterAI
import data.weapons.proj.ai.ApexLPDSystem
import java.awt.Color

class ApexLPDWeapon: OnFireEffectPlugin, EveryFrameWeaponEffectPlugin
{
    val loitering = mutableSetOf<MissileAPI>()

    override fun onFire(projectile: DamagingProjectileAPI, weapon: WeaponAPI, engine: CombatEngineAPI)
    {
        // get system controller plugin if it exists
        var plugin: ApexLPDSystem? = null
        if (engine.customData.containsKey("apex_LPDS"))
        {
            plugin = engine.customData["apex_LPDS"] as ApexLPDSystem
        } else { // if it doesn't exist, start it up and store the reference
            plugin = ApexLPDSystem(weapon.ship.owner)
            engine.addPlugin(plugin)
            engine.customData["apex_LPDS"] = plugin
        }
        //(projectile as MissileAPI).missileAI = ApexLPDLoiterAI(projectile, weapon.ship)
        // done in modplugin
        projectile as MissileAPI // this works??
        plugin.available_missiles.add(projectile)
        loitering.add(projectile)
    }

    override fun advance(amount: Float, engine: CombatEngineAPI, weapon: WeaponAPI)
    {
        if (loitering.size >= 6)
            weapon.setForceNoFireOneFrame(true)
        else
            weapon.setForceFireOneFrame(true)
        loitering.removeAll { !engine.isEntityInPlay(it) }
    }

}