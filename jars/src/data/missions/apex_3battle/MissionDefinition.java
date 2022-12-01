package data.missions.apex_3battle;

import com.fs.starfarer.api.fleet.FleetGoal;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.mission.FleetSide;
import com.fs.starfarer.api.mission.MissionDefinitionAPI;
import com.fs.starfarer.api.mission.MissionDefinitionPlugin;

import static data.ApexUtils.text;

public class MissionDefinition implements MissionDefinitionPlugin
{

    @Override
    public void defineMission(MissionDefinitionAPI api)
    {


        // Set up the fleets so we can add ships and fighter wings to them.
        // In this scenario, the fleets are attacking each other, but
        // in other scenarios, a fleet may be defending or trying to escape
        api.initFleet(FleetSide.PLAYER, text("mis1-1"), FleetGoal.ATTACK, false);
        api.initFleet(FleetSide.ENEMY, text("mis1-2"), FleetGoal.ATTACK, true);

        // Set a small blurb for each fleet that shows up on the mission detail and
        // mission results screens to identify each side.
        api.setFleetTagline(FleetSide.PLAYER, text("mis4-1"));
        api.setFleetTagline(FleetSide.ENEMY, text("mis4-2"));

        // These show up as items in the bulleted list under
        // "Tactical Objectives" on the mission detail screen
        api.addBriefingItem(text("mis4-3"));
        api.addBriefingItem(text("mis4-4"));
        api.addBriefingItem(text("mis4-5"));

        // Set up the player's fleet.  Variant names come from the
        // files in data/variants and data/variants/fighters

        // 2nd Fleet Group
        api.addToFleet(FleetSide.PLAYER, "apex_eidolon_standard", FleetMemberType.SHIP, "CDFS Pinkerton's Demise", true);
        api.addToFleet(FleetSide.PLAYER, "apex_eidolon_attack", FleetMemberType.SHIP, "CDFS Strikebreak This", false);
        api.addToFleet(FleetSide.PLAYER, "apex_crocodile_antishield", FleetMemberType.SHIP, "CDFS Coeur d'Alene", false);
        api.addToFleet(FleetSide.PLAYER, "apex_spectrum_fighter", FleetMemberType.SHIP, "CDFS This Machine Plays Folk Music", false);
        api.addToFleet(FleetSide.PLAYER, "apex_alligator_beam", FleetMemberType.SHIP, "CDFS Harlan", false);
        api.addToFleet(FleetSide.PLAYER, "apex_backscatter_bomber", FleetMemberType.SHIP, "CDFS Pittston",false);
        api.addToFleet(FleetSide.PLAYER, "apex_caiman_assault", FleetMemberType.SHIP, "CDFS Nothing to Lose", false);
        api.addToFleet(FleetSide.PLAYER, "apex_gharial_pursuit", FleetMemberType.SHIP, "CDFS Hilo",false);
        api.addToFleet(FleetSide.PLAYER, "apex_lacerta_strike", FleetMemberType.SHIP, "CDFS Tibicena Local 55", false);
        api.addToFleet(FleetSide.PLAYER, "apex_lacerta_strike", FleetMemberType.SHIP, "CDFS Culann Local 19", false);
        api.addToFleet(FleetSide.PLAYER, "apex_lacerta_strike", FleetMemberType.SHIP, "CDFS Culann Local 42", false);

        // Patrol Squadron 17



        api.addToFleet(FleetSide.PLAYER, "apex_mamba_gunner", FleetMemberType.SHIP, "CDFS Just Leave The Name Blank",false);
        api.addToFleet(FleetSide.PLAYER, "apex_agama_picket", FleetMemberType.SHIP, "CDFS Worker's Lefts",false);



        // Mark both ships as essential - losing either one results
        // in mission failure. Could also be set on an enemy ship,
        // in which case destroying it would result in a win.
        api.defeatOnShipLoss("CDFS Pinkerton's Demise");

        // Set up the enemy fleet.
        api.addToFleet(FleetSide.ENEMY, "paragon_Elite", FleetMemberType.SHIP, false);
        api.addToFleet(FleetSide.ENEMY, "doom_Strike", FleetMemberType.SHIP, false);
        api.addToFleet(FleetSide.ENEMY, "aurora_Assault", FleetMemberType.SHIP, false);
        api.addToFleet(FleetSide.ENEMY, "heron_Attack", FleetMemberType.SHIP, false);
        api.addToFleet(FleetSide.ENEMY, "heron_Attack", FleetMemberType.SHIP, false);
        api.addToFleet(FleetSide.ENEMY, "harbinger_Strike", FleetMemberType.SHIP, false);
        api.addToFleet(FleetSide.ENEMY, "hyperion_Strike", FleetMemberType.SHIP, false);
        api.addToFleet(FleetSide.ENEMY, "drover_Strike", FleetMemberType.SHIP, false);
        api.addToFleet(FleetSide.ENEMY, "drover_Strike", FleetMemberType.SHIP, false);
        api.addToFleet(FleetSide.ENEMY, "shrike_Attack", FleetMemberType.SHIP, false);
        api.addToFleet(FleetSide.ENEMY, "shrike_Attack", FleetMemberType.SHIP, false);
        api.addToFleet(FleetSide.ENEMY, "tempest_Attack", FleetMemberType.SHIP, false);
        api.addToFleet(FleetSide.ENEMY, "tempest_Attack", FleetMemberType.SHIP, false);

        // Set up the map.
        float width = 16000f;
        float height = 16000f;
        api.initMap((float) -width / 2f, (float) width / 2f, (float) -height / 2f, (float) height / 2f);

        float minX = -width / 2;
        float minY = -height / 2;

        // All the addXXX methods take a pair of coordinates followed by data for
        // whatever object is being added.

        // And a few random ones to spice up the playing field.
        // A similar approach can be used to randomize everything
        // else, including fleet composition.
        for (int i = 0; i < 7; i++)
        {
            float x = (float) Math.random() * width - width / 2;
            float y = (float) Math.random() * height - height / 2;
            float radius = 100f + (float) Math.random() * 800f;
            api.addNebula(x, y, radius);
        }

        api.addObjective(minX + width * 0.75f, minY + height * 0.25f, "sensor_array");
        api.addObjective(minX + width * 0.25f, minY + height * 0.75f, "sensor_array");
        api.addObjective(minX + width * 0.75f, minY + height * 0.75f, "nav_buoy");
        api.addObjective(minX + width * 0.25f, minY + height * 0.25f, "nav_buoy");
    }
}