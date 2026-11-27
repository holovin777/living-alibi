package com.holovin.npclife;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
class InterviewLifeTest {
    @TempDir Path folder;
    @Test void appointmentPersistsAndHiringRequiresInterview() throws Exception{
        var file=folder.resolve("interview.db");
        try(var db=new WorldDatabase(file)){
            var s=db.load();var t=db.loadTown(s);var p=t.player;
            s.day=5;s.minute=540;p.place=TownLife.Place.MARKET;p.destination=p.place;
            p.jobId=null;p.fatigue=0;p.hygiene=100;
            t.employerFunds.put("shop-assistant",100);
            InterviewLife.submit(t,s,"shop-assistant");
            assertNull(p.jobId);
            assertThrows(IllegalArgumentException.class,()->InterviewLife.attend(t,s,"shop-assistant"));
            assertEquals(8,InterviewLife.report(t,s).stream().filter(a->a.id().equals("shop-assistant")).findFirst().orElseThrow().appointmentDay());
            db.save(s,List.of(),List.of(),t,List.of(),List.of(),List.of());
        }
        try(var db=new WorldDatabase(file)){
            var s=db.load();var t=db.loadTown(s);
            s.day=8;s.minute=540;
            assertTrue(InterviewLife.report(t,s).stream().filter(a->a.id().equals("shop-assistant")).findFirst().orElseThrow().canAttend());
            InterviewLife.attend(t,s,"shop-assistant");
            assertEquals("shop-assistant",t.player.jobId);
            assertEquals(100,t.employerFunds.get("shop-assistant"));
        }
    }
    @Test void refusalUsesCooldownAndDoesNotCreateEmployment() throws Exception{
        try(var db=new WorldDatabase(folder.resolve("refusal.db"))){
            var s=db.load();var t=db.loadTown(s);var p=t.player;
            s.day=2;s.minute=540;p.place=TownLife.Place.MARKET;p.destination=p.place;p.jobId=null;
            t.employerFunds.put("shop-assistant",100);
            InterviewLife.submit(t,s,"shop-assistant");
            s.day=3;t.employerFunds.put("shop-assistant",0);
            InterviewLife.attend(t,s,"shop-assistant");
            assertNull(p.jobId);
            t.employerFunds.put("shop-assistant",100);
            assertThrows(IllegalArgumentException.class,()->InterviewLife.submit(t,s,"shop-assistant"));
            assertEquals(6,InterviewLife.report(t,s).stream().filter(a->a.id().equals("shop-assistant")).findFirst().orElseThrow().retryDay());
        }
    }
}
