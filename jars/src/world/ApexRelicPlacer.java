package world;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.impl.campaign.DerelictShipEntityPlugin;
import com.fs.starfarer.api.impl.campaign.ids.Entities;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator;
import com.fs.starfarer.api.impl.campaign.procgen.themes.BaseThemeGenerator;
import com.fs.starfarer.api.impl.campaign.procgen.themes.BaseThemeGenerator.LocationType;
import com.fs.starfarer.api.impl.campaign.procgen.themes.DerelictThemeGenerator;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.special.BaseSalvageSpecial;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.special.ShipRecoverySpecial;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.Pair;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.util.*;

import static plugins.ApexModPlugin.GENERATE_SYSTEMS;
import static plugins.ApexModPlugin.GENERATE_RELICS;
import static utils.ApexUtils.text;

public class ApexRelicPlacer implements SectorGeneratorPlugin
{
    public static final Logger LOGGER = Global.getLogger(ApexRelicPlacer.class);

    private WeightedRandomPicker<String> relicHullPicker = new WeightedRandomPicker<>();
    private WeightedRandomPicker<String> relicWeaponPicker;
    private WeightedRandomPicker<String> relicBPPicker = new WeightedRandomPicker<>();

    private static final Set<String> STAR_TYPES = new HashSet<>();
    static
    {
        String hint = "nosy little fucker, aren't you?";
        STAR_TYPES.add("star_white");
    }

    private static final LinkedHashMap<LocationType, Float> WEIGHTS = new LinkedHashMap<>();

    static
    {
        WEIGHTS.put(LocationType.NEAR_STAR, 3f);
        WEIGHTS.put(LocationType.L_POINT, 2f);
    }

    @Override
    public void generate(SectorAPI sector)
    {

        relicBPPicker.add("apex_ins_missile");
        relicBPPicker.add("apex_ins_torp_l");
        relicBPPicker.add("apex_ins_torp_s");
        relicBPPicker.add("apex_ins_flak");
        relicBPPicker.add("apex_ins_plasma");
        relicBPPicker.add("apex_ins_mhdgun");
        relicBPPicker.add("apex_ins_shattercan");
        relicBPPicker.add("apex_ins_shatterpod");
        relicWeaponPicker = relicBPPicker.clone();

        //relicHullPicker.add("apex_apotheosis_strike");
        relicHullPicker.add("apex_ins_destroyer_relic");
        relicHullPicker.add("apex_ins_destroyer_relic");
        relicHullPicker.add("apex_ins_capital_relic");

        if (!GENERATE_RELICS)
            return;

        WeightedRandomPicker<StarSystemAPI> systemPicker = getSpawnSystems(sector);

        Global.getSector().getMemoryWithoutUpdate().set("$apex_placed_relics", true);


        for (int i = 0; i < 2; i++)
        {
            // pick the system to spawn things in
            StarSystemAPI system = systemPicker.pick();
            if (system == null)
            {
                LOGGER.log(Level.WARN, "Apex Relics: Failed to find a valid system, aborting.");
                return;
            }
            // pick the hull and remove it from the list
            if (relicHullPicker.isEmpty()) break;
            String hull = relicHullPicker.pickAndRemove();
            if (hull == null || hull.equals(""))
            {
                LOGGER.log(Level.WARN, "somehow picked null for the ship hull, skipping");
                continue;
            }
            // place the hull
            DerelictShipEntityPlugin.DerelictShipData derelictData = new DerelictShipEntityPlugin.DerelictShipData(new ShipRecoverySpecial.PerShipData(hull, ShipRecoverySpecial.ShipCondition.PRISTINE), false);
            SectorEntityToken ship = BaseThemeGenerator.addSalvageEntity(system, Entities.WRECK, "apex_design", derelictData);
            ship.setFaction("neutral");
            ship.setDiscoverable(true);
            // always put these pretty close to the star rather than possibly scattered way out in the system
            ship.setCircularOrbit(system.getStar(), Misc.random.nextFloat() * 360, system.getStar().getRadius() * 2.5f, 50);
            //LOGGER.log(Level.INFO, "Apex Relics: spawned " + hull + " in " + system.getName());
        }
        // relic weapon caches
        // each one has some relic guns, some random high-value guns, and one relic weapon BP
        for (int i = 0; i < 6; i++)
        {
            if (relicWeaponPicker.isEmpty()) break;
            if (relicBPPicker.isEmpty()) break;
            // pick the system to spawn things in
            StarSystemAPI system = systemPicker.pick();
            if (system == null)
            {
                LOGGER.log(Level.WARN, "Apex Relics: Failed to find a valid system.");
                return;
            }
            String bp = relicBPPicker.pickAndRemove();
            if (bp == null || bp.equals(""))
            {
                LOGGER.log(Level.WARN, "somehow picked null for the weapon bp, skipping");
                continue;
            }

            // create our cache and loot holder thingy
            SectorEntityToken cache = DerelictThemeGenerator.addSalvageEntity(system, "weapons_cache", Factions.NEUTRAL);
            cache.setCircularOrbit(system.getStar(), 120, system.getStar().getRadius() * 2.5f, 50);
            CargoAPI extraSalvage = Global.getFactory().createCargo(true);


            extraSalvage.addSpecial(new SpecialItemData("weapon_bp", bp), 1);
            extraSalvage.addWeapons(relicWeaponPicker.pick(), Misc.random.nextInt(3)+2);
            extraSalvage.addWeapons(relicWeaponPicker.pick(), Misc.random.nextInt(3)+2);
            // :3
            if (Misc.random.nextInt() % 5000 == 0 && Global.getSettings().getModManager().isModEnabled("tahlan")) {
                extraSalvage.addSpecial(new SpecialItemData("tahlan_rare_bp_package", null), 1);
                LOGGER.log(Level.INFO, "tell nia. I want him to know it was me.");
            }

            BaseSalvageSpecial.addExtraSalvage(extraSalvage, cache.getMemoryWithoutUpdate(), -1);
            cache.setName(text("cache"));
            //LOGGER.log(Level.INFO, "Apex Relics: spawned relic cache in " + system.getName());
        }
    }

    public static WeightedRandomPicker<StarSystemAPI> getSpawnSystems(SectorAPI sector)
    {
        WeightedRandomPicker<StarSystemAPI> picker = new WeightedRandomPicker<>();
        for (StarSystemAPI system : sector.getStarSystems())
        {
            // system has star, system only has one star, system has the right star, system isn't inhabited
            if (system.getType() == StarSystemGenerator.StarSystemType.SINGLE
                    && system.getStar() != null
                    && STAR_TYPES.contains(system.getStar().getTypeId())
                    && system.isProcgen())
            {
                picker.add(system);
            }
        }
        return picker;
    }
}
