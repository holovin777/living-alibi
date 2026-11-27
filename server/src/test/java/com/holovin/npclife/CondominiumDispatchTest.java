package com.holovin.npclife;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CondominiumDispatchTest {
    @Test void realWorkerMustArriveAndFinishBeforeConservedPayment(){
        var s=new WorldState();s.day=2;s.minute=540;
        var t=TownLife.create(s);
        var c=new TownLife.Citizen("repair-test","Test Mechanic","Mechanic",TownLife.Place.WORKSHOP);
        c.generationIndex=9000;c.age=35;c.alive=true;c.employed=true;c.home=TownLife.Place.SOUTH_RESIDENCES;
        c.workplace=TownLife.Place.WORKSHOP;c.activity="WORKING";
        c.health=90;c.fatigue=20;c.hunger=20;c.thirst=20;c.money=50;
        t.people.put(c.id,c);
        var o=t.technicalOrders.get("magnolia-lighting");
        int spare=t.technicalMaterials.get("spare-parts"),copper=t.technicalMaterials.get("copper");
        int supplier=t.employerFunds.getOrDefault("technician",0);
        CondominiumLife.tick(t,s);
        assertFalse(CondominiumLife.assigned(t,c,s));
        assertEquals("WAITING_FUNDS",CondominiumLife.report(t).maintenanceStatus());
        t.industryStocks.put("condominium:reserve",36);
        CondominiumLife.tick(t,s);
        assertTrue(CondominiumLife.assigned(t,c,s));
        s.minute++;
        CondominiumLife.tick(t,s);
        assertEquals(0,CondominiumLife.report(t).repairProgress());
        c.place=TownLife.Place.NORTH_RESIDENCES;c.travelRemaining=0;c.activity="WORKING";
        for(int i=0;i<89;i++){s.minute++;CondominiumLife.tick(t,s);}
        assertFalse(TechnicalWorkLife.magnolia(t).lightingWorking());
        assertEquals(36,CondominiumLife.report(t).reserve());
        s.minute++;CondominiumLife.tick(t,s);
        assertTrue(TechnicalWorkLife.magnolia(t).lightingWorking());
        assertEquals(0,CondominiumLife.report(t).reserve());
        assertEquals(74,c.money);
        assertEquals(supplier+12,t.employerFunds.get("technician"));
        assertEquals(spare-1,t.technicalMaterials.get("spare-parts"));
        assertEquals(copper-1,t.technicalMaterials.get("copper"));
        assertEquals(1,CondominiumLife.report(t).repairsCompleted());
        s.minute++;CondominiumLife.tick(t,s);
        assertEquals(74,c.money);
    }
}
