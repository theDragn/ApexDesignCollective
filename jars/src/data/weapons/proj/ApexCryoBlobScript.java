package data.weapons.proj;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.Misc;
import data.effects.ApexCryoEffect;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;
import plugins.ApexModPlugin;

import java.awt.*;
import java.util.List;

public class ApexCryoBlobScript extends BaseEveryFrameCombatPlugin
{
    private static final float TURN_RATE = 180f;
    private static final float GUIDANCE_DELAY = 0.5f;

    private DamagingProjectileAPI proj;
    private ShipAPI target;
    private float aliveTime;
    private float actualDelay;

    public ApexCryoBlobScript(DamagingProjectileAPI proj, ShipAPI target)
    {
        this.proj = proj;
        this.target = target;
        actualDelay = GUIDANCE_DELAY * (1f + Misc.random.nextFloat() * 0.75f);
        aliveTime = 0f;
        if (proj.getSource().getHullSize() == ShipAPI.HullSize.FRIGATE || proj.getSource().getHullSize() == ShipAPI.HullSize.DESTROYER)
        {
            actualDelay = 0;
            proj.getVelocity().scale(2f);
        }
    }

    public void applyCryoEffect(ShipAPI target, DamagingProjectileAPI proj)
    {
        // TODO: sounds
        if (!ApexModPlugin.POTATO_MODE)
        {
            for (int i = 0; i < 10; i++)
            {
                Global.getCombatEngine().addNebulaParticle(
                        proj.getLocation(),
                        Vector2f.add(proj.getVelocity(), MathUtils.getRandomPointInCircle(Misc.ZERO, 200f), new Vector2f()),
                        50f * (Misc.random.nextFloat() + 0.5f),
                        (Misc.random.nextFloat() + 1f),
                        0f,
                        0f,
                        0.66f,
                        Color.CYAN
                );
            }
        }
        Global.getCombatEngine().addPlugin(new ApexCryoEffect(target, proj.getSource().getHullSize()));
        Global.getCombatEngine().removeEntity(proj);
    }

    @Override
    public void advance(float amount, List<InputEventAPI> events)
    {
        if (Global.getCombatEngine() == null || Global.getCombatEngine().isPaused())
        {
            return;
        }

        if (proj == null || proj.isFading() || !Global.getCombatEngine().isEntityInPlay(proj))
        {
            Global.getCombatEngine().removePlugin(this);
            return;
        }

        if (target == null)
        {
            Global.getCombatEngine().removeEntity(proj);
            Global.getCombatEngine().removePlugin(this);
            return;
        }

        aliveTime += amount;
        if (aliveTime < actualDelay)
            return;
        if (target != null
                && MathUtils.getDistanceSquared(proj.getLocation(), target.getLocation()) < target.getCollisionRadius() * target.getCollisionRadius()
                && !target.isPhased())
            applyCryoEffect(target, proj);



        float turnRate = TURN_RATE;
        if (proj.getSource().getHullSize() == ShipAPI.HullSize.FRIGATE || proj.getSource().getHullSize() == ShipAPI.HullSize.DESTROYER)
            turnRate *= 2f;

        float targetAngle = VectorUtils.getAngle(proj.getLocation(), target.getLocation());
        float rotateAmount = Math.min(Math.abs(MathUtils.getShortestRotation(proj.getFacing(), targetAngle)), turnRate * amount)  * Misc.getClosestTurnDirection(proj.getFacing(), targetAngle);
        proj.setFacing(proj.getFacing() + rotateAmount);
    }
}