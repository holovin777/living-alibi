package com.holovin.npclife;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Workshop company keeps its individual owner, real employee and money on disk. */
public final class BusinessDatabaseSmokeTest {
    private BusinessDatabaseSmokeTest() { }

    public static void main(String[] args) throws Exception {
        Path dir=Files.createTempDirectory("living-alibi-entrepreneur-");
        Path file=dir.resolve("brand-new-world.db");
        String hired;
        try {
            check(Files.notExists(file),"save file does not exist at fresh launch");
            try(var db=new WorldDatabase(file)) {
                var world=db.load();
                var town=db.loadTown(world);
                var residents=PopulationGenerator.add(town,12);
                var candidate=residents.getFirst();
                candidate.place=TownLife.Place.WORKSHOP;
                candidate.destination=TownLife.Place.WORKSHOP;
                candidate.travelRemaining=0;
                candidate.activity="WORKING";
                hired=candidate.id;
                town.player.money=200;
                town.player.place=TownLife.Place.WORKSHOP;
                town.player.destination=town.player.place;
                BusinessLife.open(town);
                BusinessLife.invest(town,120);
                BusinessLife.machine(town);
                BusinessLife.hire(town,hired,world.day);
                check(town.business.cash==85 &&
                        town.employerFunds.get("equipment-supplier")==25,
                        "new firm paid the machine supplier and an employee");
                db.save(world,List.of(),List.of(),town,List.of(),List.of(),List.of());
            }
            try(var db=new WorldDatabase(file)) {
                var world=db.load();
                var town=db.loadTown(world);
                check(town.business.registered && town.business.ownerIncarnation==1 &&
                        town.business.machine && town.business.cash==85,
                        "founder's company, machinery and bank account reload");
                var employee=town.get(hired);
                check(BusinessLife.isEmployee(town,employee),
                        "the actual citizen retained their independent shop job");
                var truth=new ArrayList<TownLife.Event>();
                var observed=new ArrayList<TownLife.Observation>();
                world.minute=720;
                BusinessLife.payHourly(town,employee,world,truth,observed);
                world.minute=780;
                BusinessLife.payHourly(town,employee,world,truth,observed);
                check(town.business.cash==63 && town.business.paidHours==2,
                        "two hours of real staff wages are paid");
                db.save(world,List.of(),List.of(),town,truth,observed,List.of());
            }
            try(var db=new WorldDatabase(file)) {
                var world=db.load();
                var town=db.loadTown(world);
                check(town.business.paidHours==2 && town.business.lastPayDay==1 &&
                        town.business.cash==63,
                        "mid-shift wages and daily credit survive restart");
                var truth=new ArrayList<TownLife.Event>();
                var observed=new ArrayList<TownLife.Observation>();
                world.minute=BusinessLife.FABRICATE_MINUTE;
                BusinessLife.tick(town,world,truth,observed);
                check(town.business.parts==2 && town.business.cash==59,
                        "shop acquires a real unit of city steel, not phantom ore");
                db.save(world,List.of(),List.of(),town,truth,observed,List.of());
            }
            try(var db=new WorldDatabase(file)) {
                var world=db.load();
                var town=db.loadTown(world);
                check(town.business.parts==2 && town.business.lastProductionDay==1 &&
                        town.technicalMaterials.get("steel")==3,
                        "physical finished goods and consumed steel reload correctly");
                var truth=new ArrayList<TownLife.Event>();
                var observed=new ArrayList<TownLife.Observation>();
                world.minute=BusinessLife.SELL_MINUTE;
                BusinessLife.tick(town,world,truth,observed);
                check(town.business.cash==89 &&
                        town.technicalMaterials.get("spare-parts")==8 &&
                        town.business.parts==0,
                        "municipality pays for two real delivered spare parts");
                db.save(world,List.of(),List.of(),town,truth,observed,List.of());
            }
            try(var db=new WorldDatabase(file)) {
                var world=db.load();
                var town=db.loadTown(world);
                BusinessLife.tick(town,world,new ArrayList<>(),new ArrayList<>());
                check(town.business.cash==89 && town.business.lastSaleDay==1 &&
                        town.technicalMaterials.get("spare-parts")==8,
                        "same-minute reload cannot duplicate profit or finished parts");
                town.player.health=0;
                town.player.alive=false;
                PlayerLife.beginNewLife(town,world.day,world.minute);
                check(!BusinessLife.status(town,world.day).yours() &&
                        town.business.ownerIncarnation==1,
                        "new visitor has not magically inherited ownership");
                db.save(world,List.of(),List.of(),town,List.of(),List.of(),List.of());
            }
            try(var db=new WorldDatabase(file)) {
                var world=db.load();
                var town=db.loadTown(world);
                town.player.place=TownLife.Place.WORKSHOP;
                town.player.destination=town.player.place;
                BusinessLife.open(town);
                check(town.business.ownerIncarnation==2 &&
                        town.business.cash==89 && town.business.machine &&
                        town.player.money==35,
                        "new visitor can pay to acquire old firm with its capital intact");
                db.save(world,List.of(),List.of(),town,List.of(),List.of(),List.of());
            }
            try(var db=new WorldDatabase(file)) {
                var town=db.loadTown(db.load());
                check(town.business.ownerIncarnation==2 && town.business.cash==89 &&
                        town.technicalMaterials.get("spare-parts")==8,
                        "business transfer of ownership persists after a restart");
            }
            System.out.println("BusinessDatabaseSmokeTest: ALL CHECKS PASSED");
        } finally {
            Files.deleteIfExists(file);
            Files.deleteIfExists(dir);
        }
    }

    private static void check(boolean ok,String description) {
        if(!ok)throw new AssertionError(description);
    }
}
