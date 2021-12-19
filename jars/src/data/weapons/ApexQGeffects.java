package data.weapons;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.WeaponBaseRangeModifier;
import com.fs.starfarer.api.combat.listeners.WeaponRangeModifier;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;


public class ApexQGeffects implements OnFireEffectPlugin, EveryFrameWeaponEffectPlugin
{
    public static final float ENERGY_SLOT_FLUX_MULT = 1.33f;
    public static final float ENERGY_SLOT_DAMAGE_MULT = 1.33f;
    public static final float BALLISTIC_SLOT_RANGE_BOOST = 200f;

    // hullmod bonus effect
    // stolen and tweaked from DME
    private static final float NEBULA_SIZE = 6f;
    private static final float NEBULA_SIZE_MULT = 9f;
    private static final float NEBULA_DUR = 0.33f;
    private static final float NEBULA_RAMPUP = 0.1f;
    public static final Color PARTICLE_COLOR = new Color(255,166,168,255);


    private boolean firstRun = true;

    @Override
    public void onFire(DamagingProjectileAPI proj, WeaponAPI weapon, CombatEngineAPI engine)
    {
        if (weapon.getShip().getVariant().hasHullMod("apex_coherency_amplifier"))
        {
            Vector2f shipVel = weapon.getShip().getVelocity();
            Vector2f loc = proj.getLocation();
            float size = NEBULA_SIZE * (0.75f + Misc.random.nextFloat() * 0.5f); // 0.75x to 1.25x
            // do visual fx
            engine.addSwirlyNebulaParticle(loc,
                    shipVel,
                    NEBULA_SIZE,
                    NEBULA_SIZE_MULT,
                    NEBULA_RAMPUP,
                    0.2f,
                    NEBULA_DUR,
                    PARTICLE_COLOR,
                    true
            );
            engine.addSmoothParticle(loc,
                    shipVel,
                    NEBULA_SIZE * 2,
                    0.75f,
                    NEBULA_RAMPUP,
                    NEBULA_DUR / 2,
                    PARTICLE_COLOR
            );
        }

        if (weapon.getSlot().getWeaponType().equals(WeaponAPI.WeaponType.ENERGY) || weapon.getSlot().getWeaponType().equals(WeaponAPI.WeaponType.UNIVERSAL))
        {
            weapon.getShip().getFluxTracker().increaseFlux(weapon.getFluxCostToFire()*(ENERGY_SLOT_FLUX_MULT-1f), false);
            proj.getDamage().getModifier().modifyMult("apexQG",ENERGY_SLOT_DAMAGE_MULT);
        }
    }

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon)
    {
        if (firstRun && !weapon.getShip().hasListenerOfClass(ApexQGeffectListener.class))
        {
            weapon.getShip().addListener(new ApexQGeffectListener());
            firstRun = false;
        }
    }

    private class ApexQGeffectListener implements WeaponBaseRangeModifier
    {
        @Override
        public float getWeaponBaseRangePercentMod(ShipAPI shipAPI, WeaponAPI weaponAPI)
        {
            return 0;
        }

        @Override
        public float getWeaponBaseRangeMultMod(ShipAPI shipAPI, WeaponAPI weaponAPI)
        {
            return 1f;
        }

        @Override
        public float getWeaponBaseRangeFlatMod(ShipAPI ship, WeaponAPI weapon)
        {
            if ((weapon.getId().equals("apex_qgp_med") || weapon.getId().equals("apex_qgp_small")) && (weapon.getSlot().getWeaponType() != WeaponAPI.WeaponType.ENERGY))
                return BALLISTIC_SLOT_RANGE_BOOST;
            return 0;
        }
    }
}
