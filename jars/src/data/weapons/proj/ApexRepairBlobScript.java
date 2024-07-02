package data.weapons.proj;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.Misc;
import data.effects.ApexCryoEffect;
import data.effects.ApexRegenEffect;
import org.dark.shaders.light.LightAPI;
import org.dark.shaders.light.LightShader;
import org.dark.shaders.light.StandardLight;
import org.jetbrains.annotations.NotNull;
import org.lazywizard.lazylib.CollisionUtils;
import org.lazywizard.lazylib.FastTrig;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;
import plugins.ApexModPlugin;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

//By Nicke535, licensed under CC-BY-NC-SA 4.0 (https://creativecommons.org/licenses/by-nc-sa/4.0/)


public class ApexRepairBlobScript extends BaseEveryFrameCombatPlugin
{
    private static final float TURN_RATE = 180f;
    private static final float GUIDANCE_DELAY = 0.5f;

    private DamagingProjectileAPI proj;
    private ShipAPI target;
    private float aliveTime;
    private float actualDelay;

    public ApexRepairBlobScript(DamagingProjectileAPI proj, ShipAPI target)
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

    public void applyRegenEffect(ShipAPI target, DamagingProjectileAPI proj)
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
                        Color.GREEN
                );
            }
            StandardLight light = new StandardLight();
            light.setColor(Color.GREEN);
            light.setLifetime(0.33f);
            light.setIntensity(0.25f);
            light.setAutoFadeOutTime(0.33f);
            light.setSize(120f);
            light.setLocation(proj.getLocation());
            light.setVelocity(proj.getVelocity());
            LightShader.addLight(light);
        }
        Global.getCombatEngine().addPlugin(new ApexRegenEffect(target, proj));
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
            applyRegenEffect(target, proj);



        float turnRate = TURN_RATE;
        if (proj.getSource().getHullSize() == ShipAPI.HullSize.FRIGATE || proj.getSource().getHullSize() == ShipAPI.HullSize.DESTROYER)
            turnRate *= 2f;

        float targetAngle = VectorUtils.getAngle(proj.getLocation(), target.getLocation());
        float rotateAmount = Math.min(Math.abs(MathUtils.getShortestRotation(proj.getFacing(), targetAngle)), turnRate * amount)  * Misc.getClosestTurnDirection(proj.getFacing(), targetAngle);
        proj.setFacing(proj.getFacing() + rotateAmount);
    }
}