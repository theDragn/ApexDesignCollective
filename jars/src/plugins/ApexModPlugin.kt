package plugins

import com.fs.starfarer.api.BaseModPlugin
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.PluginPick
import com.fs.starfarer.api.campaign.CampaignPlugin
import com.fs.starfarer.api.combat.MissileAIPlugin
import com.fs.starfarer.api.combat.MissileAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.impl.campaign.shared.SharedData
import data.campaign.missions.ApexExcessionAdder
import data.weapons.proj.ai.*
import exerelin.utilities.NexConfig
import exerelin.utilities.NexFactionConfig.StartFleetType
import lunalib.lunaSettings.LunaSettings
import org.json.JSONException
import world.ApexRelicPlacer
import world.ApexSectorGenerator
import java.io.IOException


class ApexModPlugin : BaseModPlugin() {
    override fun onApplicationLoad() {
        try {
            loadApexSettings()
        } catch (e: Exception) {
            System.out.println(e);
            throw RuntimeException(
                    "Apex Design Collective encountered a \"bruh moment\".\nAnd by that I mean there was an issue with the settings file."
            )
        }

        if (Global.getSettings().modManager.isModEnabled("nexerelin"))
        {
            if (Global.getSettings().getMissionScore("apex_4blackops") > 0) {
                val faction = NexConfig.getFactionConfig("apex_design")
                val fleetSet = faction.getStartFleetSet(StartFleetType.SUPER.name)
                val grandPhaseFleet: MutableList<String> = ArrayList(1)
                grandPhaseFleet.add("apex_anaconda_strike")
                fleetSet.addFleet(grandPhaseFleet)
            }
            if (Global.getSettings().getMissionScore("apex_4blackops") == 100) {
                val faction = NexConfig.getFactionConfig("apex_design")
                val fleetSet = faction.getStartFleetSet(StartFleetType.SUPER.name)
                val excFleet: MutableList<String> = ArrayList(1)
                excFleet.add("apex_excession_prototype")
                fleetSet.addFleet(excFleet)
            }
        }

    }


    override fun onNewGame() {
        ApexSectorGenerator().generate(Global.getSector())
    }

    override fun onNewGameAfterProcGen() {
        if (GENERATE_RELICS)
            ApexRelicPlacer().generate(Global.getSector())
    }

    override fun onGameLoad(newGame: Boolean) {
        /*for (system in Global.getSector().getStarSystems()) {
            if (system.baseName.equals("Vela")) {
                hasApex = true
            }
        }*/
        val hasApex = SharedData.getData().personBountyEventData.participatingFactions.contains("apex_design")
        if (!hasApex) {
            ApexSectorGenerator().generate(Global.getSector())
            ApexSectorGenerator.createInitialPeople()
            if (GENERATE_RELICS && Global.getSector().memoryWithoutUpdate.contains("\$apex_placed_relics"))
                ApexRelicPlacer().generate(Global.getSector())
        }
        Global.getSector().addTransientListener(ApexCargoListener(false))
        Global.getSector().addTransientListener(ApexExcessionAdder())
    }

