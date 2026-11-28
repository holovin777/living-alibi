package com.holovin.npclife;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class BusinessCreditLifeTest {
    @Test void lendingProtectsDepositsSettlesWagesAndRepaysFromSurplus(){
        var s=new WorldState();s.day=2;s.minute=600;var t=TownLife.create(s);
        PopulationGenerator.add(t,12);t.industryStocks.put(StaffingLife.ENABLED,1);
        var people=t.people.values().stream().filter(c->c.generationIndex>0&&c.alive).limit(2).toList();
        var worker=people.get(0);var clerk=people.get(1);
        worker.age=35;worker.employed=true;worker.occupation="Market vendor";worker.workplace=TownLife.Place.MARKET;
        clerk.age=35;clerk.employed=true;clerk.occupation="Bank clerk";clerk.workplace=TownLife.Place.OFFICE;
        clerk.place=clerk.workplace;clerk.destination=clerk.place;clerk.travelRemaining=0;clerk.activity="WORKING";clerk.sickUntilDay=0;
        String a="shop-assistant";t.employerFunds.put(a,0);
        int before=worker.money;
        BusinessSolvencyLife.payHour(t,worker,s);
        s.minute=390;
        s.day=3;BusinessSolvencyLife.tick(t,s);
        s.day=4;BusinessSolvencyLife.tick(t,s);
        s.minute=600;BankLife.set(t,BankLife.SAVINGS,100);BankLife.cash(t,220);
        assertFalse(BusinessCreditLife.lend(t,s,a));
        BankLife.cash(t,300);
        assertTrue(BusinessCreditLife.lend(t,s,a));
        assertEquals(44,BusinessCreditLife.get(t,a,"balance"));assertEquals(256,BankLife.cash(t));
        assertEquals(0,BusinessSolvencyLife.outstanding(t,a));assertEquals(before+4,worker.money);
        assertEquals(40,t.employerFunds.get(a).intValue());assertFalse(BusinessCreditLife.lend(t,s,a));
        s.day=5;BusinessCreditLife.review(t,s,a);
        assertEquals(1,BusinessCreditLife.get(t,a,"missed"));
        t.employerFunds.put(a,60);s.day=8;BusinessCreditLife.review(t,s,a);
        assertEquals(36,BusinessCreditLife.get(t,a,"balance"));assertEquals(266,BankLife.cash(t));
        assertEquals(50,t.employerFunds.get(a).intValue());assertEquals(0,BusinessCreditLife.get(t,a,"missed"));
        BusinessCreditLife.review(t,s,a);
        assertEquals(36,BusinessCreditLife.get(t.copy(),a,"balance"));
    }
}
