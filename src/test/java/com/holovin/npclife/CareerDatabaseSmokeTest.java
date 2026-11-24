package com.holovin.npclife;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Zero-save startup and SQLite restart retain chosen jobs, study, wages and employers. */
public final class CareerDatabaseSmokeTest {
    private CareerDatabaseSmokeTest() { }

    public static void main(String[] args) throws Exception {
        Path dir=Files.createTempDirectory("living-alibi-careers-");
        Path file=dir.resolve("new-world.db");
        try {
            check(!Files.exists(file),"no prior save");
            try(var db=new WorldDatabase(file)) {
                var state=db.load();
                var town=db.loadTown(state);
                check(town.player.place==TownLife.Place.PARK &&
                        town.player.money==50 && town.player.jobId==null,
                        "new visitor has starter cash but no assigned profession");
                town.player.place=TownLife.Place.MARKET;
                town.player.destination=TownLife.Place.MARKET;
                CareerLife.apply(town,"shop-assistant");
                CareerLife.work(town,state.day,540);
                town.player.place=TownLife.Place.SCHOOL;
                town.player.destination=TownLife.Place.SCHOOL;
                CareerLife.study(town,"engineering",660);
                db.save(state,List.of(),List.of(),town,List.of(),List.of(),List.of());
            }
            try(var db=new WorldDatabase(file)) {
                var state=db.load();
                var town=db.loadTown(state);
                check(town.player.money==64 && town.player.engineeringSkill==1 &&
                        "shop-assistant".equals(town.player.jobId),
                        "career and wallet survive disk restart");
                check(town.employerFunds.get("shop-assistant")==456 &&
                        town.employerFunds.get("school")==210,
                        "wages and tuition conserve institutional money across restarts");
                town.player.place=TownLife.Place.MARKET;
                town.player.destination=TownLife.Place.MARKET;
                boolean rejected=false;
                try {CareerLife.work(town,state.day,780);}
                catch(IllegalArgumentException expected) {rejected=true;}
                check(rejected && town.player.money==64,
                        "restarting cannot pay for a second shift that day");
                CareerLife.work(town,state.day+1,600);
                db.save(state,List.of(),List.of(),town,List.of(),List.of(),List.of());
            }
            try(var db=new WorldDatabase(file)) {
                var state=db.load();
                var town=db.loadTown(state);
                check(town.player.money==88 && town.player.lastWorkDay==2,
                        "additional shift preserved after another restart");
                check(town.employerFunds.get("shop-assistant")==432,
                        "employer funds decreased once per shift");
                town.player.health=0;
                town.player.alive=false;
                PlayerLife.beginNewLife(town,state.day,state.minute);
                db.save(state,List.of(),List.of(),town,List.of(),List.of(),List.of());
            }
            try(var db=new WorldDatabase(file)) {
                var town=db.loadTown(db.load());
                check(town.player.incarnation==2 && town.player.money==50 &&
                        town.player.jobId==null && town.player.engineeringSkill==0,
                        "successor arrives in same world without inherited occupation");
                check(town.employerFunds.get("shop-assistant")==432,
                        "new visitor does not magically refill company payroll");
                check(town.pastLives.containsKey(1),"previous life remembered");
            }
            System.out.println("CareerDatabaseSmokeTest: ALL CHECKS PASSED");
        } finally {
            Files.deleteIfExists(file);
            Files.deleteIfExists(dir);
        }
    }

    private static void check(boolean ok,String what) {
        if(!ok)throw new AssertionError(what);
    }
}
