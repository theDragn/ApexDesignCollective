// Borrowed from Tomatopaste's Dronelib, with minor modification

package apexsubs;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import org.lwjgl.input.Keyboard;
import org.lwjgl.util.vector.Vector2f;

public abstract class ApexBaseSubsystem implements ApexSubsystem, ApexBaseSubsystemAI {
    protected String subsystemId;
    private final SubsystemData data;

    public enum SubsystemState {
        OFF,
        IN,
        ACTIVE,
        OUT,
        COOLDOWN
    }

    protected SubsystemState state;
    protected ShipAPI ship;
    protected int index;

    protected String activeHotkey;
    protected String defaultHotkey;

    public ApexBaseSubsystem(String systemId) {
        this.data = ApexSpecLoadingUtils.getSubsystemData(systemId);
        if (data == null) throw new NullPointerException("Subsystem data is null: " + systemId);
    }
    public ApexBaseSubsystem(SubsystemData data) {
        this.data = data;
        if (data == null) throw new NullPointerException("Subsystem data is null");
    }
    public ApexBaseSubsystem() {
        String id = getSubsystemId();
        this.data = ApexSpecLoadingUtils.getSubsystemData(id);
    }

    @Override
    public void init(ShipAPI ship) {
        this.ship = ship;
        subsystemId = "apex_subsystem_" + this.hashCode() + "_" + ship.hashCode();

        state = SubsystemState.OFF;

        if (data.hotkey != null) activeHotkey = data.hotkey;

        aiInit();
    }

    @Override
    public abstract void apply(MutableShipStatsAPI stats, String id, SubsystemState state, float effectLevel);

    @Override
    public abstract void unapply(MutableShipStatsAPI stats, String id);

    @Override
    public ApexBaseSubsystemAI getAI() {
        return this;
    }

    @Override
    public abstract void aiInit();

    @Override
    public abstract void aiUpdate(float amount);

    private boolean isHotkeyDownLastUpdate = false;
    private float active = 0f;
    private float effectLevel = 0f;
    private float guiLevel = 0f;

    /**
     * Checks if activation is legal then will start subsystem cycle
     */
    public void activate() {
        if (ship.getFluxTracker().isOverloaded() && !canUseWhileOverloaded()) return;
        if (ship.getFluxTracker().isVenting() && !canUseWhileVenting()) return;
        if (!isToggle() && active > 0) return; // won't do anything if it's mid-stage or on cooldown or whatever
        if (isOff() && !isCooldown()) {
            state = SubsystemState.IN;
        }
        if (isToggle() && isActive()) {
            state = SubsystemState.OUT;
            active = 0f;
        }

        onActivation();
    }

    @Override
    public void advance(float amount) {
        if (ship == null || !ship.isAlive()) return;

        if (getActiveHotkey() == null) {
            activeHotkey = defaultHotkey;
        }
        boolean isHotkeyDown = Keyboard.isKeyDown(Keyboard.getKeyIndex(getActiveHotkey()));
        if (isHotkeyDown && !isHotkeyDownLastUpdate && ship.equals(Global.getCombatEngine().getPlayerShip())) {
            activate();
        }

        switch (state) {
            case OFF:
                guiLevel = 0f;
                effectLevel = 0f;
                active = 0f;
                break;
            case IN:
                active += amount;

                if (isToggle()) {
                    guiLevel = active / getInTime();
                } else {
                    guiLevel = active / (getInTime() + getActiveTime());
                }

                effectLevel = active / getInTime();

                if (active >= getInTime()) {
                    state = SubsystemState.ACTIVE;
                    active = 0f;
                }
                break;
            case ACTIVE:
                active += amount;

                if (isToggle()) {
                    guiLevel = 1f;
                } else {
                    guiLevel = (getInTime() + active) / (getInTime() + getActiveTime());
                }

                effectLevel = 1f;

                if (active >= getActiveTime()) {
                    state = SubsystemState.OUT;
                    active = 0f;
                }
                break;
            case OUT:
                active += amount;

                guiLevel = 1f - (active / (getCooldownTime() + getOutTime()));

                effectLevel = 1f - (active / getOutTime());

                if (active >= getOutTime()) {
                    state = SubsystemState.COOLDOWN;
                    active = 0f;
                }
                break;
            case COOLDOWN:
                active += amount;

                guiLevel = 1f - ((active + getOutTime()) / (getCooldownTime() + getOutTime()));

                effectLevel = 0f;

                if (active >= getCooldownTime()) {
                    state = SubsystemState.OFF;
                    active = 0f;
                }
                break;
        }

        if (isOn()) {
            apply(ship.getMutableStats(), subsystemId, state, effectLevel);

            ship.getFluxTracker().increaseFlux(amount * getFluxPerSecondFlat(), false);
            ship.getFluxTracker().increaseFlux(amount * getFluxPerSecondMaxCapacity() * 0.01f * ship.getMaxFlux(), false);
        } else {
            unapply(ship.getMutableStats(), subsystemId);
        }

        //CombatEngineAPI engine = Global.getCombatEngine();
        //if (engine.getPlayerShip().equals(ship)) engine.maintainStatusForPlayerShip(sysId, null, getName(), state.name(), false);

        isHotkeyDownLastUpdate = isHotkeyDown;
    }

    /**
     * Set the default hotkey string that will be used as a fallback if activeHotkey is not defined
     * @param defaultHotkey Hotkey string
     */
    public void setDefaultHotkey(String defaultHotkey) {
        this.defaultHotkey = defaultHotkey;
    }

    /**
     * Returns the currently active hotkey
     * @return Hotkey string
     */
    public String getActiveHotkey() {
        return activeHotkey;
    }

