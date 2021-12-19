package apexsubs;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.*;

import static plugins.ApexModPlugin.HAS_DRONELIB;
import static plugins.ApexModPlugin.SETTINGS_FILE;

/**
 * @author tomatopaste
 * used to load various spec from .json and .csv
 */
public final class ApexSpecLoadingUtils {
    private static HashMap<String, ApexBaseSubsystem.SubsystemData> subsystemData;

    private static final List<String> subsystemHotkeyPriority = new ArrayList<>();

    public static void loadSubsystemData() throws JSONException, IOException {
        SettingsAPI settings = Global.getSettings();
        JSONArray subsystems = settings.loadCSV("data/subsystems/apexsubsystems.csv");

        subsystemData = new HashMap<>();

        for (int i = 0; i < subsystems.length(); i++) {
            JSONObject row = subsystems.getJSONObject(i);
            String id = row.getString("id");

            float inTime = catchJsonFloatDefaultZero(row, "inTime");
            float activeTime = catchJsonFloatDefaultZero(row, "activeTime");
            float outTime = catchJsonFloatDefaultZero(row, "outTime");
            float cooldownTime = catchJsonFloatDefaultZero(row, "cooldownTime");

            boolean isToggle = catchJsonBooleanDefaultFalse(row, "isToggle");
            if (isToggle) activeTime = Float.MAX_VALUE;

            float fluxPerSecondPercentMaxCapacity = catchJsonFloatDefaultZero(row, "fluxPerSecondPercentMaxCapacity");
            float fluxPerSecondFlat = catchJsonFloatDefaultZero(row, "fluxPerSecondFlat");

            String hotkey = row.getString("hotkey");
            if (hotkey.isEmpty()) hotkey = null;

            ApexBaseSubsystem.SubsystemData data = new ApexBaseSubsystem.SubsystemData(
                    hotkey,
                    id,
                    row.getString("name"),
                    inTime,
                    activeTime,
                    outTime,
                    cooldownTime,
                    isToggle,
                    fluxPerSecondPercentMaxCapacity,
                    fluxPerSecondFlat
            );

            subsystemData.put(id, data);
        }
        if (HAS_DRONELIB)
        {
            subsystemHotkeyPriority.add(Global.getSettings().getString("dl_DefaultKeybind1"));
            subsystemHotkeyPriority.add(Global.getSettings().getString("dl_DefaultKeybind2"));
            subsystemHotkeyPriority.add(Global.getSettings().getString("dl_DefaultKeybind3"));
            subsystemHotkeyPriority.add(Global.getSettings().getString("dl_DefaultKeybind4"));
            subsystemHotkeyPriority.add(Global.getSettings().getString("dl_DefaultKeybind5"));
            subsystemHotkeyPriority.add(Global.getSettings().getString("dl_DefaultKeybind6"));
        } else
        {
            subsystemHotkeyPriority.add(Global.getSettings().loadJSON(SETTINGS_FILE).getString("subsystemKeybind1"));
            subsystemHotkeyPriority.add(Global.getSettings().loadJSON(SETTINGS_FILE).getString("subsystemKeybind2"));
            subsystemHotkeyPriority.add(Global.getSettings().loadJSON(SETTINGS_FILE).getString("subsystemKeybind3"));
        }
    }

    public static List<String> getSubsystemHotkeyPriority() {
        return subsystemHotkeyPriority;
    }

    public static ApexBaseSubsystem.SubsystemData getSubsystemData(String id) {
        return subsystemData.get(id);
    }

    public static float catchJsonFloatDefaultZero(JSONObject row, String id) {
        float value;
        try {
            value = (float) row.getDouble(id);
        } catch (JSONException e) {
            value = 0f;
        }
        return value;
    }

    public static boolean catchJsonBooleanDefaultFalse(JSONObject row, String id) {
        boolean value;
        try {
            value = row.getBoolean(id);
        } catch (JSONException e) {
            value = false;
        }
        return value;
    }

}