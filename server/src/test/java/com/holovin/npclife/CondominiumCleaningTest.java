package com.holovin.npclife;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CondominiumCleaningTest {
    @Test void cleaningRequiresFundsTravelAndCompletedWork(){
        var s=new WorldState();s.day=2;s.minute=540;
        var t=TownLife.create(s);
        t.technicalOrders.get("magnolia-lighting").condition=100;
        t.technicalOrders.get("magnolia-lighting").phase=TechnicalWorkLife.Phase.COMPLETED;
        var c=new TownLife.Citizen("clean-test","Test Cleaner","Office clerk",TownLife.Place.PARK);
        c.generationIndex=9000;c.age=35;c.alive=true;c.employed=false;c.home=TownLife.Place.SOUTH_RESIDENCES;
        c.activity="JOB_SEARCH";c.health=90;c.fatigue=20;c.hunger=20;c.thirst=20;c.money=50;
        t.people.put(c.id,c);
        for(int i=0;i<50;i++)CondominiumLife.footTraffic(t);
        CondominiumLife.tick(t,s);
        assertFalse(CondominiumLife.cleaningAssigned(t,c,s));
        t.industryStocks.put("condominium:reserve",12);
        CondominiumLife.tick(t,s);
        assertTrue(CondominiumLife.cleaningAssigned(t,c,s));
        s.minute++;CondominiumLife.tick(t,s);
        assertEquals(50,CondominiumLife.report(t).dirt());
        assertEquals(50,c.money);
        c.place=TownLife.Place.NORTH_RESIDENCES;c.activity="WORKING";
        for(int i=0;i<44;i++){s.minute++;CondominiumLife.tick(t,s);}
        assertEquals(12,CondominiumLife.report(t).reserve());
        s.minute++;CondominiumLife.tick(t,s);
        assertEquals(0,CondominiumLife.report(t).dirt());
        assertEquals(0,CondominiumLife.report(t).reserve());
        assertEquals(62,c.money);
        assertEquals(1,CondominiumLife.report(t).cleaningsCompleted());
        s.minute++;CondominiumLife.tick(t,s);
        assertEquals(62,c.money);
        assertFalse(c.employed);
    }
}
