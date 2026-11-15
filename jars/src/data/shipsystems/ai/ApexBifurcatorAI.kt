package data.shipsystems.ai

import com.fs.starfarer.api.combat.*
import com.fs.starfarer.api.util.IntervalUtil
import org.lazywizard.lazylib.CollisionUtils
import org.lazywizard.lazylib.MathUtils
import org.lazywizard.lazylib.combat.AIUtils
import org.lazywizard.lazylib.combat.CombatUtils
import org.lwjgl.util.vector.Vector2f

class ApexBifurcatorAI : ShipSystemAIScript {
    private var ship: ShipAPI? = null
    private var system: ShipSystemAPI? = null
    private var engine: CombatEngineAPI? = null
    private var flags: ShipwideAIFlags? = null
    private val timer = IntervalUtil(0.25f, 0.33f)

    override fun init(ship: ShipAPI, system: ShipSystemAPI, flags: ShipwideAIFlags?, engine: CombatEngineAPI) {
        this.ship = ship
        this.system = system
        this.engine = engine
        this.flags = flags
    }

    override fun advance(amount: Float, missileDangerDir: Vector2f?, collisionDangerDir: Vector2f?, target: ShipAPI?) {
        if (ship == null || !ship!!.isAlive || engine!!.isPaused || system!!.ammo == 0 || system!!.isCoolingDown || ship!!.isPhased) return
        timer.advance(amount)
        if (timer.intervalElapsed()) {
            if (target != null && MathUtils.getDistance(ship, target) < ship!!.mutableStats.energyWeaponRangeBonus.computeEffective(700f))
                ship!!.useSystem()
        }
    }
}