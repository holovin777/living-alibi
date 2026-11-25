package com.holovin.npclife;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** A fresh SQLite world can persist half-finished and completed real work orders. */
public final class TechnicalWorkDatabaseSmokeTest {
    private TechnicalWorkDatabaseSmokeTest() { }

    public static void main(String[] args) throws Exception {
        Path dir=Files.createTempDirectory("living-alibi-work-orders-");
        Path file=dir.resolve("new-game.db");
        try {
            check(!Files.exists(file),"no preexisting database");
            try(var db=new WorldDatabase(file)) {
                var state=db.load();
                var town=db.loadTown(state);
                check(town.technicalOrders.get("water-pump").condition==45,
                        "fresh world has broken pump");
                town.player.engineeringSkill=3;
                town.player.place=TownLife.Place.WORKSHOP;
                town.player.destination=town.player.place;
                CareerLife.apply(town,"technician");
                TechnicalWorkLife.inspect(town,"water-pump",540);
                db.save(state,List.of(),List.of(),town,List.of(),List.of(),List.of());
            }
            try(var db=new WorldDatabase(file)) {
                var state=db.load();
                var town=db.loadTown(state);
                check(town.technicalOrders.get("water-pump").phase==
                        TechnicalWorkLife.Phase.INSPECTED,
                        "inspected state persists on restart");
                check(town.technicalMaterials.get("spare-parts")==6,
                        "inspection does not spend physical stock");
                TechnicalWorkLife.complete(town,"water-pump",state.day,570);
                CareerLife.work(town,state.day,720);
                check(town.player.lastTechnicalDay==1 && town.player.money==88,
                        "real task permits payment");
                db.save(state,List.of(),List.of(),town,List.of(),List.of(),List.of());
            }
            try(var db=new WorldDatabase(file)) {
                var state=db.load();
                var town=db.loadTown(state);
                check(town.technicalOrders.get("water-pump").phase==
                        TechnicalWorkLife.Phase.COMPLETED &&
                        town.technicalOrders.get("water-pump").condition==100,
                        "successful repair remains completed after restart");
                check(town.technicalMaterials.get("spare-parts")==4,
                        "two spare parts consumed exactly once");
                check(town.player.lastTechnicalDay==1 && town.player.lastWorkDay==1,
                        "technical wage credit cannot reset on restart");
                check(town.employerFunds.get("technician")==722,
                        "employer pays once, world stores finite remaining payroll");
                boolean denied=false;
                try {CareerLife.work(town,1,820);}
                catch(IllegalArgumentException ex) {denied=true;}
                check(denied,"cannot claim second wage by reloading");
                db.save(state,List.of(),List.of(),town,List.of(),List.of(),List.of());
            }
            try(var db=new WorldDatabase(file)) {
                var state=db.load();
                var town=db.loadTown(state);
                check(town.technicalMaterials.get("spare-parts")==4 &&
                        town.technicalOrders.get("water-pump").condition==100,
                        "repeated load does not replenish stock or degrade equipment");
                town.player.alive=false;
                town.player.health=0;
                PlayerLife.beginNewLife(town,state.day,state.minute);
                db.save(state,List.of(),List.of(),town,List.of(),List.of(),List.of());
            }
            try(var db=new WorldDatabase(file)) {
                var town=db.loadTown(db.load());
                check(town.player.incarnation==2 && town.player.lastTechnicalDay==0 &&
                        town.player.jobId==null,
                        "new visitor does not inherit old technical qualifications or wage credit");
                check(town.technicalMaterials.get("spare-parts")==4 &&
                        town.technicalOrders.get("water-pump").condition==100,
                        "death does not reset town infrastructure or materials");
            }
            System.out.println("TechnicalWorkDatabaseSmokeTest: ALL CHECKS PASSED");
        } finally {
            Files.deleteIfExists(file);
            Files.deleteIfExists(dir);
        }
    }

    private static void check(boolean ok,String message) {
        if(!ok)throw new AssertionError(message);
    }
}
