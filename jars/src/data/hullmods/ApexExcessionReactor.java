package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import data.ApexUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.*;

import static plugins.ApexModPlugin.POTATO_MODE;

public class ApexExcessionReactor extends BaseHullMod
{
    public static final float BASE_CHARGE_RATE = 100f;
    public static final float PHASE_CHARGE_MULT = 2f;
    public static final float DAMAGE_CHARGE_MULT = 0.5f;
    public static final float MAX_SYSTEM_CHARGE = 1500f;

    public static final float MAX_STORED_CHARGE = 3000f; // can "store" up to this much
    public static final float FRAG_DAMAGE_MULT = 0.5f; // frag counts less when being deleted
    public static final float ARC_RANGE = 400f;
    public static final float ARC_FIGHTER_DAMAGE = 250f;
    public static final float FIGHTER_WEIGHT = 150f; // fighter value for weighting


    public static final float ARC_SIPHON_AMOUNT = 25f; // armor stored per hit
    public static final float MAX_STORED_ARMOR = 1000f;
    public static final float REPAIR_RATE = 25f; // flat armor repaired per second

    public static final HashMap<ShipAPI, Float> damageMap = new HashMap<>(); // tracks stored damage, in case there's more than one of these things
    public static final HashMap<ShipAPI, Float> repairMap = new HashMap<>(); // tracks stored damage, in case there's more than one of these things
    public static final HashMap<ShipAPI, Float> dpTimeMap = new HashMap<>();

    public static final WeightedRandomPicker<Vector2f> arcOrigins = new WeightedRandomPicker<>();
    static
    {
        // first number is front (+)/back (-) on ship model
        arcOrigins.add(new Vector2f(100,0));
        arcOrigins.add(new Vector2f(0,0));
        arcOrigins.add(new Vector2f(0,0));
        arcOrigins.add(new Vector2f(0,0));
        arcOrigins.add(new Vector2f(0,0));
        arcOrigins.add(new Vector2f(0,0));
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id)
    {
        ship.addListener(new ApexExcessionChargeListener());
    }

    public static class ApexExcessionChargeListener implements DamageDealtModifier
    {

        @Override
        public String modifyDamageDealt(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit)
        {
            if (param instanceof DamagingProjectileAPI)
                addCharge(((DamagingProjectileAPI) param).getSource(), ((DamagingProjectileAPI) param).getDamageAmount());
            return null;
        }

        private void addCharge(ShipAPI source, float damageAmount)
        {
            damageMap.put(source, Math.min(damageMap.get(source) + damageAmount * DAMAGE_CHARGE_MULT, MAX_STORED_CHARGE));
        }
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec)
    {
        if (ship != null)
        {
            float pad = 10f;
            tooltip.addSectionHeading("Details", Alignment.MID, pad);
            tooltip.addPara("\n• Charges slowly over time.", 0);
            tooltip.addPara("• %s charge rate while phased.", 0, Misc.getHighlightColor(),
                    "+" + (int)(PHASE_CHARGE_MULT * 100f - 100f) + "%");
            tooltip.addPara("• Damaging targets generates charge.", 0);
            tooltip.addPara("• Automatically expends charge to create gravitic arcs that destroy enemy projectiles and fighters.", 0);
            tooltip.addPara("• Arcs siphon mass from targets, storing it. Stored mass is consumed to repair %s armor per second.",
                    0,
                    Misc.getHighlightColor(),
                    (int)(REPAIR_RATE) + "");
            tooltip.addPara("• Armor repair rate is multiplied by any timeflow increases.", 0);

            tooltip.addPara("• Peak performance time depletion rate ignores timeflow changes, but decreases regardless of hostile presence.", 0);
        }
    }

