package apexsubs;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import data.scripts.plugins.dl_SubsystemCombatManager;
import data.scripts.subsystems.dl_BaseSubsystem;
import org.lwjgl.input.Keyboard;
import org.lwjgl.util.vector.Vector2f;

import java.util.*;

import static plugins.ApexModPlugin.HAS_DRONELIB;

public class ApexSubsystemCombatManager extends BaseEveryFrameCombatPlugin
{
    public static final String DATA_KEY = "ApexSubsystemCombatManager";
    public static String INFO_TOGGLE_KEY;

    private Map<ShipAPI, List<Class<? extends ApexBaseSubsystem>>> subsystemHullmodQueue = new HashMap<>();

    public static boolean showInfoText = true;
    private static boolean firstAdvance = true;

    private final Map<ShipAPI, List<ApexBaseSubsystem>> subsystems;
    private static final Map<String, List<Class<? extends ApexBaseSubsystem>>> subsystemsByHullId = new HashMap<>();

    public ApexSubsystemCombatManager()
    {
        subsystems = new HashMap<>();
    }

    @Override
    public void init(CombatEngineAPI engine)
    {
        engine.getCustomData().put(DATA_KEY, this);
        if (HAS_DRONELIB)
        {
            INFO_TOGGLE_KEY = Global.getSettings().getString("dl_SubsystemToggleKey");
            showInfoText = true;
        } else
        {
            INFO_TOGGLE_KEY = null;
            showInfoText = false;
        }
        subsystems.clear();

        subsystemHullmodQueue = ApexSubsystemUtils.getSubsystemQueue();
        ApexSubsystemUtils.getSubsystemQueue().clear();
    }

    private boolean isHotkeyDownLast = false;

    @Override
    public void advance(float amount, List<InputEventAPI> events)
    {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (!engine.isPaused())
        {
            List<ShipAPI> ships = engine.getShips();
            for (ShipAPI ship : ships)
            {
                // hang for a frame and let dronelib do its thing first, if present
                // this is so we can get the right offset for apex subsystems
                if (firstAdvance)
                {
                    firstAdvance = false;
                    return;
                }
                if (!ship.isAlive()) continue;

                List<ApexBaseSubsystem> subsystemsOnShip = subsystems.get(ship);
                if (subsystemsOnShip == null) subsystemsOnShip = new ArrayList<>();

                List<Class<? extends ApexBaseSubsystem>> toAdd = new ArrayList<>();

                List<Class<? extends ApexBaseSubsystem>> subsystemByHullId = subsystemsByHullId.get(ship.getHullSpec().getBaseHullId());
                if (subsystemByHullId != null)
                {
                    outer:
                    for (Class<? extends ApexBaseSubsystem> c : subsystemByHullId)
                    {
                        for (ApexBaseSubsystem s : subsystemsOnShip) if (s.getClass().equals(c)) continue outer;

                        toAdd.add(c);
                    }
                }

                List<Class<? extends ApexBaseSubsystem>> hullmodQueue = subsystemHullmodQueue.get(ship);
                if (hullmodQueue != null)
                {
                    toAdd.addAll(hullmodQueue);
                    subsystemHullmodQueue.put(ship, null);
                }

                int index = 0;
                for (Class<? extends ApexBaseSubsystem> t : toAdd)
                {
                    try
                    {
                        ApexBaseSubsystem subsystem = t.newInstance();

                        subsystemsOnShip.add(subsystem);
                        subsystem.init(ship);
                        subsystem.setIndex(index);
                        index++;
                    } catch (InstantiationException | IllegalAccessException e)
                    {
                        e.printStackTrace();
                    }
                }

                subsystems.put(ship, subsystemsOnShip);
            }

            List<ShipAPI> rem = new LinkedList<>();
            for (ShipAPI ship : subsystems.keySet())
            {
                if (!engine.isEntityInPlay(ship))
                {
                    rem.add(ship);
                    continue;
                }

                int index = 0;
                for (ApexBaseSubsystem subsystem : subsystems.get(ship))
                {
                    subsystem.setIndex(index);
                    index++;

                    subsystem.advance(amount);
                    if (engine.getPlayerShip().equals(ship))
                    {
                        if (ship.getShipAI() != null)
                        {
                            subsystem.aiUpdate(amount);
                        }
                    } else
                    {
                        subsystem.aiUpdate(amount);
                    }
                }
            }

            for (ShipAPI ship : rem) subsystems.remove(ship);
        }

        // showing info for subsystems should be unnecessary, but we'll conform with dronelib if it's active
        if (HAS_DRONELIB)
        {
            boolean isHotkeyDown = Keyboard.isKeyDown(Keyboard.getKeyIndex(INFO_TOGGLE_KEY));
            if (isHotkeyDown && !isHotkeyDownLast) showInfoText = !showInfoText;
            isHotkeyDownLast = isHotkeyDown;
        }

        ShipAPI player = engine.getPlayerShip();
        List<ApexBaseSubsystem> s;
        if (player != null)
        {
            s = subsystems.get(player);
            if (s == null || s.isEmpty()) return;

            int numBars = 0;
            for (ApexBaseSubsystem sub : s)
            {
                numBars += sub.getNumGuiBars();
                if (showInfoText) numBars++;
            }
            // dronelib UI compat stuff
            int extraIndex = 0;
            if (HAS_DRONELIB && Global.getCombatEngine().getCustomData().containsKey("dl_SubsystemCombatManager"))
            {
                dl_SubsystemCombatManager dronelibManager = (dl_SubsystemCombatManager) Global.getCombatEngine().getCustomData().get("dl_SubsystemCombatManager");
                if (dronelibManager.getSubsystems().containsKey(player))
                {
                    for (dl_BaseSubsystem sub : dronelibManager.getSubsystemsOnShip(player))
                    {
                        extraIndex++;
                        numBars += sub.getNumGuiBars();
                        if (showInfoText)
                        {
                            numBars++;
                        }
                    }
                }
            }
            System.out.println(extraIndex);
            if (showInfoText)
                numBars++;
            Vector2f rootLoc = ApexCombatUI.getSubsystemsRootLocation(player, numBars, 13f * Global.getSettings().getScreenScaleMult());

            Vector2f inputLoc = new Vector2f(rootLoc);
            //inputLoc.y -= (numExtraGuiBars * 13f + numExtraSubs * 4f) * Global.getSettings().getScreenScaleMult();
            for (ApexBaseSubsystem sub : s)
            {
                inputLoc = sub.guiRender(inputLoc, rootLoc);
            }

            ApexCombatUI.drawSubsystemsTitle(engine.getPlayerShip(), showInfoText, rootLoc);

            List<String> defaultHotkeys = new ArrayList<>(ApexSpecLoadingUtils.getSubsystemHotkeyPriority());
            while (defaultHotkeys.size() < s.size())
            {
                defaultHotkeys.add(defaultHotkeys.get(5));
            }
            //Collections.reverse(defaultHotkeys);

            for (ApexBaseSubsystem sub : s)
                sub.activeHotkey = (defaultHotkeys.get(sub.getIndex()+extraIndex));
        }
    }

    public Map<ShipAPI, List<ApexBaseSubsystem>> getSubsystems()
    {
        return subsystems;
    }

    public List<ApexBaseSubsystem> getSubsystemsOnShip(ShipAPI ship)
    {
        return subsystems.get(ship);
    }

    public static Map<String, List<Class<? extends ApexBaseSubsystem>>> getSubsystemsByHullId()
    {
        return subsystemsByHullId;
    }
}
