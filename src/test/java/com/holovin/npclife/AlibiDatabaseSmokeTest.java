package com.holovin.npclife;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.sql.DriverManager;

/** v1.9 SQLite regression: incident-time positions and alibi checks survive restart. */
public final class AlibiDatabaseSmokeTest {
    private AlibiDatabaseSmokeTest() { }
    public static void main(String[] args) throws Exception {
        var path=Files.createTempFile("living-alibi-alibis-",".db");
        try {
            try(var db=new WorldDatabase(path)) {
                var clock=db.load();var town=db.loadTown(clock);
                PopulationGenerator.add(town,4);
                clock.day=7;clock.minute=1050;
                for(var person:town.people.values()) {
                    person.place=person.id.equals("marco")||person.id.equals("elena")||
                        person.id.equals("resident-0001")?TownLife.Place.PARK:TownLife.Place.LIBRARY;
                    person.destination=person.place;person.travelRemaining=0;
                    person.activity="FREE_TIME";person.money=80;
                }
                var truth=new ArrayList<TownLife.Event>();
                var observed=new ArrayList<TownLife.Observation>();
                InvestigationLife.recordIncident(town,clock,TownLife.Place.PARK,
                        town.get("marco"),town.get("elena"),truth,observed);
                var c=town.cases.get("case-0001");
                InvestigationLife.investigate(town,c.id);
                InvestigationLife.interview(town,c.id,"resident-0002");
                InvestigationLife.interview(town,c.id,"resident-0003");
                check(InvestigationLife.checkAlibi(town,c.id,"resident-0002").contains("CORROBORATED"),
                    "original corroboration");
                db.save(clock,List.of(),List.of(),town,truth,observed,List.of());
            }
            try(var db=new WorldDatabase(path)) {
                var state=db.load();var town=db.loadTown(state);
                var c=town.cases.get("case-0001");
                check(c!=null,"case roundtripped");
                check(c.incidentSpaces.size()==6,"snapshot roundtripped");
                check(c.fabricColor!=null,"scene evidence roundtripped");
                check(c.alibiChecks.get("resident-0002").contains("CORROBORATED"),
                    "alibi notes persisted");
                check(c.interviews.size()==2,"interviews persisted");
                check(InvestigationLife.checkAlibi(town,c.id,"resident-0002").contains("CORROBORATED"),
                    "verification still possible after restart");
                db.save(state,List.of(),List.of(),town,List.of(),List.of(),List.of());
            }
            try(var db=new WorldDatabase(path)) {
                var town=db.loadTown(db.load());
                check(town.cases.get("case-0001").incidentSpaces.size()==6,
                    "incident positions upsert is idempotent");
            }
            // Simulate the exact pre-v1.9 investigation schema: both new tables and
            // the added column are absent, but a recorded case is present.
            try(var connection=DriverManager.getConnection("jdbc:sqlite:"+path.toAbsolutePath());
                var st=connection.createStatement()) {
                st.execute("DROP TABLE investigation_positions");
                st.execute("DROP TABLE investigation_alibis");
                st.execute("ALTER TABLE investigation_cases DROP COLUMN fabric_color");
            }
            try(var db=new WorldDatabase(path)) {
                var town=db.loadTown(db.load());
                check(town.cases.containsKey("case-0001"),"old-schema investigation survives migration");
                check(town.cases.get("case-0001").fabricColor==null,
                    "legacy case does not invent contemporary scene evidence");
                db.save(db.load(),List.of(),List.of(),town,List.of(),List.of(),List.of());
            }
            System.out.println("AlibiDatabaseSmokeTest: ALL CHECKS PASSED");
        } finally {Files.deleteIfExists(path);}
    }
    private static void check(boolean ok,String label){if(!ok)throw new AssertionError("Failed: "+label);}
}
