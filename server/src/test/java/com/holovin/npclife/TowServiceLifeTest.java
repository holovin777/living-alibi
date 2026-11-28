package com.holovin.npclife;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class TowServiceLifeTest {
    @Test void paidRecoveryIsDelayedAndDoesNotMoveOwnerOrRepairCar(){
        var s=new WorldState();s.day=2;s.minute=600;var t=TownLife.create(s);
        var origin=TownLife.Place.PARK;
        t.player.place=origin;t.player.destination=origin;t.player.carPlace=origin;
        t.player.carFuel=0;t.player.carCondition=40;t.player.money=100;
        t.industryStocks.put(FuelLife.STOCK,20);
        int fee=TowServiceLife.fee(origin),units=TowServiceLife.units(origin);
        TowServiceLife.request(t,s,"car");
        assertEquals(100-fee,t.player.money);assertEquals(20-units,FuelLife.stock(t));
        assertTrue(TowServiceLife.pending(t,"car"));
        assertThrows(IllegalArgumentException.class,()->TowServiceLife.request(t,s,"car"));
        assertThrows(IllegalArgumentException.class,()->TowServiceLife.requireAvailable(t,"car"));
        s.minute+=TowServiceLife.duration(origin)-1;
        TowServiceLife.tick(t,s);assertEquals(origin,t.player.carPlace);
        s.minute++;
        TowServiceLife.tick(t,s);
        assertEquals(TownLife.Place.WORKSHOP,t.player.carPlace);
        assertEquals(origin,t.player.place);assertEquals(0,t.player.carFuel);assertEquals(40,t.player.carCondition);
        assertFalse(TowServiceLife.pending(t,"car"));assertEquals(1,TowServiceLife.view(t,s).completed());
        TowServiceLife.tick(t,s);assertEquals(1,TowServiceLife.view(t,s).completed());
    }
}
