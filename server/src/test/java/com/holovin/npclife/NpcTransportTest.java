package com.holovin.npclife;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import static org.junit.jupiter.api.Assertions.*;
class NpcTransportTest {
    @Test void residentCommutesPaysAndRetainsCar(){
        var s=new WorldState();s.minute=600;
        var t=TownLife.create(s);t.industryStocks.put(NpcTransportLife.ENABLED,1);
        var c=new TownLife.Citizen("test-driver","Test driver","Mechanic",TownLife.Place.PARK);
        c.generationIndex=3;c.money=100;t.people.put(c.id,c);
        var events=new ArrayList<TownLife.Event>();var observations=new ArrayList<TownLife.Observation>();
        NpcTransportLife.prepare(t,c,s,events,observations);
        assertTrue(NpcTransportLife.owns(t,c));
        NpcTransportLife.depart(t,c,TownLife.Place.WORKSHOP,s);
        TownLife.move(t,c,TownLife.Place.WORKSHOP,s,events,observations);
        assertEquals(NpcTransportLife.CAR,NpcTransportLife.mode(t,c));
        assertEquals(5,NpcTransportLife.get(t,c,"fuel"));
        assertEquals(77,NpcTransportLife.get(t,c,"condition"));
        assertEquals(TownLife.Place.WORKSHOP,NpcTransportLife.parked(t,c));
        c.place=c.destination;c.travelRemaining=0;
        NpcTransportLife.set(t,c,"fuel",1);
        int stock=FuelLife.stock(t);
        NpcTransportLife.prepare(t,c,s,events,observations);
        assertEquals(4,NpcTransportLife.get(t,c,"fuel"));assertEquals(94,c.money);assertEquals(stock-3,FuelLife.stock(t));
        t.player.place=c.place;
        VehicleTheftLife.take(t,s,c,events,observations);
        assertTrue(VehicleTheftLife.missing(t,c));assertEquals(4,VehicleTheftLife.get(t,"fuel"));
        VehicleTheftLife.set(t,"condition",50);VehicleTheftLife.recover(t);
        assertEquals(50,NpcTransportLife.get(t,c,"condition"));
        assertFalse(VehicleTheftLife.missing(t,c));
    }
}
