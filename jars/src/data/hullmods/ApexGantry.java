package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BuffManagerAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.HullModFleetEffect;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.lazylib.MathUtils;

// the fleet buff code is mostly stolen from Approlight (which very much inspired the effect)
// it's way cleaner than the "use a manager campaign plugin" method that I was going to use
// (this also presents a clean way for hullmods to buff all sorts of other ship stats for your whole fleet)
public class ApexGantry extends BaseHullMod implements HullModFleetEffect
{
    // total multiplier = 1 + REPAIR_BONUS_MULT * x^POWER; x = number of hullmods
    public static final float REPAIR_BONUS_MULT = 0.33f;
    public static final float POWER = 0.66f;
    public static final String ID = "apex_sonora_gantry";
    private static final float UNFOLD_MULT = 1f;

    @Override
    public void advanceInCombat(ShipAPI ship, float amount)
    {
        if (ship == null || !ship.isAlive() || ship.isHulk() || ship.getShield() == null)
            return;
        float angle = Math.abs(MathUtils.getShortestRotation(ship.getShield().getFacing(), ship.getFacing()));
        float bonus = ((180f - angle)/180f * UNFOLD_MULT) + 1f;
        //System.out.println(bonus);
        ship.getMutableStats().getShieldUnfoldRateMult().modifyMult(ID, bonus);
    }

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
        // gantry icon + description
        if (ship != null && Global.getSector() != null && Global.getSector().getPlayerFleet() != null)
        {
            //float pad = 10f;
            //tooltip.addSectionHeading("Details", Alignment.MID, pad);

            tooltip.addPara("\nThe current recovery rate bonus is %s.",
                    0,
                    Misc.getHighlightColor(),
                    (int) (getBonus(Global.getSector().getPlayerFleet()) * 100f - 100f) + "%");
        }
        tooltip.addSectionHeading("Other Effects", Alignment.MID, 10f);
        TooltipMakerAPI text = tooltip.beginImageWithText("graphics/hullmods/apex_fastshields.png",40);
        text.addPara("To compensate for its wide frame, the ship is equipped with secondary shield nodes that increase shield unfolding rate " +
                "by %s. The bonus decreases as the shield rotates away from the front of the ship.",
                10f,
                Misc.getHighlightColor(),
                (int)(UNFOLD_MULT*100f) + "%");
        tooltip.addImageWithText(10);

        TooltipMakerAPI text2 = tooltip.beginImageWithText("graphics/hullmods/apex_slow_nozzles.png", 40);
        text2.addPara("The gantry consumes large amounts of internal space that would otherwise be" +
                " reserved for nozzle systems, increasing cooldown time by %s if one is installed.",
                10f,
                Misc.getNegativeHighlightColor(),
                (int)(ApexSlowNozzles.NOZZLE_COOLDOWN_MULT * 100f -100f) + "%");
        tooltip.addImageWithText(10);
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
            if (!member.isMothballed() && member.getVariant().hasHullMod("apex_gantry"))
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
