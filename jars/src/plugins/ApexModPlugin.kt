package plugins

import apexsubs.ApexSpecLoadingUtils
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
import org.json.JSONException
import world.ApexRelicPlacer
import world.ApexSectorGenerator
import java.io.IOException


class ApexModPlugin : BaseModPlugin() {
    override fun onApplicationLoad() {
        val hasLazyLib = Global.getSettings().modManager.isModEnabled("lw_lazylib")
        if (!hasLazyLib) throw RuntimeException(
                "Apex Design Collective requires LazyLib.\nGet it on the forums."
        )
        // Needs MagicLib for rendering stuff, probably
        val hasMagicLib = Global.getSettings().modManager.isModEnabled("MagicLib")
        if (!hasMagicLib) throw RuntimeException(
                "Apex Design Collective requires MagicLib.\nGet it on the forums."
        )
        /*val hasDroneLib = Global.getSettings().modManager.isModEnabled("dronelib")
        if (!hasDroneLib) throw RuntimeException(
                "Apex Design Collective requires DroneLib.\nGet it on the forums."
        )*/
        val hasGraphicslib = Global.getSettings().modManager.isModEnabled("shaderLib")
        if (!hasGraphicslib) throw RuntimeException(
                "Apex Design Collective requires GraphicsLib. Get it on the forums."
        )
        try {
            loadApexSettings()
        } catch (e: Exception) {
            System.out.println(e);
            throw RuntimeException(
                    "Apex Design Collective encountered a \"bruh moment\".\nAnd by that I mean there was an issue with the settings file."
            )
        }

        ApexSpecLoadingUtils.loadSubsystemData();
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
        var hasApex = false
        /*for (system in Global.getSector().getStarSystems()) {
            if (system.baseName.equals("Vela")) {
                hasApex = true
            }
        }*/
        hasApex = SharedData.getData().personBountyEventData.participatingFactions.contains("apex_design")
        if (!hasApex) {
            ApexSectorGenerator().generate(Global.getSector())
            ApexSectorGenerator.createInitialPeople()
            if (GENERATE_RELICS && Global.getSector().memoryWithoutUpdate.contains("\$apex_placed_relics"))
                ApexRelicPlacer().generate(Global.getSector())
        }

        Global.getSector().addTransientListener(ApexExcessionAdder())
    }

    override fun pickMissileAI(missile: MissileAPI, launchingShip: ShipAPI): PluginPick<MissileAIPlugin>? {
        when (missile.projectileSpecId) {
            "apex_nanoacid_torp_guided" -> return PluginPick(ApexGuidedTorpAI(missile, launchingShip), CampaignPlugin.PickPriority.MOD_SPECIFIC)
            "apex_arcspike" -> return PluginPick(ApexArcspikeAI(missile, launchingShip), CampaignPlugin.PickPriority.MOD_SPECIFIC)
            "apex_arcspike_canister" -> return PluginPick(ApexArcspikeCanisterAI2(missile, launchingShip), CampaignPlugin.PickPriority.MOD_SPECIFIC)
            "apex_arcspike_canister_fighter" -> return PluginPick(ApexArcspikeCanisterAI2(missile, launchingShip), CampaignPlugin.PickPriority.MOD_SPECIFIC)
            "apex_arcstorm_canister" -> return PluginPick(ApexArcspikeCanisterAI2(missile, launchingShip), CampaignPlugin.PickPriority.MOD_SPECIFIC)
            "apex_sledgehammer_missile" -> return PluginPick(ApexSledgehammerAI(missile, launchingShip), CampaignPlugin.PickPriority.MOD_SPECIFIC)
            "apex_vls_acid_missile" -> return PluginPick(ApexVLSMissileAI(missile, launchingShip), CampaignPlugin.PickPriority.MOD_SPECIFIC)
            "apex_vls_kin_missile" -> return PluginPick(ApexVLSMissileAI(missile, launchingShip), CampaignPlugin.PickPriority.MOD_SPECIFIC)
            "apex_vls_tachyon_missile" -> return PluginPick(ApexVLSMissileAI(missile, launchingShip), CampaignPlugin.PickPriority.MOD_SPECIFIC)
            "apex_thundercloud_missile_frag" -> return PluginPick(ApexThundercloudAI(missile, launchingShip), CampaignPlugin.PickPriority.MOD_SPECIFIC)
            "apex_thundercloud_missile_he" -> return PluginPick(ApexThundercloudAI(missile, launchingShip), CampaignPlugin.PickPriority.MOD_SPECIFIC)
            "apex_thundercloud_missile_emp" -> return PluginPick(ApexThundercloudAI(missile, launchingShip), CampaignPlugin.PickPriority.MOD_SPECIFIC)
            else -> {
            }
        }
        return null
    }

    companion object {
        // settings stuff and string decoder
        const val SETTINGS_FILE = "APEX_SETTINGS.json"
        var log = Global.getLogger(ApexModPlugin::class.java)
        var loaded = false

        @JvmField
        val HAS_DRONELIB = Global.getSettings().modManager.isModEnabled("dronelib");

        @JvmField
        var POTATO_MODE = false

        @JvmField
        var GENERATE_RELICS = false

        @JvmField
        var GENERATE_SYSTEMS = false

        @JvmField
        var SHOW_DARTGUN_OVERLAY = false

        @JvmField
        var EUROBEAT_MODE = false;

        @JvmField
        var EXCESSION_ID = true;

        @Throws(IOException::class, JSONException::class)
        private fun loadApexSettings() {
            val settings = Global.getSettings().loadJSON(SETTINGS_FILE)
            log.info("Loaded ADC settings json")
            loaded = true
            POTATO_MODE = settings.getBoolean("potatoMode")
            GENERATE_RELICS = settings.getBoolean("generateRelics")
            SHOW_DARTGUN_OVERLAY = settings.getBoolean("showDartgunOverlay")
            EUROBEAT_MODE = settings.getBoolean("eurobeatMode")
            EXCESSION_ID = settings.getBoolean("excessionID")
            try {
                // die mad, fash
                Global.getSettings().scriptClassLoader.loadClass(xd("ZGF0YS5zY3JpcHRzLk1hZ2ljX21vZFBsdWdpbg=="))
                GENERATE_SYSTEMS = settings.getBoolean("generateSystems")
                Global.getSettings().scriptClassLoader.loadClass(xd("ZGF0YS5zY3JpcHRzLk5HT01vZFBsdWdpbg=="))
                GENERATE_SYSTEMS = false
                GENERATE_RELICS = false
            } catch (e: ClassNotFoundException) {
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