package data.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponType;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;

import java.awt.*;
import java.util.EnumSet;

import static data.ApexUtils.text;

public class ApexDamper extends BaseShipSystemScript
{
    public static Object KEY_SHIP = new Object();
    public static float TIME_MULT = 1.5f;
    public static float DAMAGE_MULT = 0.3f;

    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel)
    {
        effectLevel = 1f;

        stats.getHullDamageTakenMult().modifyMult(id, 1f - (1f - DAMAGE_MULT) * effectLevel);
        stats.getArmorDamageTakenMult().modifyMult(id, 1f - (1f - DAMAGE_MULT) * effectLevel);
        stats.getEmpDamageTakenMult().modifyMult(id, 1f - (1f - DAMAGE_MULT) * effectLevel);
        stats.getTimeMult().modifyMult(id, TIME_MULT);


        ShipAPI ship = null;
        if (stats.getEntity() instanceof ShipAPI)
        {
            ship = (ShipAPI) stats.getEntity();
            if (ship == Global.getCombatEngine().getPlayerShip())
                Global.getCombatEngine().getTimeMult().modifyMult(id, 1f / TIME_MULT);
            else
                Global.getCombatEngine().getTimeMult().unmodify(id);
            ship.fadeToColor(KEY_SHIP, new Color(75,75,135,255), 0.25f, 0.25f, effectLevel);
            ship.getEngineController().fadeToOtherColor(KEY_SHIP, new Color(0,0,0,0), new Color(0,0,0,0), effectLevel, 0.75f * effectLevel);
            ship.setJitterUnder(KEY_SHIP, new Color(100,165,255,255), effectLevel, 15, 0f, 15f);
        }
        if (ship != null && ship == Global.getCombatEngine().getPlayerShip())
        {
            if (ship.getSystem() != null)
            {
                Global.getCombatEngine().maintainStatusForPlayerShip(
                        KEY_SHIP,
                        ship.getSystem().getSpecAPI().getIconSpriteName(),
                        ship.getSystem().getDisplayName(),
                        (int) Math.round((1f - DAMAGE_MULT) * effectLevel * 100) + text("damper"),
                        false
                );
            }
        }
        if (ship == null)
            return;


    }

    public void unapply(MutableShipStatsAPI stats, String id)
    {
        stats.getHullDamageTakenMult().unmodify(id);
        stats.getArmorDamageTakenMult().unmodify(id);
        stats.getEmpDamageTakenMult().unmodify(id);
        Global.getCombatEngine().getTimeMult().unmodify(id);
        stats.getTimeMult().unmodify(id);
    }
}
