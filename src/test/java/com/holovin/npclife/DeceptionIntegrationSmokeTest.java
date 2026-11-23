package com.holovin.npclife;

import java.util.ArrayList;

/** End-to-end rule simulation: deceptive claims emerge while world truth stays intact. */
public final class DeceptionIntegrationSmokeTest {
    private DeceptionIntegrationSmokeTest() { }
    public static void main(String[] args) {
        var clock=new WorldState();
        var town=TownLife.create(clock);
        PopulationGenerator.add(town,12);
        var truth=new ArrayList<TownLife.Event>();
        var observations=new ArrayList<TownLife.Observation>();
        var interactions=new ArrayList<SocialLife.Interaction>();
        LifeSimulation.advance(clock,28L*1440L,
                c->TownLife.tick(town,c,truth,observations,interactions));
        long secretCount=town.secrets.values().stream().mapToInt(m->m.size()).sum();
        long lies=town.pendingDecisions.stream().filter(d->d.kind().equals("LIE")).count();
        if(secretCount<1)throw new AssertionError("Sensitive eyewitness experiences produced no secrets");
        if(lies<1)throw new AssertionError("No deliberate false claim emerged in four weeks");
        for(var d:town.pendingDecisions) {
            if(!d.kind().equals("LIE"))continue;
            var matching=truth.stream().filter(e->KnowledgeLife.eventKey(e).equals(d.eventKey())).findFirst();
            if(matching.isEmpty())throw new AssertionError("A deception has no underlying truthful event");
            if(matching.get().text().equals(d.statement()))throw new AssertionError("A lie is identical to truth");
            if(truth.stream().anyMatch(e->e.text().equals(d.statement())))
                throw new AssertionError("A deceptive statement leaked into authoritative event log");
        }
        System.out.println("DeceptionIntegrationSmokeTest: ALL CHECKS PASSED ("+
                secretCount+" secrets, "+lies+" lies in 28 days)");
    }
}
