package com.holovin.npclife;

import java.util.ArrayList;

/** Financial pressure, friction, and temptation are causes, not public proof. */
public final class MotiveSmokeTest {
    private MotiveSmokeTest() { }
    public static void main(String[] args) {
        var state=new WorldState();state.day=9;state.minute=1014;
        var town=TownLife.create(state);
        PopulationGenerator.add(town,4);
        var desperate=town.get("resident-0001");
        var cautious=town.get("resident-0002");
        var victim=town.get("resident-0003");
        for(var person:town.people.values()) {
            person.place=TownLife.Place.PARK;
            person.destination=person.place;person.travelRemaining=0;
            person.activity="FREE_TIME";person.money=200;
        }
        desperate.money=2;desperate.carefulness=25;desperate.curiosity=80;
        cautious.carefulness=99;cautious.curiosity=15;
        victim.money=300;
        var need=MotiveLife.assess(town,state,desperate,victim);
        var mild=MotiveLife.assess(town,state,cautious,victim);
        check(need.strength()>mild.strength(),"economic deprivation increases criminal temptation");
        check(need.reason().equals("FINANCIAL_PRESSURE"),"financial motive chosen for a shortage");
        var truth=new ArrayList<TownLife.Event>();
        var obs=new ArrayList<TownLife.Observation>();
        var c=InvestigationLife.recordIncident(town,state,TownLife.Place.PARK,
                desperate,victim,truth,obs);
        check(c.day==9 && c.minute==1014,"no fixed weekly incident clock");
        check(c.motiveCode.equals("FINANCIAL_PRESSURE"),"private motive recorded");
        check(c.incidentProfiles.get(desperate.id).financial()==100,
              "profiles snapshot pre-incident finances");
        check(c.incidentProfiles.size()==town.people.size(),"other residents also have profiles");
        check(InvestigationLife.background(town,c.id,desperate.id).contains("Inspect"),
              "background gated by scene inspection");
        InvestigationLife.investigate(town,c.id);
        check(InvestigationLife.background(town,c.id,desperate.id).contains("Interview"),
              "background gated by interview");
        InvestigationLife.interview(town,c.id,desperate.id);
        String background=InvestigationLife.background(town,c.id,desperate.id);
        check(background.contains("Financial strain: elevated"),"visible historical context");
        check(!background.contains("FINANCIAL_PRESSURE") && !background.contains("guilty"),
              "motive code and guilt are not leaked");
        var copy=town.copy();
        copy.cases.get(c.id).incidentProfiles.clear();
        check(town.cases.get(c.id).incidentProfiles.size()==town.people.size(),
              "copy isolation of historical profiles");
        var legacy=new InvestigationLife.CaseFile("legacy",1,600,TownLife.Place.PARK,
                "marco","elena","wallet",15);
        town.cases.put("legacy",legacy);
        InvestigationLife.investigate(town,"legacy");
        InvestigationLife.interview(town,"legacy",desperate.id);
        check(InvestigationLife.background(town,"legacy",desperate.id).contains("older case"),
              "legacy case does not invent a motive profile");
        check(truth.size()==1 && !truth.getFirst().text().contains(desperate.name),
              "authoritative culprit is not included in a public event description");
        var calm=TownLife.create(state);
        PopulationGenerator.add(calm,4);
        for(var resident:calm.people.values()) {
            resident.place=TownLife.Place.PARK;
            resident.destination=resident.place;resident.travelRemaining=0;
            resident.activity="FREE_TIME";resident.money=200;
            resident.carefulness=99;resident.curiosity=15;
        }
        InvestigationLife.tick(calm,state,new ArrayList<>(),new ArrayList<>());
        check(calm.cases.isEmpty(),"no guaranteed incident in a low-pressure town");
        System.out.println("MotiveSmokeTest: ALL CHECKS PASSED");
    }
    private static void check(boolean ok,String message) {
        if(!ok)throw new AssertionError("Failed: "+message);
    }
}
