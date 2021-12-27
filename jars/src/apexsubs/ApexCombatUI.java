// Borrowed from Tomatopaste's Dronelib, with minor modification

package apexsubs;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.loading.WeaponGroupSpec;
import org.lazywizard.lazylib.ui.FontException;
import org.lazywizard.lazylib.ui.LazyFont;
import org.lazywizard.lazylib.ui.LazyFont.DrawableString;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static plugins.ApexModPlugin.HAS_DRONELIB;

/**
 *
 * @author Dark.Revenant, Tartiflette, LazyWizard, Snrasha, Tomatopaste
 * because jesus christ why are most of these private methods in magiclib
 *
 */

public class ApexCombatUI {
    //Color of the HUD when the ship is alive or the hud
    public static final Color GREENCOLOR;
    //Color of the HUD when the ship is not alive.
    public static final Color BLUCOLOR;
    //Color of the HUD for the red color.
    public static final Color REDCOLOR;
    private static DrawableString TODRAW14;

    private static final Vector2f PERCENTBARVEC1 = new Vector2f(21f, 0f); // Just 21 pixel of width of difference.
    private static final Vector2f PERCENTBARVEC2 = new Vector2f(50f, 58f);

    private static final float UIscaling = Global.getSettings().getScreenScaleMult();

    static {
        GREENCOLOR = Global.getSettings().getColor("textFriendColor");
        BLUCOLOR = Global.getSettings().getColor("textNeutralColor");
        REDCOLOR = Global.getSettings().getColor("textEnemyColor");

        try {
            LazyFont fontdraw = LazyFont.loadFont("graphics/fonts/victor14.fnt");
            TODRAW14 = fontdraw.createText();

            if (UIscaling > 1f) { //mf
                TODRAW14.setFontSize(14f * UIscaling);
            }

        } catch (FontException ignored) {
        }
    }

    ///////////////////////////////////
    //                               //
    //         SUBSYSTEM GUI         //
    //                               //
    ///////////////////////////////////

