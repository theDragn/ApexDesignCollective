package data.activators;

import activators.CombatActivator;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import utils.ApexUtils;
import data.hullmods.ApexArmorRepairHullmod;
import data.weapons.proj.ApexRepairBlobScript;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.AIUtils;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static utils.ApexUtils.text;
import static data.hullmods.ApexArmorRepairHullmod.BASE_COOLDOWN;
import static data.hullmods.ApexArmorRepairHullmod.MAX_REGEN_LOCKON_RANGE;

public class ApexArmorRepairActivator extends CombatActivator {
    public static final Color JITTER_COLOR = new Color(55, 255, 55, 75);
    public static final Color JITTER_UNDER_COLOR = new Color(55, 255, 55, 155);

    private boolean runOnce = false;
    private boolean didThings = false;
    private IntervalUtil updateTimer = new IntervalUtil(2f, 3f);
    //private float timeUntilNextActivation = 0f;

    // ship gains this much of its total flux as soft flux on activation
    private static final float ACTIVATION_FLUX_FRACTION = 0.2f;
    private float lockonRange;

    private WeaponAPI dummyWep;

    public ApexArmorRepairActivator(ShipAPI ship) {
        super(ship);
    }

    @Override
    protected void init() {
        super.init();
        lockonRange = ship.getMutableStats().getSystemRangeBonus().computeEffective(MAX_REGEN_LOCKON_RANGE);
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
    public void onActivate() {
        if (dummyWep == null)
            dummyWep = Global.getCombatEngine().createFakeWeapon(ship, "apex_repairgun");
        MutableShipStatsAPI stats = ship.getMutableStats();
        lockonRange = ship.getMutableStats().getSystemRangeBonus().computeEffective(MAX_REGEN_LOCKON_RANGE);
        runOnce = true;
        didThings = false;

        setCooldownDuration(ship.getMutableStats().getSystemCooldownBonus().computeEffective(BASE_COOLDOWN), false);

        // check to see if we have any valid repair targets in range
        WeightedRandomPicker<ShipAPI> damagedAllies = new WeightedRandomPicker<>();
        if (ship.getShipTarget() != null && ship.getShipTarget().getOwner() == ship.getOwner()) {
            if (MathUtils.getDistance(ship.getShipTarget(), ship) < lockonRange) {
                float weight = isValidRepairTarget(ship.getShipTarget(), ship);
                if (weight > 0)
                    damagedAllies.add(ship.getShipTarget(), weight * 20);
            }
        } else {
            //System.out.println("checking allies");
            List<ShipAPI> allies = AIUtils.getNearbyAllies(ship, lockonRange);
            for (ShipAPI ally : allies) {
                float weight = isValidRepairTarget(ally, ship);
                if (weight > 0)
                    damagedAllies.add(ally, (float) Math.sqrt(weight));

            }
        }
        // if we have valid repair targets, fire projectiles and apply projectile script
        if (!damagedAllies.isEmpty()) {
            //System.out.println(damagedAllies.getItems().size() + " valid repair targets in list");
            List<DamagingProjectileAPI> blobs = new ArrayList<DamagingProjectileAPI>();
            for (WeaponSlotAPI slot : ship.getHullSpec().getAllWeaponSlotsCopy()) {
                if (slot.isSystemSlot() && !slot.getId().contains("MINE")) {
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
            for (DamagingProjectileAPI blob : blobs) {
                Global.getCombatEngine().addPlugin(new ApexRepairBlobScript(blob, damagedAllies.pick()));
            }
            didThings = true;
            setCooldownDuration(ship.getMutableStats().getSystemCooldownBonus().computeEffective(BASE_COOLDOWN * ApexUtils.getNozzleCooldownMult(ship)), false);
            //System.out.println("did repair, cooldown is 30 seconds");
            ((ShipAPI) stats.getEntity()).getFluxTracker().increaseFlux(((ShipAPI) stats.getEntity()).getMaxFlux() * ACTIVATION_FLUX_FRACTION, false);
            //timeUntilNextActivation = ship.getMutableStats().getSystemCooldownBonus().computeEffective(BASE_COOLDOWN * ApexUtils.getNozzleCooldownMult(ship));
            Global.getSoundPlayer().playSound("apex_nozzle_activation", 1f, 1f, ship.getLocation(), ship.getVelocity());
        }
        // if we didn't do anything, notify player and reset cooldown
        if (!didThings) {
            //System.out.println("didn't do anything, resetting cooldown");
            ship.getFluxTracker().showOverloadFloatyIfNeeded(text("repair1"), new Color(255, 55, 55, 255), 2f, true);
            Global.getSoundPlayer().playSound("gun_out_of_ammo", 1f, 1f, ship.getLocation(), ship.getVelocity());
            setCooldownDuration(0.1f, false);
            //timeUntilNextActivation = 0.2f;
        }
        // just in case?
        damagedAllies.clear();
    }

    @Override
    public void advance(float amount) {
        if (didThings) {
            ship.setJitterUnder(this, JITTER_UNDER_COLOR, getEffectLevel(), 11, 0f, 3f + getEffectLevel() * 25);
            ship.setJitter(this, JITTER_COLOR, getEffectLevel(), 4, 0f, 0 + getEffectLevel() * 25);
        }
    }

    // returns 0 if it's not a valid target, otherwise returns the repairable armor
    public float isValidRepairTarget(ShipAPI target, ShipAPI source) {
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

    public static float getRepairableArmor(ShipAPI ship) {
        ArmorGridAPI grid = ship.getArmorGrid();
        if (grid == null)
            return 0f;
        int gridWidth = grid.getGrid().length;
        int gridHeight = grid.getGrid()[0].length;
        float maxArmorInCell = grid.getMaxArmorInCell() * ApexArmorRepairHullmod.MAX_REGEN_FRACTION;
        float missingArmor = 0f;
        for (int x = 0; x < gridWidth; x++) {
            for (int y = 0; y < gridHeight; y++) {
                if (grid.getArmorValue(x, y) < maxArmorInCell)
                    missingArmor += maxArmorInCell - grid.getArmorValue(x, y);
            }
        }
        return missingArmor;
    }

    @Override
    public void onFinished() {
        runOnce = false;
        didThings = false;
    }

    @Override
    public String getDisplayText() {
        return text("repair5");
    }

    @Override
    public String getStateText() {
        if (isOn()) return text("repair2");
        else if (isCooldown()) return text("repair3");
        else if (isOff()) return text("repair4");
        else return "";
    }

    @Override
    public boolean shouldActivateAI(float amount) {
        updateTimer.advance(amount);

        if (ship == null || !ship.isAlive() || !isReady()) //|| timeUntilNextActivation > 0)
            return false;

        if (!updateTimer.intervalElapsed() || ship.getFluxLevel() > 1f - ACTIVATION_FLUX_FRACTION * 2f)
            return false;

        List<ShipAPI> allies = AIUtils.getNearbyAllies(ship, lockonRange);
        for (ShipAPI ally : allies) {
            if (isValidRepairTarget(ally, ship) > ApexArmorRepairHullmod.regenMap.get(ship.getHullSize())) {
                return true;
            }
        }
        return false;
    }
}