package data.subsystems;

import apexsubs.ApexBaseSubsystem;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import data.ApexUtils;
import data.hullmods.ApexArmorRepairHullmod;
import data.hullmods.ApexFastNozzles;
import data.weapons.proj.ApexRepairBlobScript;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.AIUtils;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static data.hullmods.ApexArmorRepairHullmod.MAX_REGEN_LOCKON_RANGE;
import static data.hullmods.ApexArmorRepairHullmod.BASE_COOLDOWN;

public class ApexArmorRepairSubsystem extends ApexBaseSubsystem
{
    public static final String SUBSYSTEM_ID = "apex_repairsubsystem"; //this should match the id in the csv

    public static final Color JITTER_COLOR = new Color(55,255,55,75);
    public static final Color JITTER_UNDER_COLOR = new Color(55,255,55,155);

    private boolean runOnce = false;
    private boolean didThings = false;
    private IntervalUtil updateTimer = new IntervalUtil(2f, 3f);
    //private float timeUntilNextActivation = 0f;

    // ship gains this much of its total flux as soft flux on activation
    private static final float ACTIVATION_FLUX_FRACTION = 0.2f;
    private float lockonRange;

    private WeaponAPI dummyWep;

    public ApexArmorRepairSubsystem()
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
        lockonRange = ship.getMutableStats().getSystemRangeBonus().computeEffective(MAX_REGEN_LOCKON_RANGE);
        runOnce = true;
        didThings = false;
        setCooldownTime(ship.getMutableStats().getSystemCooldownBonus().computeEffective(BASE_COOLDOWN));
        // check to see if we have any valid repair targets in range
        WeightedRandomPicker<ShipAPI> damagedAllies = new WeightedRandomPicker<>();
        if (ship.getShipTarget() != null && ship.getShipTarget().getOwner() == ship.getOwner())
        {
            if (MathUtils.getDistance(ship.getShipTarget(), ship) < lockonRange)
            {
                float weight = isValidRepairTarget(ship.getShipTarget(), ship);
                if (weight > 0)
                    damagedAllies.add(ship.getShipTarget(), weight * 20);
            }
        } else
        {
            //System.out.println("checking allies");
            List<ShipAPI> allies = AIUtils.getNearbyAllies(ship, lockonRange);
            for (ShipAPI ally : allies)
            {
                float weight = isValidRepairTarget(ally, ship);
                if (weight > 0)
                    damagedAllies.add(ally, (float)Math.sqrt(weight));

            }
        }
        // if we have valid repair targets, fire projectiles and apply projectile script
        if (!damagedAllies.isEmpty())
        {
            //System.out.println(damagedAllies.getItems().size() + " valid repair targets in list");
            List<DamagingProjectileAPI> blobs = new ArrayList<DamagingProjectileAPI>();
            for (WeaponSlotAPI slot : ship.getHullSpec().getAllWeaponSlotsCopy())
            {
                if (slot.isSystemSlot())
                {
                    // N O Z Z L E
                    DamagingProjectileAPI blob = (DamagingProjectileAPI) Global.getCombatEngine().spawnProjectile(ship,
                            dummyWep,
                            "apex_repairgun",
                            slot.computePosition(ship),
                            slot.computeMidArcAngle(ship),
                            null);
                    blobs.add(blob);
                    blob.getVelocity().scale(ship.getMutableStats().getSystemRangeBonus().computeEffective(1f));
                }
            }
            for (DamagingProjectileAPI blob : blobs)
            {
                Global.getCombatEngine().addPlugin(new ApexRepairBlobScript(blob, damagedAllies.pick()));
            }
            didThings = true;
            setCooldownTime(ship.getMutableStats().getSystemCooldownBonus().computeEffective(BASE_COOLDOWN * ApexUtils.getNozzleCooldownMult(ship)));
            //System.out.println("did repair, cooldown is 30 seconds");
            ((ShipAPI) stats.getEntity()).getFluxTracker().increaseFlux(((ShipAPI) stats.getEntity()).getMaxFlux() * ACTIVATION_FLUX_FRACTION, false);
            //timeUntilNextActivation = ship.getMutableStats().getSystemCooldownBonus().computeEffective(BASE_COOLDOWN * ApexUtils.getNozzleCooldownMult(ship));
            Global.getSoundPlayer().playSound("apex_nozzle_activation",1f,1f,ship.getLocation(), ship.getVelocity());
        }
        // if we didn't do anything, notify player and reset cooldown
        if (!didThings)
        {
            //System.out.println("didn't do anything, resetting cooldown");
            ship.getFluxTracker().showOverloadFloatyIfNeeded("No Repair Targets!", new Color(255, 55, 55, 255), 2f, true);
            Global.getSoundPlayer().playSound("gun_out_of_ammo", 1f, 1f, ship.getLocation(), ship.getVelocity());
            setCooldownTime(0.1f);
            //timeUntilNextActivation = 0.2f;
        }
        // just in case?
        damagedAllies.clear();
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

