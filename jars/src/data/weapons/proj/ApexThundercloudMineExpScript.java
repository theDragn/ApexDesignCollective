package data.weapons.proj;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.Misc;

import java.awt.*;
import java.util.List;

public class ApexThundercloudMineExpScript implements EveryFrameCombatPlugin
{
    private MissileAPI mine;

    public ApexThundercloudMineExpScript(MissileAPI mine)
    {
        this.mine = mine;
    }
    @Override
    public void processInputPreCoreControls(float amount, List<InputEventAPI> events)
    {

    }

    @Override
    public void advance(float amount, List<InputEventAPI> events)
    {
        if (mine.didDamage() || (!Global.getCombatEngine().isInPlay(mine) && mine.getHitpoints() > 0))
        {
            Color explosionColor;
            switch (mine.getProjectileSpecId())
            {
                case "apex_thundercloud_mine_frag":
                    explosionColor = new Color(75, 255, 84, 64);
                    break;
                case "apex_thundercloud_mine_emp":
                    explosionColor = new Color(75, 192, 255, 64);
                    break;
                default:
                    explosionColor = new Color(255, 75, 75, 64);
            }
            CombatEngineAPI engine = Global.getCombatEngine();
            engine.addSmoothParticle(mine.getLocation(), Misc.ZERO, 300f, 1.0F, 0.05F, Color.WHITE);
            engine.addSmoothParticle(mine.getLocation(), Misc.ZERO, 300f, 1.0F, 0.1F, Color.WHITE);
            engine.spawnExplosion(
                    mine.getLocation(),
                    Misc.ZERO,
                    explosionColor,
                    150,
                    1.0f
            );
            engine.addSmoothParticle(mine.getLocation(), Misc.ZERO, 600f, 1.0F, 0.5F, explosionColor);
            engine.removePlugin(this);
        }
    }

    @Override
    public void renderInWorldCoords(ViewportAPI viewport)
    {

    }

    @Override
    public void renderInUICoords(ViewportAPI viewport)
    {

    }

    @Override
    public void init(CombatEngineAPI engine)
    {

    }
}
