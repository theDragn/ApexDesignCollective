package data.activators;

import activators.CombatActivator;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import utils.ApexUtils;
import org.lazywizard.lazylib.combat.AIUtils;

import static utils.ApexUtils.text;
import static data.hullmods.ApexFlareSystemHullmod.BASE_COOLDOWN;
import static data.hullmods.ApexFlareSystemHullmod.NUM_FLARES;

public class ApexFlareActivator extends CombatActivator
{
    private int firedFlares = 0;
    private IntervalUtil frameTracker = new IntervalUtil(0.1f, 0.1f);
    private IntervalUtil updateTimer = new IntervalUtil(0.33f, 0.66f);

    public ApexFlareActivator(ShipAPI ship) {
        super(ship);
    }

    @Override
    public float getBaseActiveDuration() {
        return 1f;
    }

    @Override
    public float getBaseCooldownDuration() {
        return 19f;
    }

    @Override
    public void advance(float amount)
    {
        if (!isActive())
            return;

        frameTracker.advance(amount);
        if (firedFlares != NUM_FLARES.get(ship.getHullSize()) && frameTracker.intervalElapsed())
        {
            for (WeaponSlotAPI slot : ship.getHullSpec().getAllWeaponSlotsCopy())
            {
                if (slot.isSystemSlot() && !slot.getId().contains("MINE"))
                {
                    // N O Z Z L E
                    Global.getCombatEngine().spawnProjectile(ship, null, "flarelauncher3", slot.computePosition(ship), slot.computeMidArcAngle(ship) + 30f * (Misc.random.nextFloat() - 0.5f), null);
                    Global.getSoundPlayer().playSound("system_flare_launcher_active", 1f, 1f, slot.computePosition(ship), ship.getVelocity());
                }
            }
            firedFlares++;
        }
    }

    @Override
    public void onFinished() {
        if (firedFlares != 0)
        {
            firedFlares = 0;
            setCooldownDuration(ship.getMutableStats().getSystemCooldownBonus().computeEffective(BASE_COOLDOWN * ApexUtils.getNozzleCooldownMult(ship)), false);
        }
    }

    @Override
    public String getStateText()
    {
        if (isOn()) return text("repair2");
        else if (isCooldown()) return text("repair3");
        else if (isOff()) return text("repair4");
        else return "";
    }

    @Override
    public String getDisplayText()
    {
        return text("flaresys1");
    }

    @Override
    public boolean shouldActivateAI(float amount)
    {
        updateTimer.advance(amount);
        if (ship == null || !ship.isAlive() || !state.equals(State.READY))
            return false;
        if (!updateTimer.intervalElapsed())
            return false;
        if (AIUtils.getNearbyEnemyMissiles(ship, 750).size() > 0)
            return true;
        return false;
    }
}
