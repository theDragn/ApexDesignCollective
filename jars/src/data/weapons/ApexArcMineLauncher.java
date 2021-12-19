package data.weapons;

import com.fs.starfarer.api.combat.*;
import data.weapons.proj.ApexArcMineScript;

public class ApexArcMineLauncher implements OnFireEffectPlugin, EveryFrameWeaponEffectPlugin
{

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon)
    {
        // do nothin
    }

    @Override
    public void onFire(DamagingProjectileAPI proj, WeaponAPI weapon, CombatEngineAPI engine)
    {
        // can't really do it with an AI script because I don't want to copy Alex's mine AI
        engine.addPlugin(new ApexArcMineScript(weapon.getShip(), proj));
    }
}
