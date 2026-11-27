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
    @Test void transitionalHousingUsesExistingFundsAndCannotBeRepeated() throws Exception {
        var file=folder.resolve("housing.db");
        try(var db=new WorldDatabase(file)){
            var s=db.load();var t=db.loadTown(s);var p=t.player;
            s.day=2;s.minute=1200;
            p.place=CaritasLife.PLACE;p.destination=p.place;p.money=7;
            p.ownsHouse=false;p.roomUntil=0;p.fatigue=90;
            t.employerFunds.put(SocialProtectionLife.ACCOUNT,27);
            int guesthouse=t.employerFunds.getOrDefault("guesthouse",0);
            assertFalse(ShelterLife.canHousing(t,s));
            ShelterLife.sleep(t,s);
            s.day++;s.minute=540;
            assertFalse(ShelterLife.canHousing(t,s));
            assertThrows(IllegalArgumentException.class,()->ShelterLife.housing(t,s));
            assertEquals(7,p.money);
            assertEquals(27,t.employerFunds.get(SocialProtectionLife.ACCOUNT));
            t.employerFunds.put(SocialProtectionLife.ACCOUNT,28);
            assertTrue(ShelterLife.canHousing(t,s));
            ShelterLife.housing(t,s);
            assertEquals(0,p.money);
            assertEquals(0,t.employerFunds.get(SocialProtectionLife.ACCOUNT));
            assertEquals(guesthouse+35,t.employerFunds.get("guesthouse"));
            assertEquals(HousingLife.now(s.day,s.minute)+15+HousingLife.TERM,p.roomUntil);
            assertTrue(ShelterLife.report(t,s).housingUsed());
            assertFalse(ShelterLife.canSleep(t,s));
            db.save(s,List.of(),List.of(),t,List.of(),List.of(),List.of());
        }
        try(var db=new WorldDatabase(file)){
            var s=db.load();var t=db.loadTown(s);
            assertTrue(ShelterLife.report(t,s).housingUsed());
            assertTrue(HousingLife.active(t.player,s.day,s.minute));
            assertEquals(0,t.player.money);
            t.player.roomUntil=0;
            t.employerFunds.put(SocialProtectionLife.ACCOUNT,100);
            assertFalse(ShelterLife.canHousing(t,s));
        }
    }
}
