package data.missions.apex_2skirmish;

import com.fs.starfarer.api.fleet.FleetGoal;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.mission.FleetSide;
import com.fs.starfarer.api.mission.MissionDefinitionAPI;
import com.fs.starfarer.api.mission.MissionDefinitionPlugin;

public class MissionDefinition implements MissionDefinitionPlugin
{

    @Override
    public void defineMission(MissionDefinitionAPI api)
    {


        // Set up the fleets so we can add ships and fighter wings to them.
        // In this scenario, the fleets are attacking each other, but
        // in other scenarios, a fleet may be defending or trying to escape
        api.initFleet(FleetSide.PLAYER, "CDFS", FleetGoal.ATTACK, false);
        api.initFleet(FleetSide.ENEMY, "TTS", FleetGoal.ATTACK, true);

        // Set a small blurb for each fleet that shows up on the mission detail and
        // mission results screens to identify each side.
        api.setFleetTagline(FleetSide.PLAYER, "CDF Patrol Squadron 17");
        api.setFleetTagline(FleetSide.ENEMY, "Tri-Tachyon HK Flotilla Scouting Party");

        // These show up as items in the bulleted list under
        // "Tactical Objectives" on the mission detail screen
        api.addBriefingItem("Collective ships are slower and tougher, but will die if isolated.");
        api.addBriefingItem("Mambas have a potent anti-fighter system.");
        api.addBriefingItem("CDFS Harlan must survive.");

        // Set up the player's fleet.  Variant names come from the
        // files in data/variants and data/variants/fighters
        api.addToFleet(FleetSide.PLAYER, "apex_alligator_beam", FleetMemberType.SHIP, "CDFS Harlan", true);
        api.addToFleet(FleetSide.PLAYER, "apex_backscatter_bomber", FleetMemberType.SHIP, "CDFS Pittston",false);
        api.addToFleet(FleetSide.PLAYER, "apex_gharial_strike", FleetMemberType.SHIP, "CDFS Massey",false);
        api.addToFleet(FleetSide.PLAYER, "apex_gharial_pursuit", FleetMemberType.SHIP, "CDFS Hilo",false);
        api.addToFleet(FleetSide.PLAYER, "apex_agama_picket", FleetMemberType.SHIP, "CDFS Worker's Rights",false);
        api.addToFleet(FleetSide.PLAYER, "apex_agama_picket", FleetMemberType.SHIP, "CDFS Worker's Lefts",false);
        api.addToFleet(FleetSide.PLAYER, "apex_mamba_gunner", FleetMemberType.SHIP,"CDFS Seven Fighters In A Trench Coat", false);
        api.addToFleet(FleetSide.PLAYER, "apex_mamba_gunner", FleetMemberType.SHIP, "CDFS Just Leave The Name Blank",false);

        // Mark both ships as essential - losing either one results
        // in mission failure. Could also be set on an enemy ship,
        // in which case destroying it would result in a win.
        api.defeatOnShipLoss("CDFS Harlan");

        // Set up the enemy fleet.
        api.addToFleet(FleetSide.ENEMY, "fury_Attack", FleetMemberType.SHIP, false);
        api.addToFleet(FleetSide.ENEMY, "fury_Attack", FleetMemberType.SHIP, false);
        api.addToFleet(FleetSide.ENEMY, "heron_Attack", FleetMemberType.SHIP, false);
        api.addToFleet(FleetSide.ENEMY, "drover_Strike", FleetMemberType.SHIP, false);
        api.addToFleet(FleetSide.ENEMY, "shrike_Attack", FleetMemberType.SHIP, false);
        api.addToFleet(FleetSide.ENEMY, "tempest_Attack", FleetMemberType.SHIP, false);
        api.addToFleet(FleetSide.ENEMY, "tempest_Attack", FleetMemberType.SHIP, false);

        // Set up the map.
        float width = 15000f;
        float height = 11000f;
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
    }
}
