package com.holovin.npclife;

import java.util.ArrayList;

/**
 * Utility repairs change real citizens' choices, not just the HUD:
 * a functioning fountain can replace a market purchase, and working
 * street lights let residents enjoy the park in the evening.
 */
public final class UtilityBehaviorSmokeTest {
    private UtilityBehaviorSmokeTest() { }

    public static void main(String[] args) {
        var clock=new WorldState();
        clock.minute=720;

        var base=TownLife.create(clock);
        var resident=PopulationGenerator.add(base,1).getFirst();
        resident.place=resident.home;
        resident.destination=resident.home;
        resident.travelRemaining=0;
        resident.money=20;
        resident.food=3;
        resident.water=0;

        var broken=base.copy();
        var brokenResident=broken.get(resident.id);
        PopulationLife.tick(broken,clock,new ArrayList<>(),new ArrayList<>());
        check(brokenResident.destination==TownLife.Place.MARKET,
                "without a working public fountain a thirsty resident buys water");

        var repaired=base.copy();
        repaired.technicalOrders.get("water-pump").condition=100;
        int riverBefore=repaired.naturalResources.get("river-water").stock;
        UtilityLife.pumpRepaired(repaired);
        check(repaired.cleanWater==6 &&
                repaired.naturalResources.get("river-water").stock==riverBefore-6,
                "new public water is transferred from the finite river stock");
        var repairedResident=repaired.get(resident.id);
        PopulationLife.tick(repaired,clock,new ArrayList<>(),new ArrayList<>());
        check(repairedResident.destination==TownLife.Place.PARK,
                "same thirsty resident now chooses the accessible park fountain");

        var events=new ArrayList<TownLife.Event>();
        var observations=new ArrayList<TownLife.Observation>();
        LifeSimulation.advance(clock,14,
                time->TownLife.tick(repaired,time,events,observations));
        check(repairedResident.water>=2,
                "resident actually reached the park and refilled drinking water");
        check(repairedResident.money==20,
                "citizen can save their own money by using a working public service");
        check(repaired.cleanWater==4,
                "two refills consume exactly two clean public units");
        check(repaired.naturalResources.get("river-water").stock==riverBefore-6,
                "drinking stored fountain water cannot double-extract river water");
        check(events.stream().anyMatch(e->e.actorId().equals(resident.id) &&
                e.text().contains("fountain")),
                "the successful refill becomes an observable world event");

        // An evening leisure preference that would normally choose a park
        // is redirected to an indoor venue while the streetlights are down.
        var eveningClock=new WorldState();
        eveningClock.day=1;
        eveningClock.minute=1111; // After scheduled creative workshops end.
        var evening=TownLife.create(eveningClock);
        var leisure=PopulationGenerator.add(evening,1).getFirst();
        // Isolate free-time behavior, not work, sport or creative commitments.
        leisure.personalSeed=4; // floorMod(4 ^ (1*31),4) = 3 => park.
        leisure.goal="FIND_BALANCE";
        leisure.age=35;
        leisure.employed=false;
        leisure.sickUntilDay=0;
        leisure.food=3;
        leisure.water=3;
        leisure.place=leisure.home;
        leisure.destination=leisure.home;
        leisure.travelRemaining=0;
        var dark=evening.copy();
        var lit=evening.copy();
        dark.technicalOrders.get("street-lights").condition=50;
        lit.technicalOrders.get("street-lights").condition=90;
        check(!UtilityLife.status(dark).lightsWorking() && UtilityLife.status(lit).lightsWorking(),
                "fixture isolates a broken light and a powered repaired light");

        PopulationLife.tick(dark,eveningClock,new ArrayList<>(),new ArrayList<>());
        PopulationLife.tick(lit,eveningClock,new ArrayList<>(),new ArrayList<>());
        check(dark.get(leisure.id).destination==TownLife.Place.CAFE,
                "resident avoids an unlit public park after dusk; actual="+dark.get(leisure.id).destination);
        check(lit.get(leisure.id).destination==TownLife.Place.PARK,
                "engineering repair restores the resident's preferred park visit; actual="+lit.get(leisure.id).destination);

        TownLife.tick(dark,eveningClock,new ArrayList<>(),new ArrayList<>());
        TownLife.tick(lit,eveningClock,new ArrayList<>(),new ArrayList<>());
        check(dark.get("elena").destination==TownLife.Place.CAFE,
                "named residents also react to missing street lighting");
        check(lit.get("elena").destination==TownLife.Place.PARK,
                "named resident enjoys the park again after public light repair");

        System.out.println("UtilityBehaviorSmokeTest: ALL CHECKS PASSED");
    }

    private static void check(boolean ok,String message) {
        if(!ok)throw new AssertionError(message);
    }
}
