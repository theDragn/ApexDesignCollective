// Borrowed from Tomatopaste's Dronelib

package apexsubs;

public interface ApexBaseSubsystemAI {
    /**
     * Returns the desired subsystem AI type.
     * @return
     */
    ApexBaseSubsystemAI getAI();

    /**
     * Initialise the AI script. Will be called regardless of ship being in player control initially.
     */
    void aiInit();

    /**
     * Used to update any system AI decisions. Called after subsystem logic is advanced. Will not be called under player
     * control.
     * @param amount frame delta
     */
    void aiUpdate(float amount);
}
