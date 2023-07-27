package data.weapons.proj

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.CombatEngineAPI
import com.fs.starfarer.api.combat.DamagingProjectileAPI
import com.fs.starfarer.api.combat.OnFireEffectPlugin
import com.fs.starfarer.api.combat.WeaponAPI

class ApexInsPlasmaticOnFire: OnFireEffectPlugin
{
    override fun onFire(projectile: DamagingProjectileAPI, weapon: WeaponAPI?, engine: CombatEngineAPI)
    {
        weapon ?: return
        if (weapon.ammo % 8 == 1 || weapon.ammo % 8 == 0)
        {
            Global.getSoundPlayer().playSound("apex_ins_plasma_charged", 1.0f, 1.0f, weapon.location, weapon.ship.velocity)
            engine.spawnProjectile(weapon.ship, weapon, "apex_ins_plasma_emp", projectile.location, projectile.facing, weapon.ship.velocity)
            engine.removeEntity(projectile)
        }
    }
}