package com.holovin.npclife;

import java.util.ArrayList;

/** New industrial salaries are transfers, not invented money. */
public final class IndustryEconomySmokeTest {
    private IndustryEconomySmokeTest() { }

    public static void main(String[] args) {
        var world=new WorldState();
        var town=TownLife.create(world);
        var player=town.player;
        long initialCash=allCash(town);
        check(CareerLife.offers().stream().anyMatch(o->o.id().equals("miner")) &&
                CareerLife.offers().stream().anyMatch(o->o.id().equals("machinist")),
                "quarry and factory are freely chosen player occupations");

        player.place=TownLife.Place.QUARRY;
        player.destination=player.place;
        CareerLife.apply(town,"miner");
        boolean denied=false;
        try {CareerLife.work(town,1,450);}
        catch(IllegalArgumentException expected) {denied=true;}
        check(denied,"night and early arrival cannot pay a miner");

        var before=IndustryLife.report(town);
        String mining=CareerLife.work(town,1,540);
        check(mining.contains("Extracted 1 iron and 1 copper") &&
                player.money==80 &&
                town.industryStocks.get("quarry:iron-ore")==1 &&
                town.industryStocks.get("quarry:copper-ore")==1,
                "miner's two-hour paid shift produces physical ore");
        check(IndustryLife.report(town).ironRemaining()==before.ironRemaining()-1 &&
                IndustryLife.report(town).copperRemaining()==before.copperRemaining()-1,
                "player mining consumes finite geological reserves");
        check(town.employerFunds.get("miner")==330 &&
                allCash(town)==initialCash,
                "player wages transfer from the same quarry budget");

        denied=false;
        try {CareerLife.work(town,1,780);}
        catch(IllegalArgumentException expected) {denied=true;}
        check(denied && player.money==80,
                "same industrial employee cannot claim a second wage today");

        int mineCash=town.employerFunds.get("miner");
        String survey=IndustryLife.prospectPlayer(town,600);
        check(survey.contains("Western") &&
                town.employerFunds.get("miner")==mineCash-30 &&
                town.employerFunds.get("survey-office")==30 &&
                allCash(town)==initialCash,
                "paid geology survey conserves money between enterprise accounts");
        denied=false;
        try {IndustryLife.prospectPlayer(town,600);}
        catch(IllegalArgumentException expected) {denied=true;}
        check(denied,"a discovered mineral seam cannot be discovered twice");

        // A plant worker is paid only if ore or recyclable feedstock exists.
        player.place=TownLife.Place.FACTORY;
        player.destination=player.place;
        CareerLife.apply(town,"machinist");
        int scrap=town.industryStocks.get("factory:scrap-metal");
        String production=CareerLife.work(town,2,600);
        check(production.contains("Recycled 2 finite scrap") &&
                town.industryStocks.get("factory:steel")==1 &&
                town.industryStocks.get("factory:scrap-metal")==scrap-2,
                "factory player shift recycles bounded waste into useful metal");
        check(player.money==114 && town.employerFunds.get("machinist")==526 &&
                allCash(town)==initialCash,
                "machinist wage is conserved across real factory and player cash");

        // No wages or output if an employer is insolvent.
        town.employerFunds.put("machinist",0);
        player.lastWorkDay=2;
        denied=false;
        try {CareerLife.work(town,3,600);}
        catch(IllegalArgumentException expected) {denied=true;}
        check(denied && player.money==114,
                "an insolvent enterprise cannot print salary");
        // Direct company wage check is also conservative.
        var worker=PopulationGenerator.add(town,8).stream()
                .filter(c->c.occupation.equals("Factory machinist"))
                .findFirst().orElseThrow();
        worker.place=TownLife.Place.FACTORY;
        worker.destination=worker.place;
        worker.activity="WORKING";
        worker.travelRemaining=0;
        int beforeWorker=worker.money;
        IndustryLife.payHourly(town,worker,world,new ArrayList<>(),new ArrayList<>());
        check(worker.money==beforeWorker,
                "NPC factory worker receives no invented money when the company is broke");

        System.out.println("IndustryEconomySmokeTest: ALL CHECKS PASSED");
    }

    private static long allCash(TownLife.Town town) {
        long cash=town.player.money;
        for(int account:town.employerFunds.values())cash+=account;
        for(var citizen:town.people.values())cash+=citizen.money;
        return cash;
    }

    private static void check(boolean ok,String message) {
        if(!ok)throw new AssertionError(message);
    }
}
