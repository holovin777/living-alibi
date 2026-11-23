package com.holovin.npclife;

import java.util.ArrayList;

/** Autonomous crimes arise from fluctuating pressure and real co-location. */
public final class MotiveIntegrationSmokeTest {
    private MotiveIntegrationSmokeTest() { }
    public static void main(String[] args) {
        var state=new WorldState();
        var town=TownLife.create(state);
        PopulationGenerator.add(town,12);
        var truth=new ArrayList<TownLife.Event>();
        var seen=new ArrayList<TownLife.Observation>();
        LifeSimulation.advance(state,21L*1440L,s->TownLife.tick(town,s,truth,seen));
        check(!town.cases.isEmpty(),"pressure produces incidents during autonomous simulation");
        boolean outsideLegacy=false;
        var previous=(InvestigationLife.CaseFile)null;
        for(var c:town.cases.values()) {
            check(c.motiveCode!=null && c.motiveStrength>=59,"every incident has a cause");
            check(c.incidentProfiles.size()==14,"contemporary profiles captured");
            check(c.witnesses.contains(c.offenderId) && c.witnesses.contains(c.victimId),
                    "participants co-present at scene");
            if(c.day%7!=0 || c.minute!=1050)outsideLegacy=true;
            if(previous!=null)
                check(((long)c.day-previous.day)*1440L+c.minute-previous.minute>=3L*1440L,
                        "minimum cooling-off period between cases");
            previous=c;
        }
        check(outsideLegacy,"incidents do not use old weekly schedule");
        check(truth.stream().filter(e->e.text().contains("wallet")&&e.text().contains("disappeared"))
                .count()==town.cases.size(),"one official incident event per case");
        System.out.println("MotiveIntegrationSmokeTest: ALL CHECKS PASSED ("+
                town.cases.size()+" motive-driven case(s) in 21 days)");
    }
    private static void check(boolean ok,String message) {
        if(!ok)throw new AssertionError("Failed: "+message);
    }
}
