package data.weapons.proj.ai

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.*
import com.fs.starfarer.api.input.InputEventAPI
import com.fs.starfarer.api.util.IntervalUtil
import org.lazywizard.lazylib.MathUtils
import java.util.PriorityQueue

class ApexLPDSystem(val owner: Int): EveryFrameCombatPlugin
{
    val available_missiles = ArrayDeque<MissileAPI>()

    // wait several update cycles to fire on these targets because they were previously fully tasked
    val hold_fire_on = mutableMapOf<CombatEntityAPI, Int>()

    val update_timer = IntervalUtil(0.1f, 0.2f)

    val engine = Global.getCombatEngine()

    val MISSILE_RANGE = 2500f
    val MISSILE_DAMAGE = 500f
    val HOLD_FIRE_CYCLES = 4


    override fun advance(amount: Float, events: MutableList<InputEventAPI>?)
    {
        if (engine.isPaused) return
        update_timer.advance(amount)
        if (!update_timer.intervalElapsed()) return

        // update list of our missiles
        // we don't need to add them, the weapon script registers them when fired
        available_missiles.removeAll { !engine.isEntityInPlay(it) }

        // if we don't have any missiles to use, we can stop here
        if (available_missiles.isEmpty()) return

        // list of PD targets to engage, by priority
        val targets = PriorityQueue<TargetData>(10, TargetComparator())

        // clean hold fire target data
        val toRemove = mutableListOf<CombatEntityAPI>()
        for (key in hold_fire_on.keys)
        {
            if (hold_fire_on[key]!! <= 0) toRemove.add(key)
            else hold_fire_on[key] = hold_fire_on[key]!! - 1
        }
        for (target in toRemove) hold_fire_on.remove(target)

        // compute targets that we need to task and their priorities
        val fog = engine.getFogOfWar(owner)
        for (missile in engine.missiles)
        {
            // ignore our missiles, dead missiles, and flares (IPDAI check is infeasible here)
            if (missile.owner == owner || missile.isFizzling || missile.isFlare) continue
            if (hold_fire_on.containsKey(missile)) continue
            // probably not necessary - sides should (almost) always see each other if they're firing missiles
            if (!fog.isVisible(missile.location)) continue
            var priority = 0f
            priority += missile.damage.damage
            // lower priority for frag missiles, since they have oversized damage values
            if (missile.damageType == DamageType.FRAGMENTATION) priority *= 0.33f
            if (priority >= 750f) priority *= 2f // bonus priority for high-damage missiles
            // if we have more than a hundred targets already, stop considering low-priority targets entirely
            if (targets.size > 100 && priority < 100) continue
            targets.add(TargetData(missile, priority, missile.hitpoints))
        }
        for (ship in engine.ships)
        {
            if (ship.owner == owner || ship.hullSize != ShipAPI.HullSize.FIGHTER || !ship.isAlive) continue
            ship.wing ?: continue
            if (hold_fire_on.containsKey(ship)) continue
            if (!fog.isVisible(ship.location)) continue
            var priority = 0f
            // if a fighter has no carrier it's probably an Aspect wing
            if (ship.wing == null || ship.wing.sourceShip == null || ship.wing.spec == null) priority += 1000f
            else priority += ship.wing.spec.getOpCost(ship.wing.sourceShip.mutableStats) * 25f
            // this is totally cheating but: prioritize fighters based on OP cost
            // presumably more dangerous fighters will cost more OP
            // ie, a broadsword will get 240 priority, slightly more than an annihilator rocket

            var hp = ship.hitpoints
            if (ship.shield != null) hp += ship.maxFlux * 2
            hp += ship.armorGrid.armorRating * 2
            targets.add(TargetData(ship, priority, hp))
        }

        // okay, we have our list
        // task missiles to highest priority targets
        // this is... really inefficient, despite my best efforts.
        // There's not really any way avoid having to iterate over every missile for every target to find a missile that's in range.
        // To avoid problems, we give the missiles a very long range, limit the number of checks we can do for each iteration, and re-order the available missiles frequently (which is cheap).
        var outerchecks = 0
        while (!targets.isEmpty() && available_missiles.isNotEmpty() && outerchecks < 50)
        {
            outerchecks++
            val targetdata = targets.poll()
            var innerchecks = 0
            // we really don't need to check every missile
            while (available_missiles.isNotEmpty() && targetdata.predicted_hp > 0 && innerchecks < available_missiles.size / 2)
            {
                val missile = available_missiles.first()
                innerchecks++
                // if our missile is in range, task it
                if (MathUtils.isWithinRange(missile.location, targetdata.target.location, MISSILE_RANGE))
                {
                    task(missile, targetdata.target)
                    available_missiles.removeFirst()
                    targetdata.predicted_hp -= MISSILE_DAMAGE
                    innerchecks = 0
                } else // otherwise, send it to the back of the line and try again with a different missile
                {
                    available_missiles.addLast(missile)
                    available_missiles.removeFirst()
                }

            }
            if (targetdata.predicted_hp <= 0) hold_fire_on[targetdata.target] = HOLD_FIRE_CYCLES
        }

        // we've tasked all our targets
    }

    override fun renderInWorldCoords(viewport: ViewportAPI?) {}
    override fun renderInUICoords(viewport: ViewportAPI?) {}
    @Deprecated("Deprecated in Java")
    override fun init(engine: CombatEngineAPI?) {}

    override fun processInputPreCoreControls(amount: Float, events: MutableList<InputEventAPI>?)
    {
        return
    }

    fun task(missile: MissileAPI, target: CombatEntityAPI) {
        missile.missileAI = ApexLPDMissileAI(missile, missile.source)
        (missile.unwrappedMissileAI as ApexLPDMissileAI).target = target
        //engine.addFloatingText(missile.location, "tasked", 12f, Color.WHITE, missile, 0f, 0f)
        //engine.addHitParticle(target.location, target.velocity, 40f, 1.0f, Color.GREEN)
    }

    // just used to make the target priority queue sort correctly
    class TargetComparator: Comparator<TargetData> {
        override fun compare(o1: TargetData?, o2: TargetData?): Int
        {
            o1 ?: return 0
            o2 ?: return 0
            return (o1.priority - o2.priority).toInt()
        }

    }

    class TargetData(val target: CombatEntityAPI, val priority: Float, var predicted_hp: Float) {}
}


