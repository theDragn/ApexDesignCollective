package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public class ApexQGAmplifier extends BaseHullMod
{
    public static final float QGP_EMP_FRACTION = 0.5f;
    public static final float QGPD_EXTRA_ENERGY = 50f;

    // all of the actual effects are done in ApexQGOnHit

    @Override
    public String getDescriptionParam(int index, ShipAPI.HullSize hullSize)
    {
        if (index == 0)
            return "Quark-Gluon Pulse weapons";
        if (index == 1)
            return "EMP damage";
        if (index == 2)
            return (int)(QGP_EMP_FRACTION * 100f) + "%";
        if (index == 3)
            return "Quark-Gluon PD weapons";
        if (index == 4)
            return (int)(QGPD_EXTRA_ENERGY) + " energy";
        return null;
    }
}
