package data.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.FighterWingAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponType;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static data.ApexUtils.text;

public class ApexRemoteDamper extends BaseShipSystemScript
{
    public static Object KEY_SHIP = new Object();
    public static float TIME_MULT = 1.5f;
    public static float DAMAGE_MULT = 0.3f;

    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel)
    {
        ShipAPI ship;
        if (stats.getEntity() instanceof ShipAPI)
            ship = ((ShipAPI) stats.getEntity());
        else
            return;

        for (FighterWingAPI wing : ship.getAllWings())
        {
            for (ShipAPI fighter : wing.getWingMembers())
            {
                fighter.getMutableStats().getHullDamageTakenMult().modifyMult(id, 1f - (1f - DAMAGE_MULT) * effectLevel);
                fighter.getMutableStats().getArmorDamageTakenMult().modifyMult(id, 1f - (1f - DAMAGE_MULT) * effectLevel);
                fighter.getMutableStats().getEmpDamageTakenMult().modifyMult(id, 1f - (1f - DAMAGE_MULT) * effectLevel);
                fighter.getMutableStats().getTimeMult().modifyMult(id, TIME_MULT);

                fighter.fadeToColor(KEY_SHIP, new Color(75, 75, 135, 255), 0.25f, 0.25f, effectLevel);
                fighter.getEngineController().fadeToOtherColor(KEY_SHIP, new Color(0, 0, 0, 0), new Color(0, 0, 0, 0), effectLevel, 0.75f * effectLevel);
                fighter.setJitterUnder(KEY_SHIP, new Color(100, 165, 255, 255), effectLevel, 15, 0f, 15f);

                if (fighter.getShield() != null)
                {
                    fighter.getShield().toggleOff();
                }
            }
            if (!wing.getReturning().isEmpty())
            {
                for (FighterWingAPI.ReturningFighter returningFighter : wing.getReturning())
                {
                    ShipAPI fighter = returningFighter.fighter;
                    fighter.getMutableStats().getHullDamageTakenMult().modifyMult(id, 1f - (1f - DAMAGE_MULT) * effectLevel);
                    fighter.getMutableStats().getArmorDamageTakenMult().modifyMult(id, 1f - (1f - DAMAGE_MULT) * effectLevel);
                    fighter.getMutableStats().getEmpDamageTakenMult().modifyMult(id, 1f - (1f - DAMAGE_MULT) * effectLevel);
                    fighter.getMutableStats().getTimeMult().modifyMult(id, TIME_MULT);

                    fighter.fadeToColor(KEY_SHIP, new Color(75, 75, 135, 255), 0.25f, 0.25f, effectLevel);
                    fighter.getEngineController().fadeToOtherColor(KEY_SHIP, new Color(0, 0, 0, 0), new Color(0, 0, 0, 0), effectLevel, 0.75f * effectLevel);
                    fighter.setJitterUnder(KEY_SHIP, new Color(100, 165, 255, 255), effectLevel, 15, 0f, 15f);

                    if (fighter.getShield() != null)
                    {
                        fighter.getShield().toggleOff();
                    }
                }
            }
        }
        if (ship != null && ship == Global.getCombatEngine().getPlayerShip())
        {
            if (ship.getSystem() != null)
            {
                Global.getCombatEngine().maintainStatusForPlayerShip(
                        KEY_SHIP,
                        ship.getSystem().getSpecAPI().getIconSpriteName(),
                        ship.getSystem().getDisplayName(),
                        text("rdamper"),
                        false
                );
            }
        }
    }

    public void unapply(MutableShipStatsAPI stats, String id)
    {
        ShipAPI ship;
        if (stats.getEntity() instanceof ShipAPI)
            ship = ((ShipAPI) stats.getEntity());
        else
            return;
        for (FighterWingAPI wing : ship.getAllWings())
        {
            for (ShipAPI fighter : wing.getWingMembers())
            {
                fighter.getMutableStats().getHullDamageTakenMult().unmodify(id);
                fighter.getMutableStats().getArmorDamageTakenMult().unmodify(id);
                fighter.getMutableStats().getEmpDamageTakenMult().unmodify(id);
                fighter.getMutableStats().getTimeMult().unmodify(id);
            }
        }
    }
}