    /**
     *
     * @author tomatopaste
     * @param ship Player ship
     * @param fill Value 0 to 1, how full the bar is from left to right
     * @param name Name of subsystem
     * @param infoText Info string opportunity
     * @param stateText Subsystem activity status
     * @param hotkey Hotkey string of key used to activate subsystem
     * @param flavourText A brief description of what the subsystem does
     */
    public static Vector2f drawSubsystemStatus(
            ShipAPI ship,
            float fill,
            String name,
            String infoText,
            String stateText,
            String hotkey,
            String flavourText,
            boolean showInfoText,
            int guiBarCount,
            Vector2f inputLoc,
            Vector2f rootLoc
    ) {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (!ship.equals(engine.getPlayerShip()) || engine.isUIShowingDialog()) return null;
        if (engine.getCombatUI() == null || engine.getCombatUI().isShowingCommandUI() || !engine.isUIShowingHUD()) return null;

        Color color = (ship.isAlive()) ? GREENCOLOR : BLUCOLOR;

        final float barHeight = 13f * UIscaling;
        final int bars = (showInfoText) ? guiBarCount + 1 : guiBarCount;

        Vector2f loc = new Vector2f(inputLoc);

        openGL11ForText();

        TODRAW14.setMaxWidth(6969);
        if (HAS_DRONELIB)
            TODRAW14.setText(name);
        else
            TODRAW14.setText(name + " ("+hotkey+")");
        TODRAW14.setColor(Color.BLACK);
        TODRAW14.draw(loc.x + 1, loc.y - 1);
        TODRAW14.setColor(color);
        TODRAW14.draw(loc);

        Vector2f hotkeyTextLoc = new Vector2f(loc);
        hotkeyTextLoc.y -= barHeight * UIscaling;
        hotkeyTextLoc.x += 20f * UIscaling;

        TODRAW14.setText("HOTKEY: " + hotkey);
        float hotkeyTextWidth = TODRAW14.getWidth();
        if (showInfoText) {
            TODRAW14.setColor(Color.BLACK);
            TODRAW14.draw(hotkeyTextLoc.x + 1, hotkeyTextLoc.y - 1);
            TODRAW14.setColor(color);
            TODRAW14.draw(hotkeyTextLoc);
        }

        Vector2f flavourTextLoc = new Vector2f(hotkeyTextLoc);
        flavourTextLoc.x += hotkeyTextWidth + 20f * UIscaling;

        TODRAW14.setText("BRIEF: " + flavourText);
        if (showInfoText) {
            TODRAW14.setColor(Color.BLACK);
            TODRAW14.draw(flavourTextLoc.x + 1, flavourTextLoc.y - 1);
            TODRAW14.setColor(color);
            TODRAW14.draw(flavourTextLoc);
        }

        Vector2f boxLoc = new Vector2f(loc);
        boxLoc.x += 200f * UIscaling;

        final float boxHeight = 9f * UIscaling;
        final float boxEndWidth = 45f * UIscaling;

        float boxWidth = boxEndWidth * fill;

        Vector2f stateLoc = new Vector2f(boxLoc);
        TODRAW14.setText(stateText);
        stateLoc.x -= TODRAW14.getWidth() + (4f * UIscaling);
        TODRAW14.setColor(Color.BLACK);
        TODRAW14.draw(stateLoc.x + 1, stateLoc.y - 1);
        TODRAW14.setColor(color);
        TODRAW14.draw(stateLoc);

        Vector2f infoLoc = new Vector2f(boxLoc);
        infoLoc.x += boxEndWidth + (5f * UIscaling);
        TODRAW14.setText(infoText);
        TODRAW14.setColor(Color.BLACK);
        TODRAW14.draw(infoLoc.x + 1, infoLoc.y - 1);
        TODRAW14.setColor(color);
        TODRAW14.draw(infoLoc);

        closeGL11ForText();

        final int width = (int) (Display.getWidth() * Display.getPixelScaleFactor());
        final int height = (int) (Display.getHeight() * Display.getPixelScaleFactor());

        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glViewport(0, 0, width, height);
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        GL11.glOrtho(0, width, 0, height, -1, 1);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glTranslatef(0.01f, 0.01f, 0);

        Vector2f nodeLoc = new Vector2f(loc);
        nodeLoc.y -= 4f * UIscaling;
        nodeLoc.x -= 2f * UIscaling;

        Vector2f titleLoc = getSubsystemTitleLoc(ship);
        boolean isHigh = loc.y > titleLoc.y;

        GL11.glLineWidth(UIscaling);
        GL11.glBegin(GL11.GL_LINE_STRIP);
        GL11.glColor4f(color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, 1f - engine.getCombatUI().getCommandUIOpacity());
        GL11.glVertex2f(nodeLoc.x, nodeLoc.y);

        nodeLoc.y += (isHigh) ? -6f * UIscaling : 6f * UIscaling;
        nodeLoc.x -= 6f * UIscaling;
        GL11.glVertex2f(nodeLoc.x, nodeLoc.y);

        boolean isTitleHigh = rootLoc.y > titleLoc.y - 16f;
        nodeLoc.y = getSubsystemTitleLoc(ship).y;
        nodeLoc.y -= 16f;
        nodeLoc.y -= (isTitleHigh) ? -6f * UIscaling : 6f * UIscaling;
        GL11.glVertex2f(nodeLoc.x, nodeLoc.y);

        GL11.glEnd();

        Vector2f boxRenderLoc = new Vector2f(boxLoc);
        boxRenderLoc.y -= 3f * UIscaling;

        //drop shadow
        Vector2f shadowLoc = new Vector2f(boxRenderLoc);
        shadowLoc.x += UIscaling;
        shadowLoc.y -= UIscaling;
        GL11.glBegin(GL11.GL_TRIANGLE_STRIP);
        GL11.glColor4f(0f, 0f, 0f, 1f - engine.getCombatUI().getCommandUIOpacity());
        GL11.glVertex2f(shadowLoc.x, shadowLoc.y);
        GL11.glVertex2f(shadowLoc.x + boxWidth, shadowLoc.y);
        GL11.glVertex2f(shadowLoc.x, shadowLoc.y - boxHeight);
        GL11.glVertex2f(shadowLoc.x + boxWidth, shadowLoc.y - boxHeight);
        GL11.glEnd();

        GL11.glBegin(GL11.GL_TRIANGLE_STRIP);
        GL11.glColor4f(color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, 1f - engine.getCombatUI().getCommandUIOpacity());
        GL11.glVertex2f(boxRenderLoc.x, boxRenderLoc.y);
        GL11.glVertex2f(boxRenderLoc.x + boxWidth, boxRenderLoc.y);
        GL11.glVertex2f(boxRenderLoc.x, boxRenderLoc.y - boxHeight);
        GL11.glVertex2f(boxRenderLoc.x + boxWidth, boxRenderLoc.y - boxHeight);
        GL11.glEnd();

        Vector2f boxEndBarLoc = new Vector2f(boxRenderLoc);
        boxEndBarLoc.x += boxEndWidth;

        GL11.glLineWidth(UIscaling);
        GL11.glBegin(GL11.GL_LINES);
        GL11.glColor4f(0f, 0f, 0f, 1f - engine.getCombatUI().getCommandUIOpacity());
        GL11.glVertex2f(boxEndBarLoc.x + 1, boxEndBarLoc.y - 1);
        GL11.glVertex2f(boxEndBarLoc.x + 1, boxEndBarLoc.y - boxHeight - 1);
        GL11.glEnd();

        GL11.glLineWidth(UIscaling);
        GL11.glBegin(GL11.GL_LINES);
        GL11.glColor4f(color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, 1f - engine.getCombatUI().getCommandUIOpacity());
        GL11.glVertex2f(boxEndBarLoc.x, boxEndBarLoc.y);
        GL11.glVertex2f(boxEndBarLoc.x, boxEndBarLoc.y - boxHeight);
        GL11.glEnd();

        GL11.glDisable(GL11.GL_BLEND);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPopMatrix();
        GL11.glPopAttrib();

        loc.y -= bars * barHeight;
        return loc;
    }

