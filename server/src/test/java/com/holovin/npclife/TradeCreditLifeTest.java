package com.holovin.npclife;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TradeCreditLifeTest {
    @Test void creditConsumesMaterialsButOnlySettlementMovesCash(){
        var s=new WorldState();s.day=3;s.minute=600;
        var t=TownLife.create(s);t.industryStocks.put(StaffingLife.ENABLED,1);FactoryNetworkLife.ensure(t);
        var p=FactoryNetworkLife.PLANTS.getFirst();
        var w=new TownLife.Citizen("credit-worker","Ada",p.occupation(),p.site());
        w.age=35;w.employed=true;w.place=p.site();w.workplace=p.site();w.travelRemaining=0;w.activity="WORKING";t.people.put(w.id,w);
        String buyer=FactoryNetworkLife.account(p),seller=FactoryNetworkLife.LUMBER;
        t.employerFunds.put(buyer,10);t.employerFunds.put(seller,100);
        FactoryNetworkLife.change(t,"logs",2);
        ProcurementLife.plan(t,s,p);assertTrue(FactoryNetworkLife.produce(t,p,s.day,true));
        assertEquals(0,FactoryNetworkLife.stock(t,"logs"));
        assertEquals(10,t.employerFunds.get(buyer));assertEquals(100,t.employerFunds.get(seller));
        assertEquals(4,TradeCreditLife.payable(t,buyer));assertEquals(4,TradeCreditLife.receivable(t,seller));
        assertEquals("ON_CREDIT",ProcurementLife.view(t,buyer).getFirst().status());
        assertFalse(TradeCreditLife.eligible(t,p,"logs",4));
        var restored=t.copy();
        TradeCreditLife.settle(restored,buyer,5);
        assertEquals(4,TradeCreditLife.payable(restored,buyer));
        assertEquals(4,TradeCreditLife.overdue(restored,buyer,6));
        TradeCreditLife.settle(restored,buyer,6);
        assertEquals(4,TradeCreditLife.payable(restored,buyer));
        restored.employerFunds.put(buyer,42);
        TradeCreditLife.settle(restored,buyer,7);
        assertEquals(2,TradeCreditLife.payable(restored,buyer));
        assertEquals(102,restored.employerFunds.get(seller));
        restored.employerFunds.put(buyer,50);
        TradeCreditLife.settle(restored,buyer,8);TradeCreditLife.settle(restored,buyer,8);
        assertEquals(0,TradeCreditLife.payable(restored,buyer));assertEquals(0,TradeCreditLife.receivable(restored,seller));
        assertEquals(48,restored.employerFunds.get(buyer));assertEquals(104,restored.employerFunds.get(seller));
    }
}
