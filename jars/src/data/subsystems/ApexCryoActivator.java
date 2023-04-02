package data.subsystems;

import activators.CombatActivator;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import data.ApexUtils;
import data.weapons.proj.ApexCryoBlobScript;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.AIUtils;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static data.ApexUtils.text;
import static data.hullmods.ApexCryoSystemHullmod.MAX_COOLANT_LOCKON_RANGE;
import static data.hullmods.ApexCryoSystemHullmod.BASE_COOLDOWN;

public class ApexCryoActivator extends CombatActivator
{
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

    public ApexCryoActivator(ShipAPI ship) {
        super(ship);
    }

    @Override
    protected void initialized() {
        lockonRange = ship.getMutableStats().getSystemRangeBonus().computeEffective(MAX_COOLANT_LOCKON_RANGE);
    }

    @Override
    public float getBaseActiveDuration() {
        return 0.1f;
    }

    @Override
    public float getBaseOutDuration() {
        return 0.5f;
    }

    @Override
    public float getBaseCooldownDuration() {
        return 30f;
    }

    @Override
    public void onActivate()
    {
        if (dummyWep == null)
            dummyWep = Global.getCombatEngine().createFakeWeapon(ship, "apex_repairgun");
        MutableShipStatsAPI stats = ship.getMutableStats();
        lockonRange = ship.getMutableStats().getSystemRangeBonus().computeEffective(MAX_COOLANT_LOCKON_RANGE);
        runOnce = true;
        didThings = false;
        setCooldownDuration(ship.getMutableStats().getSystemCooldownBonus().computeEffective(BASE_COOLDOWN), false);
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
            setCooldownDuration(ship.getMutableStats().getSystemCooldownBonus().computeEffective(BASE_COOLDOWN  * ApexUtils.getNozzleCooldownMult(ship)), false);
            //System.out.println("did repair, cooldown is 30 seconds");
            ((ShipAPI) stats.getEntity()).getFluxTracker().increaseFlux(((ShipAPI) stats.getEntity()).getMaxFlux() * ACTIVATION_FLUX_FRACTION, false);
            //timeUntilNextActivation = ship.getMutableStats().getSystemCooldownBonus().computeEffective(BASE_COOLDOWN * ApexUtils.getNozzleCooldownMult(ship));
            Global.getSoundPlayer().playSound("apex_nozzle_activation",1f,1f,ship.getLocation(), ship.getVelocity());
        }
        // if we didn't do anything, notify player why and reset cooldown
        if (!didThings)
        {
            //System.out.println("didn't do anything, resetting cooldown");
            ship.getFluxTracker().showOverloadFloatyIfNeeded(text("cryosys1"), new Color(255, 55, 55, 255), 2f, true);
            Global.getSoundPlayer().playSound("gun_out_of_ammo", 1f, 1f, ship.getLocation(), ship.getVelocity());
            setCooldownDuration(0.1f, false);
            //timeUntilNextActivation = 0.2f;
        }
        // just in case?
        buffTargets.clear();
    }

    @Override
    public void advance(float amount) {
        if (didThings)
        {
            ship.setJitterUnder(this, JITTER_UNDER_COLOR, getEffectLevel(), 11, 0f, 3f + getEffectLevel() * 25);
            ship.setJitter(this, JITTER_COLOR, getEffectLevel(), 4, 0f, 0 + getEffectLevel() * 25);
        }
    }

    @Override
    public void onFinished() {
        runOnce = false;
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
    public String getDisplayText() {
        return text("cryosys2");
    }

    @Override
    public boolean shouldActivateAI(float amount)
    {
        updateTimer.advance(amount);

        if (ship == null || !ship.isAlive() || !state.equals(State.READY) )//|| timeUntilNextActivation > 0)
            return false;
        if (!updateTimer.intervalElapsed() || ship.getFluxLevel() > 1f - ACTIVATION_FLUX_FRACTION * 2f)
            return false;

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
                return true;
            }
        }
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