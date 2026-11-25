package com.holovin.npclife;

import java.util.ArrayList;

/** The shop produces on ordinary NPC schedules, not just manually set activity. */
public final class BusinessIntegrationSmokeTest {
    private BusinessIntegrationSmokeTest() { }

    public static void main(String[] args) {
        var clock=new WorldState();
        var town=TownLife.create(clock);
        var residents=PopulationGenerator.add(town,12);
        check(residents.stream().anyMatch(c->c.occupation.equals("Mechanic") &&
                        c.workplace==TownLife.Place.WORKSHOP),
                "first launch has a living workshop mechanic available during actual work hours");
        var recruit=residents.getFirst();
        recruit.place=TownLife.Place.WORKSHOP;
        recruit.destination=TownLife.Place.WORKSHOP;
        recruit.travelRemaining=0;
        town.player.money=200;
        town.player.place=TownLife.Place.WORKSHOP;
        town.player.destination=TownLife.Place.WORKSHOP;
        BusinessLife.open(town);
        BusinessLife.invest(town,120);
        BusinessLife.machine(town);
        BusinessLife.hire(town,recruit.id,clock.day);
        int workerBefore=recruit.money;

        var events=new ArrayList<TownLife.Event>();
        var observations=new ArrayList<TownLife.Observation>();
        LifeSimulation.advance(clock,390,
                minute->TownLife.tick(town,minute,events,observations));
        check(clock.minute==840 && clock.day==1,
                "autonomous simulation reached 14:00 on the first working day");
        check(recruit.alive && BusinessLife.isEmployee(town,recruit),
                "actual resident remained the shop's employed worker");
        check(town.business.lastPayDay==1 && town.business.paidHours==2 &&
                town.business.lastPayMinute==780,
                "independent resident showed up and worked two paid hours");
        check(recruit.money>=workerBefore+22,
                "autonomous worker actually received wages from the company");
        check(town.business.lastProductionDay==1 && town.business.lastSaleDay==1 &&
                town.business.parts==0 && town.business.machineCondition==85,
                "worker made and sold physical parts and wore down the machine");
        check(town.business.cash==89 &&
                town.technicalMaterials.get("spare-parts")==8,
                "normal NPC life generated exactly one paid municipal order");
        check(events.stream().anyMatch(e->e.actorId().equals(recruit.id) &&
                        e.text().contains("fabricated 2 spare parts")),
                "production is observable to residents physically nearby");
        check(events.stream().anyMatch(e->e.actorId().equals(recruit.id) &&
                        e.text().contains("Local workshop sold")),
                "real worker is the actor of the sale, not a hidden global narrator");

        System.out.println("BusinessIntegrationSmokeTest: ALL CHECKS PASSED");
    }

    private static void check(boolean condition,String message) {
        if(!condition)throw new AssertionError(message);
    }
}
