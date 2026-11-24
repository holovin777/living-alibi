package com.holovin.npclife;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Fresh-save public utilities stay finite, consistent, and independent of visitor lives. */
public final class UtilityDatabaseSmokeTest {
    private UtilityDatabaseSmokeTest() { }

    public static void main(String[] args) throws Exception {
        Path dir=Files.createTempDirectory("living-alibi-utilities-");
        Path file=dir.resolve("world.db");
        try {
            check(!Files.exists(file),"blank first launch needs no old player or save");
            try(var db=new WorldDatabase(file)) {
                var state=db.load();
                var town=db.loadTown(state);
                check(town.cleanWater==0 && town.lastWaterPumpDay==0,
                        "fresh town has no fabricated treated water");
                town.player.place=TownLife.Place.WORKSHOP;
                town.player.destination=town.player.place;
                town.player.engineeringSkill=3;
                CareerLife.apply(town,"technician");
                TechnicalWorkLife.inspect(town,"water-pump",540);
                TechnicalWorkLife.complete(town,"water-pump",1,570);
                db.save(state,List.of(),List.of(),town,List.of(),List.of(),List.of());
                check(Files.isRegularFile(file),"SQLite created automatically");
            }
            try(var db=new WorldDatabase(file)) {
                var state=db.load();
                var town=db.loadTown(state);
                check(town.cleanWater==6 && UtilityLife.status(town).fountainWorking(),
                        "pump state and six primed water units survive restart");
                check(town.naturalResources.get("river-water").stock==114,
                        "finite river extraction persisted with tank filling");
                town.player.place=TownLife.Place.PARK;
                town.player.destination=town.player.place;
                UtilityLife.drinkAtFountain(town);
                check(town.cleanWater==5,"park visitor uses one unit");
                db.save(state,List.of(),List.of(),town,List.of(),List.of(),List.of());
            }
            try(var db=new WorldDatabase(file)) {
                var state=db.load();
                var town=db.loadTown(state);
                check(town.cleanWater==5 && town.lastWaterPumpDay==0,
                        "loading cannot duplicate the pump's initial commissioning");
                state.day=2;
                state.minute=0;
                UtilityLife.tick(town,state,new ArrayList<>(),new ArrayList<>());
                check(town.cleanWater==9 && town.lastWaterPumpDay==2,
                        "once-per-day refill transfers four river units");
                check(town.naturalResources.get("river-water").stock==110,
                        "new clean water debits finite source stock");
                db.save(state,List.of(),List.of(),town,List.of(),List.of(),List.of());
            }
            try(var db=new WorldDatabase(file)) {
                var state=db.load();
                var town=db.loadTown(state);
                check(town.cleanWater==9 && town.lastWaterPumpDay==2,
                        "tank and last pumping day survive SQLite restart");
                UtilityLife.tick(town,state,new ArrayList<>(),new ArrayList<>());
                check(town.cleanWater==9 &&
                        town.naturalResources.get("river-water").stock==110,
                        "resuming on the same midnight cannot mint more water");
                town.player.health=0;town.player.alive=false;
                PlayerLife.beginNewLife(town,state.day,state.minute);
                db.save(state,List.of(),List.of(),town,List.of(),List.of(),List.of());
            }
            try(var db=new WorldDatabase(file)) {
                var town=db.loadTown(db.load());
                check(town.player.incarnation==2 && town.player.jobId==null,
                        "new visitor is still independent and untrained");
                check(town.cleanWater==9 && UtilityLife.status(town).fountainWorking(),
                        "player death cannot reset the city's water tank or repaired pump");
            }
            System.out.println("UtilityDatabaseSmokeTest: ALL CHECKS PASSED");
        } finally {
            Files.deleteIfExists(file);
            Files.deleteIfExists(dir);
        }
    }

    private static void check(boolean ok,String label) {
        if(!ok)throw new AssertionError(label);
    }
}
