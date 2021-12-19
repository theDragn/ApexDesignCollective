package data.shipsystems.ai;

import java.util.List;

import org.lwjgl.util.vector.Vector2f;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipSystemAIScript;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import com.fs.starfarer.api.combat.ShipwideAIFlags;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponSize;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponType;
import com.fs.starfarer.api.util.IntervalUtil;


public class ApexRapidLoaderAI implements ShipSystemAIScript
{

    private ShipAPI ship;
    private CombatEngineAPI engine;
    private ShipwideAIFlags flags;
    private ShipSystemAPI system;

    private IntervalUtil tracker = new IntervalUtil(0.5f, 1f);

    public void init(ShipAPI ship, ShipSystemAPI system, ShipwideAIFlags flags, CombatEngineAPI engine)
    {
        this.ship = ship;
        this.flags = flags;
        this.engine = engine;
        this.system = system;
    }

    public void advance(float amount, Vector2f missileDangerDir, Vector2f collisionDangerDir, ShipAPI target)
    {

        tracker.advance(amount);
        if (system.getCooldownRemaining() > 0)
            return;
        if (system.isActive())
            return;
        if (target == null)
            return;

        if (tracker.intervalElapsed())
        {
            int emptyWeapons = 0;
            for (WeaponAPI wep : ship.getAllWeapons())
            {
                if (wep.getAmmo() == 0)
                    emptyWeapons++;
            }

            boolean targetIsVulnerable =
                    target.getFluxTracker().isOverloadedOrVenting() ||
                    target.getFluxTracker().getFluxLevel() > 0.75f;

            float remainingFluxLevel = 1f - ship.getFluxTracker().getFluxLevel();

            float fluxFractionPerUse = system.getFluxPerUse() / ship.getFluxTracker().getMaxFlux();
            if (fluxFractionPerUse > remainingFluxLevel)
                return;

            if ((targetIsVulnerable && emptyWeapons >= 2) || (emptyWeapons >= 3))
                ship.useSystem();
        }
    }

}
