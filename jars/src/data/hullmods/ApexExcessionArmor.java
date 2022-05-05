package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.scripts.util.MagicIncompatibleHullmods;

import java.util.HashSet;
import java.util.Set;

public class ApexExcessionArmor extends BaseHullMod
{
    public static final float MAX_DAMAGE_REDUCTION_MULT = 0.5f;
    public static final float ARMOR_ABSORPTION = 100f;
    public static final float MIN_ARMOR_FRACTION_MULT = 2f;

    private static final Set<String> BLOCKED_HULLMODS = new HashSet<>();
    static {
        BLOCKED_HULLMODS.add("heavyarmor");
        BLOCKED_HULLMODS.add("ugh_spongearmor");
        BLOCKED_HULLMODS.add("apex_cryo_armor");
        BLOCKED_HULLMODS.add("apex_armor");
        BLOCKED_HULLMODS.add("mhmods_hullfoam");
        BLOCKED_HULLMODS.add("mhmods_integratedarmor");
        BLOCKED_HULLMODS.add("ugh_autostruct");
        BLOCKED_HULLMODS.add("ugh_antiterminalbreakers");
        BLOCKED_HULLMODS.add("ugh_improvisedbracing");
        BLOCKED_HULLMODS.add("specialsphmod_soilnanites_upgrades");
        BLOCKED_HULLMODS.add("eis_damperhull");

        // I hope that's all of em
    }

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id)
    {
        stats.getMinArmorFraction().modifyMult(id, MIN_ARMOR_FRACTION_MULT);
        // should max it out
        stats.getMaxArmorDamageReduction().modifyFlat(id, ARMOR_ABSORPTION);
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
                        "apex_excession_armor"
                );
            }
        }
    }

    @Override
    public void advanceInCombat(ShipAPI ship, float amount)
    {
        float armorMult = 1f - MAX_DAMAGE_REDUCTION_MULT * (1f - ship.getFluxLevel());
        ship.getMutableStats().getArmorDamageTakenMult().modifyMult("apex_excession_armor", armorMult);

        if (ship == Global.getCombatEngine().getPlayerShip())
        {
            Global.getCombatEngine().maintainStatusForPlayerShip(
                    "apex_excession_armor",
                    "graphics/icons/hullsys/damper_field.png",
                    "Cryonic Nanolattice",
                    (int) (100f - armorMult * 100f ) + "% less armor damage taken",
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

            tooltip.addPara("• Armor takes less damage as flux level decreases, up to %s less at %s flux.",
                    0,
                    Misc.getHighlightColor(),
                    (int)(100f - 100f * MAX_DAMAGE_REDUCTION_MULT) + "%", "0%");

            tooltip.addPara("• Incompatible with most armor modifications.",
                    0,
                    Misc.getNegativeHighlightColor(),
                    "Incompatible");
        }
    }
}
