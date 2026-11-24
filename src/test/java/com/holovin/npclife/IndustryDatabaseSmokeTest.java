package com.holovin.npclife;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** New SQLite saves retain industrial ore, finished goods and production stamps. */
public final class IndustryDatabaseSmokeTest {
    private IndustryDatabaseSmokeTest() { }

    public static void main(String[] args) throws Exception {
        Path directory=Files.createTempDirectory("living-alibi-industry-");
        Path file=directory.resolve("world.db");
        try {
            check(!Files.exists(file),"brand-new launch needs no previous city database");
            var world=new WorldState();
            try(var db=new WorldDatabase(file)) {
                world=db.load();
                var town=db.loadTown(world);
                var citizens=PopulationGenerator.add(town,12);
                for(var c:citizens) {
                    if(!c.occupation.equals("Miner") &&
                            !c.occupation.equals("Factory machinist"))continue;
                    c.place=c.workplace;c.destination=c.workplace;
                    c.travelRemaining=0;c.activity="WORKING";c.employed=true;
                }
                world.minute=IndustryLife.EXTRACT_MINUTE;
                IndustryLife.tick(town,world,new ArrayList<>(),new ArrayList<>());
                check(town.industryStocks.get("quarry:iron-ore")==2,
                        "real ore was extracted");
                db.save(world,List.of(),List.of(),town,List.of(),List.of(),List.of());
            }
            try(var db=new WorldDatabase(file)) {
                world=db.load();
                var town=db.loadTown(world);
                check(town.industryStocks.get("seam:iron-ore")==78 &&
                        town.industryStocks.get("quarry:iron-ore")==2 &&
                        town.lastIndustryExtractDay==1,
                        "extracted mineral and daily receipt survive restart");
                IndustryLife.tick(town,world,new ArrayList<>(),new ArrayList<>());
                check(town.industryStocks.get("seam:iron-ore")==78,
                        "repeating the saved extraction minute cannot mint new ore");
                for(int minute:new int[]{IndustryLife.TRANSPORT_MINUTE,
                        IndustryLife.REFINE_MINUTE,IndustryLife.FABRICATE_MINUTE,
                        IndustryLife.DELIVER_MINUTE}) {
                    world.minute=minute;
                    IndustryLife.tick(town,world,new ArrayList<>(),new ArrayList<>());
                }
                check(town.technicalMaterials.get("spare-parts")==8 &&
                        town.technicalMaterials.get("steel")==5 &&
                        town.technicalMaterials.get("copper")==4,
                        "city maintenance inventory receives physical manufactured goods");
                db.save(world,List.of(),List.of(),town,List.of(),List.of(),List.of());
            }
            try(var db=new WorldDatabase(file)) {
                world=db.load();
                var town=db.loadTown(world);
                check(town.industryStocks.get("seam:iron-ore")==78 &&
                        town.industryStocks.get("seam:copper-ore")==39,
                        "finite underground seams remain depleted");
                check(town.lastIndustryDeliverDay==1 &&
                        town.technicalMaterials.get("spare-parts")==8,
                        "shipment and daily delivery receipt survive restart");
                IndustryLife.tick(town,world,new ArrayList<>(),new ArrayList<>());
                check(town.technicalMaterials.get("spare-parts")==8,
                        "same-minute reload cannot ship components a second time");
                town.player.health=0;town.player.alive=false;
                PlayerLife.beginNewLife(town,world.day,world.minute);
                db.save(world,List.of(),List.of(),town,List.of(),List.of(),List.of());
            }
            try(var db=new WorldDatabase(file)) {
                var town=db.loadTown(db.load());
                check(town.player.incarnation==2 && town.player.jobId==null,
                        "new player gets independent life and career");
                check(town.technicalMaterials.get("spare-parts")==8 &&
                        town.industryStocks.get("seam:iron-ore")==78 &&
                        town.lastIndustryDeliverDay==1,
                        "old city factories and stock remain intact when player dies");
            }
            System.out.println("IndustryDatabaseSmokeTest: ALL CHECKS PASSED");
        } finally {
            Files.deleteIfExists(file);
            Files.deleteIfExists(directory);
        }
    }

    private static void check(boolean ok,String label) {
        if(!ok)throw new AssertionError(label);
    }
}
