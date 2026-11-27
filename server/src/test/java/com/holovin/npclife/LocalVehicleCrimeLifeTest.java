package com.holovin.npclife;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import static org.junit.jupiter.api.Assertions.*;
import static com.holovin.npclife.TownLife.Place.*;
class LocalVehicleCrimeLifeTest {
    @Test void busTheftKeepsLegalCarAndRecordsCrime(){
        var s=new WorldState();s.minute=613;var t=TownLife.create(s);
        t.player.place=PARK;t.player.carPlace=LIBRARY;t.player.carFuel=8;
        assertTrue(LocalVehicleCrimeLife.view(t,s).busAvailable());
        LocalVehicleCrimeLife.take(t,s,"bus",new ArrayList<>(),new ArrayList<>());
        assertEquals(1,JusticeLife.thefts(t));
        if(!JusticeLife.confined(t)){
            assertTrue(LocalVehicleCrimeLife.active(t));
            assertThrows(IllegalArgumentException.class,()->LocalVehicleCrimeLife.take(t,s,"bus",new ArrayList<>(),new ArrayList<>()));
            int minutes=LocalVehicleCrimeLife.drive(t,WORKSHOP);
            for(int i=0;i<minutes;i++)PlayerLife.tick(t.player);
            assertEquals(WORKSHOP,t.player.place);
            LocalVehicleCrimeLife.abandon(t);
            assertFalse(LocalVehicleCrimeLife.active(t));
        }
        assertEquals(LIBRARY,t.player.carPlace);assertEquals(8,t.player.carFuel);
    }
    @Test void noTheftOfBusOutsideSchedule(){
        var s=new WorldState();s.minute=10;var t=TownLife.create(s);t.player.place=PARK;
        assertFalse(LocalVehicleCrimeLife.view(t,s).busAvailable());
        assertThrows(IllegalArgumentException.class,()->LocalVehicleCrimeLife.take(t,s,"bus",new ArrayList<>(),new ArrayList<>()));
        assertEquals(0,JusticeLife.thefts(t));
    }
}
