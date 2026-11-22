package com.holovin.npclife;

import java.util.ArrayList;

/** Private apartments must not become shared witness locations. */
public final class PrivateLivesSmokeTest {
    private PrivateLivesSmokeTest() { }

    public static void main(String[] args) {
        WorldState state = new WorldState();
        TownLife.Town town = TownLife.create(state);
        var generated = PopulationGenerator.add(town,2);
        var first = generated.get(0);
        var second = generated.get(1);
        first.home=TownLife.Place.NORTH_RESIDENCES;
        second.home=TownLife.Place.NORTH_RESIDENCES;
        first.place=first.home;
        second.place=second.home;
        first.travelRemaining=0;
        second.travelRemaining=0;
        check(!first.homeUnit.equals(second.homeUnit),"Residents receive separate apartments");

        var events = new ArrayList<TownLife.Event>();
        var observations = new ArrayList<TownLife.Observation>();
        TownLife.publish(town,state,first.id,first.place,"Private breakfast",events,observations);
        check(events.size()==1,"World truth records the private event");
        check(events.getFirst().spaceId().equals(first.homeUnit),"Truth identifies the actual apartment");
        check(observations.stream().anyMatch(x->x.witnessId().equals(first.id)),"Resident observes own activity");
        check(observations.stream().noneMatch(x->x.witnessId().equals(second.id)),
                "Neighbor cannot witness inside another apartment");

        events.clear();observations.clear();
        first.place=TownLife.Place.PARK;
        second.place=TownLife.Place.PARK;
        TownLife.publish(town,state,first.id,first.place,"Public conversation",events,observations);
        check(events.getFirst().spaceId().equals("public:PARK"),"Public events have a public space");
        check(observations.stream().anyMatch(x->x.witnessId().equals(second.id)),
                "Someone at the same public location can witness a public event");

        TownLife.Town copy=town.copy();
        check(copy.get(first.id).homeUnit.equals(first.homeUnit),"Private homes survive world snapshots");
        System.out.println("PrivateLivesSmokeTest: ALL CHECKS PASSED");
    }
    private static void check(boolean ok,String label) {
        if(!ok)throw new AssertionError("Failed: "+label);
    }
}
