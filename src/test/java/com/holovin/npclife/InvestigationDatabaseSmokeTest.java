package com.holovin.npclife;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/** SQLite regression: unresolved and resolved case state persists across restarts. */
public final class InvestigationDatabaseSmokeTest {
    private InvestigationDatabaseSmokeTest() { }
    public static void main(String[] args) throws Exception {
        var file=Files.createTempFile("living-alibi-cases-",".db");
        try {
            try(var db=new WorldDatabase(file)) {
                var clock=db.load();var town=db.loadTown(clock);
                PopulationGenerator.add(town,4);
                clock.day=7;clock.minute=1050;
                for(var p:town.people.values()) {
                    p.place=TownLife.Place.PARK;p.destination=p.place;p.travelRemaining=0;
                    p.activity="FREE_TIME";p.money=80;
                }
                var events=new ArrayList<TownLife.Event>();
                var observations=new ArrayList<TownLife.Observation>();
                InvestigationLife.tick(town,clock,events,observations);
                var c=town.cases.get("case-0001");
                InvestigationLife.investigate(town,c.id);
                InvestigationLife.interview(town,c.id,c.victimId);
                db.save(clock,List.of(),List.of(),town,events,observations,List.of());
            }
            try(var db=new WorldDatabase(file)) {
                var clock=db.load();var town=db.loadTown(clock);
                check(town.cases.size()==1,"case loaded from SQLite");
                var c=town.cases.get("case-0001");
                check(c.inspected,"inspected scene persisted");
                check(c.interviews.size()==1,"interview persisted");
                check(c.witnesses.size()==town.people.size(),"witness list persisted");
                var someone=town.people.keySet().stream()
                        .filter(id->!id.equals(c.victimId)&&!id.equals(c.offenderId))
                        .findFirst().orElseThrow();
                InvestigationLife.interview(town,c.id,someone);
                InvestigationLife.accuse(town,c.id,c.offenderId);
                db.save(clock,List.of(),List.of(),town,List.of(),List.of(),List.of());
            }
            try(var db=new WorldDatabase(file)) {
                var clock=db.load();var town=db.loadTown(clock);
                var c=town.cases.get("case-0001");
                check(c.solved,"case resolution persisted");
                check(c.interviews.size()==2,"two interview notes persisted");
                check(db.townEvents(20).stream().anyMatch(e->e.text().contains("disappeared")),
                      "authoritative incident persists independently");
            }
            System.out.println("InvestigationDatabaseSmokeTest: ALL CHECKS PASSED");
        } finally {Files.deleteIfExists(file);}
    }
    private static void check(boolean ok,String label){if(!ok)throw new AssertionError("Failed: "+label);}
}
