package data.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import utils.ApexUtils;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

import static plugins.ApexModPlugin.POTATO_MODE;

public class ApexTachyonEffects implements OnHitEffectPlugin, EveryFrameWeaponEffectPlugin
{
    public static final float ARC_CHANCE = 0.5f;
    public static final float ARC_DAMAGE = 0.75f;
    public static final Color ARC_COLOR = new Color(150, 100, 255);
    private IntervalUtil zapInterval = new IntervalUtil(0.05f, 0.1f);
    private boolean firedThisCycle = false;
    private boolean playedCharge = false;

    @Override
    public void onHit(DamagingProjectileAPI proj, CombatEntityAPI target, Vector2f point, boolean shieldhit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine)
    {
        if (!shieldhit && target instanceof ShipAPI && Misc.random.nextFloat() <= ARC_CHANCE)
        {
            engine.spawnEmpArc(proj.getSource(), point, target, target,
                    DamageType.ENERGY,
                    proj.getDamageAmount() * ARC_DAMAGE,
                    proj.getEmpAmount() * ARC_DAMAGE, // emp
                    100000f, // max range
                    "tachyon_lance_emp_impact",
                    20f, // thickness
                    ARC_COLOR,
                    Color.WHITE
            );
        }
        ApexUtils.plasmaEffects(proj, ARC_COLOR, 50f);
    }

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon)
    {
        if (weapon == null)
            return;
        Vector2f barrelLoc;
        float charge = weapon.getChargeLevel();

        if (weapon.getSlot().isHardpoint()) {
            barrelLoc = weapon.getSpec().getHardpointFireOffsets().get(0);
        } else {
            barrelLoc = weapon.getSpec().getTurretFireOffsets().get(0);
        }

        barrelLoc = VectorUtils.rotate(barrelLoc, weapon.getCurrAngle(), new Vector2f(0f, 0f));
        Vector2f.add(barrelLoc, weapon.getLocation(), barrelLoc);
        if (weapon.getAmmo() < 5 && weapon.getCooldownRemaining() <= 0.01f)
            weapon.setRemainingCooldownTo(0.01f);

        if (!playedCharge && charge > 0)
        {
            Global.getSoundPlayer().playSound("apex_inverter_charge", 1f, 1f, weapon.getLocation(), new Vector2f(0f, 0f));
            playedCharge = true;
        }

        if (charge > 0 && !firedThisCycle && !weapon.getShip().getFluxTracker().isOverloadedOrVenting() && weapon.getAmmo() >= 5)//&& weapon.getAmmo() >= 10)
        {
            zapInterval.advance(engine.getElapsedInLastFrame());
            if (zapInterval.intervalElapsed())
            {
                engine.spawnEmpArcVisual(barrelLoc, weapon.getShip(),
                        MathUtils.getRandomPointInCircle(barrelLoc, 80f * charge),
                        weapon.getShip(),
                        5f + 10f * charge,
                        ARC_COLOR,
                        Color.WHITE
                );/*
                engine.spawnEmpArcPierceShields(weapon.getShip(), barrelLoc, weapon.getShip(),
                        new SimpleEntity(MathUtils.getRandomPointInCircle(barrelLoc, 80f * charge)),
                        DamageType.FRAGMENTATION,
                        0f,
                        0f,
                        75f,
                        null,
                        5f+10f*charge,
                        ARC_COLOR,
                        Color.WHITE
                );*/
            }
        }
        if (charge >= 1f && !firedThisCycle )
        {
            if (!POTATO_MODE)
                ApexUtils.addWaveDistortion(barrelLoc, 10f, 15f, 0.15f);
            firedThisCycle = true;
        }
        if (firedThisCycle && (charge <= 0f || !weapon.isFiring()))
        {
            firedThisCycle = false;
        }
        if (charge == 0f)
            playedCharge = false;
    }
}