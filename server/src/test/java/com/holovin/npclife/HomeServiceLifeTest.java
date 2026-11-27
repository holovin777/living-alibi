package com.holovin.npclife;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static com.holovin.npclife.TownLife.Place.*;
class HomeServiceLifeTest {
    @Test void heatAndGasAreSavedAndPaid(){
        var s=new WorldState();s.day=2;s.minute=0;var t=TownLife.create(s);t.player.ownsHouse=true;t.player.place=SOUTH_RESIDENCES;t.player.money=50;
        HomeGoodsLife.set(t,"boiler",1);HomeServiceLife.thermostat(t,s,20);
        HomeServiceLife.tick(t,s);HomeServiceLife.tick(t,s);
        assertEquals(2,HomeServiceLife.get(t,"gas-bill"));
        assertEquals(99,HomeServiceLife.condition(t,"boiler"));assertTrue(HomeServiceLife.heating(t.copy()));
        HomeServiceLife.set(t,"gas-bill",12);assertFalse(HomeServiceLife.heating(t));
        HomeServiceLife.payGas(t,s);assertEquals(38,t.player.money);assertTrue(HomeServiceLife.heating(t));
    }
    @Test void repairRequiresArrivalAndPaysOnlyOnce(){
        var s=new WorldState();s.minute=600;var t=TownLife.create(s);t.player.ownsHouse=true;t.player.place=SOUTH_RESIDENCES;t.player.money=50;
        HomeGoodsLife.set(t,"fridge",1);HomeServiceLife.wear(t,"fridge",100);
        var c=new TownLife.Citizen("repairer","Repairer","Mechanic",PARK);c.age=35;c.generationIndex=301;c.employed=true;c.place=WORKSHOP;c.money=20;t.people.put(c.id,c);
        HomeServiceLife.request(t,s,"fridge");assertEquals(2,HomeServiceLife.get(t,"appointment-day"));
        s.day=2;HomeServiceLife.tick(t,s);assertEquals(0,HomeServiceLife.get(t,"minutes"));
        c.place=SOUTH_RESIDENCES;c.travelRemaining=0;
        // If a deterministic sick day blocks the visit, confirm it is not charged.
        if(!HomeServiceLife.assigned(t,c,s)){assertEquals(50,t.player.money);return;}
        for(int i=0;i<30;i++){s.minute=600+i;HomeServiceLife.tick(t,s);}
        assertEquals(100,HomeServiceLife.condition(t,"fridge"));assertEquals(35,t.player.money);
        assertEquals(28,c.money);assertFalse(HomeServiceLife.pending(t));
        assertEquals(2,ServiceCompanyLife.cash(t));
        assertEquals(1,ServiceCompanyLife.view(t).completed());
    }
    @Test void warrantyChargesMerchantAndConsumesOnePartOnlyAfterRepair(){
        var s=new WorldState();s.day=10;s.minute=600;var t=TownLife.create(s);
        t.player.ownsHouse=true;t.player.place=PORTICI;t.player.money=80;
        HomeGoodsLife.buy(t,s,"fridge");
        assertEquals(0,t.player.money);
        HomeServiceLife.wear(t,"fridge",100);
        var c=new TownLife.Citizen("repairer","Repairer","Mechanic",WORKSHOP);
        c.age=35;c.generationIndex=301;c.money=20;t.people.put(c.id,c);
        t.player.place=SOUTH_RESIDENCES;
        assertEquals(0,HomeServiceLife.quote(t,"fridge",s.day));
        assertEquals(15,HomeServiceLife.quote(t,"fridge",70));
        HomeServiceLife.request(t,s,"fridge");
        s.day=HomeServiceLife.get(t,"appointment-day");
        c.place=SOUTH_RESIDENCES;c.travelRemaining=0;
        int cash=t.employerFunds.get("shop-assistant");
        t.technicalMaterials.put("spare-parts",0);
        HomeServiceLife.tick(t,s);
        assertEquals(0,HomeServiceLife.get(t,"minutes"));
        assertEquals(cash,t.employerFunds.get("shop-assistant").intValue());
        t.technicalMaterials.put("spare-parts",1);
        for(int i=0;i<30;i++){s.minute=600+i;HomeServiceLife.tick(t,s);}
        assertEquals(100,HomeServiceLife.condition(t,"fridge"));
        assertEquals(0,t.player.money);
        assertEquals(cash-15,t.employerFunds.get("shop-assistant").intValue());
        assertEquals(28,c.money);
        assertEquals(0,t.technicalMaterials.get("spare-parts").intValue());
        assertEquals(2,ServiceCompanyLife.cash(t));
        assertEquals(15,ServiceCompanyLife.view(t).revenue());
        assertFalse(HomeServiceLife.pending(t));
        HomeServiceLife.wear(t,"fridge",100);
        assertEquals(15,HomeServiceLife.quote(t,"fridge",s.day));
    }
    @Test void serviceWarehouseBuysPartsFromFiniteSupplier(){
        var s=new WorldState();s.day=5;s.minute=480;var t=TownLife.create(s);
        t.employerFunds.put(ServiceCompanyLife.ACCOUNT,10);
        t.technicalMaterials.put("spare-parts",2);
        int supplier=t.employerFunds.get("city-maintenance");
        ServiceCompanyLife.tick(t,s);ServiceCompanyLife.tick(t,s);
        assertEquals(1,ServiceCompanyLife.view(t).parts());
        assertEquals(1,t.technicalMaterials.get("spare-parts").intValue());
        assertEquals(5,ServiceCompanyLife.cash(t));
        assertEquals(supplier+5,t.employerFunds.get("city-maintenance").intValue());
        assertEquals(ServiceCompanyLife.view(t),ServiceCompanyLife.view(t.copy()));
    }
    @Test void residentsBuyWearAndRepairWithoutPlayerFunds(){
        var s=new WorldState();s.day=8;s.minute=360;var t=TownLife.create(s);
        t.industryStocks.put(StaffingLife.ENABLED,1);
        var customer=new TownLife.Citizen("resident-0301","Customer","Retired",NORTH_RESIDENCES);
        customer.generationIndex=301;customer.age=70;customer.money=400;
        var tech=new TownLife.Citizen("resident-0302","Technician","Mechanic",WORKSHOP);
        tech.generationIndex=302;tech.home=SOUTH_RESIDENCES;tech.money=20;
        t.people.put(customer.id,customer);t.people.put(tech.id,tech);
        t.industryStocks.put(SupplyChainLife.retail("washer"),1);
        int player=t.player.money;
        assertTrue(ResidentServiceLife.buy(t,customer));
        assertEquals(0,SupplyChainLife.stock(t,"washer"));
        assertEquals(305,customer.money);
        ResidentServiceLife.set(t,customer,"wear",70);
        ResidentServiceLife.prepare(t,s);ResidentServiceLife.prepare(t,s);
        assertTrue(ResidentServiceLife.pending(t));
        s.minute=1200;tech.place=NORTH_RESIDENCES;tech.activity="WORKING";tech.travelRemaining=0;
        assertEquals(NORTH_RESIDENCES,ResidentServiceLife.destination(t,tech,s));
        t.technicalMaterials.put("spare-parts",1);
        for(int i=0;i<30;i++){s.minute=1200+i;ResidentServiceLife.tick(t,s);}
        assertEquals(290,customer.money);
        assertEquals(28,tech.money);
        assertEquals(100,ResidentServiceLife.condition(t,customer));
        assertEquals(1,ServiceCompanyLife.get(t,"resident-completed"));
        assertEquals(player,t.player.money);
        assertFalse(ResidentServiceLife.pending(t));
    }
    @Test void dispatcherAssignsDistinctMechanicsAndRetriesUnservedCustomers(){
        var s=new WorldState();s.day=8;s.minute=360;var t=TownLife.create(s);
        t.industryStocks.put(StaffingLife.ENABLED,1);
        for(int i=1;i<=3;i++){
            var c=new TownLife.Citizen("customer-"+i,"Customer","Retired",NORTH_RESIDENCES);
            c.generationIndex=400+i;c.age=70;c.money=300;
            t.people.put(c.id,c);EducationLife.set(t,c,"interior-laundry",1);
            ResidentServiceLife.set(t,c,"wear",80);
        }
        for(int i=1;i<=2;i++){
            var tech=new TownLife.Citizen("tech-"+i,"Technician","Mechanic",WORKSHOP);
            tech.generationIndex=500+i;tech.home=SOUTH_RESIDENCES;tech.money=20;
            t.people.put(tech.id,tech);
        }
        ResidentServiceLife.prepare(t,s);
        assertEquals(2,ResidentServiceLife.active(t));assertEquals(3,ResidentServiceLife.queued(t));
        assertNotEquals(ResidentServiceLife.slot(t,0,"mechanic"),ResidentServiceLife.slot(t,1,"mechanic"));
        assertNotEquals(ResidentServiceLife.slot(t,0,"customer"),ResidentServiceLife.slot(t,1,"customer"));
        s.minute=1200;t.technicalMaterials.put("spare-parts",1);
        for(int i=1;i<=2;i++){
            var tech=t.people.get("tech-"+i);tech.place=NORTH_RESIDENCES;tech.activity="WORKING";tech.travelRemaining=0;
        }
        for(int i=0;i<30;i++){s.minute=1200+i;ResidentServiceLife.tick(t,s);ResidentServiceLife.tick(t,s);}
        assertEquals(1,ServiceCompanyLife.get(t,"resident-completed"));
        assertEquals(2,ResidentServiceLife.queued(t));
        s.minute=1290;ResidentServiceLife.tick(t,s);
        assertEquals(0,ResidentServiceLife.active(t));
        s.day=9;s.minute=360;ResidentServiceLife.prepare(t,s);
        assertEquals(2,ResidentServiceLife.active(t));
        assertEquals(2,ResidentServiceLife.queued(t.copy()));
    }
}
