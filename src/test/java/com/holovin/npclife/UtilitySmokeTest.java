package com.holovin.npclife;

import java.util.ArrayList;

/** Working water and lighting provide concrete, bounded benefits to city residents. */
public final class UtilitySmokeTest {
    private UtilitySmokeTest() { }

    public static void main(String[] args) {
        var town=TownLife.create(new WorldState());
        check(!UtilityLife.status(town).fountainWorking() &&
                !UtilityLife.status(town).lightsWorking(),
                "both initial public services need investment");
        check(town.cleanWater==0,"new city never starts with imaginary purified water");
        int river=town.naturalResources.get("river-water").stock;

        boolean rejected=false;
        try {UtilityLife.drinkAtFountain(town);}
        catch(IllegalArgumentException expected) {rejected=true;}
        check(rejected,"broken pump means no clean public drinking");
        check(town.naturalResources.get("river-water").stock==river,
                "rejected action must not consume natural water");

        var resident=PopulationGenerator.add(town,1).getFirst();
        resident.place=TownLife.Place.PARK;
        resident.destination=resident.place;
        resident.travelRemaining=0;
        resident.water=0;
        var events=new ArrayList<TownLife.Event>();
        var seen=new ArrayList<TownLife.Observation>();
        var now=new WorldState();
        now.minute=720;
        UtilityLife.tick(town,now,events,seen);
        check(resident.water==0,"a resident cannot fill a bottle at broken fountain");

        var worker=town.player;
        worker.place=TownLife.Place.WORKSHOP;
        worker.destination=worker.place;
        worker.engineeringSkill=3;
        CareerLife.apply(town,"technician");
        TechnicalWorkLife.inspect(town,"water-pump",540);
        TechnicalWorkLife.complete(town,"water-pump",1,570);
        check(UtilityLife.status(town).fountainWorking(),
                "repair physically restores the public fountain");
        check(town.cleanWater==6 && town.naturalResources.get("river-water").stock==river-6,
                "repair primes exactly six filtered units from finite river abstraction");

        UtilityLife.tick(town,now,events,seen);
        check(resident.water==1 && town.cleanWater==5,
                "resident refills a bottle using one public water unit");
        check(events.stream().anyMatch(e->e.actorId().equals(resident.id) &&
                e.text().contains("fountain")),
                "refilling water becomes a real, witnessable resident event");

        worker.place=TownLife.Place.PARK;
        worker.destination=worker.place;
        worker.thirst=80;
        UtilityLife.drinkAtFountain(town);
        check(worker.thirst==45 && town.cleanWater==4,
                "visitor uses the same finite fountain supply as residents");
        check(town.naturalResources.get("river-water").stock==river-6,
                "drinking tank water never secretly consumes river twice");
        rejected=false;
        try {
            var far=town.copy();
            far.player.place=TownLife.Place.OFFICE;
            UtilityLife.drinkAtFountain(far);
        } catch(IllegalArgumentException expected) {rejected=true;}
        check(rejected,"remote public water access rejected");

        resident.water=2; // Isolate daily pump refill from voluntary citizen withdrawals.
        now.day=2;now.minute=0;
        UtilityLife.tick(town,now,events,seen);
        check(town.cleanWater==8 &&
                town.naturalResources.get("river-water").stock==river-10,
                "operating pump transfers only four more units at midnight");
        UtilityLife.tick(town,now,events,seen);
        check(town.cleanWater==8,"duplicate midnight must not refill twice");
        check(town.lastWaterPumpDay==2,"last refill day recorded");

        check(UtilityLife.nightExposure(town,TownLife.Place.PARK,1260),
                "bad streetlights create outdoor night exposure");
        check(!UtilityLife.nightExposure(town,TownLife.Place.PARK,720),
                "streetlights don't affect noon");
        check(!UtilityLife.nightExposure(town,TownLife.Place.WOODS,1260),
                "urban street lamps are not assigned to remote forests");
        resident.water=2;
        resident.fatigue=25;
        now.minute=1260;
        UtilityLife.tick(town,now,events,seen);
        check(resident.fatigue==26,
                "actual resident becomes more fatigued at night with broken lighting");

        worker.place=TownLife.Place.OFFICE;
        worker.destination=worker.place;
        worker.engineeringSkill=8;
        CareerLife.apply(town,"engineer");
        TechnicalWorkLife.inspect(town,"street-lights",540);
        TechnicalWorkLife.complete(town,"street-lights",2,570);
        check(UtilityLife.status(town).lightsWorking(),
                "engineer upgrade restores public lighting");
        check(!UtilityLife.nightExposure(town,TownLife.Place.PARK,1260),
                "working lights remove extra night fatigue");
        now.minute=1320;
        UtilityLife.tick(town,now,events,seen);
        check(resident.fatigue==26,
                "lit streets remove extra fatigue for the same outdoor resident");
        var clone=town.copy();
        clone.cleanWater=0;
        clone.technicalOrders.get("street-lights").condition=0;
        check(town.cleanWater==8 && UtilityLife.status(town).lightsWorking(),
                "speculative clone cannot mutate persistent city services");
        System.out.println("UtilitySmokeTest: ALL CHECKS PASSED");
    }

    private static void check(boolean ok,String label) {
        if(!ok)throw new AssertionError(label);
    }
}
