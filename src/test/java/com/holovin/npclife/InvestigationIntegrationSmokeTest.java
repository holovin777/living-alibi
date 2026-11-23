package com.holovin.npclife;

import java.util.ArrayList;

/** World-scale regression: incidents emerge from co-location during autonomous play. */
public final class InvestigationIntegrationSmokeTest {
    private InvestigationIntegrationSmokeTest() { }
    public static void main(String[] args) {
        var clock=new WorldState();var town=TownLife.create(clock);
        PopulationGenerator.add(town,12);
        var truth=new ArrayList<TownLife.Event>();
        var witness=new ArrayList<TownLife.Observation>();
        LifeSimulation.advance(clock,14L*1440L,s->TownLife.tick(town,s,truth,witness));
        check(town.cases.size()>0,"at least one incident during a fortnight in a busy town");
        for(var c:town.cases.values()) {
            check(c.witnesses.contains(c.victimId)&&c.witnesses.contains(c.offenderId),
                  "theft only when participants share public space");
            check(c.day%7==0,"incident follows opportunity sampling schedule");
        }
        check(truth.stream().anyMatch(e->e.text().contains("wallet")&&e.text().contains("disappeared")),
              "a report exists in the world event log");
        System.out.println("InvestigationIntegrationSmokeTest: ALL CHECKS PASSED ("+
                town.cases.size()+" emergent case(s) in 14 days)");
    }
    private static void check(boolean ok,String label){if(!ok)throw new AssertionError("Failed: "+label);}
}
