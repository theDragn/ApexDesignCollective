package data.effects;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.Misc;
import data.hullmods.ApexCryoSystemHullmod;
import data.subsystems.ApexCryoSubsystem;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ApexCryoEffect extends BaseEveryFrameCombatPlugin
{
    private HullSize sourceSize;
    private float remainingDuration;
    private int numCombinations;
    private boolean removeMapEntry;
    private float effect;
    private ShipAPI target;

    public static final Map<ShipAPI, ApexCryoEffect> effectMap = new HashMap<>();

    public ApexCryoEffect() {}

    public ApexCryoEffect(ShipAPI target, HullSize sourceSize)
    {
        this.removeMapEntry = true;
        this.remainingDuration = ApexCryoSystemHullmod.CRYO_BUFF_DURATION;
        this.target = target;
        this.sourceSize = sourceSize;
        this.numCombinations = 0;
        if (ApexCryoSubsystem.shouldReduceBonus(target.getHullSize(), sourceSize))
            effect = 1f - (1f - ApexCryoSystemHullmod.CRYO_GENERATION_MULT) * ApexCryoSystemHullmod.CRYO_BUFF_EFFECTIVENESS_VS_LARGER;
        else
            effect = ApexCryoSystemHullmod.CRYO_GENERATION_MULT;
        // check to see if ship already has a regen effect going
        if (effectMap.containsKey(target))
        {
            // call the existing effect's combine function (this effect gets deleted, the original one stays with new numbers)
            effectMap.get(target).combineEffects(this);
            // don't remove the map entry, since it points to the existing effect plugin (remember, a new value for an existing key overwrites it)
            removeMapEntry = false;
            // can't remove the plugin in the constructor, so we'll do it when the next frame hits
            remainingDuration = 0;
        } else
        {
            effectMap.put(target, this);
        }
    }

    public void combineEffects(ApexCryoEffect newEffect)
    {
        numCombinations++;
        remainingDuration += ApexCryoSystemHullmod.CRYO_BUFF_DURATION/(2.5 * numCombinations);

        // if the target isn't larger than the new effect's source size
        if (!ApexCryoSubsystem.shouldReduceBonus(target.getHullSize(), newEffect.sourceSize))
            effect = ApexCryoSystemHullmod.CRYO_GENERATION_MULT;
    }

    @Override
    public void advance(float amount, List<InputEventAPI> events)
    {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine.isPaused()) return;
        if (!target.isAlive() || remainingDuration <= 0)
        {
            engine.removePlugin(this);
            // this might cause it to drop the buff for one frame when effects combine but it shouldn't be noticeable
            unmodify(target);
            if (removeMapEntry)
                effectMap.remove(target);
            return;
        }
        remainingDuration -= amount;
        target.getMutableStats().getBallisticWeaponFluxCostMod().modifyMult("apexCryo", effect);
        target.getMutableStats().getEnergyWeaponFluxCostMod().modifyMult("apexCryo", effect);
        target.getMutableStats().getMissileWeaponFluxCostMod().modifyMult("apexCryo", effect);
        target.getMutableStats().getShieldUpkeepMult().modifyMult("apexCryo", effect);

        if (engine.getPlayerShip() == target && remainingDuration > 0f)
        {
            engine.maintainStatusForPlayerShip("apex_cryo", "graphics/icons/buffs/apex_cryo.png", "-" + (int)(100f-effect*100f) + "% weapon and shield flux generation." , "Remaining duration: " + Misc.getRoundedValue(remainingDuration), false);
        }
    }

    private void unmodify(ShipAPI target)
    {
        target.getMutableStats().getBallisticWeaponFluxCostMod().unmodify("apexCryo");
        target.getMutableStats().getEnergyWeaponFluxCostMod().unmodify("apexCryo");
        target.getMutableStats().getMissileWeaponFluxCostMod().unmodify("apexCryo");
        target.getMutableStats().getShieldUpkeepMult().unmodify("apexCryo");
    }
}