    public static void renderAuxiliaryStatusBar(ShipAPI ship, float indent, float fillStartX, float fillLength, float fillLevel, String text1, String text2, Vector2f inputLoc) {
        CombatEngineAPI engine = Global.getCombatEngine();

        Color color = (ship.isAlive()) ? GREENCOLOR : BLUCOLOR;

        inputLoc.x += indent * UIscaling;

        Vector2f boxLoc = new Vector2f(inputLoc);
        boxLoc.x += fillStartX * UIscaling;

        final float boxHeight = 9f * UIscaling;
        final float boxEndWidth = fillLength * UIscaling;

        float boxWidth = boxEndWidth * fillLevel;

        openGL11ForText();

        TODRAW14.setMaxWidth(6969);

        TODRAW14.setText(text1);
        TODRAW14.setColor(Color.BLACK);
        TODRAW14.draw(inputLoc.x + 1, inputLoc.y - 1);
        TODRAW14.setColor(color);
        TODRAW14.draw(inputLoc);

        Vector2f text2Pos = new Vector2f(boxLoc);
        text2Pos.x += boxEndWidth + (4f * UIscaling);
        TODRAW14.setText(text2);
        TODRAW14.setColor(Color.BLACK);
        TODRAW14.draw(text2Pos.x + 1, text2Pos.y - 1);
        TODRAW14.setColor(color);
        TODRAW14.draw(text2Pos);

        closeGL11ForText();

        final int width = (int) (Display.getWidth() * Display.getPixelScaleFactor());
        final int height = (int) (Display.getHeight() * Display.getPixelScaleFactor());

        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glViewport(0, 0, width, height);
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        GL11.glOrtho(0, width, 0, height, -1, 1);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glTranslatef(0.01f, 0.01f, 0);

        Vector2f nodeLoc = new Vector2f(inputLoc);
        nodeLoc.x -= 2f * UIscaling;
        nodeLoc.y -= 11f * UIscaling;

        GL11.glLineWidth(UIscaling);
        GL11.glBegin(GL11.GL_LINE_STRIP);
        GL11.glColor4f(color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, 1f - engine.getCombatUI().getCommandUIOpacity());
        GL11.glVertex2f(nodeLoc.x, nodeLoc.y);

        nodeLoc.x -= 6f * UIscaling;
        nodeLoc.y += 6f * UIscaling;
        GL11.glVertex2f(nodeLoc.x, nodeLoc.y);

        nodeLoc.y += 4f * UIscaling; //7
        GL11.glVertex2f(nodeLoc.x, nodeLoc.y);

        GL11.glEnd();

        Vector2f boxRenderLoc = new Vector2f(boxLoc);
        boxRenderLoc.y -= 3f * UIscaling;

        //drop shadow
        Vector2f shadowLoc = new Vector2f(boxRenderLoc);
        shadowLoc.x += UIscaling;
        shadowLoc.y -= UIscaling;
        GL11.glBegin(GL11.GL_TRIANGLE_STRIP);
        GL11.glColor4f(0f, 0f, 0f, 1f - engine.getCombatUI().getCommandUIOpacity());
        GL11.glVertex2f(shadowLoc.x, shadowLoc.y);
        GL11.glVertex2f(shadowLoc.x + boxWidth, shadowLoc.y);
        GL11.glVertex2f(shadowLoc.x, shadowLoc.y - boxHeight);
        GL11.glVertex2f(shadowLoc.x + boxWidth, shadowLoc.y - boxHeight);
        GL11.glEnd();

        GL11.glBegin(GL11.GL_TRIANGLE_STRIP);
        GL11.glColor4f(color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, 1f - engine.getCombatUI().getCommandUIOpacity());
        GL11.glVertex2f(boxRenderLoc.x, boxRenderLoc.y);
        GL11.glVertex2f(boxRenderLoc.x + boxWidth, boxRenderLoc.y);
        GL11.glVertex2f(boxRenderLoc.x, boxRenderLoc.y - boxHeight);
        GL11.glVertex2f(boxRenderLoc.x + boxWidth, boxRenderLoc.y - boxHeight);
        GL11.glEnd();

        Vector2f boxEndBarLoc = new Vector2f(boxRenderLoc);
        boxEndBarLoc.x += boxEndWidth;

        GL11.glLineWidth(UIscaling);
        GL11.glBegin(GL11.GL_LINES);
        GL11.glColor4f(0f, 0f, 0f, 1f - engine.getCombatUI().getCommandUIOpacity());
        GL11.glVertex2f(boxEndBarLoc.x + 1, boxEndBarLoc.y - 1);
        GL11.glVertex2f(boxEndBarLoc.x + 1, boxEndBarLoc.y - boxHeight - 1);
        GL11.glEnd();

        GL11.glLineWidth(UIscaling);
        GL11.glBegin(GL11.GL_LINES);
        GL11.glColor4f(color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, 1f - engine.getCombatUI().getCommandUIOpacity());
        GL11.glVertex2f(boxEndBarLoc.x, boxEndBarLoc.y);
        GL11.glVertex2f(boxEndBarLoc.x, boxEndBarLoc.y - boxHeight);
        GL11.glEnd();

        GL11.glDisable(GL11.GL_BLEND);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPopMatrix();
        GL11.glPopAttrib();
    }

