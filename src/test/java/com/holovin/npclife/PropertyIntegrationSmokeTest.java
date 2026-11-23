package com.holovin.npclife;

import java.util.ArrayList;

/** Newly emerging crimes transfer real items while legacy case snapshots remain untouched. */
public final class PropertyIntegrationSmokeTest {
    private PropertyIntegrationSmokeTest() { }
    private static void check(boolean ok,String message) {
        if(!ok)throw new AssertionError("Failed: "+message);
    }
    public static void main(String[] args) {
        var clock=new WorldState();var town=TownLife.create(clock);
        PopulationGenerator.add(town,14);
        var truth=new ArrayList<TownLife.Event>();
        var observed=new ArrayList<TownLife.Observation>();
        LifeSimulation.advance(clock,21L*1440L,s->TownLife.tick(town,s,truth,observed));
        check(!town.cases.isEmpty(),"test city generates at least one crime");
        for(var c:town.cases.values()) {
            check(c.physicalItemId!=null,"new incidents link to an actual item");
            var item=town.items.get(c.physicalItemId);
            check(item!=null,"every case-linked item still exists");
            check(item.ownerId.equals(c.victimId),"the victim keeps legal ownership");
            check(item.stolen,"the stolen item remains stolen until a documented recovery");
            check(town.pendingProperty.stream().anyMatch(change->change.action()==PropertyLife.Action.STOLEN &&
                    change.itemId().equals(item.id)),"theft history matches the case record");
        }
        check(town.items.values().stream().map(i->i.id).distinct().count()==town.items.size(),
                "item identities are unique");
        System.out.println("PropertyIntegrationSmokeTest: ALL CHECKS PASSED ("+town.cases.size()+
                " physical-property cases in 21 days)");
    }
}