    @Override
    public void advanceInCombat(ShipAPI ship, float amount)
    {
        // update maps first
        if (!damageMap.containsKey(ship))
            damageMap.put(ship, MAX_STORED_CHARGE);
        if (!repairMap.containsKey(ship))
            repairMap.put(ship, 0f);
        if (!dpTimeMap.containsKey(ship))
            dpTimeMap.put(ship, 0f);

        if (!ship.getFluxTracker().isOverloadedOrVenting())
        {
            doArcs(ship, amount);
            doRepair(ship, amount);
            fixDeploymentTime(ship, amount);
        } else
        {
            repairMap.put(ship, 0f);
            damageMap.put(ship, 0f);
        }
        // show reactor/repair status
        if (ship == Global.getCombatEngine().getPlayerShip())
        {
            float storedDamage = damageMap.get(ship);
            float storedRepair = repairMap.get(ship);
            Global.getCombatEngine().maintainStatusForPlayerShip(
                    "apex_excession_reactor",
                    "graphics/icons/buffs/apex_breachcore.png",
                    "Breach Core",
                    "Charge: " + (int) (storedDamage / MAX_STORED_CHARGE * 100f) + "%" + " / Stored Repair: " + (int)storedRepair,
                    false
            );

        }
    }

    private void fixDeploymentTime(ShipAPI ship, float amount)
    {
        float dpTime = dpTimeMap.get(ship);
        dpTime += amount / ship.getMutableStats().getTimeMult().getModifiedValue();
        ship.setTimeDeployed(dpTime);
        dpTimeMap.put(ship, dpTime);
    }

    private void doRepair(ShipAPI ship, float amount)
    {
        //System.out.println("did effect tick");
        CombatEngineAPI engine = Global.getCombatEngine();
        float timeMult = ship.getMutableStats().getTimeMult().getModifiedValue();
        amount *= timeMult * timeMult; // first one is to bring it back to "normal" timeflow, second is to multiply it by timeflow.
        float repairThisFrame = Math.min(REPAIR_RATE * amount, repairMap.get(ship));
        if (repairThisFrame <= 0)
            return;

        ArmorGridAPI grid = ship.getArmorGrid();
        if (grid == null) return;
        int gridWidth = grid.getGrid().length;
        int gridHeight = grid.getGrid()[0].length;
        float maxArmorInCell = grid.getMaxArmorInCell() * ApexArmorRepairHullmod.MAX_REGEN_FRACTION;

        // first, get number of cells missing armor
        int numCellsToRepair = 0;
        for (int x = 0; x < gridWidth; x++)
        {
            for (int y = 0; y < gridHeight; y++)
            {
                if (grid.getArmorValue(x, y) < maxArmorInCell)
                    numCellsToRepair++;
            }
        }
        if (numCellsToRepair == 0)
            return;

        // then, repair the cells

        float repairPerCell = repairThisFrame / (float)numCellsToRepair;
        float repairDoneThisFrame = 0f;
        for (int x = 0; x < gridWidth; x++)
        {
            for (int y = 0; y < gridHeight; y++)
            {
                if (grid.getArmorValue(x, y) < maxArmorInCell)
                {
                    repairDoneThisFrame += Math.min(repairPerCell, maxArmorInCell - grid.getArmorValue(x, y));
                    grid.setArmorValue(x, y, Math.min(grid.getArmorValue(x, y) + repairPerCell, maxArmorInCell));
                    /*if (!ApexModPlugin.POTATO_MODE)
                    {
                        Global.getCombatEngine().addSmokeParticle(
                                Vector2f.add(grid.getLocation(x, y), MathUtils.getRandomPointInCircle(Misc.ZERO, 12f), new Vector2f()),
                                Vector2f.add(target.getVelocity(),MathUtils.getRandomPointInCircle(Misc.ZERO, 15f), new Vector2f()),
                                Misc.random.nextFloat() * 5f,
                                0.75f,
                                0.66f,
                                Color.GREEN
                        );
                    }*/
                }
            }
        }

        if (repairDoneThisFrame > 0)
        {
            if (Misc.shouldShowDamageFloaty(ship, ship))
            {
                engine.addFloatingDamageText(ship.getLocation(), repairDoneThisFrame, Color.GREEN, ship, ship);
            }
            repairMap.put(ship, Math.max(repairMap.get(ship) - repairDoneThisFrame, 0f));
            ship.syncWithArmorGridState();
        }
    }

