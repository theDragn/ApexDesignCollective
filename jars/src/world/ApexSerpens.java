package world;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.MarketConditionAPI;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.impl.campaign.procgen.NebulaEditor;
import com.fs.starfarer.api.impl.campaign.terrain.HyperspaceTerrainPlugin;
import com.fs.starfarer.api.util.Misc;

import static data.ApexUtils.text;
import static plugins.ApexModPlugin.GENERATE_SYSTEMS;

public class ApexSerpens implements SectorGeneratorPlugin
{
    @Override
    public void generate(SectorAPI sector)
    {
        if (!GENERATE_SYSTEMS)
            return;
        // TODO: blue giant star, green corona, station + market, Sapphire planet, Topaz planet, asteroid belt
        StarSystemAPI system = sector.createStarSystem("apex_serpens");
        system.setBaseName(text("serpens"));

        system.getLocation().set(-5800, -16000);
        system.setBackgroundTextureFilename("graphics/backgrounds/background6.jpg");
        PlanetAPI star = system.initStar("apex_serpens", StarTypes.BLUE_GIANT, 1000f, 1000f);
        PlanetAPI sapphire = system.addPlanet("apex_sapphire", star, text("sapphire"), "water", 69f, 150f, 4000f, 365f);
        PlanetAPI topaz = system.addPlanet("apex_topaz", star, text("topaz"), "gas_giant", 270f, 350f, 6000f, 490f);
        SectorEntityToken serpensStation = system.addCustomEntity("apex_serpens_station",text("serpensStation"), "station_hightech1", "apex_design");

        // add comm relay for stability
        SectorEntityToken commRelay = system.addCustomEntity(
                "apex_serpens_comm",
                text("commrel"),
                Entities.COMM_RELAY,
                "apex_design"
        );
        commRelay.setCircularOrbit(star, 90f, 6000f, 490f);

        Misc.initConditionMarket(sapphire);
        MarketAPI sapphireMarket = sapphire.getMarket();
        sapphireMarket.addCondition(Conditions.TOXIC_ATMOSPHERE);
        // TODO: custom condition for toxic atmosphere
        sapphireMarket.addCondition(Conditions.WATER_SURFACE);
        sapphireMarket.addCondition(Conditions.INIMICAL_BIOSPHERE);
        sapphireMarket.addCondition(Conditions.MILD_CLIMATE);
        sapphireMarket.setPrimaryEntity(sapphire);
        sapphire.setMarket(sapphireMarket);
        sapphire.setCustomDescriptionId("apex_sapphire");
        for (MarketConditionAPI mc : sapphireMarket.getConditions())
        {
            mc.setSurveyed(true);
        }

        Misc.initConditionMarket(topaz);
        MarketAPI topazMarket = topaz.getMarket();
        topazMarket.addCondition(Conditions.DENSE_ATMOSPHERE);
        topazMarket.addCondition(Conditions.VOLATILES_DIFFUSE);
        topazMarket.setPrimaryEntity(topaz);
        topaz.setMarket(topazMarket);
        for (MarketConditionAPI mc : topazMarket.getConditions())
        {
            mc.setSurveyed(true);
        }

        MarketAPI serpensStationMarket = Global.getFactory().createMarket("apex_serpens_market", serpensStation.getName(), 3);
        serpensStationMarket.setPrimaryEntity(serpensStation);
        serpensStationMarket.setFactionId("apex_design");
        serpensStationMarket.getTariff().modifyFlat("default_tariff", serpensStationMarket.getFaction().getTariffFraction());
        serpensStationMarket.addCondition(Conditions.POPULATION_3);
        serpensStationMarket.addCondition(Conditions.NO_ATMOSPHERE);
        serpensStationMarket.addCondition("apex_collectivedefense");
        serpensStationMarket.addIndustry(Industries.SPACEPORT);
        serpensStationMarket.addIndustry(Industries.WAYSTATION);
        serpensStationMarket.addIndustry(Industries.POPULATION);
        serpensStationMarket.addIndustry(Industries.MILITARYBASE);
        serpensStationMarket.addIndustry(Industries.GROUNDDEFENSES);
        serpensStationMarket.addIndustry(Industries.ORBITALSTATION_HIGH);
        serpensStationMarket.addSubmarket("apex_fake_open_market");
        serpensStationMarket.addSubmarket(Submarkets.SUBMARKET_BLACK);
        serpensStationMarket.addSubmarket(Submarkets.SUBMARKET_STORAGE);
        serpensStationMarket.addSubmarket(Submarkets.GENERIC_MILITARY);

        serpensStation.setFaction("apex_design");
        for (MarketConditionAPI mc : serpensStationMarket.getConditions())
        {
            mc.setSurveyed(true);
        }
        serpensStationMarket.setSurveyLevel(MarketAPI.SurveyLevel.FULL);
        serpensStation.setMarket(serpensStationMarket);
        serpensStation.setCircularOrbitPointingDown(star, 50, 2450f, 200f);
        serpensStation.setCustomDescriptionId("apex_serpens_station");
        sector.getEconomy().addMarket(serpensStationMarket, true);

        // the following is hyperspace cleanup code that will remove hyperstorm clouds around this system's location in hyperspace
        // don't need to worry about this, it's more or less copied from vanilla
        system.autogenerateHyperspaceJumpPoints(true, true);
        system.generateAnchorIfNeeded();
        // set up hyperspace editor plugin
        HyperspaceTerrainPlugin hyperspaceTerrainPlugin = (HyperspaceTerrainPlugin) Misc.getHyperspaceTerrain().getPlugin(); // get instance of hyperspace terrain
        NebulaEditor nebulaEditor = new NebulaEditor(hyperspaceTerrainPlugin); // object used to make changes to hyperspace nebula

        // set up radiuses in hyperspace of system
        float minHyperspaceRadius = hyperspaceTerrainPlugin.getTileSize() * 2.5f;
        float maxHyperspaceRadius = system.getMaxRadiusInHyperspace();

        // hyperstorm-b-gone (around system in hyperspace)
        nebulaEditor.clearArc(system.getLocation().x, system.getLocation().y, 0,
                minHyperspaceRadius + maxHyperspaceRadius, 0f, 360f, 0.25f);
    }
}
