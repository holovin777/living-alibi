package com.holovin.npclife;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** v1.13 SQLite migration and persistent seizure/search/release chain of custody. */
public final class RecoveryDatabaseSmokeTest {
    private RecoveryDatabaseSmokeTest() { }
    private static void check(boolean ok,String label) {
        if(!ok)throw new AssertionError("Failed: "+label);
    }
    public static void main(String[] args) throws Exception {
        Path path=Files.createTempFile("living-alibi-recovery-",".db");
        String itemId;String caseId;
        int originalChanges;
        try {
            try(var db=new WorldDatabase(path)) {
                var clock=db.load();var town=db.loadTown(clock);
                PopulationGenerator.add(town,3);
                db.save(clock,List.of(),List.of(),town,List.of(),List.of(),List.of());
                originalChanges=db.propertyChanges(100).size();
                // Historic v1.8 case must not be backfilled with a fake item record.
                town.cases.put("case-0001",new InvestigationLife.CaseFile(
                        "case-0001",1,1040,TownLife.Place.PARK,"elena","marco","wallet",15));
                clock.day=2;clock.minute=1050;
                for(var p:town.people.values()) {
                    p.place=TownLife.Place.PARK;p.destination=p.place;p.travelRemaining=0;
                    p.activity="FREE_TIME";p.money=100;
                }
                var c=InvestigationLife.recordIncident(town,clock,TownLife.Place.PARK,
                        town.get("marco"),town.get("elena"),new ArrayList<>(),new ArrayList<>());
                caseId=c.id;itemId=c.physicalItemId;
                InvestigationLife.investigate(town,c.id);
                InvestigationLife.interview(town,c.id,"marco");
                InvestigationLife.interview(town,c.id,"elena");
                String result=InvestigationLife.search(town,clock,c.id,"marco");
                check(result.startsWith("MATCH:"),"physical item seized in fixture");
                db.save(clock,List.of(),List.of(),town,List.of(),List.of(),List.of());
                check(db.propertyChanges(100).size()==originalChanges+2,"theft and seizure audited");
            }
            try(var db=new WorldDatabase(path)) {
                var clock=db.load();var town=db.loadTown(clock);
                var c=town.cases.get(caseId);
                check(c.searches.size()==1 && c.searches.get("marco").startsWith("MATCH:"),
                        "investigative search survives restart");
                check(town.items.get(itemId).storedSpace.equals("evidence:"+caseId),
                        "item stays in police custody after restart");
                check(town.cases.get("case-0001").physicalItemId==null,
                        "historic case remains unlinked to a physical item");
                check(InvestigationLife.accuse(town,c.id,"marco").startsWith("Case solved:"),
                        "can close preserved case");
                check(InvestigationLife.returnItem(town,clock,c.id).contains("returned to"),
                        "return after valid resolution");
                db.save(clock,List.of(),List.of(),town,List.of(),List.of(),List.of());
                check(db.propertyChanges(100).size()==originalChanges+3,"return adds one audit entry");
                db.save(clock,List.of(),List.of(),town,List.of(),List.of(),List.of());
                check(db.propertyChanges(100).size()==originalChanges+3,"repeat save adds no duplicates");
            }
            try(var db=new WorldDatabase(path)) {
                var clock=db.load();var town=db.loadTown(clock);
                var wallet=town.items.get(itemId);
                check(!wallet.stolen && wallet.holderId.equals("elena"),
                        "returned custody and title survive second restart");
                check(town.cases.get(caseId).solved,"resolution persists");
                check(town.cases.get(caseId).returned,"return record survives restart");
                check(town.cases.get(caseId).searches.size()==1,"search history not duplicated");
                check(town.pendingProperty.isEmpty(),"reopening does not invent changes");
            }
            System.out.println("RecoveryDatabaseSmokeTest: ALL CHECKS PASSED");
        } finally {
            Files.deleteIfExists(path);
        }
    }
}