    public static Vector2f getSubsystemsRootLocation(ShipAPI ship, int numBars, float barHeight) {
        Vector2f loc = new Vector2f(529f, 74f);
        Vector2f.add(loc, getUIElementOffset(ship, ship.getVariant()), loc);

        float height = numBars * barHeight * UIscaling;

        int numWeapons = getNumWeapons(ship);
        float maxWeaponHeight = numWeapons * (13f * UIscaling) + 30f;
        if (numWeapons == 0) maxWeaponHeight -= 5f * UIscaling;

        final float minOffset = 10f * UIscaling;
        float weaponOffset = maxWeaponHeight + minOffset;

        loc.y = weaponOffset + height;

        loc.x *= UIscaling;

        return loc;
    }

    private static int getNumWeapons(ShipAPI ship) {
        WeaponGroupAPI groups = ship.getSelectedGroupAPI();
        List<WeaponAPI> weapons = (groups == null) ? null : groups.getWeaponsCopy();
        return (weapons == null) ? 0 : weapons.size();
    }

    private static Vector2f getSubsystemTitleLoc(ShipAPI ship) {
        Vector2f loc = new Vector2f(529f, 72f);
        Vector2f.add(loc, getUIElementOffset(ship, ship.getVariant()), loc);
        loc.scale(UIscaling);

        return loc;
    }

