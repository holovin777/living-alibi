package com.holovin.npclife;

import java.util.ArrayList;
import java.util.HashMap;

/** A living town must spread victimization rather than repeatedly target one wallet holder. */
public final class RecoveryIntegrationSmokeTest {
    private RecoveryIntegrationSmokeTest() { }
    public static void main(String[] args) {
        var clock=new WorldState();var town=TownLife.create(clock);
        PopulationGenerator.add(town,12);
        var truth=new ArrayList<TownLife.Event>();
        var seen=new ArrayList<TownLife.Observation>();
        LifeSimulation.advance(clock,28L*1440L,s->TownLife.tick(town,s,truth,seen));
        if(town.cases.isEmpty())throw new AssertionError("No case emerged in 28 days");
        var previous=new HashMap<String,Long>();
        for(var c:town.cases.values()) {
            long tick=((long)c.day-1)*1440L+c.minute;
            Long earlier=previous.put(c.victimId,tick);
            if(earlier!=null && tick-earlier<14L*1440L)
                throw new AssertionError("Same victim attacked again too early: "+c.victimId);
            var item=town.items.get(c.physicalItemId);
            if(item==null || !item.stolen || !item.ownerId.equals(c.victimId))
                throw new AssertionError("Crime lacks the original stolen wallet");
        }
        System.out.println("RecoveryIntegrationSmokeTest: ALL CHECKS PASSED ("+
                town.cases.size()+" cases, "+previous.size()+" distinct victims in 28 days)");
    }
}
