package data.missions.apex_1demo;

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
        api.setFleetTagline(FleetSide.PLAYER, text("mis1-3"));
        api.setFleetTagline(FleetSide.ENEMY, text("mis1-4"));

        // These show up as items in the bulleted list under
        // "Tactical Objectives" on the mission detail screen
        api.addBriefingItem(text("mis1-5"));
        api.addBriefingItem(text("mis1-6"));
        api.addBriefingItem(text("mis1-7"));

        // Set up the player's fleet.  Variant names come from the
        // files in data/variants and data/variants/fighters
        api.addToFleet(FleetSide.PLAYER, "apex_apex_strike", FleetMemberType.SHIP, text("mis1-8"), true);
        api.addToFleet(FleetSide.PLAYER, "apex_sunbeam_strike", FleetMemberType.SHIP, false);
        api.addToFleet(FleetSide.PLAYER, "apex_eidolon_standard", FleetMemberType.SHIP, false);
        api.addToFleet(FleetSide.PLAYER, "apex_anaconda_strike", FleetMemberType.SHIP, false);
        api.addToFleet(FleetSide.PLAYER, "apex_apotheosis_standard", FleetMemberType.SHIP, false);
        api.addToFleet(FleetSide.PLAYER, "apex_komodo_standard", FleetMemberType.SHIP, false);
        api.addToFleet(FleetSide.PLAYER, "apex_goanna_attack", FleetMemberType.SHIP, false);
        api.addToFleet(FleetSide.PLAYER, "apex_python_strike", FleetMemberType.SHIP, false);
        api.addToFleet(FleetSide.PLAYER, "apex_crocodile_antishield", FleetMemberType.SHIP, false);
        api.addToFleet(FleetSide.PLAYER, "apex_alligator_torpedo", FleetMemberType.SHIP, false);
        api.addToFleet(FleetSide.PLAYER, "apex_boa_blaster", FleetMemberType.SHIP, false);
        api.addToFleet(FleetSide.PLAYER, "apex_spectrum_fighter", FleetMemberType.SHIP, false);
        api.addToFleet(FleetSide.PLAYER, "apex_backscatter_bomber", FleetMemberType.SHIP, false);
        api.addToFleet(FleetSide.PLAYER, "apex_tuatara_frag", FleetMemberType.SHIP, false);
        api.addToFleet(FleetSide.PLAYER, "apex_caiman_line", FleetMemberType.SHIP, false);
        api.addToFleet(FleetSide.PLAYER, "apex_gharial_strike", FleetMemberType.SHIP, false);
        api.addToFleet(FleetSide.PLAYER, "apex_lacerta_attack", FleetMemberType.SHIP, false);
        api.addToFleet(FleetSide.PLAYER, "apex_agama_picket", FleetMemberType.SHIP, false);
        api.addToFleet(FleetSide.PLAYER, "apex_mamba_gunner", FleetMemberType.SHIP, false);
        api.addToFleet(FleetSide.PLAYER, "apex_iguana_attack", FleetMemberType.SHIP, false);

        // Mark both ships as essential - losing either one results
        // in mission failure. Could also be set on an enemy ship,
        // in which case destroying it would result in a win.
        api.defeatOnShipLoss(text("mis1-8"));

        // Set up the enemy fleet.
        api.addToFleet(FleetSide.ENEMY, "paragon_Raider", FleetMemberType.SHIP, false);
        api.addToFleet(FleetSide.ENEMY, "odyssey_Balanced ", FleetMemberType.SHIP, false);
        api.addToFleet(FleetSide.ENEMY, "astral_Elite", FleetMemberType.SHIP, false);
        api.addToFleet(FleetSide.ENEMY, "doom_Strike", FleetMemberType.SHIP, false);
        api.addToFleet(FleetSide.ENEMY, "aurora_Assault", FleetMemberType.SHIP, false);
        api.addToFleet(FleetSide.ENEMY, "heron_Attack", FleetMemberType.SHIP, false);
        api.addToFleet(FleetSide.ENEMY, "fury_Attack", FleetMemberType.SHIP, false);
        api.addToFleet(FleetSide.ENEMY, "shrike_Attack", FleetMemberType.SHIP, false);
        api.addToFleet(FleetSide.ENEMY, "wolf_Assault", FleetMemberType.SHIP, false);
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
