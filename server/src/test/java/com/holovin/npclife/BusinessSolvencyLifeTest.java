package com.holovin.npclife;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class BusinessSolvencyLifeTest {
    @Test void wagesAccrueOnceAndInsolvencySuspendsUntilDebtPaid(){
        var s=new WorldState();s.day=2;s.minute=600;var t=TownLife.create(s);
        PopulationGenerator.add(t,12);t.industryStocks.put(StaffingLife.ENABLED,1);
        var c=t.people.values().stream().filter(x->x.generationIndex>0&&x.alive).findFirst().orElseThrow();
        c.age=35;c.employed=true;c.occupation="Market vendor";c.workplace=TownLife.Place.MARKET;
        String a="shop-assistant";int before=c.money;
        t.employerFunds.put(a,1);
        assertTrue(BusinessSolvencyLife.payHour(t,c,s));assertTrue(BusinessSolvencyLife.payHour(t,c,s));
        assertEquals(before+1,c.money);assertEquals(3,BusinessSolvencyLife.debt(t,a,c));
        s.minute=390;
        for(int day=3;day<=5;day++){s.day=day;BusinessSolvencyLife.tick(t,s);}
        assertFalse(c.employed);assertTrue(BusinessSolvencyLife.suspended(t.copy(),a));
        t.employerFunds.put(a,10);s.day=6;BusinessSolvencyLife.tick(t,s);
        assertEquals(0,BusinessSolvencyLife.debt(t,a,c));assertEquals(before+4,c.money);
        assertEquals(7,t.employerFunds.get(a).intValue());assertFalse(BusinessSolvencyLife.suspended(t,a));
    }
}
