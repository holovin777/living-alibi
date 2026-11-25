package com.holovin.npclife;

import java.util.ArrayList;

/** Real ownership: cash transfers, machines, workers and tangible part output. */
public final class BusinessSmokeTest {
    private BusinessSmokeTest() { }

    public static void main(String[] args) {
        var clock=new WorldState();
        var town=TownLife.create(clock);
        town.player.money=150;
        var generated=PopulationGenerator.add(town,12);
        var candidate=generated.stream()
                .filter(c->c.occupation.equals("Mechanic"))
                .findFirst().orElse(generated.getFirst());
        candidate.place=TownLife.Place.WORKSHOP;
        candidate.destination=candidate.place;
        candidate.travelRemaining=0;
        candidate.activity="WORKING";
        town.player.place=TownLife.Place.WORKSHOP;
        town.player.destination=town.player.place;
        long totalBefore=allCash(town);
        check(!BusinessLife.status(town,1).registered(),
                "fresh world has no company and no assigned owner");
        BusinessLife.open(town);
        check(town.player.money==135 &&
                town.employerFunds.get("city-maintenance")==515,
                "founder pays registration to town, not a money sink");
        BusinessLife.invest(town,100);
        check(town.player.money==35 && town.business.cash==100,
                "company account is separate from the founder's wallet");
        BusinessLife.machine(town);
        check(town.business.machine && town.business.cash==75 &&
                town.employerFunds.get("equipment-supplier")==25,
                "real equipment purchase pays the equipment supplier");
        check(BusinessLife.candidates(town,1).stream()
                        .anyMatch(c->c.id.equals(candidate.id)),
                "a nearby real resident is available for an actual job offer");
        int personalMoney=candidate.money;
        BusinessLife.hire(town,candidate.id,1);
        check(candidate.money==personalMoney+10 && town.business.cash==65 &&
                BusinessLife.isEmployee(town,candidate),
                "signed worker receives a real ten-euro advance");
        check(allCash(town)==totalBefore,
                "registration, investment and employee signing conserve money");

        clock.minute=720;
        BusinessLife.payHourly(town,candidate,clock,new ArrayList<>(),new ArrayList<>());
        clock.minute=780;
        BusinessLife.payHourly(town,candidate,clock,new ArrayList<>(),new ArrayList<>());
        BusinessLife.payHourly(town,candidate,clock,new ArrayList<>(),new ArrayList<>());
        check(town.business.paidHours==2 && town.business.cash==43,
                "replaying the same minute cannot pay a third industrial wage");
        check(town.business.paidHours==2 && town.business.cash==43 &&
                candidate.money==personalMoney+32,
                "one small shift has two honest hourly wages, not eight imaginary hours");

        int steel=town.technicalMaterials.get("steel");
        clock.minute=BusinessLife.FABRICATE_MINUTE;
        BusinessLife.tick(town,clock,new ArrayList<>(),new ArrayList<>());
        check(town.business.parts==2 && town.technicalMaterials.get("steel")==steel-1 &&
                town.business.cash==39 && town.business.machineCondition==85,
                "the business buys and consumes a real city steel unit before fabrication");

        clock.minute=BusinessLife.SELL_MINUTE;
        BusinessLife.tick(town,clock,new ArrayList<>(),new ArrayList<>());
        check(town.business.parts==0 && town.business.cash==69 &&
                town.technicalMaterials.get("spare-parts")==8 &&
                town.employerFunds.get("city-maintenance")==489,
                "city buys real delivered parts using finite budget");
        check(allCash(town)==totalBefore,
                "full payroll, steel and equipment trades conserve all participant cash");
        var serviced=town.copy();
        String maintenance=BusinessLife.service(serviced);
        check(maintenance.contains("100/100") &&
                serviced.business.cash==59 &&
                serviced.business.machineCondition==100 &&
                serviced.technicalMaterials.get("spare-parts")==6 &&
                serviced.employerFunds.get("city-maintenance")==499,
                "maintenance consumes real city components and business cash");
        check(allCash(serviced)==totalBefore &&
                town.business.cash==69 && town.business.machineCondition==85,
                "servicing is a balanced transfer and speculative copy changes nothing");
        BusinessLife.tick(town,clock,new ArrayList<>(),new ArrayList<>());
        check(town.business.cash==69 && town.technicalMaterials.get("spare-parts")==8,
                "same-minute replay cannot duplicate a sale");

        var other=town.copy();
        other.player.money=0;
        BusinessLife.withdraw(other,1);
        check(other.business.cash==68 && other.player.money==1 &&
                town.business.cash==69,
                "speculative branch cannot mutate original company before save");

        BusinessLife.withdraw(town,10);
        check(town.player.money==45 && town.business.cash==59,
                "owner can withdraw real available profit, but not create it");
        boolean rejected=false;
        try {BusinessLife.withdraw(town,1000);}
        catch(IllegalArgumentException expected) {rejected=true;}
        check(rejected,"owner cannot overdraw company reserves");

        candidate.alive=false;
        town.player.health=0;
        town.player.alive=false;
        PlayerLife.beginNewLife(town,clock.day,clock.minute);
        check(town.business.registered && town.business.ownerIncarnation==1 &&
                town.player.incarnation==2 && town.player.place==TownLife.Place.PARK,
                "company and workers survive, but new visitor has no inherited ownership");
        town.player.place=TownLife.Place.WORKSHOP;
        town.player.destination=town.player.place;
        BusinessLife.open(town);
        check(town.business.ownerIncarnation==2 && town.business.machine &&
                town.business.cash==59 && town.player.money==35,
                "new life must pay to acquire existing company without resetting assets");

        System.out.println("BusinessSmokeTest: ALL CHECKS PASSED");
    }

    private static long allCash(TownLife.Town town) {
        long amount=town.player.money+town.business.cash;
        for(var entry:town.employerFunds.values())amount+=entry;
        for(var citizen:town.people.values())amount+=citizen.money;
        return amount;
    }

    private static void check(boolean yes,String detail) {
        if(!yes)throw new AssertionError(detail);
    }
}
