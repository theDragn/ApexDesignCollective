package data.subsystems;

import apexsubs.ApexBaseSubsystem;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.impl.campaign.ids.Skills;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import data.ApexUtils;
import data.hullmods.ApexFastNozzles;
import org.lazywizard.lazylib.combat.AIUtils;

import static data.hullmods.ApexFlareSystemHullmod.BASE_COOLDOWN;
import static data.hullmods.ApexFlareSystemHullmod.NUM_FLARES;

public class ApexFlareSubsystem extends ApexBaseSubsystem
{
    public static final String SUBSYSTEM_ID = "apex_flaresubsystem"; //this should match the id in the csv

    private boolean runOnce = false;
    private int firedFlares = 0;
    private IntervalUtil frameTracker = new IntervalUtil(0.1f, 0.1f);
    private IntervalUtil updateTimer = new IntervalUtil(0.33f, 0.66f);

    public ApexFlareSubsystem()
    {
        super(SUBSYSTEM_ID);
    }

    @Override
    public void apply(MutableShipStatsAPI stats, String id, SubsystemState state, float effectLevel)
    {
        frameTracker.advance(Global.getCombatEngine().getElapsedInLastFrame());
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
    public void unapply(MutableShipStatsAPI mutableShipStatsAPI, String s)
    {
        if (firedFlares != 0)
        {
            firedFlares = 0;
            setCooldownTime(ship.getMutableStats().getSystemCooldownBonus().computeEffective(BASE_COOLDOWN * ApexUtils.getNozzleCooldownMult(ship)));
        }
    }

    @Override
    public void aiInit()
    {
    }

    @Override
    public String getStatusString()
    {
        return null;
    }

    @Override
    public String getInfoString()
    {
        if (isOn()) return "FIRING";
        else if (isCooldown()) return "RECHARGING";
        else if (isOff()) return "READY";
        else return "";
    }

    @Override
    public String getFlavourString()
    {
        return "FLARE LAUNCHER";
    }

    @Override
    public int getNumGuiBars()
    {
        return 1;
    }

    @Override
    public void aiUpdate(float amount)
    {
        updateTimer.advance(amount);
        if (ship == null || !ship.isAlive() || !state.equals(SubsystemState.OFF))
            return;
        if (!updateTimer.intervalElapsed())
            return;
        float damage = 0;
        if (AIUtils.getNearbyEnemyMissiles(ship, 750).size() > 0)
            activate();
    }
    @Override
    public boolean canUseWhileOverloaded()
    {
        return false;
    }

    @Override
    public boolean canUseWhileVenting()
    {
        return false;
    }
}
