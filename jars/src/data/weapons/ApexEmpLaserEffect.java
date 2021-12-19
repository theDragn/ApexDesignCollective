package data.weapons;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lwjgl.util.vector.Vector2f;

public class ApexEmpLaserEffect implements BeamEffectPlugin
{
    private IntervalUtil fireInterval = new IntervalUtil(0.25f, 4f);
    private boolean wasZero = true;


    public void advance(float amount, CombatEngineAPI engine, BeamAPI beam)
    {
        CombatEntityAPI target = beam.getDamageTarget();
        if (target instanceof ShipAPI && beam.getBrightness() >= 1f)
        {
            float dur = beam.getDamage().getDpsDuration();
            // needed because when the ship is in fast-time, dpsDuration will not be reset every frame as it should be?
            // idk who wrote that but it wasn't me
            // no clue if that's true but it works so I'm not checking
            if (!wasZero) dur = 0;
            wasZero = beam.getDamage().getDpsDuration() <= 0;
            fireInterval.advance(dur);

            boolean hitShield = target.getShield() != null && target.getShield().isWithinArc(beam.getTo());
            if (!hitShield && fireInterval.intervalElapsed())
            {
                Vector2f point = beam.getRayEndPrevFrame();
                float emp = beam.getDamage().getFluxComponent() * 1f;
                float dam = beam.getDamage().getDamage() * 1f;
                engine.spawnEmpArc(
                        beam.getSource(), point, beam.getDamageTarget(), beam.getDamageTarget(),
                        DamageType.ENERGY,
                        dam, // damage
                        emp, // emp
                        100000f, // max range
                        "tachyon_lance_emp_impact",
                        beam.getWidth() + 9f,
                        beam.getFringeColor(),
                        beam.getCoreColor()
                );
            }
        }
    }
}
