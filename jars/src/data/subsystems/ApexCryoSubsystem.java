package data.subsystems;

import apexsubs.ApexBaseSubsystem;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.impl.campaign.ids.Skills;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import data.ApexUtils;
import data.hullmods.ApexArmorRepairHullmod;
import data.hullmods.ApexCryoSystemHullmod;
import data.hullmods.ApexFastNozzles;
import data.weapons.proj.ApexCryoBlobScript;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.AIUtils;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static data.hullmods.ApexCryoSystemHullmod.MAX_COOLANT_LOCKON_RANGE;
import static data.hullmods.ApexCryoSystemHullmod.BASE_COOLDOWN;

public class ApexCryoSubsystem extends ApexBaseSubsystem
{
    public static final String SUBSYSTEM_ID = "apex_cryosubsystem"; //this should match the id in the csv

    private boolean runOnce = false;
    private boolean didThings = false;
    private IntervalUtil updateTimer = new IntervalUtil(2f, 3f);
    //private float timeUntilNextActivation = 0f;

    public static final Color JITTER_COLOR = new Color(128,255,255,75);
    public static final Color JITTER_UNDER_COLOR = new Color(128,255,255,155);

    // ship gains this much of its total flux as soft flux on activation
    private static final float ACTIVATION_FLUX_FRACTION = 0.2f;
    private float lockonRange;
    private WeaponAPI dummyWep;

    public ApexCryoSubsystem()
    {
        super(SUBSYSTEM_ID);
    }

    @Override
    public void onActivation()
    {
        if (!state.equals(SubsystemState.IN))
            return;
        if (dummyWep == null)
            dummyWep = Global.getCombatEngine().createFakeWeapon(ship, "apex_repairgun");
        MutableShipStatsAPI stats = ship.getMutableStats();
        lockonRange = ship.getMutableStats().getSystemRangeBonus().computeEffective(MAX_COOLANT_LOCKON_RANGE);
        runOnce = true;
        didThings = false;
        setCooldownTime(ship.getMutableStats().getSystemCooldownBonus().computeEffective(BASE_COOLDOWN));
        WeightedRandomPicker<ShipAPI> buffTargets = new WeightedRandomPicker<>();
        if (ship.getShipTarget() != null && ship.getShipTarget().getOwner() == ship.getOwner() && MathUtils.getDistanceSquared(ship.getShipTarget(), ship) < lockonRange && !ship.getShipTarget().getHullSize().equals(ShipAPI.HullSize.FIGHTER))
        {
            float weight = ship.getShipTarget().getFluxLevel();
            buffTargets.add(ship.getShipTarget(), weight + 5000);
        } else
        {
            List<ShipAPI> allies = AIUtils.getNearbyAllies(ship, lockonRange);
            for (ShipAPI ally : allies)
            {
                if (ally.getHullSize().equals(ShipAPI.HullSize.FIGHTER))
                    continue;
                float weight = Math.min(ally.getFluxLevel(), 0.2f);
                if (shouldReduceBonus(ally.getHullSize(), ship.getHullSize()))
                    weight *= 0.75f;
                buffTargets.add(ally, weight);
            }
        }
        // if we have valid buff targets, fire projectiles and apply projectile script
        if (!buffTargets.isEmpty())
        {
            List<DamagingProjectileAPI> blobs = new ArrayList<DamagingProjectileAPI>();
            for (WeaponSlotAPI slot : ship.getHullSpec().getAllWeaponSlotsCopy())
            {
                if (slot.isSystemSlot() && !slot.getId().contains("MINE"))
                {
                    // N O Z Z L E
                    DamagingProjectileAPI blob = (DamagingProjectileAPI) Global.getCombatEngine().spawnProjectile(ship,
                            dummyWep,
                            "apex_cryogun",
                            slot.computePosition(ship),
                            slot.computeMidArcAngle(ship),
                            null);
                    blobs.add(blob);
                    blob.getVelocity().scale(ship.getMutableStats().getSystemRangeBonus().computeEffective(1f));
                }
            }
            for (DamagingProjectileAPI blob : blobs)
            {
                if (buffTargets.getItems().size() == 1)
                    Global.getCombatEngine().addPlugin(new ApexCryoBlobScript(blob, buffTargets.pick()));
                else
                    Global.getCombatEngine().addPlugin(new ApexCryoBlobScript(blob, buffTargets.pickAndRemove()));
            }
            didThings = true;
            setCooldownTime(ship.getMutableStats().getSystemCooldownBonus().computeEffective(BASE_COOLDOWN  * ApexUtils.getNozzleCooldownMult(ship)));
            //System.out.println("did repair, cooldown is 30 seconds");
            ((ShipAPI) stats.getEntity()).getFluxTracker().increaseFlux(((ShipAPI) stats.getEntity()).getMaxFlux() * ACTIVATION_FLUX_FRACTION, false);
            //timeUntilNextActivation = ship.getMutableStats().getSystemCooldownBonus().computeEffective(BASE_COOLDOWN * ApexUtils.getNozzleCooldownMult(ship));
            Global.getSoundPlayer().playSound("apex_nozzle_activation",1f,1f,ship.getLocation(), ship.getVelocity());
        }
        // if we didn't do anything, notify player why and reset cooldown
        if (!didThings)
        {
            //System.out.println("didn't do anything, resetting cooldown");
            ship.getFluxTracker().showOverloadFloatyIfNeeded("No Targets!", new Color(255, 55, 55, 255), 2f, true);
            Global.getSoundPlayer().playSound("gun_out_of_ammo", 1f, 1f, ship.getLocation(), ship.getVelocity());
            setCooldownTime(0.1f);
            //timeUntilNextActivation = 0.2f;
        }
        // just in case?
        buffTargets.clear();
    }

