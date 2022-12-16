package data.missions.apex_4blackops;

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
        api.initFleet(FleetSide.ENEMY, text("mis5-1"), FleetGoal.ATTACK, true);

        // Set a small blurb for each fleet that shows up on the mission detail and
        // mission results screens to identify each side.
        api.setFleetTagline(FleetSide.PLAYER, text("mis5-2"));
        api.setFleetTagline(FleetSide.ENEMY, text("mis5-3"));

        // These show up as items in the bulleted list under
        // "Tactical Objectives" on the mission detail screen
        api.addBriefingItem(text("mis5-4"));
        api.addBriefingItem(text("mis5-5"));
        api.addBriefingItem(text("mis5-6"));

        // 137 DP
        api.addToFleet(FleetSide.PLAYER, "apex_anaconda_strike", FleetMemberType.SHIP, text("mis5-7"), true);
        api.addToFleet(FleetSide.PLAYER, "apex_python_blaster", FleetMemberType.SHIP, false);
        api.addToFleet(FleetSide.PLAYER, "apex_boa_blaster", FleetMemberType.SHIP, false);
        api.addToFleet(FleetSide.PLAYER, "apex_boa_blaster", FleetMemberType.SHIP, false);
        api.addToFleet(FleetSide.PLAYER, "apex_mamba_gunner", FleetMemberType.SHIP, false);
        api.addToFleet(FleetSide.PLAYER, "apex_mamba_gunner", FleetMemberType.SHIP, false);

        // Mark both ships as essential - losing either one results
        // in mission failure. Could also be set on an enemy ship,
        // in which case destroying it would result in a win.
        api.defeatOnShipLoss(text("mis5-7"));

        // 45 + 35 + 25 + 4 + 4 + 4 + 14 +
        api.addToFleet(FleetSide.ENEMY, "odyssey_Balanced", FleetMemberType.SHIP, true);
        api.addToFleet(FleetSide.ENEMY, "champion_Elite", FleetMemberType.SHIP, false);
        api.addToFleet(FleetSide.ENEMY, "falcon_CS", FleetMemberType.SHIP, false);
        api.addToFleet(FleetSide.ENEMY, "enforcer_Overdriven", FleetMemberType.SHIP, false);
        api.addToFleet(FleetSide.ENEMY, "enforcer_Overdriven", FleetMemberType.SHIP, false);
        api.addToFleet(FleetSide.ENEMY, "sunder_CS", FleetMemberType.SHIP, false);
        api.addToFleet(FleetSide.ENEMY, "wolf_Strike", FleetMemberType.SHIP, false);
        api.addToFleet(FleetSide.ENEMY, "lasher_Standard", FleetMemberType.SHIP, false);
        api.addToFleet(FleetSide.ENEMY, "lasher_Standard", FleetMemberType.SHIP, false);
        api.addToFleet(FleetSide.ENEMY, "lasher_Standard", FleetMemberType.SHIP, false);


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

        // Add objectives. These can be captured by each side
        // and provide stat bonuses and extra command points to
        // bring in reinforcements.
        // Reinforcements only matter for large fleets - in this
        // case, assuming a 100 command point battle size,
        // both fleets will be able to deploy fully right away.

        api.addObjective(minX + width * 0.7f, minY + height * 0.25f, "sensor_array");
        api.addObjective(minX + width * 0.8f, minY + height * 0.75f, "nav_buoy");
        api.addObjective(minX + width * 0.2f, minY + height * 0.25f, "nav_buoy");
    }
}