    public static void drawSubsystemsTitle(ShipAPI ship, boolean showInfo, Vector2f rootLoc) {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (!ship.equals(engine.getPlayerShip()) || engine.isUIShowingDialog()) return;
        if (engine.getCombatUI() == null || engine.getCombatUI().isShowingCommandUI() || !engine.isUIShowingHUD()) return;

        Color color = (ship.isAlive()) ? GREENCOLOR : BLUCOLOR;

        float barHeight = 13f * UIscaling;
        Vector2f loc = getSubsystemTitleLoc(ship);

        openGL11ForText();

        /*String info = "TOGGLE INFO WITH \"" + ApexSubsystemCombatManager.INFO_TOGGLE_KEY + "\"";

        Vector2f titleTextLoc = new Vector2f(loc);
        Vector2f infoTextLoc = new Vector2f(rootLoc);

        TODRAW14.setText("SUBSYSTEMS");
        titleTextLoc.x -= TODRAW14.getWidth() + (14f * UIscaling);

        TODRAW14.setColor(Color.BLACK);
        TODRAW14.draw(titleTextLoc.x + 1f, titleTextLoc.y - 1f);
        TODRAW14.setColor(color);
        TODRAW14.draw(titleTextLoc);

        if (showInfo) {
            infoTextLoc.y += barHeight * UIscaling;
            infoTextLoc.x -= 4f * UIscaling;

            TODRAW14.setText(info);
            TODRAW14.setColor(Color.BLACK);
            TODRAW14.draw(infoTextLoc.x + 1f, infoTextLoc.y - 1f);
            TODRAW14.setColor(color);
            TODRAW14.draw(infoTextLoc);
        }*/

        closeGL11ForText();

        final int width = (int) (Display.getWidth() * Display.getPixelScaleFactor());
        final int height = (int) (Display.getHeight() * Display.getPixelScaleFactor());

        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glViewport(0, 0, width, height);
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        GL11.glOrtho(0, width, 0, height, -1, 1);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glTranslatef(0.01f, 0.01f, 0);

        GL11.glLineWidth(UIscaling);
        GL11.glBegin(GL11.GL_LINE_STRIP);
        GL11.glColor4f(color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, 1f - engine.getCombatUI().getCommandUIOpacity());

        Vector2f sysBarNode = new Vector2f(loc);

        final float length = 354f * UIscaling; //354
        sysBarNode.x -= length;
        sysBarNode.y += 4f * UIscaling;
        GL11.glVertex2f(sysBarNode.x, sysBarNode.y);

        //sysBarNode.x += length - (16f * UIscaling);
        sysBarNode.x += length - (113f * UIscaling);
        GL11.glVertex2f(sysBarNode.x, sysBarNode.y);

        sysBarNode.x += 20f * UIscaling;
        sysBarNode.y -= 20f * UIscaling;
        GL11.glVertex2f(sysBarNode.x, sysBarNode.y);

        sysBarNode.x += (85f - 6f * UIscaling);
        GL11.glVertex2f(sysBarNode.x, sysBarNode.y);

        boolean isTitleHigh = rootLoc.y > loc.y - 16f;
        sysBarNode.y += (isTitleHigh) ? 6f * UIscaling : -6f * UIscaling;
        sysBarNode.x += 6f * UIscaling;
        GL11.glVertex2f(sysBarNode.x, sysBarNode.y);

        GL11.glEnd();

        GL11.glDisable(GL11.GL_BLEND);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPopMatrix();
        GL11.glPopAttrib();
    }

    ///////////////////////////////////
    //                               //
    //          STATUS BAR           //
    //                               //
    ///////////////////////////////////

    /**
     * Draw a third status bar above the Flux and Hull ones on the User Interface.
     * With a text of the left and the number on the right.
     *
     * @param ship Player ship.
     *
     * @param fill Filling level of the bar. 0 to 1
     *
     * @param innerColor Color of the bar. If null, the vanilla green UI color will be used.
     *
     * @param borderColor Color of the border. If null, the vanilla green UI color will be used.
     *
     * @param secondfill Wider filling like the soft/hard-flux. 0 to 1.
     *
     * @param text The text written to the left, automatically cut if too large.
     *
     * @param rearText The text displayed on the right.
     */
    public static void drawSecondUnlimitedInterfaceStatusBar(ShipAPI ship, float fill, Color innerColor, Color borderColor, float secondfill, String text, String rearText) {
        if (ship != Global.getCombatEngine().getPlayerShip()) {
            return;
        }

        if (Global.getCombatEngine().getCombatUI()==null || Global.getCombatEngine().getCombatUI().isShowingCommandUI() || !Global.getCombatEngine().isUIShowingHUD()) {
            return;
        }

        addSecondUnlimitedInterfaceStatusBar(ship, fill, innerColor, borderColor, secondfill);
        if (TODRAW14 != null) {
            addInterfaceStatusText(ship, text);
            addInterfaceStatusNumber(ship, rearText);
        }
    }

    ///// UTILS /////

    /**
     * Get the UI Element Offset for the Third bar. (Depends of the group
     * layout, or if the player has some wing)
     *
     * @param ship The player ship.
     * @param variant The variant of the ship.
     * @return The offset who depends of weapon and wing.
     */
    private static Vector2f getInterfaceOffsetFromStatusBars(ShipAPI ship, ShipVariantAPI variant) {
        return getUIElementOffset(ship, variant);
    }

