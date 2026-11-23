package com.holovin.npclife;

import java.util.ArrayList;

/** A crime must have world truth, possible witnesses and a solvable non-omniscient case file. */
public final class InvestigationSmokeTest {
    private InvestigationSmokeTest() { }
    public static void main(String[] args) {
        var clock=new WorldState();clock.day=7;clock.minute=1050;
        var town=TownLife.create(clock);
        PopulationGenerator.add(town,4);
        // Four people really occupy the same public scene at the incident instant.
        for(var p:town.people.values()) {
            p.place=TownLife.Place.PARK;p.destination=p.place;p.travelRemaining=0;
            p.activity="FREE_TIME";p.money=80;
        }
        var truth=new ArrayList<TownLife.Event>();
        var seen=new ArrayList<TownLife.Observation>();
        InvestigationLife.recordIncident(town,clock,TownLife.Place.PARK,
                town.get("marco"),town.get("elena"),truth,seen);
        check(town.cases.size()==1,"one reported incident");
        var c=town.cases.values().iterator().next();
        check(c.witnesses.size()==town.people.size(),"only co-present witnesses recorded");
        check(!c.offenderId.equals(c.victimId),"no self theft");
        check(truth.size()==1,"authoritative event logged");
        check(truth.getFirst().actorId().equals(c.offenderId),"engine alone knows offender");
        check(!truth.getFirst().text().contains(town.get(c.offenderId).name),
              "public account does not disclose suspect");
        check(seen.size()==town.people.size(),"observations distributed by scene");
        InvestigationLife.tick(town,clock,truth,seen);
        check(town.cases.size()==1,"duplicate tick does not duplicate incident");
        var other=town.copy();
        check(other.cases.size()==1,"copy includes cases");
        check(InvestigationLife.accuse(other,c.id,c.offenderId).contains("Insufficient"),
              "cannot accuse without investigation");
        check(InvestigationLife.investigate(other,c.id).contains("receipt"),"scene examination");
        InvestigationLife.interview(other,c.id,c.victimId);
        var outside=other.people.keySet().stream().filter(id->!id.equals(c.victimId) && !id.equals(c.offenderId))
                .findFirst().orElseThrow();
        InvestigationLife.interview(other,c.id,outside);
        check(InvestigationLife.accuse(other,c.id,c.victimId).contains("remains open"),
              "incorrect accusation does not reveal culprit");
        check(!other.cases.get(c.id).solved,"case still open after wrong accusation");
        check(InvestigationLife.accuse(other,c.id,c.offenderId).contains("Case solved"),
              "correct accusation resolves case");
        check(other.cases.get(c.id).solved,"case marked solved");
        check(!town.cases.get(c.id).solved,"copy is independent");
        System.out.println("InvestigationSmokeTest: ALL CHECKS PASSED");
    }
    private static void check(boolean ok,String label){if(!ok)throw new AssertionError("Failed: "+label);}
}
