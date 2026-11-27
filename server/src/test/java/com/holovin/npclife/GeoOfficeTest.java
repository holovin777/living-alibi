package com.holovin.npclife;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class GeoOfficeTest {
    @Test void officeLinesDependOnRealPresentWorkersAndClock(){
        var s=new WorldState();s.minute=600;
        var t=TownLife.create(s);var c=t.get("marco");
        c.place=TownLife.Place.OFFICE;c.destination=c.place;c.travelRemaining=0;c.activity="WORKING";c.age=35;
        c.sociability=85;c.curiosity=80;c.fatigue=0;
        var actor=new GeoProjection.Actor(c.id,0,0,"WORKING",true,"DESK",0,"","","ADULT");
        var lines=GeoOffice.remarks(t,s,List.of(actor));
        assertEquals(1,lines.size());
        assertEquals("JOKER",lines.getFirst().style());
        assertEquals(c.id,lines.getFirst().speakerId());
        assertEquals(lines,GeoOffice.remarks(t,s,List.of(actor)));
        s.minute=601;assertTrue(GeoOffice.remarks(t,s,List.of(actor)).isEmpty());
        s.minute=600;c.place=TownLife.Place.CAFE;
        assertTrue(GeoOffice.remarks(t,s,List.of(actor)).isEmpty());
    }
}
