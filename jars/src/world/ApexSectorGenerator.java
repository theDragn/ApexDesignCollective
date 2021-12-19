package world;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.RepLevel;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorGeneratorPlugin;
import com.fs.starfarer.api.impl.campaign.shared.SharedData;
import exerelin.campaign.SectorManager;

import java.util.List;

import static plugins.ApexModPlugin.GENERATE_SYSTEMS;
import static world.ApexRelationships.relMap;

public class ApexSectorGenerator implements SectorGeneratorPlugin
{


    @Override
    public void generate(SectorAPI sector)
    {
        boolean isNexRandomMode = false;
        boolean hasNex = Global.getSettings().getModManager().isModEnabled("nexerelin");
        if (hasNex)
            isNexRandomMode = !SectorManager.getManager().isCorvusMode();
        if (GENERATE_SYSTEMS && !isNexRandomMode)
        {
            (new ApexSerpens()).generate(sector);
            (new ApexVela()).generate(sector);
        }
        FactionAPI apex = sector.getFaction("apex_design");
        //default relation
        List<FactionAPI> allFactions = sector.getAllFactions();
        for (FactionAPI f : allFactions)
        {
            apex.setRelationship(f.getId(), RepLevel.NEUTRAL);
        }

        for (String faction : relMap.keySet())
        {
            apex.setRelationship(faction, relMap.get(faction));
        }
        if (GENERATE_SYSTEMS)
        {
            SharedData.getData().getPersonBountyEventData().addParticipatingFaction("apex_design");
        } else {
            Global.getSector().getFaction("apex_design").setShowInIntelTab(false);
        }
    }
}
