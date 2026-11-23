package com.holovin.npclife;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/** SQLite migration, stable contract IDs and exactly-once ledger inserts after restart. */
public final class EconomyDatabaseSmokeTest {
    private EconomyDatabaseSmokeTest() { }
    public static void main(String[] args) throws Exception {
        var path=Files.createTempFile("living-alibi-economy-",".db");
        try {
            // Simulate the essential v1.1 town table before the automatic additive migration.
            try(var legacy=java.sql.DriverManager.getConnection("jdbc:sqlite:"+path.toAbsolutePath());
                var st=legacy.createStatement()) {
                st.execute("""
                    CREATE TABLE town_people (
                        id TEXT PRIMARY KEY, place TEXT NOT NULL, destination TEXT NOT NULL,
                        travel_remaining INTEGER NOT NULL, activity TEXT NOT NULL,
                        hunger INTEGER NOT NULL, thirst INTEGER NOT NULL, fatigue INTEGER NOT NULL,
                        health INTEGER NOT NULL, money INTEGER NOT NULL, food INTEGER NOT NULL,
                        water INTEGER NOT NULL, age INTEGER NOT NULL, alive INTEGER NOT NULL)
                    """);
                st.execute("""
                    INSERT INTO town_people VALUES('marco','BAKERY','BAKERY',0,'WORKING',20,20,20,100,55,3,3,36,1)
                    """);
            }
            try(var db=new WorldDatabase(path)) {
                var state=db.load();var town=db.loadTown(state);
                check(town.get("marco").money==55,"legacy resident migrated without changing cash");
                check(town.loans.isEmpty(),"migration never invents old debts");
                PopulationGenerator.add(town,2);
                var lender=town.get("resident-0001");var borrower=town.get("resident-0002");
                lender.place=TownLife.Place.PARK;lender.destination=lender.place;
                borrower.place=TownLife.Place.PARK;borrower.destination=borrower.place;
                lender.money=350;borrower.money=10;
                var relation=new SocialLife.Relationship(lender.id,borrower.id);
                relation.trust=80;
                town.peerRelationships.put(SocialNetwork.key(lender.id,borrower.id),relation);
                EconomyLife.issueLoan(town,state,lender,borrower,50,new ArrayList<>(),new ArrayList<>());
                EconomyLife.addBill(town,state,borrower,125,"home bill",new ArrayList<>(),new ArrayList<>());
                db.save(state,List.of(),List.of(),town,List.of(),List.of());
                check(db.economyLedger(null,20).size()==3,"loan, bill and bill payment written once");
                db.save(state,List.of(),List.of(),town,List.of(),List.of());
                check(db.economyLedger(null,20).size()==3,"re-saving does not append duplicate entries");
            }
            try(var db=new WorldDatabase(path)) {
                var state=db.load();var town=db.loadTown(state);
                var loan=town.loans.get("loan-0001");
                check(loan!=null && loan.outstanding==50,"outstanding contract restored");
                check(town.get("resident-0002").unpaidBills>0,"household arrears restored");
                check(db.economyLedger("resident-0001",20).size()==1,"actor ledger filter");
                check(db.economyLedger("resident-0002",20).size()==3,"borrower ledger filter");
                town.get("resident-0002").money=200;
                EconomyLife.repay(town,state,loan,50);
                db.save(state,List.of(),List.of(),town,List.of(),List.of());
            }
            try(var db=new WorldDatabase(path)) {
                var town=db.loadTown(db.load());
                check(town.loans.get("loan-0001").status==EconomyLife.Status.PAID,
                        "closed loan survives restart");
                check(db.economyLedger(null,20).size()==4,"exactly one repayment appended");
            }
            System.out.println("EconomyDatabaseSmokeTest: ALL CHECKS PASSED");
        } finally {Files.deleteIfExists(path);}
    }
    private static void check(boolean ok,String label) {if(!ok)throw new AssertionError("Failed: "+label);}
}
