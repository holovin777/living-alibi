package com.holovin.npclife;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static com.holovin.npclife.TownLife.Place.*;

class BusLifeTest {
    @Test void timetabledPaidJourneyPreservesParkedCar(){
        var s=new WorldState();s.minute=607;var t=TownLife.create(s);
        t.player.place=PARK;t.player.money=50;t.player.carPlace=WORKSHOP;
        assertTrue(BusLife.view(t,s).available());assertEquals(8,BusLife.view(t,s).waitMinutes());
        assertTrue(BusLife.view(t,s).serviceOpen());assertEquals(615,BusLife.view(t,s).nextDepartureMinute());
        assertThrows(IllegalArgumentException.class,()->BusLife.ride(t,s,OUTER_20_20));
        int minutes=BusLife.ride(t,s,WORKSHOP);
        assertEquals(12,minutes);assertEquals(48,t.player.money);assertEquals(WORKSHOP,t.player.carPlace);
        for(int i=0;i<minutes;i++)PlayerLife.tick(t.player);
        assertEquals(WORKSHOP,t.player.place);
        s.minute=1320;
        assertFalse(BusLife.view(t,s).available());
        assertFalse(BusLife.view(t,s).serviceOpen());assertEquals(-1,BusLife.view(t,s).nextDepartureMinute());
        assertThrows(IllegalArgumentException.class,()->BusLife.ride(t,s,PARK));
    }
}
