package data.campaign.missions;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.PersonImportance;
import com.fs.starfarer.api.campaign.RepLevel;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.impl.campaign.ids.Ranks;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithBarEvent;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import plugins.ApexModPlugin;

import java.util.List;
import java.util.Map;

import static utils.ApexUtils.text;

// this is mostly a modification of the "surplus hull" mission
// dear future me: good luck figuring this one out, asshole
public class ApexExcession extends HubMissionWithBarEvent
{
    protected FleetMemberAPI excession;
    protected FleetMemberAPI omega;
    private static final WeightedRandomPicker<String> NAMES = new WeightedRandomPicker<>();

    static
    {
        NAMES.add(text("excn1"), 1000);
        NAMES.add(text("excn2"), 100);
        NAMES.add(text("excn3"), 100);
        NAMES.add(text("excn4"), 100);
        NAMES.add(text("excn5"), 0.1f);
    }


    @Override
    protected boolean create(MarketAPI createdAt, boolean barEvent)
    {
        //System.out.println("Apex: called excession quest creation code");
        // requires cooperative rep and a working janus device
        if (Global.getSector().getMemoryWithoutUpdate().contains("$gatesActive"))
        {
            Object o = Global.getSector().getMemoryWithoutUpdate().get("$gatesActive");
            if (o instanceof Boolean && !(boolean)o)
                return false;
            if (o instanceof String && !o.equals("true"))
                return false;
        } else
            return false;

        if (!Global.getSector().getPlayerFaction().getRelationshipLevel("apex_design").isAtWorst(RepLevel.COOPERATIVE))
            return false;

        setGiverRank(Ranks.SPACE_COMMANDER);
        setGiverPost(Ranks.POST_STATION_COMMANDER);
        setGiverImportance(PersonImportance.VERY_HIGH);
        setGiverTags(Tags.CONTACT_MILITARY);
        findOrCreateGiver(createdAt, false, false);


        PersonAPI person = getPerson();
        if (person == null) return false;
        MarketAPI market = person.getMarket();
        if (market == null || !market.getFactionId().equals("apex_design")) return false;

        if (!Misc.isMilitary(market) && market.getSize() < 7) return false;

        if (!setPersonMissionRef(person, "$apexPrototype_ref"))
        {
            return false;
        }

        setGiverIsPotentialContactOnSuccess();
        // create excession to give to the player
        ShipVariantAPI variant = Global.getSettings().getVariant("apex_excession_Hull").clone();
        if (ApexModPlugin.EXCESSION_ID)
            variant.addTag(Tags.SHIP_UNIQUE_SIGNATURE);
        excession = Global.getFactory().createFleetMember(FleetMemberType.SHIP, variant);
        assignShipName(excession, "apex_design");
        // TODO: small chance of funny names
        excession.setShipName(NAMES.pick());
        excession.getCrewComposition().setCrew(100000);
        excession.getRepairTracker().setCR(0.7f);

        setRepFactionChangesHigh();
        setRepPersonChangesHigh();

        // create omega to show to the player
        ShipVariantAPI variant2 = Global.getSettings().getVariant("facet_Hull").clone();
        omega = Global.getFactory().createFleetMember(FleetMemberType.SHIP, variant2);
        omega.setShipName("Unknown Entity");

        return true;
    }

    @Override
    protected void updateInteractionDataImpl()
    {
        // this is weird - in the accept() method, the mission is aborted, which unsets
        // $sShip_ref. So: we use $sShip_ref2 in the ContactPostAccept rule
        // and $sShip_ref2 has an expiration of 0, so it'll get unset on its own later.
        set("$apexPrototype_ref2", this);

        set("$apexPrototype_barEvent", isBarEvent());
        set("$apexPrototype_hullSize", excession.getHullSpec().getDesignation().toLowerCase());
        set("$apexPrototype_hullClass", excession.getHullSpec().getHullNameWithDashClass());
        set("$apexPrototype_manOrWoman", getPerson().getManOrWoman());
        set("$apexPrototype_rank", getPerson().getRank().toLowerCase());
        set("$apexPrototype_rankAOrAn", getPerson().getRankArticle());
        set("$apexPrototype_hisOrHer", getPerson().getHisOrHer());
        set("$apexPrototype_member", excession);
        //
        set("$apexPrototype_menOrWomen", (Misc.random.nextBoolean() ? text("excmen") : text("excwomen"))); // your contact's sexuality is randomized
    }

    @Override
    protected boolean callAction(String action, String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params, Map<String, MemoryAPI> memoryMap)
    {
        // idk put it in here, this will make sure the flag gets set
        Global.getSector().getMemoryWithoutUpdate().set("$didExcession", true);

        if ("showShip".equals(action))
        {
            dialog.getVisualPanel().showFleetMemberInfo(excession, true);
            return true;
        } else if ("showPerson".equals(action))
        {
            dialog.getVisualPanel().showPersonInfo(getPerson(), true);
            return true;
        } else if ("showOmega".equals(action))
        {
            dialog.getVisualPanel().showFleetMemberInfo(omega, true);
            return true;
        }
        return false;
    }

    @Override
    public String getBaseName()
    {
        return "Fleet Trials"; // not used I don't think
    }

    @Override
    public void accept(InteractionDialogAPI dialog, Map<String, MemoryAPI> memoryMap)
    {
        // it's just an immediate transaction handled in rules.csv
        // no intel item etc

        currentStage = new Object(); // so that the abort() assumes the mission was successful
        abort();
    }
}