    override fun pickMissileAI(missile: MissileAPI, launchingShip: ShipAPI): PluginPick<MissileAIPlugin>? {
        return when (missile.projectileSpecId) {
            "apex_nanoacid_torp_guided" -> PluginPick(ApexGuidedTorpAI(missile, launchingShip), CampaignPlugin.PickPriority.MOD_SPECIFIC)
            "apex_arcspike" -> PluginPick(ApexArcspikeAI(missile, launchingShip), CampaignPlugin.PickPriority.MOD_SPECIFIC)
            "apex_arcspike_canister" -> PluginPick(ApexArcspikeCanisterAI2(missile, launchingShip), CampaignPlugin.PickPriority.MOD_SPECIFIC)
            "apex_arcspike_canister_fighter" -> PluginPick(ApexArcspikeCanisterAI2(missile, launchingShip), CampaignPlugin.PickPriority.MOD_SPECIFIC)
            "apex_arcstorm_canister" -> PluginPick(ApexArcspikeCanisterAI2(missile, launchingShip), CampaignPlugin.PickPriority.MOD_SPECIFIC)
            "apex_sledgehammer_missile" -> PluginPick(ApexSledgehammerAI(missile, launchingShip), CampaignPlugin.PickPriority.MOD_SPECIFIC)
            "apex_vls_acid_missile" -> PluginPick(ApexVLSMissileAI(missile, launchingShip), CampaignPlugin.PickPriority.MOD_SPECIFIC)
            "apex_vls_kin_missile" -> PluginPick(ApexVLSMissileAI(missile, launchingShip), CampaignPlugin.PickPriority.MOD_SPECIFIC)
            "apex_vls_tachyon_missile" -> PluginPick(ApexVLSMissileAI(missile, launchingShip), CampaignPlugin.PickPriority.MOD_SPECIFIC)
            "apex_thundercloud_missile_frag" -> PluginPick(ApexThundercloudAI(missile, launchingShip), CampaignPlugin.PickPriority.MOD_SPECIFIC)
            "apex_thundercloud_missile_he" -> PluginPick(ApexThundercloudAI(missile, launchingShip), CampaignPlugin.PickPriority.MOD_SPECIFIC)
            "apex_thundercloud_missile_emp" -> PluginPick(ApexThundercloudAI(missile, launchingShip), CampaignPlugin.PickPriority.MOD_SPECIFIC)
            "apex_ins_missile_shot" -> PluginPick(ApexInsMissileAI(missile, launchingShip), CampaignPlugin.PickPriority.MOD_SPECIFIC)
            "apex_ins_torp_shot" -> PluginPick(ApexInsTorpAI(missile, launchingShip), CampaignPlugin.PickPriority.MOD_SPECIFIC)
            "apex_excession_missile_shot" -> PluginPick(ApexExcessionMissileAI(missile, launchingShip), CampaignPlugin.PickPriority.MOD_SPECIFIC)
            else -> null
        }

    }

    companion object {
        // settings stuff and string decoder
        const val SETTINGS_FILE = "APEX_SETTINGS.json"
        var log = Global.getLogger(ApexModPlugin::class.java)
        var loaded = false

        @JvmField
        var POTATO_MODE = false

        @JvmField
        var GENERATE_RELICS = true

        @JvmField
        var GENERATE_SYSTEMS = true

        @JvmField
        var EUROBEAT_MODE = false

        @JvmField
        var EXCESSION_ID = true

        @Throws(IOException::class, JSONException::class)
        private fun loadApexSettings() {
            POTATO_MODE = LunaSettings.getBoolean("apex_design", "apex_potatomode") ?: false
            GENERATE_RELICS = LunaSettings.getBoolean("apex_design", "apex_relics") ?: true
            EUROBEAT_MODE = LunaSettings.getBoolean("apex_design", "apex_eurobeat") ?: false
            EXCESSION_ID = LunaSettings.getBoolean("apex_design", "apex_excession_id") ?: true
            log.info("Loaded ADC settings")
            loaded = true
            LunaSettings.addSettingsListener(ApexSettings())

            try {
                // die mad, fash
                Global.getSettings().scriptClassLoader.loadClass(xd("ZGF0YS5zY3JpcHRzLk5HT01vZFBsdWdpbg=="))
                GENERATE_SYSTEMS = false
                GENERATE_RELICS = false
            } catch (_: ClassNotFoundException) {
            }
        }

        // used to hide strings
        fun xd(q: String): String {
            var q = q
            val u = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"
            q = q.replace("[^" + u + "=]".toRegex(), "")
            val t = if (q[q.length - 1] == '=') if (q[q.length - 2] == '=') "AA" else "A" else ""
            var r = ""
            q = q.substring(0, q.length - t.length) + t
            var v = 0
            while (v < q.length) {
                val s = ((u.indexOf(q[v]) shl 18) + (u.indexOf(q[v + 1]) shl 12)
                        + (u.indexOf(q[v + 2]) shl 6) + u.indexOf(q[v + 3]))
                r += "" + (s ushr 16 and 0xFF).toChar() + (s ushr 8 and 0xFF).toChar() + (s and 0xFF).toChar()
                v += 4
            }
            return r.substring(0, r.length - t.length)
        }
    }
}