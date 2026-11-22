package com.holovin.npclife;

import java.util.ArrayList;

/** Integration: information spreads through actual scheduled conversations, not telepathy. */
public final class KnowledgeIntegrationSmokeTest {
    private KnowledgeIntegrationSmokeTest() { }
    public static void main(String[] args) {
        var world=new WorldState();
        var town=TownLife.create(world);
        PopulationGenerator.add(town,12);
        var truth=new ArrayList<TownLife.Event>();
        var seen=new ArrayList<TownLife.Observation>();
        var social=new ArrayList<SocialLife.Interaction>();
        LifeSimulation.advance(world,14L*1440L,s->TownLife.tick(town,s,truth,seen,social));
        long direct=town.beliefs.values().stream().flatMap(x->x.values().stream())
                .filter(x->x.origin().equals("DIRECT")).count();
        long heard=town.beliefs.values().stream().flatMap(x->x.values().stream())
                .filter(x->x.origin().equals("HEARD")).count();
        if(direct<1)throw new AssertionError("No first-hand beliefs in two weeks");
        if(heard<1 || town.pendingTransfers.isEmpty())
            throw new AssertionError("Conversations failed to transmit any information");
        System.out.println("KnowledgeIntegrationSmokeTest: ALL CHECKS PASSED ("+direct+
                " direct, "+heard+" heard, "+town.pendingTransfers.size()+" transfers)");
    }
}
