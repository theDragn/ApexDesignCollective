package world;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.characters.ImportantPeopleAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator;
import com.fs.starfarer.api.impl.campaign.shared.SharedData;
import exerelin.campaign.SectorManager;
import exerelin.utilities.NexConfig;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;

import static com.fs.starfarer.api.impl.campaign.CoreLifecyclePluginImpl.dedupePortraits;
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

    // duplicate of vanilla's method, populates station/colony commanders and such
    public static void createInitialPeople()
    {
        boolean isNexRandomMode = false;
        boolean hasNex = Global.getSettings().getModManager().isModEnabled("nexerelin");
        if (hasNex)
            isNexRandomMode = !SectorManager.getManager().isCorvusMode();
        if (isNexRandomMode)
            return;
        ImportantPeopleAPI ip = Global.getSector().getImportantPeople();


        for (MarketAPI market : Global.getSector().getEconomy().getMarketsCopy())
        {
            if (!market.getFactionId().equals("apex_design"))
                continue;
            if (market.getMemoryWithoutUpdate().getBoolean(MemFlags.MARKET_DO_NOT_INIT_COMM_LISTINGS)) continue;
            boolean addedPerson = false;

            PersonAPI admin = null;

            LinkedHashSet<PersonAPI> randomPeople = new LinkedHashSet<PersonAPI>();


            if (market.hasIndustry(Industries.MILITARYBASE) || market.hasIndustry(Industries.HIGHCOMMAND))
            {
                PersonAPI person = market.getFaction().createRandomPerson(StarSystemGenerator.random);
                String rankId = Ranks.GROUND_MAJOR;
                if (market.getSize() >= 6)
                {
                    rankId = Ranks.GROUND_GENERAL;
                } else if (market.getSize() >= 4)
                {
                    rankId = Ranks.GROUND_COLONEL;
                }
                person.setRankId(rankId);
                person.setPostId(Ranks.POST_BASE_COMMANDER);
                if (market.getSize() >= 8)
                {
                    person.setImportanceAndVoice(PersonImportance.VERY_HIGH, StarSystemGenerator.random);
                } else if (market.getSize() >= 6)
                {
                    person.setImportanceAndVoice(PersonImportance.HIGH, StarSystemGenerator.random);
                } else
                {
                    person.setImportanceAndVoice(PersonImportance.MEDIUM, StarSystemGenerator.random);
                }

                market.getCommDirectory().addPerson(person);
                market.addPerson(person);
                ip.addPerson(person);
                ip.getData(person).getLocation().setMarket(market);
                ip.checkOutPerson(person, "permanent_staff");
                addedPerson = true;
                randomPeople.add(person);
            }

            boolean hasStation = false;
            for (Industry curr : market.getIndustries())
            {
                if (curr.getSpec().hasTag(Industries.TAG_STATION))
                {
                    hasStation = true;
                    break;
                }
            }
            if (hasStation)
            {
                PersonAPI person = market.getFaction().createRandomPerson(StarSystemGenerator.random);
                String rankId = Ranks.SPACE_COMMANDER;
                if (market.getSize() >= 6)
                {
                    rankId = Ranks.SPACE_ADMIRAL;
                } else if (market.getSize() >= 4)
                {
                    rankId = Ranks.SPACE_CAPTAIN;
                }
                person.setRankId(rankId);
                person.setPostId(Ranks.POST_STATION_COMMANDER);

                if (market.getSize() >= 8)
                {
                    person.setImportanceAndVoice(PersonImportance.VERY_HIGH, StarSystemGenerator.random);
                } else if (market.getSize() >= 6)
                {
                    person.setImportanceAndVoice(PersonImportance.HIGH, StarSystemGenerator.random);
                } else
                {
                    person.setImportanceAndVoice(PersonImportance.MEDIUM, StarSystemGenerator.random);
                }

                market.getCommDirectory().addPerson(person);
                market.addPerson(person);
                ip.addPerson(person);
                ip.getData(person).getLocation().setMarket(market);
                ip.checkOutPerson(person, "permanent_staff");
                addedPerson = true;
                randomPeople.add(person);

                if (market.getPrimaryEntity().hasTag(Tags.STATION))
                {
                    admin = person;
                }
            }

            if (market.hasSpaceport())
            {
                PersonAPI person = market.getFaction().createRandomPerson(StarSystemGenerator.random);
                //person.setRankId(Ranks.SPACE_CAPTAIN);
                person.setPostId(Ranks.POST_PORTMASTER);

                if (market.getSize() >= 8)
                {
                    person.setImportanceAndVoice(PersonImportance.HIGH, StarSystemGenerator.random);
                } else if (market.getSize() >= 6)
                {
                    person.setImportanceAndVoice(PersonImportance.MEDIUM, StarSystemGenerator.random);
                } else if (market.getSize() >= 4)
                {
                    person.setImportanceAndVoice(PersonImportance.LOW, StarSystemGenerator.random);
                } else
                {
                    person.setImportanceAndVoice(PersonImportance.VERY_LOW, StarSystemGenerator.random);
                }

                market.getCommDirectory().addPerson(person);
                market.addPerson(person);
                ip.addPerson(person);
                ip.getData(person).getLocation().setMarket(market);
                ip.checkOutPerson(person, "permanent_staff");
                addedPerson = true;
                randomPeople.add(person);
            }

            if (addedPerson)
            {
                PersonAPI person = market.getFaction().createRandomPerson(StarSystemGenerator.random);
                person.setRankId(Ranks.SPACE_COMMANDER);
                person.setPostId(Ranks.POST_SUPPLY_OFFICER);

                if (market.getSize() >= 6)
                {
                    person.setImportanceAndVoice(PersonImportance.MEDIUM, StarSystemGenerator.random);
                } else if (market.getSize() >= 4)
                {
                    person.setImportanceAndVoice(PersonImportance.LOW, StarSystemGenerator.random);
                } else
                {
                    person.setImportanceAndVoice(PersonImportance.VERY_LOW, StarSystemGenerator.random);
                }


                market.getCommDirectory().addPerson(person);
                market.addPerson(person);
                ip.addPerson(person);
                ip.getData(person).getLocation().setMarket(market);
                ip.checkOutPerson(person, "permanent_staff");
                addedPerson = true;
                randomPeople.add(person);
            }

            if (!addedPerson || admin == null)
            {
                PersonAPI person = market.getFaction().createRandomPerson(StarSystemGenerator.random);
                person.setRankId(Ranks.CITIZEN);
                person.setPostId(Ranks.POST_ADMINISTRATOR);

                if (market.getSize() >= 8)
                {
                    person.setImportanceAndVoice(PersonImportance.VERY_HIGH, StarSystemGenerator.random);
                } else if (market.getSize() >= 6)
                {
                    person.setImportanceAndVoice(PersonImportance.HIGH, StarSystemGenerator.random);
                } else
                {
                    person.setImportanceAndVoice(PersonImportance.MEDIUM, StarSystemGenerator.random);
                }

                market.getCommDirectory().addPerson(person);
                market.addPerson(person);
                ip.addPerson(person);
                ip.getData(person).getLocation().setMarket(market);
                ip.checkOutPerson(person, "permanent_staff");
                admin = person;
                randomPeople.add(person);
            }

            if (admin != null)
            {
                addSkillsAndAssignAdmin(market, admin);
            }

            List<PersonAPI> people = new ArrayList<PersonAPI>(randomPeople);
            Iterator<PersonAPI> iter = people.iterator();
            while (iter.hasNext())
            {
                PersonAPI curr = iter.next();
                if (curr == null || curr.getFaction() == null)
                {
                    iter.remove();
                    continue;
                }
                if (curr.isDefault() || curr.isAICore() || curr.isPlayer())
                {
                    iter.remove();
                    continue;
                }
            }
            dedupePortraits(people);
        }
    }

    private static void addSkillsAndAssignAdmin(MarketAPI market, PersonAPI admin)
    {
        List<String> skills = Global.getSettings().getSortedSkillIds();

        if (!skills.contains(Skills.INDUSTRIAL_PLANNING)) {
            return;
        }

        int size = market.getSize();
        if (size <= 4) return;

        int industries = 0;

        for (Industry curr : market.getIndustries()) {
            if (curr.isIndustry()) {
                industries++;
            }
        }


        admin.getStats().setSkipRefresh(true);

        if (industries >= 2 || size >= 6) {
            admin.getStats().setSkillLevel(Skills.INDUSTRIAL_PLANNING, 1);
        }

        admin.getStats().setSkipRefresh(false);
        admin.getStats().refreshCharacterStatsEffects();

        market.setAdmin(admin);
    }


}
