package data.weapons.proj;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.DamageTakenModifier;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

import static utils.ApexUtils.text;
import static plugins.ApexModPlugin.POTATO_MODE;

// plugin determines if ship needs a listener, and keeps track of debuffs
public class ApexDartgunPlugin extends BaseCombatLayeredRenderingPlugin
{
    public static final float DEBUFF_DURATION = 5f;
    public static final float BONUS_PER_DEBUFF = 0.08f;
    public static final int MAX_NUM_DEBUFFS = 13;
    //public static final String RING_SPRITE = "graphics/fx/shields256ring.png";
    //public static final String PIP_SPRITE = "graphics/ui/icons/16x_starburst_circle.png";
    private static final Color LOW_STACKS_COLOR = Color.YELLOW;
    private static final Color HIGH_STACKS_COLOR = Color.RED;
    public static final Color JITTER_COLOR = new Color(255,55,55,75);
    public static final Color JITTER_UNDER_COLOR = new Color(255,55,55,155);


    public float damageMult;
    private boolean removeThisPlugin;
    private float[] timers;
    private ShipAPI target;
    private ApexDartgunListener listener;
    private int activeTimers;


    // consists of two parts: controller plugin and damage listener
    // controller plugin does graphics and timer updates
    // damage listener links back to the controller and modifies damage

    public ApexDartgunPlugin(ShipAPI target)
    {
        this.target = target;
        timers = new float[MAX_NUM_DEBUFFS];
        damageMult = 0f;

        timers[0] = DEBUFF_DURATION;
        activeTimers = 0;
        // you can't easily check for an instance of the plugin from the engine
        // instead, it checks the ship for the listener, and then gets the controller plugin through that
        if (!target.hasListenerOfClass(ApexDartgunListener.class))
        {
            listener = new ApexDartgunListener(this);
            target.addListener(listener);
            removeThisPlugin = false;
            if (target.getFluxTracker().showFloaty() || target == Global.getCombatEngine().getPlayerShip()) {
                target.getFluxTracker().showOverloadFloatyIfNeeded(text("harmonic1"), new Color(255,155,155), 4f, true);
            }
        } else
        {
            target.getListeners(ApexDartgunListener.class).get(0).controller.addHit();
            removeThisPlugin = true;
        }
    }

    @Override
    public float getRenderRadius()
    {
        return 1500f;
    }


    @Override
    public void advance(float amount)
    {
        if (Global.getCombatEngine() == null)
            return;
        if (!target.isAlive())
            return;
        activeTimers = 0;
        for (int i = 0; i < timers.length; i++)
        {
            timers[i] = Math.max(timers[i] - amount, 0f);
            if (timers[i] > 0f)
                activeTimers++;
        }
        damageMult = Math.min(1f + activeTimers * BONUS_PER_DEBUFF, 2f);

        if (activeTimers == 0)
        {
            removeThisPlugin = true;
            target.removeListener(listener);
        }

        float effectLevel = activeTimers / 13f;

        if (!POTATO_MODE)
        {
            target.setJitterUnder("apex_dartgun", JITTER_UNDER_COLOR, effectLevel, 4, 0f, 3f + effectLevel * 17);
            target.setJitter("apex_dartgun", JITTER_COLOR, effectLevel, 4, 1f, 2 + effectLevel * 17);
        }
    }

    @Override
    public void render(CombatEngineLayers layer, ViewportAPI viewport)
    {

    }

    @Override
    public boolean isExpired()
    {
        return removeThisPlugin || !target.isAlive();
    }

    public void addHit()
    {
        for (int i = 0; i < timers.length; i++)
        {
            if (timers[i] == 0f)
            {
                timers[i] = DEBUFF_DURATION;
                return;
            }
        }
    }

    public class ApexDartgunListener implements DamageTakenModifier
    {
        private ApexDartgunPlugin controller;

        public ApexDartgunListener(ApexDartgunPlugin controller)
        {
            this.controller = controller;
        }

        @Override
        public String modifyDamageTaken(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit)
        {
            if (param instanceof DamagingProjectileAPI && !shieldHit)
            {
                DamagingProjectileAPI proj = (DamagingProjectileAPI) param;
                if (proj.getWeapon() != null && proj.getWeapon().getId() != null
                        && (   proj.getWeapon().getId().equals("apex_dartgun")
                            || proj.getWeapon().getId().equals("apex_thundercloud_mine_he")
                            || proj.getWeapon().getId().equals("apex_harmonic_rocket_rack")
                            )
                    )
                {
                    damage.getModifier().modifyMult("apexDartgun", controller.damageMult);
                    //System.out.println("damage mult was " + controller.damageMult);
                    return "apexDartgun";
                }
                // ready for some jank?
                // explosions are instanceof DamagingProjectileAPI
                // but the (obfuscated) DamagingExplosion class returns null for basically everything
                // instead, we detect specific explosions by giving them specific damage values
                if (proj.getWeapon() == null && proj.getBaseDamageAmount() == 249f)
                {
                    damage.getModifier().modifyMult("apexDartgun", controller.damageMult);
                    return "apexDartgun";
                }
            }
            return null;
        }
    }
}
