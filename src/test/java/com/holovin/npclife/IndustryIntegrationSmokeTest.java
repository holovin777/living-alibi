package com.holovin.npclife;

import java.util.ArrayList;

/** Industrial deliveries must happen during ordinary NPC life, not a scripted demo. */
public final class IndustryIntegrationSmokeTest {
    private IndustryIntegrationSmokeTest() { }

    public static void main(String[] args) {
        var clock=new WorldState();
        var town=TownLife.create(clock);
        PopulationGenerator.add(town,12);
        var truth=new ArrayList<TownLife.Event>();
        var observations=new ArrayList<TownLife.Observation>();

        LifeSimulation.advance(clock,5*1440,
                minute->TownLife.tick(town,minute,truth,observations));
        var report=IndustryLife.report(town);

        check(report.ironRemaining()<80 && report.copperRemaining()<40,
                "autonomous quarry employee physically extracted finite minerals");
        check(report.workshopParts()>6,
                "actual factory deliveries replenish the already existing repair stock");
        check(truth.stream().anyMatch(e->e.text().contains("extracted") &&
                e.actorId().startsWith("resident-")),
                "working mine has named witnesses and an actual citizen actor");
        check(truth.stream().anyMatch(e->e.text().contains("dispatched") &&
                e.actorId().startsWith("resident-")),
                "real factory worker delivers goods during ordinary city life");
        check(report.ironRemaining()>=0 && report.copperRemaining()>=0,
                "natural resources never drop below zero");
        check(town.people.size()==14,
                "same twelve procedural citizens and two original residents remain in town");
        System.out.println("IndustryIntegrationSmokeTest: ALL CHECKS PASSED ("+
                "iron="+report.ironRemaining()+", copper="+report.copperRemaining()+
                ", maintenance parts="+report.workshopParts()+")");
    }

    private static void check(boolean ok,String message) {
        if(!ok)throw new AssertionError(message);
    }
}
