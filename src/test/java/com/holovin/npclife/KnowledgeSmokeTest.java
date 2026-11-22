package com.holovin.npclife;

import java.util.ArrayList;

/** Direct evidence, hearsay, source attribution and separate private scenes. */
public final class KnowledgeSmokeTest {
    private KnowledgeSmokeTest() { }
    public static void main(String[] args) {
        var world=new WorldState();
        world.day=4;world.minute=1065;
        var town=TownLife.create(world);
        var people=PopulationGenerator.add(town,2);
        var sara=people.get(0);var pietro=people.get(1);
        var marco=town.get("marco");var elena=town.get("elena");
        position(marco,TownLife.Place.PARK);
        position(sara,TownLife.Place.PARK);
        position(elena,TownLife.Place.MARKET);
        position(pietro,TownLife.Place.CAFE);

        var truth=new ArrayList<TownLife.Event>();
        var witnesses=new ArrayList<TownLife.Observation>();
        String statement=sara.name+" stole a letter at Park."; // synthetic *test* incident
        TownLife.publish(town,world,sara.id,TownLife.Place.PARK,statement,truth,witnesses);
        String eventKey=KnowledgeLife.eventKey(truth.getFirst());
        check(town.beliefs.get("marco").get(eventKey).origin().equals("DIRECT"),"Marco witnessed the event");
        check(town.beliefs.get(sara.id).get(eventKey).confidence()==100,"witness confidence is 100");
        check(!town.beliefs.containsKey("elena"),"remote Elena does not know");
        check(!town.beliefs.containsKey(pietro.id),"remote Pietro does not know");

        KnowledgeLife.exchange(town,world,"marco","elena");
        check(town.pendingTransfers.isEmpty(),"no remote gossip");
        position(elena,TownLife.Place.PARK);
        KnowledgeLife.exchange(town,world,"marco","elena");
        var heard=town.beliefs.get("elena").get(eventKey);
        check(heard!=null && heard.origin().equals("HEARD"),"hearsay is not direct evidence");
        check(heard.learnedFrom().equals("marco") && heard.hops()==1,"provenance recorded");
        check(heard.confidence()<100,"confidence diminished");
        KnowledgeLife.exchange(town,world,"marco","elena");
        check(town.pendingTransfers.size()==1,"no duplicate sharing");

        position(pietro,TownLife.Place.PARK);
        KnowledgeLife.exchange(town,world,"elena",pietro.id);
        var relayed=town.beliefs.get(pietro.id).get(eventKey);
        check(relayed!=null && relayed.hops()==2,"second-hand claim can propagate");
        check(relayed.confidence()<heard.confidence(),"relay confidence decreases");

        KnowledgeLife.witnessed(town,truth.getFirst(),"elena");
        check(town.beliefs.get("elena").get(eventKey).origin().equals("DIRECT"),"direct proof outranks hearsay");
        check(town.beliefs.get("elena").get(eventKey).confidence()==100,"direct proof restores confidence");

        position(sara,sara.home);
        position(pietro,pietro.home);
        // Test set-up forces both into same neighborhood; distinct homeUnit prevents leakage.
        pietro.home=sara.home;position(pietro,sara.home);
        var truth2=new ArrayList<TownLife.Event>();
        TownLife.publish(town,world,sara.id,sara.home,sara.name+" stole a diary at home.",
                truth2,new ArrayList<>());
        var key2=KnowledgeLife.eventKey(truth2.getFirst());
        check(town.beliefs.get(sara.id).containsKey(key2),"private event recorded for resident");
        check(!town.beliefs.get(pietro.id).containsKey(key2),"neighbor cannot witness private event");
        System.out.println("KnowledgeSmokeTest: ALL CHECKS PASSED");
    }
    private static void position(TownLife.Citizen person,TownLife.Place place) {
        person.place=place;person.destination=place;person.travelRemaining=0;person.activity="FREE_TIME";
    }
    private static void check(boolean ok,String msg) {if(!ok)throw new AssertionError(msg);}
}
