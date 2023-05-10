package data.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import org.magiclib.util.MagicRender;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;

public class ApexVLSEffects implements OnFireEffectPlugin, EveryFrameWeaponEffectPlugin
{
    public static final Vector2f SPRITE_SIZE = new Vector2f(12f, 12f);
    public static final Vector2f OFFSET_LARGE = new Vector2f(-0.5f, -0.5f);
    public static final Vector2f OFFSET_SMALL_1 = new Vector2f(-0.5f, -1.5f);
    public static final Vector2f OFFSET_SMALL_2 = new Vector2f(-0.5f, 0.5f);
    public static final int[] FRAMES_LARGE = {1, 2, 3, 4, 5, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 5, 4, 3, 2, 1};
    public static final int[] FRAMES_SMALL = {1, 2, 3, 4, 5, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 5, 4, 3, 2, 1};
    public static final float FRAMES_PER_SECOND = 15f;
    public static final float DELAY_BETWEEN_TUBES = 0.1f;
    private float animTimer = 0f;


    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon)
    {
        if (engine == null || weapon == null)
            return;

        float missileRofMult = weapon.getShip().getMutableStats().getMissileRoFMult().getModifiedValue();

        float chargeLevel = weapon.getChargeLevel();
        if (engine.isPaused())
            amount = 0;
        if (weapon.isFiring() || animTimer > 0)
            animTimer += amount * missileRofMult;

        if (animTimer > 0)
        {
            Vector2f offset = OFFSET_LARGE;
            int[] frames = FRAMES_LARGE;
            // idk why it needs this but I'm not figuring out this script again
            // just fucking with it until it works
            if (weapon.getSize().equals(WeaponAPI.WeaponSize.SMALL))
            {
                frames = FRAMES_SMALL;
                if (weapon.getId().contains("left"))
                    offset = OFFSET_SMALL_2;
                else
                    offset = OFFSET_SMALL_1;
            }
            // would normally have to check hardpoint/turret, but this will always be a turret
            // or at least they'll always have the same offsets
            for (int i = 0; i < weapon.getSpec().getTurretFireOffsets().size(); i++)
            {
                int frame = (int) ((animTimer - (DELAY_BETWEEN_TUBES / missileRofMult) * i) * FRAMES_PER_SECOND);
                if (frame >= frames.length || frame < 0)
                    continue;
                Vector2f barrelLoc = new Vector2f(weapon.getSpec().getTurretFireOffsets().get(i));
                Vector2f.add(barrelLoc, offset, barrelLoc);
                Vector2f renderPoint = VectorUtils.rotate(barrelLoc, weapon.getCurrAngle());
                Vector2f.add(renderPoint, weapon.getLocation(), renderPoint);

                SpriteAPI hatchFrame = Global.getSettings().getSprite("apex_vls", "vls_frame" + frames[frame]);
                MagicRender.singleframe(
                        hatchFrame,
                        renderPoint,
                        SPRITE_SIZE,
                        weapon.getCurrAngle() - 90,
                        hatchFrame.getColor(),
                        false
                );
            }
        }
        float cutoff = weapon.getSize().equals(WeaponAPI.WeaponSize.SMALL) ? 3f : 5f;
        if (chargeLevel == 0 && animTimer >= cutoff / missileRofMult)
            animTimer = 0;
    }

    @Override
    public void onFire(DamagingProjectileAPI proj, WeaponAPI weapon, CombatEngineAPI engine)
    {
        GuidedMissileAI ai = (GuidedMissileAI) ((MissileAPI) proj).getUnwrappedMissileAI();
        if (ai.getTarget() != null)
        {
            rotateMissileToTarget(proj, ai.getTarget());
        } else
        {
            ShipAPI source = proj.getSource();
            if (source == null)
                return;
            ShipAPI target = null;
            if (source.getWeaponGroupFor(weapon) != null && source.getWeaponGroupFor(weapon).isAutofiring()  //weapon group is autofiring
                    && source.getSelectedGroupAPI() != source.getWeaponGroupFor(weapon))
            { //weapon group is not the selected group
                target = source.getWeaponGroupFor(weapon).getAutofirePlugin(weapon).getTargetShip();
            } else
            {
                target = source.getShipTarget();
            }
            if (target != null && target.isAlive())
            {
                rotateMissileToTarget(proj, target);
                return;
            }
            target = getNearestNonFighterEnemy(proj);
            if (target != null)
            {
                rotateMissileToTarget(proj, target);
                return;
            }
            // well, no
            float randFacing = MathUtils.getRandomNumberInRange(-60f, 60f) + weapon.getCurrAngle();
            proj.setFacing(randFacing);
            VectorUtils.rotate(proj.getVelocity(), MathUtils.getShortestRotation(proj.getFacing(), randFacing));

        }
        // projectile swap and smoke here
    }

    private void rotateMissileToTarget(DamagingProjectileAPI proj, CombatEntityAPI target)
    {
        float randFacing = VectorUtils.getAngle(proj.getLocation(), target.getLocation()) + MathUtils.getRandomNumberInRange(-60f, 60f);
        VectorUtils.rotate(proj.getVelocity(), MathUtils.getShortestRotation(proj.getFacing(), randFacing));
        proj.setFacing(randFacing);
        ((GuidedMissileAI) ((MissileAPI) proj).getUnwrappedMissileAI()).setTarget(target);
    }

    public static ShipAPI getNearestNonFighterEnemy(CombatEntityAPI entity)
    {
        ShipAPI closest = null;
        float distance, closestDistance = Float.MAX_VALUE;

        for (ShipAPI tmp : AIUtils.getEnemiesOnMap(entity))
        {
            if (tmp.getHullSize().equals(ShipAPI.HullSize.FIGHTER) || !tmp.isAlive())
                continue;
            distance = MathUtils.getDistance(tmp, entity.getLocation());
            if (distance > 2000)
                continue;
            if (distance < closestDistance)
            {
                closest = tmp;
                closestDistance = distance;
            }
        }

        return closest;
    }
}