    private void doArcs(ShipAPI ship, float amount)
    {

        // updates stored dps
        float storedDamage = damageMap.get(ship);
        if (storedDamage < MAX_STORED_CHARGE)
        {
            float toStore = BASE_CHARGE_RATE * amount * (ship.isPhased() ? PHASE_CHARGE_MULT : 1f);
            storedDamage = Math.min(MAX_STORED_CHARGE, storedDamage + toStore);
        }
        if (!ship.isPhased() && ship.isAlive())
        {
            // bias arcs towards more dangerous threats - fighters and high-damage projectiles
            // this is not particularly efficient, but it should be pretty damn hard to get more than one of these things
            WeightedRandomPicker<CombatEntityAPI> targets = new WeightedRandomPicker<>();
            for (CombatEntityAPI entity : CombatUtils.getEntitiesWithinRange(ship.getLocation(), ARC_RANGE))
            {
                if (entity.getOwner() != ship.getOwner())
                {
                    if (entity instanceof DamagingProjectileAPI)
                    {
                        DamagingProjectileAPI proj = ((DamagingProjectileAPI) entity);
                        if (proj.getDamageType().equals(DamageType.FRAGMENTATION))
                            targets.add(entity, proj.getDamageAmount() * FRAG_DAMAGE_MULT);
                        else
                            targets.add(entity, proj.getDamageAmount());
                    } else if (entity instanceof ShipAPI && ((ShipAPI) entity).getHullSize().equals(ShipAPI.HullSize.FIGHTER))
                    {
                        targets.add(entity, FIGHTER_WEIGHT);
                    }
                }
            }

            while (!targets.isEmpty() && storedDamage > 0f)
            {
                CombatEntityAPI target = targets.pickAndRemove();
                float value;
                if (target instanceof ShipAPI)
                    value = ARC_FIGHTER_DAMAGE;
                else
                    value = ((DamagingProjectileAPI) target).getDamageAmount();
                if (value < storedDamage)
                {
                    strike(target, ship);
                    storedDamage -= value;
                }
            }
        }
        // update after whatever we've done this frame
        damageMap.put(ship, storedDamage);
    }

    private void strike(CombatEntityAPI target, ShipAPI ship)
    {
        Vector2f origin = new Vector2f(arcOrigins.pick());
        VectorUtils.rotate(origin, ship.getFacing(), origin);
        Vector2f.add(ship.getLocation(), origin, origin);
        Vector2f to = target.getLocation();
        Global.getCombatEngine().spawnEmpArc(
                ship,
                origin,
                ship,
                target,
                DamageType.ENERGY,
                ARC_FIGHTER_DAMAGE,
                ARC_FIGHTER_DAMAGE,
                999999f,
                "tachyon_lance_emp_arc_impact",
                5f,
                Color.WHITE,
                Color.MAGENTA
        );
        if (!POTATO_MODE)
            ApexUtils.addWaveDistortion(target.getLocation(), 30f, 30f, 0.1f);
        float siphonAmount = ARC_SIPHON_AMOUNT;
        if (target instanceof DamagingProjectileAPI)
        {
            DamagingProjectileAPI proj = (DamagingProjectileAPI)target;
            siphonAmount = Math.min(proj.getDamageAmount() * 0.25f, siphonAmount);
        }
        repairMap.put(ship, Math.min(repairMap.get(ship) + siphonAmount, MAX_STORED_ARMOR));
        if (target instanceof DamagingProjectileAPI)
            Global.getCombatEngine().removeEntity(target);

    }


}
