package data.campaign.missions;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCampaignEventListener;
import com.fs.starfarer.api.campaign.RepLevel;
import com.fs.starfarer.api.campaign.econ.MarketAPI;

import java.util.ArrayList;

// ensures that the event is in the market a
public class ApexExcessionAdder extends BaseCampaignEventListener
{
    public ApexExcessionAdder()
    {
        super(false);
    }

    @Override
    public void reportPlayerOpenedMarket(MarketAPI market)
    {
        //System.out.println("Excession Adder Triggered");
        if (Global.getSector().getMemoryWithoutUpdate().contains("$gatesActive"))
        {
            Object o = Global.getSector().getMemoryWithoutUpdate().get("$gatesActive");
            if (o instanceof Boolean && !(boolean)o)
                return;
            if (o instanceof String && !o.equals("true"))
                return;
        } else
            return;
        if (Global.getSector().getMemoryWithoutUpdate().contains("$didExcession"))
            return;
        if (!Global.getSector().getPlayerFaction().getRelationshipLevel("apex_design").isAtWorst(RepLevel.COOPERATIVE))
            return;
        if (!market.getId().equals("apex_granite_market"))
            return;

        Object o = market.getMemoryWithoutUpdate().get("$BarCMD_shownEvents");
        if (o instanceof ArrayList)
        {
            ArrayList<String> list = (ArrayList<String>) o;
            if (!list.contains("apexPrototype"))
                list.add("apexPrototype");
        } /*else
        {
            ArrayList<String> list = new ArrayList<>();
            list.add("apexPrototype");
            market.getMemoryWithoutUpdate().set("$BarCMD_shownEvents", list);
        }*/
    }
}
