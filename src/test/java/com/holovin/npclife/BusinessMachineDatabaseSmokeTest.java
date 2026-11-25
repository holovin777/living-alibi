package com.holovin.npclife;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.List;

/** Older saves migrate without resets, and clock replay cannot duplicate pay. */
public final class BusinessMachineDatabaseSmokeTest {
    private BusinessMachineDatabaseSmokeTest() { }

    public static void main(String[] args) throws Exception {
        Path dir=Files.createTempDirectory("alibi-machine-migration-");
        Path file=dir.resolve("world.db");
        try {
            String workerId;
            try(var db=new WorldDatabase(file)) {
                var clock=db.load();
                var town=db.loadTown(clock);
                var worker=PopulationGenerator.add(town,12).getFirst();
                workerId=worker.id;
                worker.place=TownLife.Place.WORKSHOP;
                worker.destination=worker.place;
                worker.travelRemaining=0;
                worker.activity="WORKING";
                town.player.money=200;
                town.player.place=TownLife.Place.WORKSHOP;
                town.player.destination=town.player.place;
                BusinessLife.open(town);
                BusinessLife.invest(town,120);
                BusinessLife.machine(town);
                BusinessLife.hire(town,workerId,1);
                clock.minute=720;
                BusinessLife.payHourly(town,worker,clock,new ArrayList<>(),new ArrayList<>());
                clock.minute=780;
                BusinessLife.payHourly(town,worker,clock,new ArrayList<>(),new ArrayList<>());
                check(town.business.paidHours==2 && town.business.cash==63,
                        "two old-style paid hours have been persisted");
                db.save(clock,List.of(),List.of(),town,List.of(),List.of(),List.of());
            }
            // Mimic a REAL existing v1.16 file predating this new migration,
            // rather than requiring a user's old save as a test fixture.
            try(var connection=DriverManager.getConnection("jdbc:sqlite:"+file);
                    var st=connection.createStatement()) {
                // Re-create the real old DDL including the PRIMARY KEY.
                // CREATE TABLE AS SELECT would incorrectly drop constraints,
                // invalidating the production ON CONFLICT save path.
                st.executeUpdate("""
                        CREATE TABLE previous_business (
                            id INTEGER PRIMARY KEY CHECK(id=1),
                            registered INTEGER NOT NULL DEFAULT 0,
                            owner_incarnation INTEGER NOT NULL DEFAULT 0,
                            cash INTEGER NOT NULL DEFAULT 0 CHECK(cash>=0),
                            machine INTEGER NOT NULL DEFAULT 0,
                            employee_id TEXT,
                            spare_parts INTEGER NOT NULL DEFAULT 0 CHECK(spare_parts>=0),
                            last_pay_day INTEGER NOT NULL DEFAULT 0,
                            paid_hours INTEGER NOT NULL DEFAULT 0,
                            last_production_day INTEGER NOT NULL DEFAULT 0,
                            last_sale_day INTEGER NOT NULL DEFAULT 0)
                        """);
                st.executeUpdate("""
                        INSERT INTO previous_business
                        SELECT id,registered,owner_incarnation,cash,machine,
                               employee_id,spare_parts,last_pay_day,paid_hours,
                               last_production_day,last_sale_day
                        FROM player_business
                        """);
                st.executeUpdate("DROP TABLE player_business");
                st.executeUpdate("ALTER TABLE previous_business RENAME TO player_business");
            }
            try(var db=new WorldDatabase(file)) {
                var clock=db.load();
                var town=db.loadTown(clock);
                var worker=town.get(workerId);
                check(town.business.machineCondition==100 &&
                        town.business.lastPayMinute==780 &&
                        town.business.paidHours==2,
                        "additive migration restores existing equipment and hourly pay marker");
                int employeeCash=worker.money;
                BusinessLife.payHourly(town,worker,clock,new ArrayList<>(),new ArrayList<>());
                check(town.business.cash==63 && worker.money==employeeCash,
                        "replaying a paid hour after old-save migration does not mint money");
                clock.minute=BusinessLife.FABRICATE_MINUTE;
                BusinessLife.tick(town,clock,new ArrayList<>(),new ArrayList<>());
                check(town.business.machineCondition==85 && town.business.parts==2 &&
                        town.business.cash==59,
                        "migrated machine can fabricate real stock and incur wear");
                db.save(clock,List.of(),List.of(),town,List.of(),List.of(),List.of());
            }
            try(var db=new WorldDatabase(file)) {
                var clock=db.load();
                var town=db.loadTown(clock);
                check(town.business.machineCondition==85 &&
                        town.business.cash==59 && town.business.parts==2 &&
                        town.business.lastPayMinute==780,
                        "equipment wear, product inventory and wage stamp survive SQLite restart");
                String result=BusinessLife.service(town);
                check(result.contains("100/100") &&
                        town.business.cash==49 &&
                        town.technicalMaterials.get("spare-parts")==4,
                        "owner buys true repair parts and restores worn equipment");
                db.save(clock,List.of(),List.of(),town,List.of(),List.of(),List.of());
            }
            try(var db=new WorldDatabase(file)) {
                var town=db.loadTown(db.load());
                check(town.business.machineCondition==100 &&
                        town.business.cash==49 &&
                        town.business.parts==2 &&
                        town.technicalMaterials.get("spare-parts")==4 &&
                        town.employerFunds.get("city-maintenance")==529,
                        "fully repaired machine, physical parts and company costs are persistent");
            }
            System.out.println("BusinessMachineDatabaseSmokeTest: ALL CHECKS PASSED");
        } finally {
            Files.deleteIfExists(file);
            Files.deleteIfExists(dir);
        }
    }

    private static void check(boolean condition,String description) {
        if(!condition)throw new AssertionError(description);
    }
}
