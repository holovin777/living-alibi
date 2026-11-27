package com.holovin.npclife;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
class ShelterLifeTest {
    @TempDir Path folder;
    @Test void freeBedAndShowerRespectEligibilityAndPersist() throws Exception{
        var file=folder.resolve("shelter.db");
        try(var db=new WorldDatabase(file)){
            var s=db.load();var t=db.loadTown(s);
            s.day=2;s.minute=1200;
            var p=t.player;p.money=10;p.health=75;p.fatigue=90;p.hunger=30;p.thirst=30;p.hygiene=40;
            assertFalse(ShelterLife.canSleep(t,s));
            p.place=CaritasLife.PLACE;p.destination=p.place;
            p.ownsHouse=true;assertFalse(ShelterLife.canSleep(t,s));p.ownsHouse=false;
            p.roomUntil=HousingLife.now(s.day,s.minute)+60;assertFalse(ShelterLife.canSleep(t,s));p.roomUntil=0;
            assertTrue(ShelterLife.canWash(t,s));ShelterLife.wash(t,s);
            assertEquals(100,p.hygiene);assertFalse(ShelterLife.canWash(t,s));
            assertTrue(ShelterLife.canSleep(t,s));
            assertEquals(480,ShelterLife.sleep(t,s).minutes());
            assertEquals(2,p.fatigue);assertEquals(77,p.health);assertEquals(10,p.money);
            assertFalse(ShelterLife.canSleep(t,s));
            db.save(s,List.of(),List.of(),t,List.of(),List.of(),List.of());
        }
        try(var db=new WorldDatabase(file)){
            var s=db.load();var t=db.loadTown(s);
            assertEquals(1,ShelterLife.report(t,s).stays());
            assertFalse(ShelterLife.canSleep(t,s));
            s.day++;assertTrue(ShelterLife.canSleep(t,s));
        }
    }
}