    @Override
    public void apply(MutableShipStatsAPI stats, String id, SubsystemState state, float effectLevel)
    {
        if (didThings)
        {
            ship.setJitterUnder(this, JITTER_UNDER_COLOR, effectLevel, 11, 0f, 3f + effectLevel * 25);
            ship.setJitter(this, JITTER_COLOR, effectLevel, 4, 0f, 0 + effectLevel * 25);
        }
    }

    @Override
    public void unapply(MutableShipStatsAPI mutableShipStatsAPI, String s)
    {
        runOnce = false;
        //if (timeUntilNextActivation > 0 && !Global.getCombatEngine().isPaused())
        //    timeUntilNextActivation -= Global.getCombatEngine().getElapsedInLastFrame();
    }

    @Override
    public void aiInit()
    {
        lockonRange = ship.getMutableStats().getSystemRangeBonus().computeEffective(MAX_COOLANT_LOCKON_RANGE);
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
        return "CRYOCOOLANT PROJECTOR";
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

        if (ship == null || !ship.isAlive() || !state.equals(SubsystemState.OFF) )//|| timeUntilNextActivation > 0)
            return;
        if (!updateTimer.intervalElapsed() || ship.getFluxLevel() > 1f - ACTIVATION_FLUX_FRACTION * 2f)
            return;
        // no self-repair
        //if (getMissingArmor(ship) > ApexArmorRepairHullmod.regenMap.get(ship.getHullSize()))
        //    activate();
        List<ShipAPI> allies = AIUtils.getNearbyAllies(ship, lockonRange);
        for (ShipAPI ally : allies)
        {
            if (ally.getHullSize().equals(ShipAPI.HullSize.FIGHTER))
                continue;
            if (ally.getFluxLevel() > 0.2f)
            {
                activate();
                return;
            }
        }
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

    /**
     * Returns true if a is larger than b, otherwise returns false
     */
    public static boolean shouldReduceBonus(ShipAPI.HullSize targetSize, ShipAPI.HullSize sourceSize)
    {
        if (targetSize == ShipAPI.HullSize.FRIGATE)
            return false;
        else if (targetSize == ShipAPI.HullSize.DESTROYER && sourceSize != ShipAPI.HullSize.FRIGATE)
            return false;
        else if (targetSize == ShipAPI.HullSize.CRUISER && sourceSize != ShipAPI.HullSize.FRIGATE && sourceSize != ShipAPI.HullSize.DESTROYER)
            return false;
        else if (targetSize == ShipAPI.HullSize.CAPITAL_SHIP && sourceSize == ShipAPI.HullSize.CAPITAL_SHIP)
            return false;
        return true;
    }
}