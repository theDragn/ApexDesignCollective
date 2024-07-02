package plugins

import lunalib.lunaSettings.LunaSettings
import lunalib.lunaSettings.LunaSettingsListener
import plugins.ApexModPlugin.Companion.EUROBEAT_MODE
import plugins.ApexModPlugin.Companion.EXCESSION_ID
import plugins.ApexModPlugin.Companion.GENERATE_RELICS
import plugins.ApexModPlugin.Companion.GENERATE_SYSTEMS
import plugins.ApexModPlugin.Companion.POTATO_MODE
import plugins.ApexModPlugin.Companion.REPAIR_FLOATY

class ApexSettings: LunaSettingsListener
{
    override fun settingsChanged(modID: String)
    {
        POTATO_MODE = LunaSettings.getBoolean("apex_design", "apex_potatomode") ?: false
        GENERATE_RELICS = LunaSettings.getBoolean("apex_design", "apex_relics") ?: true
        REPAIR_FLOATY = LunaSettings.getBoolean("apex_design", "apex_repair_floaty") ?: false
        EUROBEAT_MODE = LunaSettings.getBoolean("apex_design", "apex_eurobeat") ?: false
        EXCESSION_ID = LunaSettings.getBoolean("apex_design", "apex_excession_id") ?: true
        //GENERATE_SYSTEMS = LunaSettings.getBoolean("apex_design", "apex_systems") ?: true
    }
}