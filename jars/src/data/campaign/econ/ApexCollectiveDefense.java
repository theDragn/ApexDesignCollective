package data.campaign.econ;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.impl.campaign.econ.BaseMarketConditionPlugin;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

import static utils.ApexUtils.text;

public class ApexCollectiveDefense extends BaseMarketConditionPlugin
{
    public static float DEF_BONUS_PER_ENEMY = 50f;
    public static float STAB_BONUS = 1f;
    public static String tooltip = text("cdtooltip");

    @Override
    public void apply(String id)
    {
        super.apply(id);
        if (market.getFaction() != null)
        {
            if (market.getFaction().getId().contains("apex_design"))
            {
                market.getStability().modifyFlat(id, STAB_BONUS, tooltip);
                market.getStats().getDynamic().getMod(Stats.GROUND_DEFENSES_MOD).modifyFlat(id, getDefBonus(), tooltip);
                market.getStats().getDynamic().getMod(Stats.COMBAT_FLEET_SIZE_MULT).modifyPercent(id, 10f * (getDefBonus() / 50f), tooltip);
            } else
            {
                market.getStability().unmodify(id);
                market.getStats().getDynamic().getMod(Stats.GROUND_DEFENSES_MOD).unmodify(id);
                market.getStats().getDynamic().getMod(Stats.COMBAT_FLEET_SIZE_MULT).unmodify(id);
            }
        }
    }

    @Override
    public void unapply(String id)
    {
        super.unapply(id);
        market.getStability().unmodify(id);
        market.getStats().getDynamic().getMod(Stats.GROUND_DEFENSES_MOD).unmodify(id);
    }

    @Override
    protected void createTooltipAfterDescription(TooltipMakerAPI tooltip, boolean expanded) {
        super.createTooltipAfterDescription(tooltip, expanded);

        if (market == null) {
            return;
        }

        tooltip.addPara("%s " + text("cdl1"), 10f, Misc.getHighlightColor(), "+" + (int)STAB_BONUS);
        tooltip.addPara("%s " + text("cdl2"), 10f, Misc.getHighlightColor(), "+" + (int)(10f * (getDefBonus() / 50f)) + "%");
        tooltip.addPara("%s " + text("cdl3"), 10f, Misc.getHighlightColor(), "+" + (int)(getDefBonus()));
    }

    private float getDefBonus()
    {
        float bonus = 0f;
        FactionAPI apex = Global.getSector().getFaction("apex_design");
        for (FactionAPI faction : Global.getSector().getAllFactions())
        {
            if (!faction.isShowInIntelTab() || faction.getId().equals(Factions.REMNANTS) || faction.getId().equals("blade_breakers"))
                continue;
            if (faction.isHostileTo(apex))
            {
                bonus += DEF_BONUS_PER_ENEMY;
            }
        }
        return bonus;
    }

}
