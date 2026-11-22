package com.holovin.npclife;

import java.util.ArrayList;

/** Dependency-free town tests; run with java -ea. */
public final class TownSmokeTest {
    private TownSmokeTest() { }
    public static void main(String[] args) {
        WorldState marco=new WorldState();
        TownLife.Town town=TownLife.create(marco);
        require(town.people.size()==2,"two citizens initialized");
        require(town.get("marco").place==TownLife.Place.BAKERY,"Marco initially at work");
        require(town.get("elena").place==TownLife.Place.MARKET,"Elena initially at work");
        require(town.get("elena").money==55,"Elena has independent finances");

        var events=new ArrayList<TownLife.Event>();
        var observed=new ArrayList<TownLife.Observation>();
        LifeSimulation.advance(marco,1440, s -> TownLife.tick(town,s,events,observed));
        require(marco.day==2,"global time advanced");
        require(!events.isEmpty(),"world generated town events");
        require(!observed.isEmpty(),"NPCs recorded local observations");
        require(town.get("elena").money!=55,"Elena's money changes autonomously");
        require(town.get("elena").hunger>=0&&town.get("elena").hunger<=100,"Elena hunger is bounded");
        require(town.get("elena").thirst>=0&&town.get("elena").thirst<=100,"Elena thirst is bounded");

        // A co-located witness knows about a meeting; an absent person does not.
        WorldState moment=new WorldState();moment.minute=1000;moment.activity="EXPLORING_TOWN";
        TownLife.Town meeting=TownLife.create(moment);
        for(var c:meeting.people.values()) {
            c.place=TownLife.Place.PARK;c.destination=TownLife.Place.PARK;c.travelRemaining=0;
        }
        meeting.together=false;
        var meetingEvents=new ArrayList<TownLife.Event>();
        var meetingKnowledge=new ArrayList<TownLife.Observation>();
        TownLife.tick(meeting,moment,meetingEvents,meetingKnowledge);
        require(meetingEvents.stream().anyMatch(e->e.text().contains("crossed paths")),"meeting has a world-truth event");
        require(meetingKnowledge.stream().anyMatch(o->o.witnessId().equals("marco")&&o.text().contains("crossed paths")),"Marco witnessed meeting");
        require(meetingKnowledge.stream().anyMatch(o->o.witnessId().equals("elena")&&o.text().contains("crossed paths")),"Elena witnessed meeting");

        WorldState separatedMoment=new WorldState();separatedMoment.minute=720;separatedMoment.activity="WORKING";
        TownLife.Town separated=TownLife.create(separatedMoment);
        separated.get("elena").hunger=64;
        separated.get("elena").food=3;
        var separatedEvents=new ArrayList<TownLife.Event>();
        var separatedKnowledge=new ArrayList<TownLife.Observation>();
        TownLife.tick(separated,separatedMoment,separatedEvents,separatedKnowledge);
        require(separatedEvents.stream().anyMatch(e->e.text().contains("ate a meal")),"Elena performed private action");
        require(separatedKnowledge.stream().noneMatch(o->o.witnessId().equals("marco")&&o.text().contains("ate a meal")),
                "Marco does not know unobserved actions");

        TownLife.Town copied=town.copy();
        copied.get("elena").money=0;
        require(town.get("elena").money!=copied.get("elena").money,"town copies do not share mutable state");
        System.out.println("TownSmokeTest: ALL CHECKS PASSED");
    }
    private static void require(boolean condition,String message) {
        if(!condition)throw new AssertionError("Failed: "+message);
    }
}
