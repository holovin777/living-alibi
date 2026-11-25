package com.holovin.npclife;

import java.util.ArrayList;

/** Equipment is finite, actually breaks, and can be repaired with city materials. */
public final class BusinessMachineSmokeTest {
    private BusinessMachineSmokeTest() { }

    public static void main(String[] args) {
        var clock=new WorldState();
        var town=TownLife.create(clock);
        var worker=PopulationGenerator.add(town,12).getFirst();
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
        BusinessLife.hire(town,worker.id,1);
        check(town.business.machineCondition==100,
                "a purchased machine starts in excellent condition");
        town.technicalMaterials.put("steel",20); // Seed a multi-day test quarry stock.
        var events=new ArrayList<TownLife.Event>();
        var seen=new ArrayList<TownLife.Observation>();

        for(int day=1;day<=7;day++) {
            clock.day=day;
            clock.minute=720;
            BusinessLife.payHourly(town,worker,clock,events,seen);
            clock.minute=780;
            BusinessLife.payHourly(town,worker,clock,events,seen);
            int cash=town.business.cash, employeeMoney=worker.money;
            BusinessLife.payHourly(town,worker,clock,events,seen);
            check(town.business.cash==cash && worker.money==employeeMoney &&
                            town.business.paidHours==2,
                    "hourly pay is idempotent even when same minute is ticked twice");
            clock.minute=BusinessLife.FABRICATE_MINUTE;
            BusinessLife.tick(town,clock,events,seen);
            clock.minute=BusinessLife.SELL_MINUTE;
            BusinessLife.tick(town,clock,events,seen);
        }
        check(town.business.machineCondition==10,
                "six production batches wear the machine to a broken state");
        check(town.technicalMaterials.get("steel")==14 &&
                        town.technicalMaterials.get("spare-parts")==18,
                "broken machine stops consuming steel and making new goods");
        check(town.business.lastProductionDay==7,
                "the failed seventh production opportunity cannot be retried for free");
        check(events.stream().filter(e->e.text().contains("fabricated 2 spare parts"))
                        .count()==6,
                "only six physical batches were produced before breakdown");
        int partsBefore=town.technicalMaterials.get("spare-parts");
        int companyBefore=town.business.cash;
        int cityBefore=town.employerFunds.get("city-maintenance");
        String repaired=BusinessLife.service(town);
        check(repaired.contains("70/100") &&
                        town.business.machineCondition==70 &&
                        town.business.cash==companyBefore-10 &&
                        town.employerFunds.get("city-maintenance")==cityBefore+10 &&
                        town.technicalMaterials.get("spare-parts")==partsBefore-2,
                "company pays for and physically consumes two municipal service parts");
        boolean denied=false;
        var insufficient=town.copy();
        insufficient.technicalMaterials.put("spare-parts",0);
        try { BusinessLife.service(insufficient); }
        catch(IllegalArgumentException expected) { denied=true; }
        check(denied && insufficient.business.machineCondition==70,
                "machine cannot be repaired without actual stocked spare parts");

        clock.day=8;
        clock.minute=720;
        BusinessLife.payHourly(town,worker,clock,events,seen);
        clock.minute=780;
        BusinessLife.payHourly(town,worker,clock,events,seen);
        clock.minute=BusinessLife.FABRICATE_MINUTE;
        BusinessLife.tick(town,clock,events,seen);
        check(town.business.machineCondition==55 &&
                        town.business.parts==2 &&
                        town.technicalMaterials.get("steel")==13,
                "repairs restore actual productivity but new production continues to wear metal");
        System.out.println("BusinessMachineSmokeTest: ALL CHECKS PASSED");
    }

    private static void check(boolean valid,String reason) {
        if(!valid)throw new AssertionError(reason);
    }
}
