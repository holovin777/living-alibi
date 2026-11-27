package com.holovin.npclife;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/** SQLite regression for pre-v1.12 migrations and exactly-once life event history. */
public final class EmploymentDatabaseSmokeTest {
    private EmploymentDatabaseSmokeTest() { }
    public static void main(String[] args) throws Exception {
        var path=Files.createTempFile("living-alibi-employment-",".db");
        try {
            // A legacy town table deliberately has none of v1.12's career columns.
            try(var old=java.sql.DriverManager.getConnection("jdbc:sqlite:"+path.toAbsolutePath());
                var st=old.createStatement()) {
                st.execute("""
                    CREATE TABLE town_people (
                        id TEXT PRIMARY KEY,place TEXT NOT NULL,destination TEXT NOT NULL,
                        travel_remaining INTEGER NOT NULL,activity TEXT NOT NULL,
                        hunger INTEGER NOT NULL,thirst INTEGER NOT NULL,fatigue INTEGER NOT NULL,
                        health INTEGER NOT NULL,money INTEGER NOT NULL,food INTEGER NOT NULL,
                        water INTEGER NOT NULL,age INTEGER NOT NULL,alive INTEGER NOT NULL)
                    """);
                st.execute("""
                    INSERT INTO town_people VALUES ('marco','BAKERY','BAKERY',0,'WORKING',20,20,20,100,55,3,3,36,1)
                    """);
            }
            try(var db=new WorldDatabase(path)) {
                var state=db.load();var town=db.loadTown(state);
                check(town.get("marco").employed,"old resident retains employment by default");
                check(db.residentLifeEvents(null,20).isEmpty(),"no fabricated historical life events");
                PopulationGenerator.add(town,1);
                var c=town.get("resident-0001");c.money=25;
                EmploymentLife.loseJob(town,state,c);
                EmploymentLife.startIllness(town,state,c,3,150,new ArrayList<>(),new ArrayList<>());
                db.save(state,List.of(),List.of(),town,List.of(),List.of());
                check(db.residentLifeEvents(null,20).size()==3,"loss, illness and medical expense inserted");
                db.save(state,List.of(),List.of(),town,List.of(),List.of());
                check(db.residentLifeEvents(null,20).size()==3,"no duplicate events on re-save");
            }
            try(var db=new WorldDatabase(path)) {
                var state=db.load();var town=db.loadTown(state);
                var c=town.get("resident-0001");
                check(!c.employed,"unemployment survives restart");
                check(c.sickUntilDay==state.day+2,"sick leave survives restart");
                check(c.unpaidBills>0,"medical arrears survive restart");
                check(db.residentLifeEvents(c.id,20).size()==3,"personal life event filtering");
                EmploymentLife.hire(town,state,c,4);
                db.save(state,List.of(),List.of(),town,List.of(),List.of());
            }
            try(var db=new WorldDatabase(path)) {
                var town=db.loadTown(db.load());
                var c=town.get("resident-0001");
                check(c.employed && c.occupation.equals("Office clerk") &&
                        c.workplace==TownLife.Place.OFFICE,"new occupation and workplace survive restart");
                check(c.jobChanges==1,"career change count persisted");
                check(db.residentLifeEvents(null,20).size()==4,"hiring recorded once");
                workplaceRoundTrip(town,c,db);
            }
            try(var db=new WorldDatabase(path)) {
                var town=db.loadTown(db.load());
                var c=town.get("resident-0001");
                check(WorkplacePerformance.report(town,c).samples()==1,"performance survives restart");
                check(town.industryStocks.getOrDefault(WorkplaceManagement.teamKey(c,"briefing"),0)==10,
                    "workplace daily management marker survives restart");
            }
            System.out.println("EmploymentDatabaseSmokeTest: ALL CHECKS PASSED");
        } finally {Files.deleteIfExists(path);}
    }
    private static void workplaceRoundTrip(TownLife.Town town,TownLife.Citizen c,WorldDatabase db) throws Exception {
        var state=db.load();
        state.day=10;state.minute=600;
        c.place=c.workplace;c.destination=c.workplace;c.travelRemaining=0;c.activity="WORKING";c.sickUntilDay=0;
        WorkplacePerformance.tick(town,state);
        check(WorkplacePerformance.report(town,c).samples()==1,"workplace sample recorded");
        town.industryStocks.put(WorkplaceManagement.teamKey(c,"briefing"),10);
        db.save(state,List.of(),List.of(),town,List.of(),List.of());
    }
    private static void check(boolean condition,String message) {
        if(!condition)throw new AssertionError("Failed: "+message);
    }
}
