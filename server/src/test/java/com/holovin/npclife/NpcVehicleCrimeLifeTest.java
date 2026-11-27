package com.holovin.npclife;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import static org.junit.jupiter.api.Assertions.*;
import static com.holovin.npclife.TownLife.Place.*;
class NpcVehicleCrimeLifeTest {
    private TownLife.Citizen resident(TownLife.Town t,String id,int index){
        var c=new TownLife.Citizen(id,id,"Baker",PARK);
        c.generationIndex=index;c.age=35;c.home=MARKET;c.place=WORKSHOP;c.destination=WORKSHOP;c.carefulness=85;
        t.people.put(id,c);return c;
    }
    @Test void autonomousCrimePersistsAndBlocksOwnerTransport(){
        var s=new WorldState();s.minute=600;var t=TownLife.create(s);t.industryStocks.put(NpcTransportLife.ENABLED,1);
        var victim=resident(t,"victim",300);var actor=resident(t,"actor",301);var witness=resident(t,"witness",302);
        NpcTransportLife.set(t,victim,"initialized",1);NpcTransportLife.set(t,victim,"owned",1);
        NpcTransportLife.set(t,victim,"parked",WORKSHOP.ordinal()+1);NpcTransportLife.set(t,victim,"fuel",6);NpcTransportLife.set(t,victim,"condition",80);
        NpcVehicleCrimeLife.record(t,s,actor,victim,new ArrayList<>(),new ArrayList<>());
        assertTrue(VehicleTheftLife.missing(t,victim));assertTrue(VehicleTheftLife.missing(t.copy(),victim));
        assertFalse(VehicleTheftLife.active(t));assertEquals(0,JusticeLife.thefts(t));
        NpcTransportLife.depart(t,victim,FACTORY,s);
        assertNotEquals(NpcTransportLife.CAR,NpcTransportLife.get(t,victim,"mode"));
        assertTrue(NpcVehicleCrimeLife.borrowTrip(t,actor,FACTORY,1));
        s.day=2;NpcVehicleCrimeLife.set(t,"examined",1);
        NpcVehicleCrimeLife.tick(t,s,new ArrayList<>(),new ArrayList<>());
        assertFalse(VehicleTheftLife.missing(t,victim));assertEquals(5,NpcTransportLife.get(t,victim,"fuel"));
        assertEquals(77,NpcTransportLife.get(t,victim,"condition"));assertEquals(WORKSHOP,NpcTransportLife.parked(t,victim));
        assertTrue(NpcJusticeLife.confined(t,actor,s));assertEquals(1,NpcJusticeLife.convictions(t,actor));
        assertEquals(1,NpcVehicleCrimeLife.view(t).recovered());
        assertTrue(t.archive.values().stream().noneMatch(e->e.text().contains("actor")));
    }
    @Test void unsupportedCaseDoesNotConvict(){
        var s=new WorldState();s.minute=600;var t=TownLife.create(s);t.industryStocks.put(NpcTransportLife.ENABLED,1);
        var victim=resident(t,"victim",300);var actor=resident(t,"actor",301);
        NpcVehicleCrimeLife.record(t,s,actor,victim,new ArrayList<>(),new ArrayList<>());
        s.day=4;NpcVehicleCrimeLife.tick(t,s,new ArrayList<>(),new ArrayList<>());
        assertFalse(VehicleTheftLife.missing(t,victim));assertEquals(0,NpcJusticeLife.convictions(t,actor));
    }
}
