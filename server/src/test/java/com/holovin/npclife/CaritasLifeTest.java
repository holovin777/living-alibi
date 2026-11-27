package com.holovin.npclife;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class CaritasLifeTest {
    @TempDir Path folder;
    @Test void substituteDoesNotCollectAnotherDonation(){
        var s=new WorldState();s.day=2;s.minute=630;
        var t=TownLife.create(s);
        for(var c:t.people.values())c.employed=true;
        var a=new TownLife.Citizen("substitute-a","Test A","Job seeker",TownLife.Place.PARK);
        var b=new TownLife.Citizen("substitute-b","Test B","Job seeker",TownLife.Place.PARK);
        a.generationIndex=9001;b.generationIndex=9002;
        for(var c:List.of(a,b)){c.employed=false;c.money=60;t.people.put(c.id,c);}
        a.sociability=90;b.sociability=80;
        t.retailStock.put("bread",20);t.retailStock.put("water",20);
        CaritasLife.prepare(t,s);
        assertTrue(CaritasLife.assigned(t,a,s));
        a.sickUntilDay=s.day;
        s.minute=645;CaritasLife.prepare(t,s);
        assertFalse(CaritasLife.assigned(t,a,s));
        assertTrue(CaritasLife.assigned(t,b,s));
        assertEquals(16,t.retailStock.get("bread"));
        assertEquals(4,CaritasLife.report(t,s).bread());
        assertEquals(8,CaritasLife.report(t,s).donated());
    }

    @Test void foodTransfersRequireVolunteerAndCannotBeDuplicated() throws Exception{
        var file=folder.resolve("caritas.db");
        try(var db=new WorldDatabase(file)){
            var s=db.load();var t=db.loadTown(s);
            for(var c:t.people.values())c.employed=true;
            var volunteer=new TownLife.Citizen("volunteer-test","Test Volunteer","Job seeker",TownLife.Place.PARK);
            volunteer.generationIndex=9001;volunteer.employed=false;volunteer.money=60;volunteer.sociability=90;
            t.people.put(volunteer.id,volunteer);
            t.retailStock.put("bread",20);t.retailStock.put("water",20);
            s.day=2;s.minute=630;CaritasLife.prepare(t,s);
            assertTrue(CaritasLife.assigned(t,volunteer,s));
            assertEquals(16,t.retailStock.get("bread"));assertEquals(4,CaritasLife.report(t,s).bread());
            CaritasLife.prepare(t,s);assertEquals(4,CaritasLife.report(t,s).bread());
            s.minute=720;t.player.place=TownLife.Place.LIBRARY;t.player.destination=t.player.place;
            t.player.money=5;t.player.bread=0;t.player.bottledWater=0;
            assertFalse(CaritasLife.report(t,s).open());
            volunteer.place=TownLife.Place.LIBRARY;volunteer.activity="VOLUNTEERING";
            assertTrue(CaritasLife.report(t,s).open());
            CaritasLife.act(t,s,"caritas-receive");
            assertEquals(1,t.player.bread);assertEquals(1,t.player.bottledWater);
            assertEquals(5,t.player.money);assertEquals(3,CaritasLife.report(t,s).bread());
            assertThrows(IllegalArgumentException.class,()->CaritasLife.act(t,s,"caritas-receive"));
            CaritasLife.act(t,s,"caritas-donate-bread");
            assertEquals(0,t.player.bread);assertEquals(4,CaritasLife.report(t,s).bread());
            db.save(s,List.of(),List.of(),t,List.of(),List.of(),List.of());
        }
        try(var db=new WorldDatabase(file)){
            var s=db.load();var t=db.loadTown(s);
            assertEquals(4,CaritasLife.report(t,s).bread());
            assertEquals(3,CaritasLife.report(t,s).water());
            assertEquals(1,CaritasLife.report(t,s).served());
            assertFalse(CaritasLife.report(t,s).canReceive());
        }
    }
}
