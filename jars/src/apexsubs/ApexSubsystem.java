package apexsubs;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;

public interface ApexSubsystem {
    void init(ShipAPI ship);

    void apply(MutableShipStatsAPI stats, String id, ApexBaseSubsystem.SubsystemState state, float effectLevel);

    void unapply(MutableShipStatsAPI stats, String id);

    void advance(float amount);

    /**
     * Is the subsystem on?!??!??!?!?
     * @return true if subsystem is fading in, fully active or fading out
     */
    boolean isOn();

    /**
     * Is the subsystem active?
     * @return Whether the subsystem is in the middle state of being fully active
     */
    boolean isActive();

    /**
     * Is the subsystem fading out?
     * @return Whether subsystem is deactivating
     */
    boolean isFadingOut();

    /**
     * yeah
     * @return Whether the subsystem is ramping up effect while activating
     */
    boolean isFadingIn();

    /**
     * is it on cooldown who knows
     * @return duh
     */
    boolean isCooldown();

    /**
     * Is it off??
     * @return is subsystem inactive
     */
    boolean isOff();

    /**
     * Will replace default status text (READY/ACTIVE/--) with a custom string.
     * @return Custom status string
     */
    String getStatusString();

    /**
     * Text rendered next to gui status bar.
     * @return text to render
     */
    String getInfoString();

    /**
     * A brief string containing a description of what the subsystem does
     * @return text to render
     */
    String getFlavourString();

    /**
     * The number of lines the systems uses of subsystem gui. Ignore info line.
     * @return number of bars used
     */
    int getNumGuiBars();

    /**
     * Whether the subsystem can be used while ship is venting.
     * @return duh
     */
    boolean canUseWhileVenting();

    /**
     * Whether the subsystem can be used while ship is venting.
     * @return duh
     */
    boolean canUseWhileOverloaded();

    //todo
    //String getStatusText(int index);

    //String getStatusIconFilepath(int index);
}