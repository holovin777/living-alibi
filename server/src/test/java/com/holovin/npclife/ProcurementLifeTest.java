package com.holovin.npclife;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ProcurementLifeTest {
    @Test void purchaseConsumesStockTransfersCashAndPreservesInvoice(){
        var s=new WorldState();s.day=3;s.minute=600;
        var t=TownLife.create(s);t.industryStocks.put(StaffingLife.ENABLED,1);FactoryNetworkLife.ensure(t);
        var p=FactoryNetworkLife.PLANTS.stream().filter(x->x.id().equals("sawmill")).findFirst().orElseThrow();
        var worker=new TownLife.Citizen("purchase-worker","Ada",p.occupation(),p.site());
        worker.age=35;worker.employed=true;worker.workplace=p.site();worker.place=p.site();worker.travelRemaining=0;worker.activity="WORKING";
        t.people.put(worker.id,worker);
        FactoryNetworkLife.change(t,"logs",2);
        String a=FactoryNetworkLife.account(p);
        int buyer=t.employerFunds.get(a),seller=t.employerFunds.get(FactoryNetworkLife.LUMBER);
        ProcurementLife.plan(t,s,p);
        assertTrue(ProcurementLife.ordered(t,p,"logs",3));
        assertEquals(buyer,t.employerFunds.get(a));
        assertTrue(FactoryNetworkLife.produce(t,p,3,true));
        assertEquals(0,FactoryNetworkLife.stock(t,"logs"));
        assertEquals(buyer-4,t.employerFunds.get(a));assertEquals(seller+4,t.employerFunds.get(FactoryNetworkLife.LUMBER));
        var invoice=ProcurementLife.view(t.copy(),a).getFirst();
        assertEquals("PAID",invoice.status());assertEquals(4,invoice.invoiceAmount());
        FactoryNetworkLife.change(t,"logs",2);
        assertFalse(FactoryNetworkLife.produce(t,p,3,true));
        assertEquals(2,FactoryNetworkLife.stock(t,"logs"));
    }
    @Test void scarceStockDoesNotGeneratePaymentOrInvoice(){
        var s=new WorldState();s.day=4;s.minute=600;
        var t=TownLife.create(s);t.industryStocks.put(StaffingLife.ENABLED,1);FactoryNetworkLife.ensure(t);
        var p=FactoryNetworkLife.PLANTS.getFirst();
        var w=new TownLife.Citizen("buyer","Buyer",p.occupation(),p.site());
        w.age=35;w.employed=true;w.place=p.site();w.workplace=p.site();w.travelRemaining=0;w.activity="WORKING";t.people.put(w.id,w);
        String a=FactoryNetworkLife.account(p);int cash=t.employerFunds.get(a);
        ProcurementLife.plan(t,s,p);
        assertFalse(FactoryNetworkLife.produce(t,p,s.day,true));
        ProcurementLife.finish(t,p,s.day);
        assertEquals(cash,t.employerFunds.get(a));assertEquals("UNFULFILLED",ProcurementLife.view(t,a).getFirst().status());
        assertEquals(0,ProcurementLife.view(t,a).getFirst().invoiceDay());
    }
}