    /**
     * Get the UI Element Offset.
     * (Depends on the weapon groups and wings present)
     *
     * @param ship The player ship.
     * @param variant The variant of the ship.
     * @return the offset.
     */
    private static Vector2f getUIElementOffset(ShipAPI ship, ShipVariantAPI variant) {
        int numEntries = 0;
        final List<WeaponGroupSpec> weaponGroups = variant.getWeaponGroups();
        final List<WeaponAPI> usableWeapons = ship.getUsableWeapons();
        for (WeaponGroupSpec group : weaponGroups) {
            final Set<String> uniqueWeapons = new HashSet<>(group.getSlots().size());
            for (String slot : group.getSlots()) {
                boolean isUsable = false;
                for (WeaponAPI weapon : usableWeapons) {
                    if (weapon.getSlot().getId().contentEquals(slot)) {
                        isUsable = true;
                        break;
                    }
                }
                if (!isUsable) {
                    continue;
                }
                String id = Global.getSettings().getWeaponSpec(variant.getWeaponId(slot)).getWeaponName();
                if (id != null) {
                    uniqueWeapons.add(id);
                }
            }
            numEntries += uniqueWeapons.size();
        }
        if (variant.getFittedWings().isEmpty()) {
            if (numEntries < 2) {
                return ApexCombatUI.PERCENTBARVEC1;
            }
            return new Vector2f(30f + ((numEntries - 2) * 13f), 18f + ((numEntries - 2) * 26f));
        } else {
            if (numEntries < 2) {
                return ApexCombatUI.PERCENTBARVEC2;
            }
            return new Vector2f(59f + ((numEntries - 2) * 13f), 76f + ((numEntries - 2) * 26f));
        }
    }


    /**
     * Draws a small UI bar above the flux bar. The HUD color change to blue
     * when the ship is not alive. Bug: When you left the battle, the hud
     * keep for qew second, no solution found. Bug: Also for other
     * normal drawBox, when paused, they switch brutally of "color".
     *
     * @param ship Ship concerned (the element will only be drawn if that ship
     * is the player ship)
     * @param fill Filling level
     * @param innerColor Color of the bar. If null, use the vanilla HUD color.
     * @param borderColor Color of the border. If null, use the vanilla HUD
     * color.
     * @param secondfill Like the hardflux of the fluxbar. 0 per default.
     */
    private static void addSecondUnlimitedInterfaceStatusBar(ShipAPI ship, float fill, Color innerColor, Color borderColor, float secondfill) {
        float boxWidth = 79;
        float boxHeight = 7;
        if (UIscaling > 1f) {
            boxWidth *= UIscaling;
            boxHeight *= UIscaling;
        }
        final Vector2f element = getInterfaceOffsetFromStatusBars(ship, ship.getVariant());
        final Vector2f boxLoc = Vector2f.add(new Vector2f(224f, 120f), element, null);
        final Vector2f shadowLoc = Vector2f.add(new Vector2f(225f, 119f), element, null);
        if (UIscaling > 1f) {
            boxLoc.scale(UIscaling);
            shadowLoc.scale(UIscaling);
        }

        // Used to properly interpolate between colors
        float alpha = 1;
        if (Global.getCombatEngine().isUIShowingDialog()) {
            alpha = 0.28f;
        }

        Color innerCol = innerColor == null ? GREENCOLOR : innerColor;
        Color borderCol = borderColor == null ? GREENCOLOR : borderColor;
        if (!ship.isAlive()) {
            innerCol = BLUCOLOR;
            borderCol = BLUCOLOR;
        }
        float hardfill = secondfill < 0 ? 0 : secondfill;
        hardfill = hardfill > 1 ? 1 : hardfill;
        int pixelHardfill = (int) (boxWidth * hardfill);
        pixelHardfill = pixelHardfill <= 3 ? -pixelHardfill : -3;

        int hfboxWidth = (int) (boxWidth * hardfill);
        int fboxWidth = (int) (boxWidth * fill);

        OpenGLBar(ship, alpha, borderCol, innerCol, fboxWidth, hfboxWidth, boxHeight, boxWidth, pixelHardfill, shadowLoc, boxLoc);
    }

    /**
     * Draw the text with the font victor14.
     * @param ship The player ship
     * @param text The text to write.
     */
    private static void addInterfaceStatusText(ShipAPI ship, String text) {
        if (ship != Global.getCombatEngine().getPlayerShip()) {
            return;
        }
        if (Global.getCombatEngine().getCombatUI().isShowingCommandUI() || !Global.getCombatEngine().isUIShowingHUD()) {
            return;
        }
        Color borderCol = GREENCOLOR;
        if (!ship.isAlive()) {
            borderCol = BLUCOLOR;
        }
        float alpha = 1;
        if (Global.getCombatEngine().isUIShowingDialog()) {
            alpha = 0.28f;
        }
        Color shadowcolor = new Color(Color.BLACK.getRed() / 255f, Color.BLACK.getGreen() / 255f, Color.BLACK.getBlue() / 255f,
                1f - Global.getCombatEngine().getCombatUI().getCommandUIOpacity());
        Color color = new Color(borderCol.getRed() / 255f, borderCol.getGreen() / 255f, borderCol.getBlue() / 255f,
                alpha * (borderCol.getAlpha() / 255f)
                        * (1f - Global.getCombatEngine().getCombatUI().getCommandUIOpacity()));

        final Vector2f boxLoc = Vector2f.add(new Vector2f(176f, 131f),
                getInterfaceOffsetFromStatusBars(ship, ship.getVariant()), null);
        final Vector2f shadowLoc = Vector2f.add(new Vector2f(177f, 130f),
                getInterfaceOffsetFromStatusBars(ship, ship.getVariant()), null);

        openGL11ForText();
        if (UIscaling > 1f) {
            TODRAW14.setFontSize(14*UIscaling);

            boxLoc.scale(UIscaling);
            shadowLoc.scale(UIscaling);
        }
        TODRAW14.setText(text);
        TODRAW14.setMaxWidth(46*UIscaling);
        TODRAW14.setMaxHeight(14*UIscaling);
        TODRAW14.setColor(shadowcolor);
        TODRAW14.draw(shadowLoc);
        TODRAW14.setColor(color);
        TODRAW14.draw(boxLoc);
        closeGL11ForText();

    }

