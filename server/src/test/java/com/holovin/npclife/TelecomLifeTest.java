package com.holovin.npclife;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
class TelecomLifeTest {
    @TempDir Path dir;
    @Test void simAndNewLifeStateSurviveDatabaseReload()throws Exception{
        Path file=dir.resolve("telecom.db");String id;
        try(var db=new WorldDatabase(file)){
            var s=db.load();var t=db.loadTown(s);
            var c=PopulationGenerator.add(t,1).getFirst();id=c.id;
            c.age=30;c.money=100;c.activity="FREE_TIME";c.travelRemaining=0;
            t.industryStocks.put(StaffingLife.ENABLED,1);
            t.industryStocks.put(RebirthLife.PHASE,RebirthLife.LINKED_ADULT);
            t.industryStocks.put(RebirthLife.CHILD,c.generationIndex);
            TelecomLife.subscribe(t,c,s);
            assertEquals(92,c.money);
            assertTrue(TelecomLife.connected(t,c,s));
            PsychologyLife.set(t,c,"stress",77);
            db.save(s,List.of(),List.of(),t,List.of(),List.of(),List.of());
        }
        try(var db=new WorldDatabase(file)){
            var s=db.load();var t=db.loadTown(s);var c=t.get(id);
            assertEquals(77,PsychologyLife.get(t,c,"stress",20));
            assertTrue(TelecomLife.connected(t,c,s));
            assertTrue(TelecomLife.cash(t)>=8);
            assertFalse(TelecomLife.view(t,c,s).number().isEmpty());
            s.day+=30;assertFalse(TelecomLife.connected(t,c,s));
        }
    }
}
