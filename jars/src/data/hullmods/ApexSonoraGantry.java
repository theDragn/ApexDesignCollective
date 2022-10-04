package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BuffManagerAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.HullModFleetEffect;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

// this is mostly stolen from Approlight (which very much inspired the effect)
// it's way cleaner than the "use a manager campaign plugin" method that I was going to use
// (this also presents a clean way for hullmods to buff all sorts of other ship stats for your whole fleet)
public class ApexSonoraGantry extends BaseHullMod implements HullModFleetEffect
{
    // total multiplier = 1 + REPAIR_BONUS_MULT * x^POWER; x = number of hullmods
    public static final float REPAIR_BONUS_MULT = 0.33f;
    public static final float POWER = 0.66f;
    public static final String ID = "apex_sonora_gantry";

    @Override
    public void advanceInCampaign(CampaignFleetAPI fleet)
    {
        if (!fleet.isInCurrentLocation())
            return;
        float bonus = getBonus(fleet);
        fleet.getCustomData().put(ID, bonus);
        if (bonus > 1)
        {
            boolean sync = false;
            for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy())
            {
                if (member.getVariant() != null && !member.isMothballed())
                {
                    BuffManagerAPI.Buff buff = member.getBuffManager().getBuff(ID);
                    if (buff instanceof ApexRecoveryBuff)
                    {
                        ((ApexRecoveryBuff) buff).update();
                    } else {
                        member.getBuffManager().addBuff(new ApexRecoveryBuff(0.1f));
                        sync = true;
                    }
                }
            }
            if (sync) fleet.forceSync();
        }
    }

    @Override
    public boolean withAdvanceInCampaign() { return true; }

    @Override
    public boolean withOnFleetSync() { return false; }

    @Override
    public void onFleetSync(CampaignFleetAPI campaignFleetAPI) {}

    @Override
    public String getDescriptionParam(int index, ShipAPI.HullSize hullSize)
    {
        if (index == 0)
            return (int)(REPAIR_BONUS_MULT * 100f) + "%";
        return null;
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec)
    {
        if (ship != null && Global.getSector() != null && Global.getSector().getPlayerFleet() != null)
        {
            //float pad = 10f;
            //tooltip.addSectionHeading("Details", Alignment.MID, pad);

            tooltip.addPara("\nThe current recovery rate bonus is %s.",
                    0,
                    Misc.getHighlightColor(),
                    (int) (getBonus(Global.getSector().getPlayerFleet()) * 100f - 100f) + "%");
        }
    }

    public float getBonus(CampaignFleetAPI fleet)
    {
        if (Global.getSector() == null)
            return 0;
        if (fleet == null)
            return 0;
        int numBoosters = 0;
        float totalBoostMult = 0;
        for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy())
        {
            if (!member.isMothballed() && member.getVariant().hasHullMod("apex_sonora_gantry"))
                numBoosters++;
        }
        return REPAIR_BONUS_MULT * (float)Math.pow(numBoosters, POWER) + 1f;
    }

    public static class ApexRecoveryBuff implements BuffManagerAPI.Buff
    {
        private float duration;

        public ApexRecoveryBuff(float duration)
        {
            this.duration = duration;
        }

        public void update()
        {
            duration = 0.1f;
        }

        @Override
        public void apply(FleetMemberAPI member)
        {
            if (member == null || member. getFleetData() == null || member.getFleetData().getFleet() == null)
                return;
            if (!member.getFleetData().getFleet().getCustomData().containsKey(ID))
                return;
            float bonus = (float)member.getFleetData().getFleet().getCustomData().get(ID);
            member.getStats().getRepairRatePercentPerDay().modifyMult(ID, bonus);
            member.getStats().getBaseCRRecoveryRatePercentPerDay().modifyMult(ID, bonus);
        }


        @Override
        public String getId()
        {
            return ID;
        }

        @Override
        public boolean isExpired()
        {
            return duration < 0f;
        }

        @Override
        public void advance(float amount)
        {
            duration -= amount;
        }
    }
}
