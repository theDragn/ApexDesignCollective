package world;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.MarketConditionAPI;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.impl.campaign.procgen.NebulaEditor;
import com.fs.starfarer.api.impl.campaign.procgen.StarAge;
import com.fs.starfarer.api.impl.campaign.terrain.HyperspaceTerrainPlugin;
import com.fs.starfarer.api.util.Misc;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;

import static data.ApexUtils.text;
import static plugins.ApexModPlugin.GENERATE_SYSTEMS;

public class ApexVela implements SectorGeneratorPlugin
{
    @Override
    public void generate(SectorAPI sector)
    {
        if (!GENERATE_SYSTEMS)
            return;
        StarSystemAPI system = sector.createStarSystem("apex_vela");
        system.setBaseName(text("Vela"));

        system.getLocation().set(-6275,	-13800);
        system.setBackgroundTextureFilename("graphics/backgrounds/background_galatia.jpg");
        // make star
        PlanetAPI star = system.initStar("apex_vela_star", StarTypes.ORANGE, 1000f, 500f);
        // make planets
        PlanetAPI glass = system.addPlanet("apex_glass", star,text("glass"), "desert", 69f, 125f,4300f, 225f);
        PlanetAPI granite = system.addPlanet("apex_granite", star,text("granite"), "rocky_metallic", 420f, 90f,3000f, 290f);
        PlanetAPI emerald = system.addPlanet("apex_emerald", star,text("emerald"), "jungle", 0f, 115f,5150f, 365f);
        PlanetAPI onyx = system.addPlanet("apex_onyx", star,text("onyx"), "barren", 180f, 65f,6000f, 600f);
        // make station for Onyx
        SectorEntityToken onyxStation = system.addCustomEntity("apex_onyx_station",text("onyxStation"), "station_hightech2", "apex_design");

        // add some nebula clouds around the outer system
        // supposedly you can do this with pure code, but a png is really easy
        SectorEntityToken vela_nebula = Misc.addNebulaFromPNG("graphics/terrain/apex_vela_terrain.png",
                0, 0, // center of nebula
                system, // location to add to
                "terrain", "nebula", // "nebula_blue", // texture to use, uses xxx_map for map
                4, 4, StarAge.AVERAGE); // number of cells in texture

        // add comm relay for stability
        SectorEntityToken commRelay = system.addCustomEntity(
                "apex_vela_comm",
                text("commrel"),
                Entities.COMM_RELAY,
                "apex_design"
        );
        commRelay.setCircularOrbit(star, 90f, 6500f, 600f);

        // nav relay for nyoom
        SectorEntityToken navRelay = system.addCustomEntity(
                "apex_vela_nav",
                text("navrel"),
                Entities.NAV_BUOY,
                "apex_design"
        );
        navRelay.setCircularOrbit(star, 270f, 3500f, 290f);

        // a lil asteroid belt to hold up our asteroid pants
        system.addAsteroidBelt(
                star, //orbit focus
                80, //number of asteroid entities
                3700, //orbit radius is 500 gap for outer randomly generated entity above
                300, //width of band
                190, //minimum and maximum visual orbit speeds of asteroids
                220,
                Terrain.ASTEROID_BELT, //ID of the terrain type that appears in the section above the abilities bar
                text("belt") //display name
        );

        //add a ring texture. it will go under the asteroid entities generated above
        system.addRingBand(star,
                "misc", //used to access band texture, this is the name of a category in settings.json
                "rings_asteroids0", //specific texture id in category misc in settings.json
                256f, //texture width, can be used for scaling shenanigans
                2,
                Color.WHITE, //colour tint
                256f, //band width in game
                3700, //same as above
                200f,
                null,
                null
        );

        // added these jump points manually because it wasn't autogenerating them
        // outer jump point
        JumpPointAPI jumpPoint1 = Global.getFactory().createJumpPoint("apex_vela_jp1", text("jp1"));
        jumpPoint1.setOrbit(Global.getFactory().createCircularOrbit(star, 180, 6500, 600));
        jumpPoint1.setRelatedPlanet(onyx);
        jumpPoint1.setStandardWormholeToHyperspaceVisual();
        system.addEntity(jumpPoint1);

        // inner jump point
        JumpPointAPI jumpPoint2 = Global.getFactory().createJumpPoint("apex_vela_jp2", text("jp2"));
        jumpPoint2.setOrbit(Global.getFactory().createCircularOrbit(star, 0, 2000, 270));
        jumpPoint2.setRelatedPlanet(granite);
        jumpPoint2.setStandardWormholeToHyperspaceVisual();
        system.addEntity(jumpPoint2);

        // market setup bullshit
        // this is for Glass
        MarketAPI glassMarket = Global.getFactory().createMarket("apex_glass_market", glass.getName(), 5);
        glassMarket.setPrimaryEntity(glass);
        glassMarket.setFactionId("apex_design");
        glassMarket.getTariff().modifyFlat("default_tariff", glassMarket.getFaction().getTariffFraction());
        glassMarket.addCondition("apex_collectivedefense");
        glassMarket.addCondition(Conditions.POPULATION_5);
        glassMarket.addCondition(Conditions.EXTREME_TECTONIC_ACTIVITY);
        glassMarket.addCondition(Conditions.HOT);
        glassMarket.addCondition(Conditions.EXTREME_WEATHER);
        glassMarket.addCondition(Conditions.HABITABLE);
        glassMarket.addCondition(Conditions.RARE_ORE_ULTRARICH);
        glassMarket.addCondition(Conditions.ORE_ULTRARICH);
        glassMarket.addCondition(Conditions.VOLATILES_DIFFUSE);
        glassMarket.addIndustry(Industries.SPACEPORT);
        glassMarket.addIndustry(Industries.MINING);
        glassMarket.addIndustry(Industries.POPULATION);
        glassMarket.addIndustry(Industries.MILITARYBASE);
        glassMarket.addIndustry(Industries.REFINING);
        glassMarket.addIndustry(Industries.HEAVYBATTERIES);
        glassMarket.addIndustry(Industries.BATTLESTATION_HIGH);
        glassMarket.addSubmarket("apex_fake_open_market");
        glassMarket.addSubmarket(Submarkets.SUBMARKET_BLACK);
        glassMarket.addSubmarket(Submarkets.SUBMARKET_STORAGE);
        glassMarket.addSubmarket(Submarkets.GENERIC_MILITARY);

        glass.setFaction("apex_design");
        for (MarketConditionAPI mc : glassMarket.getConditions())
        {
            mc.setSurveyed(true);
        }
        glassMarket.setSurveyLevel(MarketAPI.SurveyLevel.FULL);
        glass.setMarket(glassMarket);
        glass.setCustomDescriptionId("apex_glass");
        sector.getEconomy().addMarket(glassMarket, true);

        // Granite market and condition setup
        MarketAPI graniteMarket = Global.getFactory().createMarket("apex_granite_market", granite.getName(), 7);
        graniteMarket.setPrimaryEntity(granite);
        graniteMarket.setFactionId("apex_design");
        graniteMarket.getTariff().modifyFlat("default_tariff", graniteMarket.getFaction().getTariffFraction());
        graniteMarket.addCondition("apex_collectivedefense");
        graniteMarket.addCondition(Conditions.POPULATION_7);
        graniteMarket.addCondition(Conditions.NO_ATMOSPHERE);
        graniteMarket.addCondition(Conditions.ORE_SPARSE);
        graniteMarket.addCondition(Conditions.LOW_GRAVITY);
        graniteMarket.addIndustry(Industries.SPACEPORT);
        graniteMarket.addIndustry(Industries.WAYSTATION);
        graniteMarket.addIndustry(Industries.POPULATION);
        graniteMarket.addIndustry(Industries.HIGHCOMMAND);
        graniteMarket.addIndustry(Industries.FUELPROD);
        graniteMarket.addIndustry(Industries.ORBITALWORKS, new ArrayList<>(Arrays.asList(Items.PRISTINE_NANOFORGE)));
        graniteMarket.addIndustry(Industries.LIGHTINDUSTRY);
        graniteMarket.addIndustry(Industries.HEAVYBATTERIES);
        graniteMarket.addIndustry(Industries.STARFORTRESS_HIGH);
        graniteMarket.addSubmarket("apex_fake_open_market");
        graniteMarket.addSubmarket(Submarkets.SUBMARKET_BLACK);
        graniteMarket.addSubmarket(Submarkets.SUBMARKET_STORAGE);
        graniteMarket.addSubmarket(Submarkets.GENERIC_MILITARY);
        granite.setFaction("apex_design");
        for (MarketConditionAPI mc : graniteMarket.getConditions())
        {
            mc.setSurveyed(true);
        }
        graniteMarket.setSurveyLevel(MarketAPI.SurveyLevel.FULL);
        granite.setMarket(graniteMarket);
        granite.setCustomDescriptionId("apex_granite");
        sector.getEconomy().addMarket(graniteMarket, true);

        // Emerald market and condition setup
        MarketAPI emeraldMarket = Global.getFactory().createMarket("apex_emerald_market", emerald.getName(), 5);
        emeraldMarket.setPrimaryEntity(emerald);
        emeraldMarket.setFactionId("apex_design");
        emeraldMarket.getTariff().modifyFlat("default_tariff", emeraldMarket.getFaction().getTariffFraction());
        emeraldMarket.addCondition("apex_collectivedefense");
        emeraldMarket.addCondition(Conditions.POPULATION_5);
        emeraldMarket.addCondition(Conditions.HABITABLE);
        emeraldMarket.addCondition(Conditions.ORGANICS_ABUNDANT);
        emeraldMarket.addCondition(Conditions.FARMLAND_RICH);
        emeraldMarket.addIndustry(Industries.SPACEPORT);
        emeraldMarket.addIndustry(Industries.FARMING);
        emeraldMarket.addIndustry(Industries.LIGHTINDUSTRY);
        emeraldMarket.addIndustry(Industries.MINING);
        emeraldMarket.addIndustry(Industries.POPULATION);
        emeraldMarket.addIndustry(Industries.ORBITALSTATION_HIGH);
        emeraldMarket.addSubmarket("apex_fake_open_market");
        emeraldMarket.addSubmarket(Submarkets.SUBMARKET_BLACK);
        emeraldMarket.addSubmarket(Submarkets.SUBMARKET_STORAGE);
        emerald.setFaction("apex_design");
        for (MarketConditionAPI mc : emeraldMarket.getConditions())
        {
            mc.setSurveyed(true);
        }
        emeraldMarket.setSurveyLevel(MarketAPI.SurveyLevel.FULL);
        emerald.setMarket(emeraldMarket);
        emerald.setCustomDescriptionId("apex_emerald");
        sector.getEconomy().addMarket(emeraldMarket, true);

        // onyx planet setup
        Misc.initConditionMarket(onyx);
        MarketAPI onyxMarket = onyx.getMarket();
        onyxMarket.addCondition(Conditions.NO_ATMOSPHERE);
        onyxMarket.addCondition(Conditions.LOW_GRAVITY);
        for (MarketConditionAPI mc : onyxMarket.getConditions())
        {
            mc.setSurveyed(true);
        }
        onyx.setCustomDescriptionId("apex_onyx");

        // onyx station setup
        MarketAPI onyxStationMarket = Global.getFactory().createMarket("apex_onyx_station_market", onyxStation.getName(), 3);
        onyxStationMarket.setPrimaryEntity(onyxStation);
        onyxStationMarket.setFactionId("apex_design");
        onyxStationMarket.getTariff().modifyFlat("default_tariff", onyxStationMarket.getFaction().getTariffFraction());
        onyxStationMarket.addCondition("apex_collectivedefense");
        onyxStationMarket.addCondition(Conditions.POPULATION_3);
        onyxStationMarket.addCondition(Conditions.NO_ATMOSPHERE);
        onyxStationMarket.addIndustry(Industries.MEGAPORT);
        onyxStationMarket.addIndustry(Industries.POPULATION);
        onyxStationMarket.addIndustry("commerce");
        onyxStationMarket.addIndustry(Industries.GROUNDDEFENSES);
        onyxStationMarket.addIndustry(Industries.ORBITALSTATION_MID);
        onyxStationMarket.addIndustry(Industries.PATROLHQ);
        onyxStationMarket.addSubmarket("apex_fake_open_market");
        onyxStationMarket.addSubmarket(Submarkets.SUBMARKET_BLACK);
        onyxStationMarket.addSubmarket(Submarkets.SUBMARKET_STORAGE);
        onyxStation.setFaction("apex_design");
        for (MarketConditionAPI mc : onyxStationMarket.getConditions())
        {
            mc.setSurveyed(true);
        }
        onyxStationMarket.setSurveyLevel(MarketAPI.SurveyLevel.FULL);
        onyxStation.setMarket(onyxStationMarket);
        onyxStation.setCircularOrbitPointingDown(onyx, 50, 200f, 50f);
        onyxStation.setCustomDescriptionId("apex_onyx_station");
        sector.getEconomy().addMarket(onyxStationMarket, true);

        if (Global.getSettings().getModManager().isModEnabled("IndEvo"))
        {
            onyxStationMarket.addIndustry("IndEvo_ComArray");
            graniteMarket.addIndustry("IndEvo_ComArray");
        }

        // if you don't have this in there your system won't show up
        // why? idfk
        system.autogenerateHyperspaceJumpPoints();
        system.generateAnchorIfNeeded();

        // this stuff clears a radius of hyperspace clouds around the system
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
