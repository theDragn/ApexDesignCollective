package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.DamageTakenModifier;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.scripts.util.MagicIncompatibleHullmods;
import data.scripts.util.MagicRender;
import org.jetbrains.annotations.Nullable;
import org.lazywizard.lazylib.CollisionUtils;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.DefenseUtils;
import org.lwjgl.util.vector.Vector2f;
import plugins.ApexModPlugin;

import java.awt.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ApexShield extends BaseHullMod
{
    private Map<ShipAPI, ApexShieldTracker> trackerMap = new HashMap<>(); // stores the base shield arc for each ship when deployed

    public static final float DAMAGE_REDUCTION = 20f; // reduces incoming damage/beam dps by this flat amount
    public static final float TRANSFER_MULT = 1.25f;
    public static final float CUTOFF_FRACTION = 0.3f;

    public static final float UNFOLD_BONUS = 50f;
    public static final float SMOD_ARC_BONUS = 60f;

    public static final Color FRAG_COLOR = new Color(255, 225, 131);
    public static final Color KIN_COLOR = new Color(199, 182, 158);
    public static final Color ENG_COLOR = new Color(125, 194, 255);
    public static final Color HE_COLOR = new Color(208, 52, 56);

    private static final Set<String> BLOCKED_HULLMODS = new HashSet<>();

    static
    {
        BLOCKED_HULLMODS.add("frontemitter");
        BLOCKED_HULLMODS.add("shield_shunt");
        BLOCKED_HULLMODS.add("adaptiveshields");
        BLOCKED_HULLMODS.add("hardenedshieldemitter");
        // TODO: any others that need to be blocked?
    }

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id)
    {
        stats.getShieldUnfoldRateMult().modifyPercent(id, UNFOLD_BONUS);
        if (stats.getVariant().getSMods().contains("apex_geodesic_shield"))
        {
            stats.getShieldArcBonus().modifyFlat(id, SMOD_ARC_BONUS);
        }
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id)
    {
        for (String hullmod : BLOCKED_HULLMODS)
        {
            if (ship.getVariant().getHullMods().contains(hullmod))
            {
                MagicIncompatibleHullmods.removeHullmodWithWarning(
                        ship.getVariant(),
                        hullmod,
                        "apex_geodesic_shield"
                );
            }
        }
        if (!ship.hasListenerOfClass(ApexShieldListener.class))
            ship.addListener(new ApexShieldListener(ship));
    }

    public boolean isApplicableToShip(ShipAPI ship)
    {
        if (ship == null || ship.getShield() == null)
            return false;
        for (String hullmod : BLOCKED_HULLMODS)
        {
            if (ship.getVariant().getHullMods().contains(hullmod))
                return false;
        }
        return true;
    }

    public String getUnapplicableReason(ShipAPI ship)
    {
        if (ship == null)
        {
            return "Ship does not exist, what the fuck";
        }
        if (ship.getShield() == null)
            return "Ship must have a shield.";
        for (String hullmod : BLOCKED_HULLMODS)
        {
            if (ship.getVariant().getHullMods().contains(hullmod))
            {
                return "Incompatible with " + Global.getSettings().getHullModSpec(hullmod).getDisplayName() + ".";
            }
        }

        return null;
    }

    @Override
    public void advanceInCombat(ShipAPI ship, float amount)
    {
        if (Global.getCombatEngine().isPaused() || ship == null || !ship.isAlive() || ship.getShield() == null)
        {
            return;
        }
        CombatEngineAPI engine = Global.getCombatEngine();

        // this stuff makes sure that nothing can decrease the shield arc while the shield is active
        if (!trackerMap.containsKey(ship))
        {
            trackerMap.put(ship, new ApexShieldTracker(ship.getShield().getArc(), 0f));
        }
        ApexShieldTracker tracker = trackerMap.get(ship);
        // shield is on
        if (ship.getShield().isOn())
        {
            // shield was on last frame, arc has shrunk, and is now smaller than the original maximum arc
            if (tracker.arcLastFrame > ship.getShield().getActiveArc()
                    && tracker.originalMaxArc > ship.getShield().getActiveArc())
                ship.getShield().setActiveArc(tracker.originalMaxArc);
            tracker.arcLastFrame = ship.getShield().getActiveArc();
            for (WeaponAPI weapon : ship.getAllWeapons())
            {
                // basically if the weapon is near max health and under the shield arc, make it full health
                // can't make guns immune to bleedthrough damage but also not immune to EMP arcs
                if (weapon.getCurrHealth() > weapon.getMaxHealth() * 0.75f && Math.abs(MathUtils.getShortestRotation(VectorUtils.getAngle(ship.getShield().getLocation(), weapon.getLocation()), ship.getShield().getFacing())) < ship.getShield().getActiveArc() / 2f)
                {
                    if (weapon.isDisabled() || weapon.getCurrHealth() == weapon.getMaxHealth())
                        continue;
                    else
                        weapon.setCurrHealth(weapon.getMaxHealth());
                    if (weapon.isDisabled() && !weapon.isPermanentlyDisabled())
                        weapon.repair();
                }
            }
            for (ShipEngineControllerAPI.ShipEngineAPI shipEngine : ship.getEngineController().getShipEngines())
            {
                // basically if the engine is near max health and under the shield arc, make it full health
                // can't make them immune to bleedthrough damage but also not immune to EMP arcs
                if (shipEngine.getHitpoints() > shipEngine.getMaxHitpoints() * 0.75f && Math.abs(MathUtils.getShortestRotation(VectorUtils.getAngle(ship.getShield().getLocation(), shipEngine.getLocation()), ship.getShield().getFacing())) < ship.getShield().getActiveArc() / 2f)
                {
                    if (shipEngine.isDisabled() || shipEngine.getHitpoints() == shipEngine.getMaxHitpoints())
                        continue;
                    else
                        shipEngine.setHitpoints(shipEngine.getMaxHitpoints());
                }
            }
        }
        // shield is off, reset last frame to zero
        else
        {
            tracker.arcLastFrame = 0f;
        }
    }

    private class ApexShieldTracker
    {
        public float arcLastFrame = 0f;
        public float originalMaxArc = 0f;

        public ApexShieldTracker(float arcLastFrame, float originalMaxArc)
        {
            this.arcLastFrame = arcLastFrame;
            this.originalMaxArc = originalMaxArc;
        }
    }

    public static class ApexShieldListener implements DamageTakenModifier
    {
        protected ShipAPI ship;

        public ApexShieldListener(ShipAPI ship)
        {
            this.ship = ship;
        }

        public String modifyDamageTaken(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit)
        {
            if (shieldHit && target instanceof ShipAPI) {
                if (param instanceof DamagingProjectileAPI) {
                    DamagingProjectileAPI proj = (DamagingProjectileAPI) param;
                    float originalDamage = damage.computeDamageDealt(Global.getCombatEngine().getElapsedInLastFrame());
                    ApexShield.spawnGeodesicSpall(point, target, damage);
                    Vector2f bypassLoc = getBypassLocation(proj, target);
                    if (bypassLoc == null)
                        return null;
                    float armorFraction = DefenseUtils.getArmorLevel((ShipAPI) target, bypassLoc);
                    if (armorFraction < CUTOFF_FRACTION)
                        return null;
                    float reduction = DAMAGE_REDUCTION * armorFraction * (originalDamage / damage.getDamage());
                    if (originalDamage < reduction)
                        reduction = originalDamage;
                    applyBypassDamage(proj.getSource(), reduction, target, bypassLoc);
                    damage.getModifier().modifyMult("apexGeodesic", 1f - reduction / originalDamage);
                    spawnGeodesicSpall(point, target, damage);
                } else if (param instanceof BeamAPI)
                {
                    BeamAPI beam = (BeamAPI)param;

                    float originalDamage = damage.getDamage();
                    ApexShield.spawnGeodesicSpall(point, target, damage);
                    Vector2f bypassLoc = getBypassLocation(beam, target);
                    if (bypassLoc == null)
                        return null;
                    float armorFraction = DefenseUtils.getArmorLevel((ShipAPI) target, bypassLoc);
                    if (armorFraction < CUTOFF_FRACTION)
                        return null;
                    float reduction = DAMAGE_REDUCTION * armorFraction * Global.getCombatEngine().getElapsedInLastFrame();
                    if (originalDamage < reduction)
                        reduction = originalDamage;
                    applyBypassDamage(beam.getSource(), reduction, target, bypassLoc);
                    damage.getModifier().modifyMult("apexGeodesic", 1f - reduction / originalDamage);
                    spawnGeodesicSpall(point, target, damage);
                }
            }
            return null;
        }

        public @Nullable Vector2f getBypassLocation(DamagingProjectileAPI proj, CombatEntityAPI target)
        {
            Vector2f endpoint = new Vector2f();
            Vector2f.add(proj.getLocation(), proj.getVelocity(), endpoint);
            return CollisionUtils.getCollisionPoint(proj.getLocation(), endpoint, target);
        }

        public @Nullable Vector2f getBypassLocation(BeamAPI beam, CombatEntityAPI target)
        {
            // doubles the beam's length and checks that line for collision
            Vector2f endpoint = new Vector2f();
            Vector2f beamVel = Vector2f.sub(beam.getTo(), beam.getFrom(), null);
            Vector2f.add(beamVel, beam.getTo(), endpoint);
            return CollisionUtils.getCollisionPoint(beam.getFrom(), endpoint, target);
        }

        public void applyBypassDamage(ShipAPI source, float remainingDamage, CombatEntityAPI target, Vector2f bypassloc)
        {
            Global.getCombatEngine().applyDamage(target,
                    bypassloc,
                    remainingDamage * TRANSFER_MULT,
                    DamageType.ENERGY,
                    0f,
                    true,
                    false,
                    source);
        }
    }

    @Override
    public String getDescriptionParam(int index, ShipAPI.HullSize hullSize)
    {
        return null;
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec)
    {
        if (ship != null)
        {
            float pad = 10f;
            tooltip.addSectionHeading("Details", Alignment.MID, pad);

            tooltip.addPara("\n• Shields take %s less damage from projectile hits. \n• Shields take %s less damage per second from beams.",
                    0,
                    Misc.getHighlightColor(),
                    (int) (DAMAGE_REDUCTION) + "", (int) (DAMAGE_REDUCTION) + "");
            Color[] colors = {ENG_COLOR, Misc.getHighlightColor()};
            tooltip.addPara("• Each time this bonus prevents damage, the ship's armor takes %s damage equal to %s of the prevented damage.",
                    0,
                    colors,
                    "energy", (int) (TRANSFER_MULT * 100) + "%");
            tooltip.addPara("• The damage reduction decreases proportionally with the ship's armor, becoming ineffective at %s armor.",
                    0,
                    Misc.getHighlightColor(),
                    (int) (CUTOFF_FRACTION * 100f) + "%");
            tooltip.addPara("• Increases shield unfolding rate by %s.",
                    0f,
                    Misc.getHighlightColor(),
                    (int) (UNFOLD_BONUS) + "%");
            tooltip.addPara("• Prevents the shield arc from being reduced while the shield is active.", 0);
            Color incompatTextColor = Misc.getTextColor();
            for (String hullmod : BLOCKED_HULLMODS)
            {
                if (ship.getVariant().getHullMods().contains(hullmod))
                {
                    incompatTextColor = Misc.getNegativeHighlightColor();
                }
            }
            tooltip.addPara("%s", 0, incompatTextColor, "• Incompatible with hardened shields and other shield conversions.");

            if (ship.getVariant().getSMods().contains("apex_geodesic_shield"))
            {
                tooltip.addPara("S-mod Bonus: Shield arc is increased by %s degrees",
                        10f,
                        Misc.getPositiveHighlightColor(),
                        Misc.getHighlightColor(),
                        (int) (SMOD_ARC_BONUS) + "");
            } else
            {
                tooltip.addPara("If this hullmod is built in, it will increase shield arc by %s degrees",
                        10f,
                        Misc.getHighlightColor(),
                        (int) (SMOD_ARC_BONUS) + "", (int) (UNFOLD_BONUS) + "%");
            }
        }
    }

    public static void spawnGeodesicSpall(Vector2f point, CombatEntityAPI target, DamageAPI damage)
    {
        if (ApexModPlugin.POTATO_MODE)
            return;
        if (!MagicRender.screenCheck(500f, point))
            return;
        // figure out offset

        float damAmount = damage.getDamage();
        Vector2f offset = Vector2f.sub(point, target.getLocation(), new Vector2f());
        offset = Misc.rotateAroundOrigin(offset, -target.getFacing());

        // scale factor
        float scaleFactor = Math.min(Misc.random.nextFloat() * 0.66f + 0.33f, 1f);
        scaleFactor *= (damAmount > 100) ? 1.5 : 1;
        scaleFactor *= (damAmount > 500) ? 1.5 : 1;
        Vector2f scale = new Vector2f(32f, 32f);
        scale.scale(scaleFactor);

        // more spalling for higher damage
        int numSpall = 1;
        if (damAmount > 100)
            numSpall++;
        if (damAmount > 500)
            numSpall += 2;
        for (int i = 0; i < numSpall; i++)
        {
            MagicRender.objectspace(
                    Global.getSettings().getSprite("graphics/effects/geodesic_spall.png"),    // sprite
                    target,         // anchor
                    offset,         // offset from anchor
                    MathUtils.getRandomPointInCircle(Misc.ZERO, 100f),        // velocity
                    scale,  // scale
                    new Vector2f(-5, -5),            // growth
                    Misc.random.nextFloat() * 360f,//angle,          // angle
                    Misc.random.nextFloat() * 90f - 45f,             // spin
                    true,           // parent? true makes it follow anchor in orientation and position
                    target.getShield().getRingColor(),  // will never be null since we need a shield to get to this point
                    true,           // additive blending?
                    0,             // fadein time
                    0,             // full time
                    0.5f,           // fadeout time
                    true        // fade on death
            );
        }
    }
}