    public Vector2f guiRender(Vector2f inputLoc, Vector2f rootLoc) {
        String info = getInfoString();
        String flavour = getFlavourString();
        if (info == null || info.isEmpty()) info = "COOL INFO PLACEHOLDER";
        if (flavour == null || flavour.isEmpty()) flavour = "COOL FLAVOUR PLACEHOLDER";

        String stateText = "DEV_INIT";
        if (isToggle()) {
            switch (state) {
                case ACTIVE:
                case OFF:
                    stateText = "READY";
                    break;
                case COOLDOWN:
                case OUT:
                case IN:
                    stateText = "--";
            }
        } else {
            switch (state) {
                case OFF:
                    stateText = "READY";
                    break;
                case COOLDOWN:
                    stateText = "--";
                    break;
                case OUT:
                case ACTIVE:
                case IN:
                    stateText = "ACTIVE";
            }
        }

        String customStatus = getStatusString();
        if (customStatus != null) stateText = customStatus;

        return ApexCombatUI.drawSubsystemStatus(
                ship,
                guiLevel,
                getName(),
                info,
                stateText,
                getActiveHotkey(),
                flavour,
                ApexSubsystemCombatManager.showInfoText,
                getNumGuiBars(),
                inputLoc,
                rootLoc
        );
    }

    /**
     * Called when a subsystem successfully activates. Is also called when a toggle subsystem is turned off.
     */
    public void onActivation() {

    }

    public String getSubsystemId() {
        if (data == null || data.getId() == null) throw new NullPointerException("Subsystem data has not been defined. You may need to override the getSubsystemId() method.");
        return data.getId();
    }

    @Override
    public boolean isOff() {
        return state == SubsystemState.OFF || state == SubsystemState.COOLDOWN;
    }

    @Override
    public boolean isOn() {
        return !isOff();
    }

    @Override
    public boolean isFadingIn() {
        return state == SubsystemState.IN;
    }

    @Override
    public boolean isActive() {
        return state == SubsystemState.ACTIVE;
    }

    @Override
    public boolean isFadingOut() {
        return state == SubsystemState.OUT;
    }

    @Override
    public boolean isCooldown() {
        return state == SubsystemState.COOLDOWN;
    }

    @Override
    public boolean canUseWhileOverloaded() {
        return false;
    }

    @Override
    public boolean canUseWhileVenting() {
        return false;
    }

    public static class SubsystemData {
        private final String id;
        private final String name;

        private final String hotkey;

        private float inTime;
        private float activeTime;
        private float outTime;
        private float cooldownTime;

        private final boolean isToggle;

        private float fluxPerSecondMaxCapacity;
        private float fluxPerSecondFlat;

        public SubsystemData(
                String hotkey,
                String systemID,
                String name,
                float inTime,
                float activeTime,
                float outTime,
                float cooldownTime,
                boolean isToggle,
                float fluxPerSecondMaxCapacity,
                float fluxPerSecondFlat
        ) {
            this.hotkey = hotkey;
            this.id = systemID;
            this.name = name;
            this.inTime = inTime;
            this.activeTime = activeTime;
            this.outTime = outTime;
            this.cooldownTime = cooldownTime;
            this.isToggle = isToggle;
            this.fluxPerSecondMaxCapacity = fluxPerSecondMaxCapacity;
            this.fluxPerSecondFlat = fluxPerSecondFlat;
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getHotkey() {
            return hotkey;
        }

        public float getInTime() {
            return inTime;
        }

        public float getActiveTime() {
            return activeTime;
        }

        public float getOutTime() {
            return outTime;
        }

        public float getCooldownTime() {
            return cooldownTime;
        }

        public boolean isToggle() {
            return isToggle;
        }

        public float getFluxPerSecondMaxCapacity() {
            return fluxPerSecondMaxCapacity;
        }

        public float getFluxPerSecondFlat() {
            return fluxPerSecondFlat;
        }

        public void setActiveTime(float activeTime) {
            this.activeTime = activeTime;
        }

        public void setCooldownTime(float cooldownTime) {
            this.cooldownTime = cooldownTime;
        }

        public void setFluxPerSecondFlat(float fluxPerSecondFlat) {
            this.fluxPerSecondFlat = fluxPerSecondFlat;
        }

        public void setFluxPerSecondMaxCapacity(float fluxPerSecondMaxCapacity) {
            this.fluxPerSecondMaxCapacity = fluxPerSecondMaxCapacity;
        }

        public void setInTime(float inTime) {
            this.inTime = inTime;
        }

        public void setOutTime(float outTime) {
            this.outTime = outTime;
        }
    }

    public void setActiveTime(float activeTime) {
        data.setActiveTime(activeTime);
    }

    public void setInTime(float inTime) {
        data.setInTime(inTime);
    }

    public void setOutTime(float outTime) {
        data.setOutTime(outTime);
    }

    public void setCooldownTime(float cooldownTime) {
        data.setCooldownTime(cooldownTime);
    }

    /**
     * Returns hotkey string as defined in csv, will return null if none was defined
     * @return Hotkey provided in csv entry
     */
    public String getHotkey() {
        return data.getHotkey();
    }

    public String getName() {
        return data.getName();
    }

    public float getInTime() {
        return data.getInTime();
    }

    public float getActiveTime() {
        return data.getActiveTime();
    }

    public float getOutTime() {
        return data.getOutTime();
    }

    public float getCooldownTime() {
        return data.getCooldownTime();
    }

    public boolean isToggle() {
        return data.isToggle();
    }

    public float getFluxPerSecondMaxCapacity() {
        return data.getFluxPerSecondMaxCapacity();
    }

    public float getFluxPerSecondFlat() {
        return data.getFluxPerSecondFlat();
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public int getIndex() {
        return index;
    }
}