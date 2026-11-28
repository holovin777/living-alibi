package com.holovin.npclife;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class BusinessOwnershipLifeTest {
    @Test void saleRequiresSuspensionAndFundsAndPreservesLiabilities(){
        var s=new WorldState();s.day=2;s.minute=600;var t=TownLife.create(s);
        PopulationGenerator.add(t,12);t.industryStocks.put(StaffingLife.ENABLED,1);
        var c=t.people.values().stream().filter(x->x.generationIndex>0&&x.alive).findFirst().orElseThrow();
        c.age=35;c.employed=true;c.occupation="Market vendor";c.workplace=TownLife.Place.MARKET;
        String a="shop-assistant";t.employerFunds.put(a,0);int workerCash=c.money;
        BusinessSolvencyLife.payHour(t,c,s);
        t.player.place=TownLife.Place.OFFICE;t.player.destination=t.player.place;t.player.money=100;
        s.minute=390;
        for(int day=3;day<=7;day++){s.day=day;BusinessSolvencyLife.tick(t,s);}
        assertTrue(BusinessOwnershipLife.listed(t,a));
        t.industryStocks.put("business-credit:"+a+":balance",25);
        s.minute=600;int city=t.employerFunds.get("city-maintenance");
        assertEquals(64,BusinessOwnershipLife.total(t,a));
        BusinessOwnershipLife.buy(t,s,a);
        assertEquals(36,t.player.money);assertEquals(city+20,t.employerFunds.get("city-maintenance").intValue());
        assertEquals(workerCash+4,c.money);assertEquals(40,t.employerFunds.get(a).intValue());
        assertEquals(25,BusinessCreditLife.get(t,a,"balance"));assertTrue(BusinessOwnershipLife.yours(t.copy(),a));
        assertFalse(BusinessOwnershipLife.canBuy(t,s,a));assertFalse(BusinessOwnershipLife.canDividend(t,s,a));
        BusinessOwnershipLife.invest(t,s,a);assertEquals(60,t.employerFunds.get(a).intValue());
        t.player.incarnation++;s.minute=390;s.day++;BusinessOwnershipLife.tick(t,s);
        assertEquals(0,BusinessOwnershipLife.owner(t,a));
    }
}
