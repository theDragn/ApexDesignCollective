package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

public class ApexExcessionArmor extends BaseHullMod
{
    public static final float MAX_ARMOR_EFFECTIVENESS = 2f;
    public static final float ARMOR_ABSORPTION = 100f;
    public static final float MIN_ARMOR_FRACTION_MULT = 2f;

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id)
    {
        stats.getMinArmorFraction().modifyMult(id, MIN_ARMOR_FRACTION_MULT);
        // should max it out
        stats.getMaxArmorDamageReduction().modifyFlat(id, ARMOR_ABSORPTION);
    }

    @Override
    public void advanceInCombat(ShipAPI ship, float amount)
    {
        float armorMult = 1f + MAX_ARMOR_EFFECTIVENESS * (1f - ship.getFluxLevel());
        ship.getMutableStats().getEffectiveArmorBonus().modifyMult("apex_excession_armor", armorMult);

        if (ship == Global.getCombatEngine().getPlayerShip())
        {
            Global.getCombatEngine().maintainStatusForPlayerShip(
                    "apex_excession_armor",
                    "graphics/icons/hullsys/damper_field.png",
                    "Cryonic Nanolattice",
                    (int) (armorMult * 100f - 100f) + "% increased armor effectiveness",
                    false
            );

        }
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec)
    {
        if (ship != null)
        {
            float pad = 10f;
            tooltip.addSectionHeading("Details", Alignment.MID, pad);

            tooltip.addPara("\n• Maximum armor damage reduction is %s.",
                    0,
                    Misc.getHighlightColor(),
                    (int) (ARMOR_ABSORPTION) + "%");

            tooltip.addPara("• %s minimum armor rating.",
                    0,
                    Misc.getHighlightColor(),
                    "Doubles");

            tooltip.addPara("• Armor effectiveness increases as flux level decreases, up to %s at %s flux.",
                    0,
                    Misc.getHighlightColor(),
                    (int)(100f * MAX_ARMOR_EFFECTIVENESS) + "%", "0%");
        }
    }
}
