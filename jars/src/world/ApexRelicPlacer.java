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
import com.fs.starfarer.api.util.WeightedRandomPicker;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.util.*;

import static plugins.ApexModPlugin.GENERATE_SYSTEMS;
import static plugins.ApexModPlugin.GENERATE_RELICS;

public class ApexRelicPlacer
{
    public static final Logger LOGGER = Global.getLogger(ApexRelicPlacer.class);

    private static final WeightedRandomPicker<String> relicPicker = new WeightedRandomPicker<>();
    static
    {
        relicPicker.add("apex_apotheosis_strike");
        relicPicker.add("apex_spectrum_fighter");
        //relicPicker.add("apex_spectrum_fighter");
        relicPicker.add("apex_alligator_line");
        relicPicker.add("apex_crocodile_antishield");
        relicPicker.add("apex_caiman_line");
        relicPicker.add("apex_gharial_strike");
    }

    private static final Set<String> STAR_TYPES = new HashSet<>();

    static
    {
        String hint = "nosy little fucker, aren't you?";
        STAR_TYPES.add("star_white");
        //STAR_TYPES.add("star_red_dwarf");
        //STAR_TYPES.add("star_browndwarf");
    }

    private static final LinkedHashMap<LocationType, Float> WEIGHTS = new LinkedHashMap<>();

    static
    {
        WEIGHTS.put(LocationType.NEAR_STAR, 3f);
        WEIGHTS.put(LocationType.L_POINT, 2f);
    }


    public void generate(SectorAPI sector)
    {
        if (!GENERATE_RELICS)
            return;
        if (!GENERATE_SYSTEMS)
        {
            relicPicker.add("apex_apex_line");
            //relicPicker.add("apex_apotheosis_strike");
        }
        WeightedRandomPicker<StarSystemAPI> systemPicker = getSpawnSystems(sector);

        Global.getSector().getMemoryWithoutUpdate().set("$apex_placed_relics", true);

        int attempts = 0;
        int maxAttempts = relicPicker.getItems().size() + 5;
        while (!relicPicker.isEmpty() && attempts < maxAttempts)
        {
            attempts++;
            if (attempts == maxAttempts)
                LOGGER.warn("Apex Relics: reached maximum relic placement attempts, giving up.");
            // pick the system to spawn things in
            StarSystemAPI system = systemPicker.pick();
            if (system == null)
            {
                LOGGER.log(Level.WARN, "Apex Relics: Failed to find a valid system.");
                return;
            }
            // pick the location in the system
            WeightedRandomPicker<BaseThemeGenerator.EntityLocation> points = BaseThemeGenerator.getLocations(new Random(), system, 50f, WEIGHTS);
            BaseThemeGenerator.EntityLocation spawnLoc = points.pick();
            if (spawnLoc == null)
            {
                LOGGER.warn("Apex Relics: failed to place hull.");
                continue;
            }
            // pick the hull and remove it from the list
            String hull = relicPicker.pickAndRemove();
            // place the hull
            DerelictShipEntityPlugin.DerelictShipData derelictData = new DerelictShipEntityPlugin.DerelictShipData(new ShipRecoverySpecial.PerShipData(hull, ShipRecoverySpecial.ShipCondition.AVERAGE), false);
            SectorEntityToken ship = BaseThemeGenerator.addSalvageEntity(system, Entities.WRECK, "apex_design", derelictData);
            ship.setFaction("neutral");
            ship.setDiscoverable(true);
            ship.setOrbit(spawnLoc.orbit);
            LOGGER.log(Level.INFO, "Apex Relics: spawned " + hull + " in " + system.getName());
        }
        if (!GENERATE_SYSTEMS)
        {// pick star for research station and spawn it
            StarSystemAPI system = systemPicker.pick();
            SectorEntityToken station = DerelictThemeGenerator.addSalvageEntity(system, "station_research", Factions.NEUTRAL);
            station.setCircularOrbit(system.getStar(), 120, system.getStar().getRadius() * 2.5f, 50);
            CargoAPI extraSalvage = Global.getFactory().createCargo(true);
            extraSalvage.addSpecial(new SpecialItemData("apex_weapons_package", null), 1);
            extraSalvage.addSpecial(new SpecialItemData("apex_hulls_package", null), 1);
            extraSalvage.addHullmods("apex_armor", 1);
            extraSalvage.addHullmods("apex_cryo_armor", 1);
            extraSalvage.addHullmods("apex_geodesic_shield", 1);
            extraSalvage.addHullmods("apex_armor_repairer", 1);
            extraSalvage.addHullmods("apex_range_sync", 1);
            extraSalvage.addHullmods("apex_cryo_projector", 1);
            extraSalvage.addHullmods("apex_flare_system", 1);
            BaseSalvageSpecial.addExtraSalvage(extraSalvage, station.getMemoryWithoutUpdate(), -1);
            LOGGER.log(Level.INFO, "Apex Relics: spawned research station in " + system.getName());
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
