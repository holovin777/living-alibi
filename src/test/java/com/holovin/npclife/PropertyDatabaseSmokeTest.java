package com.holovin.npclife;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** SQLite round-trip including migration, item custody, incident linking, and no repeated audit rows. */
public final class PropertyDatabaseSmokeTest {
    private PropertyDatabaseSmokeTest() { }
    private static void check(boolean ok,String message) {
        if(!ok)throw new AssertionError("Failed: "+message);
    }
    public static void main(String[] args) throws Exception {
        Path path=Files.createTempFile("living-alibi-property-",".db");
        String stolenId;int initialSize;int baselineHistory;
        try {
            try(var db=new WorldDatabase(path)) {
                var clock=db.load();var town=db.loadTown(clock);
                PopulationGenerator.add(town,3);
                db.save(clock,List.of(),List.of(),town,List.of(),List.of(),List.of());
                initialSize=town.items.size();
                // An incident pre-dating property simulation has no defensible physical-item link.
                town.cases.put("case-0001",new InvestigationLife.CaseFile(
                        "case-0001",7,1050,TownLife.Place.PARK,
                        "elena","marco","wallet",15));
                baselineHistory=db.propertyChanges(100).size();
                check(baselineHistory==initialSize,"all starter items have an initialization record");
                var offender=town.get("marco");var victim=town.get("elena");
                clock.day=8;clock.minute=1050;
                for(var p:town.people.values()) {
                    p.place=TownLife.Place.PARK;p.destination=p.place;p.travelRemaining=0;
                    p.activity="FREE_TIME";p.money=100;
                }
                var worldEvents=new ArrayList<TownLife.Event>();
                var witnesses=new ArrayList<TownLife.Observation>();
                var c=InvestigationLife.recordIncident(town,clock,TownLife.Place.PARK,
                        offender,victim,worldEvents,witnesses);
                stolenId=c.physicalItemId;
                db.save(clock,List.of(),List.of(),town,worldEvents,witnesses,List.of());
                check(db.propertyChanges(100).size()==baselineHistory+1,"one theft journal row added");
            }
            try(var db=new WorldDatabase(path)) {
                var clock=db.load();var town=db.loadTown(clock);
                check(town.items.size()==initialSize,"inventory survives opening the database");
                check(town.pendingProperty.isEmpty(),"restart must not mint starter items again");
                var wallet=town.items.get(stolenId);
                check(wallet!=null && wallet.stolen,"stolen wallet persists after restart");
                check(wallet.ownerId.equals("elena") && wallet.holderId.equals("marco"),
                        "ownership and possession survive SQLite persistence");
                check(town.cases.get("case-0002").physicalItemId.equals(stolenId),
                        "investigation case retains physical item link");
                check(town.cases.get("case-0001").physicalItemId==null,
                        "historic cases must not gain invented item links");
                db.save(clock,List.of(),List.of(),town,List.of(),List.of(),List.of());
                check(db.propertyChanges(100).size()==baselineHistory+1,
                        "re-saving must not duplicate inventory or theft history");
            }
            System.out.println("PropertyDatabaseSmokeTest: ALL CHECKS PASSED");
        } finally {
            Files.deleteIfExists(path);
        }
    }
}