    /**
     * Draw number at the right of the percent bar.
     * @param ship The player ship died or alive.
     * @param rearText The number NOT displayed, Not bounded per the method to 0 at 999
     * 999.
     */
    private static void addInterfaceStatusNumber(ShipAPI ship, String rearText) {
        if (ship != Global.getCombatEngine().getPlayerShip()) {
            return;
        }
        if (Global.getCombatEngine().getCombatUI().isShowingCommandUI() || !Global.getCombatEngine().isUIShowingHUD()) {
            return;
        }

        Color borderCol = GREENCOLOR;
        if (!ship.isAlive()) {
            borderCol = BLUCOLOR;
        }
        float alpha = 1;
        if (Global.getCombatEngine().isUIShowingDialog()) {
            alpha = 0.28f;
        }
        Color shadowcolor = new Color(Color.BLACK.getRed() / 255f, Color.BLACK.getGreen() / 255f, Color.BLACK.getBlue() / 255f,
                1f - Global.getCombatEngine().getCombatUI().getCommandUIOpacity());
        Color color = new Color(borderCol.getRed() / 255f, borderCol.getGreen() / 255f, borderCol.getBlue() / 255f,
                alpha * (borderCol.getAlpha() / 255f)
                        * (1f - Global.getCombatEngine().getCombatUI().getCommandUIOpacity()));

        final Vector2f boxLoc = Vector2f.add(new Vector2f(355f, 131f),
                getInterfaceOffsetFromStatusBars(ship, ship.getVariant()), null);
        final Vector2f shadowLoc = Vector2f.add(new Vector2f(356f, 130f),
                getInterfaceOffsetFromStatusBars(ship, ship.getVariant()), null);
        if (UIscaling > 1f) {
            boxLoc.scale(UIscaling);
            shadowLoc.scale(UIscaling);
        }

        openGL11ForText();
        TODRAW14.setText(rearText);
        float width = TODRAW14.getWidth() - 1;
        TODRAW14.setColor(shadowcolor);
        TODRAW14.draw(shadowLoc.x - width, shadowLoc.y);
        TODRAW14.setColor(color);
        TODRAW14.draw(boxLoc.x - width, boxLoc.y);
        closeGL11ForText();
    }

