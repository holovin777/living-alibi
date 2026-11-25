package com.holovin.npclife;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Freight remains on the truck until actual arrival; salvage/surveys persist. */
public final class IndustryTransportDatabaseSmokeTest {
    private IndustryTransportDatabaseSmokeTest() { }

    public static void main(String[] args) throws Exception {
        Path dir=Files.createTempDirectory("living-alibi-freight-");
        Path file=dir.resolve("world.db");
        try {
            var clock=new WorldState();
            try(var db=new WorldDatabase(file)) {
                clock=db.load();
                var town=db.loadTown(clock);
                for(var person:PopulationGenerator.add(town,12)) {
                    if(!List.of("Miner","Factory machinist","Transport driver")
                            .contains(person.occupation))continue;
                    person.place=person.workplace;
                    person.destination=person.workplace;
                    person.travelRemaining=0;
                    person.activity="WORKING";
                }
                clock.minute=IndustryLife.EXTRACT_MINUTE;
                IndustryLife.tick(town,clock,new ArrayList<>(),new ArrayList<>());
                clock.minute=IndustryLife.TRANSPORT_MINUTE;
                IndustryLife.tick(town,clock,new ArrayList<>(),new ArrayList<>());
                check(town.industryStocks.get("truck:iron-ore")==2 &&
                        town.industryStocks.get("truck:copper-ore")==1 &&
                        town.industryStocks.get("factory:iron-ore")==0,
                        "physical freight has left the quarry but not reached the factory");
                db.save(clock,List.of(),List.of(),town,List.of(),List.of(),List.of());
            }
            try(var db=new WorldDatabase(file)) {
                clock=db.load();
                var town=db.loadTown(clock);
                check(town.lastIndustryTransportDay==1 &&
                        town.industryStocks.get("truck:iron-ore")==2 &&
                        town.industryStocks.get("truck:copper-ore")==1,
                        "truck cargo and dispatch receipt survive restart");
                IndustryLife.tick(town,clock,new ArrayList<>(),new ArrayList<>());
                check(town.industryStocks.get("truck:iron-ore")==2 &&
                        town.employerFunds.get("machinist")==551,
                        "reloading at departure time cannot pay for ore twice");

                clock.minute=IndustryLife.ARRIVE_MINUTE;
                IndustryLife.tick(town,clock,new ArrayList<>(),new ArrayList<>());
                check(town.lastIndustryArrivalDay==1 &&
                        town.industryStocks.get("truck:iron-ore")==0 &&
                        town.industryStocks.get("factory:iron-ore")==2,
                        "arrival unloads a real truck manifest");
                clock.minute=IndustryLife.REFINE_MINUTE;
                IndustryLife.tick(town,clock,new ArrayList<>(),new ArrayList<>());
                clock.minute=IndustryLife.RECYCLE_MINUTE;
                IndustryLife.tick(town,clock,new ArrayList<>(),new ArrayList<>());
                check(town.industryStocks.get("factory:scrap-metal")==8 &&
                        town.industryStocks.get("factory:steel")==3,
                        "2 historic scrap units recover 1 steel with a real worker");
                clock.minute=IndustryLife.FABRICATE_MINUTE;
                IndustryLife.tick(town,clock,new ArrayList<>(),new ArrayList<>());
                clock.minute=IndustryLife.DELIVER_MINUTE;
                IndustryLife.tick(town,clock,new ArrayList<>(),new ArrayList<>());
                check(town.technicalMaterials.get("spare-parts")==8 &&
                        town.technicalMaterials.get("steel")==5 &&
                        town.technicalMaterials.get("copper")==4,
                        "only paid factory delivery replenishes actual repair warehouse");
                check(town.employerFunds.get("city-maintenance")==481 &&
                        town.employerFunds.get("machinist")==570 &&
                        town.employerFunds.get("miner")==369,
                        "city/factory/quarry cash transfers keep all parties' balances honest");
                db.save(clock,List.of(),List.of(),town,List.of(),List.of(),List.of());
            }
            try(var db=new WorldDatabase(file)) {
                clock=db.load();
                var town=db.loadTown(clock);
                IndustryLife.tick(town,clock,new ArrayList<>(),new ArrayList<>());
                check(town.technicalMaterials.get("spare-parts")==8 &&
                        town.industryStocks.get("factory:scrap-metal")==8,
                        "reopening a completed production minute cannot re-deliver stock");
                town.industryStocks.put("seam:iron-ore",0);
                town.industryStocks.put("seam:copper-ore",0);
                town.player.place=TownLife.Place.QUARRY;
                town.player.destination=TownLife.Place.QUARRY;
                CareerLife.apply(town,"miner");
                int initialBudget=town.employerFunds.get("miner");
                IndustryLife.prospectPlayer(town,600);
                check(town.employerFunds.get("miner")==initialBudget-30 &&
                        town.employerFunds.get("survey-office")==30,
                        "survey has a real payable fee, not invented money");
                db.save(clock,List.of(),List.of(),town,List.of(),List.of(),List.of());
            }
            try(var db=new WorldDatabase(file)) {
                clock=db.load();
                var town=db.loadTown(clock);
                check(IndustryLife.report(town).westDepositDiscovered() &&
                        IndustryLife.report(town).ironRemaining()==30 &&
                        IndustryLife.report(town).copperRemaining()==15,
                        "a newly surveyed finite pocket is remembered between play sessions");
                check(town.industryStocks.get("factory:scrap-metal")==8 &&
                        town.employerFunds.get("survey-office")==30,
                        "materials and survey contract funds both survive restart");
                town.player.health=0;
                town.player.alive=false;
                PlayerLife.beginNewLife(town,clock.day,clock.minute);
                db.save(clock,List.of(),List.of(),town,List.of(),List.of(),List.of());
            }
            try(var db=new WorldDatabase(file)) {
                var town=db.loadTown(db.load());
                check(town.player.incarnation==2 && town.player.jobId==null &&
                        IndustryLife.report(town).westDepositDiscovered() &&
                        town.industryStocks.get("factory:scrap-metal")==8,
                        "new visitor inherits the same physical city, not previous career");
            }
            System.out.println("IndustryTransportDatabaseSmokeTest: ALL CHECKS PASSED");
        } finally {
            Files.deleteIfExists(file);
            Files.deleteIfExists(dir);
        }
    }

    private static void check(boolean ok,String reason) {
        if(!ok)throw new AssertionError(reason);
    }
}