    // returns 0 if it's not a valid target, otherwise returns the repairable armor
    public float isValidRepairTarget(ShipAPI target, ShipAPI source)
    {
        // range check
        if (target.getHullSize().equals(ShipAPI.HullSize.FIGHTER) || target.getHullSize().equals(ShipAPI.HullSize.DEFAULT))
            return 0f;
        if (MathUtils.getDistanceSquared(target.getLocation(), source.getLocation()) > lockonRange * lockonRange)
            return 0f;
        // missing armor check
        float repairableArmor = getRepairableArmor(target);
        if (repairableArmor <= 0)
            return 0f;
        // always repairable regardless of shields if it's got cryo-armor
        if (target.getVariant().hasHullMod("apex_cryo_armor"))
            return repairableArmor;
        // shield check otherwise
        if (target.getShield() != null && target.getShield().getType() != ShieldAPI.ShieldType.PHASE && target.getShield().getType() != ShieldAPI.ShieldType.NONE)
            return repairableArmor;
        return 0;
    }

    public static float getRepairableArmor(ShipAPI ship)
    {
        ArmorGridAPI grid = ship.getArmorGrid();
        if (grid == null)
            return 0f;
        int gridWidth = grid.getGrid().length;
        int gridHeight = grid.getGrid()[0].length;
        float maxArmorInCell = grid.getMaxArmorInCell() * ApexArmorRepairHullmod.MAX_REGEN_FRACTION;
        float missingArmor = 0f;
        for (int x = 0; x < gridWidth; x++)
        {
            for (int y = 0; y < gridHeight; y++)
            {
                if (grid.getArmorValue(x, y) < maxArmorInCell)
                    missingArmor += maxArmorInCell - grid.getArmorValue(x, y);
            }
        }
        return missingArmor;
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

    @Override
    public void unapply(MutableShipStatsAPI stats, String id)
    {
        //System.out.println("called unapply()");
        runOnce = false;
        didThings = false;
        //if (timeUntilNextActivation > 0 && !Global.getCombatEngine().isPaused())
        //    timeUntilNextActivation -= Global.getCombatEngine().getElapsedInLastFrame();
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
        return "REMOTE ARMOR REPAIR";
    }

    @Override
    public int getNumGuiBars()
    {
        return 1;
    }

    @Override
    public String getStatusString()
    {
        return null;
    }

    @Override
    public void aiInit()
    {
        lockonRange = ship.getMutableStats().getSystemRangeBonus().computeEffective(MAX_REGEN_LOCKON_RANGE);
    }

    @Override
    public void aiUpdate(float amount)
    {
        updateTimer.advance(amount);
        if (ship == null || !ship.isAlive() || !state.equals(SubsystemState.OFF)) //|| timeUntilNextActivation > 0)
            return;
        if (!updateTimer.intervalElapsed() || ship.getFluxLevel() > 1f - ACTIVATION_FLUX_FRACTION * 2f)
            return;
        List<ShipAPI> allies = AIUtils.getNearbyAllies(ship, lockonRange);
        for (ShipAPI ally : allies)
        {
            if (isValidRepairTarget(ally, ship) > ApexArmorRepairHullmod.regenMap.get(ship.getHullSize()))
            {
                activate();
                return;
            }
        }
    }

}