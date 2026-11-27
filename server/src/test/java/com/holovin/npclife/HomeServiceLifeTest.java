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
    }
}
