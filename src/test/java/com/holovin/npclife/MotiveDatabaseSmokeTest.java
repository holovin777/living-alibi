package com.holovin.npclife;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/** SQLite integration: real incident context survives reload; legacy cases remain unknown. */
public final class MotiveDatabaseSmokeTest {
    private MotiveDatabaseSmokeTest() { }
    public static void main(String[] args) throws Exception {
        var file=Files.createTempFile("living-alibi-motives-",".db");
        try {
            try(var db=new WorldDatabase(file)) {
                var state=db.load();var town=db.loadTown(state);
                PopulationGenerator.add(town,4);
                state.day=9;state.minute=1014;
                for(var p:town.people.values()) {
                    p.place=TownLife.Place.PARK;p.destination=p.place;
                    p.travelRemaining=0;p.activity="FREE_TIME";p.money=80;
                }
                town.get("resident-0001").money=2;
                var truth=new ArrayList<TownLife.Event>();
                var obs=new ArrayList<TownLife.Observation>();
                var c=InvestigationLife.recordIncident(town,state,TownLife.Place.PARK,
                        town.get("resident-0001"),town.get("resident-0002"),truth,obs);
                InvestigationLife.investigate(town,c.id);
                InvestigationLife.interview(town,c.id,"resident-0001");
                var legacy=new InvestigationLife.CaseFile("case-legacy",7,1050,
                        TownLife.Place.PARK,"marco","elena","wallet",15);
                town.cases.put(legacy.id,legacy);
                db.save(state,List.of(),List.of(),town,truth,obs,List.of());
            }
            try(var db=new WorldDatabase(file)) {
                var state=db.load();var town=db.loadTown(state);
                var c=town.cases.get("case-0001");
                check(c!=null && c.motiveCode!=null,"motive persisted");
                check(c.incidentProfiles.size()==6,"historical resident backgrounds persisted");
                check(c.incidentProfiles.get("resident-0001").financial()==100,
                      "financial snapshot persisted before the stolen money transfer");
                check(InvestigationLife.background(town,c.id,"resident-0001").contains("elevated"),
                      "interviewed background available after restart");
                var legacy=town.cases.get("case-legacy");
                check(legacy!=null && legacy.motiveCode==null && legacy.incidentProfiles.isEmpty(),
                      "legacy world not retroactively assigned new motives");
                check(db.townEvents(10).stream().anyMatch(e->e.text().contains("wallet")),
                      "authoritative incident remains separate");
            }
            System.out.println("MotiveDatabaseSmokeTest: ALL CHECKS PASSED");
        } finally {Files.deleteIfExists(file);}
    }
    private static void check(boolean ok,String label) {
        if(!ok)throw new AssertionError("Failed: "+label);
    }
}
