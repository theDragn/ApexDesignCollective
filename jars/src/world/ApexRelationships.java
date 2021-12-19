package world;

import com.fs.starfarer.api.impl.campaign.ids.Factions;

import java.util.HashMap;
import java.util.Map;

public class ApexRelationships
{
    // list of faction relationships
    public static final Map<String, Float> relMap = new HashMap<>();
    static {
        // vanilla factions
        // self-explanatory
        relMap.put(Factions.PLAYER, 0f);
        // gotta be friendly with a big power
        relMap.put(Factions.HEGEMONY, 0.15f);
        // but not every big power
        relMap.put(Factions.PERSEAN, 0f);
        // long history
        relMap.put(Factions.TRITACHYON, -0.6f);
        // some minor relationships, still fight
        relMap.put(Factions.PIRATES, -0.6f);
        // indies are good
        relMap.put(Factions.INDEPENDENT, 0.1f);
        // apex is technosatan
        relMap.put(Factions.LUDDIC_CHURCH, -0.2f);
        relMap.put(Factions.KOL, -0.2f);
        relMap.put(Factions.LUDDIC_PATH, -1f);
        // seems kinda fash to me
        relMap.put(Factions.DIKTAT, -0.35f);
        relMap.put(Factions.LIONS_GUARD, -0.35f);
        // spawn of tritach
        relMap.put(Factions.REMNANTS, -0.5f);
        relMap.put(Factions.DERELICT, 0f);

        // mod factions

        // actually decent people

        // progress is good
        relMap.put("exalted", 0.3f);
        // good guys are good
        relMap.put("shadow_industry", 0.3f);
        // helping people is good, actually
        relMap.put("brighton", 0.3f);
        // not quite worker-owned, but their heart's in the right spot
        relMap.put("metelson", 0.3f);
        // a little bit incoherently communist, but that's not a bad thing
        relMap.put("communist_clouds", 0.3f);
        // unions? fucking sick
        relMap.put("roider", 0.3f);
        // I don't remember anything about these guys other than that they're vaguely good
        relMap.put("unitedpamed", 0.15f);

        // not socialist but vaguely good-aligned
        relMap.put("dassault_mikoyan", 0.1f);
        relMap.put("6eme_bureau", 0.1f);

        // have you ever heard of worker ownership
        // edit, kind of, they've heard of workplace democracy which is pretty cool
        relMap.put("scalartech", 0.15f);

        // evil factions
        relMap.put("xhanempire", -1f);
        // seriously just give it up at this point
        relMap.put("new_galactic_order", -1f);
        relMap.put("draco", -1f);
        relMap.put("fang", -1f);
        relMap.put("fpe", -1f);
        relMap.put("templars", -1f);
        relMap.put("cabal", -1f);
        relMap.put("blade_breakers", -1f);
        relMap.put("tahlan_legioinfernalis", -1f);

        // evil megacorps
        // anime tritach
        relMap.put("diableavionics", -0.3f);
        // evil (non-union labor)
        relMap.put("hmi", -0.5f);
        // mercs
        relMap.put("united_security", -0.1f);
        // tritachyon associates
        relMap.put("galaxytigers", -0.1f);
        // shady megacorp
        relMap.put("blackrock_driveyards", -0.25f);
        // dunno but it's a corporation
        relMap.put("pearson_exotronics", -0.1f);
        // tritach associates
        relMap.put("sylphon", -0.3f);
        // big corp equals bad
        relMap.put("neutrinocorp", -0.15f);


        // "pig" is insulting to actual pigs, who to my knowledge have never executed anyone during a traffic stop
        relMap.put("gmda", -0.4f);
        relMap.put("gmda_patrol", -0.4f);
        relMap.put("COPS", -0.4f);

        // other weirdos
        // shady motherfuckers
        relMap.put("SCY", -0.1f);
        // aren't you guys kinda culty?
        relMap.put("ORA", -0.1f);
        // "imperium"? cringe.
        relMap.put("interstellarimperium", -0.15f);
        relMap.put("ii_imperial_guard", -0.15f);
        relMap.put("mayasura", 0f);
        // what's their deal? well, they haven't attacked us, yet
        relMap.put("al_ars", -0.25f);


        relMap.put("mess", 0f);


        relMap.put("cmc", 0f);
        relMap.put("kadur_remnant", 0f);
        relMap.put("rb", 0f);


        relMap.put("prv", 0f);
        relMap.put("xlu", 0f);
        relMap.put("ocu", 0f);

        relMap.put("tiandong", 0f);


    }
}

