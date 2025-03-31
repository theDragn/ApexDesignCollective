package data.weapons.proj;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;

public class ApexDEMOnFire implements OnFireEffectPlugin {
    public void onFire(DamagingProjectileAPI projectile, WeaponAPI weapon, CombatEngineAPI engine) {
        if (!(projectile instanceof MissileAPI)) return;

        MissileAPI missile = (MissileAPI) projectile;

        ShipAPI ship = null;
        if (weapon != null) ship = weapon.getShip();
        if (ship == null) return;

        ApexDEMScript script = new ApexDEMScript(missile, ship, weapon);
        Global.getCombatEngine().addPlugin(script);
    }
}
