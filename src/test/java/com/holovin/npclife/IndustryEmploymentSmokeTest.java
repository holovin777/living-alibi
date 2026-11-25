package com.holovin.npclife;

import java.util.ArrayList;
import java.util.List;

/** A bankrupt factory cannot mint wages or imprison workers in unpaid jobs. */
public final class IndustryEmploymentSmokeTest {
    private IndustryEmploymentSmokeTest() { }

    public static void main(String[] args) {
        var clock=new WorldState();
        var town=TownLife.create(clock);
        var people=PopulationGenerator.add(town,12);
        var miner=people.stream().filter(c->c.occupation.equals("Miner"))
                .findFirst().orElseThrow();
        var driver=people.stream().filter(c->c.occupation.equals("Transport driver"))
                .findFirst().orElseThrow();
        var machinist=people.stream().filter(c->c.occupation.equals("Factory machinist"))
                .findFirst().orElseThrow();

        town.employerFunds.put("miner",0);
        town.employerFunds.put("machinist",0);
        clock.minute=390;
        var truth=new ArrayList<TownLife.Event>();
        var observations=new ArrayList<TownLife.Observation>();
        EmploymentLife.tick(town,clock,truth,observations);
        check(!miner.employed && !driver.employed && !machinist.employed,
                "all industrial companies with empty payrolls really lay off staff");
        check(miner.unemploymentDays==1 &&
                driver.unemploymentDays==1 && machinist.unemploymentDays==1,
                "laid-off workers begin a distinct unemployment spell");
        int minerMoney=miner.money;
        int driverMoney=driver.money;
        int factoryMoney=machinist.money;
        IndustryLife.payHourly(town,miner,clock,truth,observations);
        IndustryLife.payHourly(town,driver,clock,truth,observations);
        IndustryLife.payHourly(town,machinist,clock,truth,observations);
        check(miner.money==minerMoney && driver.money==driverMoney &&
                machinist.money==factoryMoney,
                "bankruptcy never creates free industrial wages");

        clock.day=2;
        EmploymentLife.tick(town,clock,truth,observations);
        check(miner.employed && driver.employed && machinist.employed &&
                IndustryLife.companyFor(miner.occupation)==null &&
                IndustryLife.companyFor(driver.occupation)==null &&
                IndustryLife.companyFor(machinist.occupation)==null,
                "former industrial staff can take unrelated paid jobs in the town");
        check(town.employerFunds.get("miner")==0 &&
                town.employerFunds.get("machinist")==0,
                "switching careers does not create new enterprise budget");

        System.out.println("IndustryEmploymentSmokeTest: ALL CHECKS PASSED");
    }

    private static void check(boolean ok,String detail) {
        if(!ok)throw new AssertionError(detail);
    }
}