    private static void OpenGLBar(ShipAPI ship, float alpha, Color borderCol, Color innerCol, int fboxWidth, int hfboxWidth, float boxHeight, float boxWidth, int pixelHardfill, Vector2f shadowLoc, Vector2f boxLoc) {
        final int width = (int) (Display.getWidth() * Display.getPixelScaleFactor());
        final int height = (int) (Display.getHeight() * Display.getPixelScaleFactor());

        // Set OpenGL flags
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glViewport(0, 0, width, height);
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        GL11.glOrtho(0, width, 0, height, -1, 1);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glTranslatef(0.01f, 0.01f, 0);

        if (ship.isAlive()) {
            // Render the drop shadow
            if (fboxWidth != 0) {
                GL11.glBegin(GL11.GL_TRIANGLE_STRIP);
                GL11.glColor4f(Color.BLACK.getRed() / 255f, Color.BLACK.getGreen() / 255f, Color.BLACK.getBlue() / 255f,
                        1f - Global.getCombatEngine().getCombatUI().getCommandUIOpacity());
                GL11.glVertex2f(shadowLoc.x - 1, shadowLoc.y);
                GL11.glVertex2f(shadowLoc.x + fboxWidth, shadowLoc.y);
                GL11.glVertex2f(shadowLoc.x - 1, shadowLoc.y + boxHeight + 1);
                GL11.glVertex2f(shadowLoc.x + fboxWidth, shadowLoc.y + boxHeight + 1);
                GL11.glEnd();
            }
        }

        // Render the drop shadow of border.
        GL11.glBegin(GL11.GL_LINES);
        GL11.glColor4f(Color.BLACK.getRed() / 255f, Color.BLACK.getGreen() / 255f, Color.BLACK.getBlue() / 255f,
                1f - Global.getCombatEngine().getCombatUI().getCommandUIOpacity());
        GL11.glVertex2f(shadowLoc.x + hfboxWidth + pixelHardfill, shadowLoc.y - 1);
        GL11.glVertex2f(shadowLoc.x + 3 + hfboxWidth + pixelHardfill, shadowLoc.y - 1);
        GL11.glVertex2f(shadowLoc.x + hfboxWidth + pixelHardfill, shadowLoc.y + boxHeight);
        GL11.glVertex2f(shadowLoc.x + 3 + hfboxWidth + pixelHardfill, shadowLoc.y + boxHeight);
        GL11.glVertex2f(shadowLoc.x + boxWidth, shadowLoc.y);
        GL11.glVertex2f(shadowLoc.x + boxWidth, shadowLoc.y + boxHeight);

        // Render the border transparency fix
        GL11.glColor4f(Color.BLACK.getRed() / 255f, Color.BLACK.getGreen() / 255f, Color.BLACK.getBlue() / 255f,
                1f - Global.getCombatEngine().getCombatUI().getCommandUIOpacity());
        GL11.glVertex2f(boxLoc.x + hfboxWidth + pixelHardfill, boxLoc.y - 1);
        GL11.glVertex2f(boxLoc.x + 3 + hfboxWidth + pixelHardfill, boxLoc.y - 1);
        GL11.glVertex2f(boxLoc.x + hfboxWidth + pixelHardfill, boxLoc.y + boxHeight);
        GL11.glVertex2f(boxLoc.x + 3 + hfboxWidth + pixelHardfill, boxLoc.y + boxHeight);
        GL11.glVertex2f(boxLoc.x + boxWidth, boxLoc.y);
        GL11.glVertex2f(boxLoc.x + boxWidth, boxLoc.y + boxHeight);

        // Render the border
        GL11.glColor4f(borderCol.getRed() / 255f, borderCol.getGreen() / 255f, borderCol.getBlue() / 255f,
                alpha * (1 - Global.getCombatEngine().getCombatUI().getCommandUIOpacity()));
        GL11.glVertex2f(boxLoc.x + hfboxWidth + pixelHardfill, boxLoc.y - 1);
        GL11.glVertex2f(boxLoc.x + 3 + hfboxWidth + pixelHardfill, boxLoc.y - 1);
        GL11.glVertex2f(boxLoc.x + hfboxWidth + pixelHardfill, boxLoc.y + boxHeight);
        GL11.glVertex2f(boxLoc.x + 3 + hfboxWidth + pixelHardfill, boxLoc.y + boxHeight);
        GL11.glVertex2f(boxLoc.x + boxWidth, boxLoc.y);
        GL11.glVertex2f(boxLoc.x + boxWidth, boxLoc.y + boxHeight);
        GL11.glEnd();

        // Render the fill element
        if (ship.isAlive()) {
            GL11.glBegin(GL11.GL_TRIANGLE_STRIP);
            GL11.glColor4f(innerCol.getRed() / 255f, innerCol.getGreen() / 255f, innerCol.getBlue() / 255f,
                    alpha * (innerCol.getAlpha() / 255f)
                            * (1f - Global.getCombatEngine().getCombatUI().getCommandUIOpacity()));
            GL11.glVertex2f(boxLoc.x, boxLoc.y);
            GL11.glVertex2f(boxLoc.x + fboxWidth, boxLoc.y);
            GL11.glVertex2f(boxLoc.x, boxLoc.y + boxHeight);
            GL11.glVertex2f(boxLoc.x + fboxWidth, boxLoc.y + boxHeight);
            GL11.glEnd();
        }
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPopMatrix();
        GL11.glPopAttrib();

    }

    /**
     * GL11 to start, when you want render text of Lazyfont.
     */
    private static void openGL11ForText() {
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT);
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glViewport(0, 0, Display.getWidth(), Display.getHeight());
        GL11.glOrtho(0.0, Display.getWidth(), 0.0, Display.getHeight(), -1.0, 1.0);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
    }

    /**
     * GL11 to close, when you want render text of Lazyfont.
     */
    private static void closeGL11ForText() {
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glPopMatrix();
        GL11.glPopAttrib();
    }
}