package com.holovin.npclife;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;
class CorruptionLifeTest {
    @TempDir Path dir;
    @Test void auditAndCourtRequiredBeforeRemoval()throws Exception{
        try(var db=new WorldDatabase(dir.resolve("integrity.db"))){
            var s=db.load();var t=db.loadTown(s);
            var people=PopulationGenerator.add(t,2);var mayor=people.get(0);var detective=people.get(1);
            mayor.age=40;detective.age=35;mayor.money=100;
            for(var c:people){c.place=TownLife.Place.OFFICE;c.travelRemaining=0;c.activity="FREE_TIME";}
            detective.occupation="Detective";detective.employed=true;
            ElectionLife.set(t,ElectionLife.MAYOR,mayor.generationIndex);
            t.employerFunds.put(MunicipalLife.CITY,500);
            s.day=1;s.minute=600;CorruptionLife.offense(t,mayor,s);
            assertEquals(130,mayor.money);assertEquals(470,MunicipalLife.balance(t,MunicipalLife.CITY));
            assertFalse(NpcJusticeLife.confined(t,mayor,s));
            assertThrows(IllegalArgumentException.class,()->CorruptionLife.audit(t,detective,s));
            s.day=2;s.minute=660;CorruptionLife.audit(t,detective,s);
            s.minute=720;CorruptionLife.tick(t,s);assertTrue(ElectionLife.npcMayor(t));
            s.day=4;CorruptionLife.tick(t,s);
            assertFalse(ElectionLife.npcMayor(t));assertTrue(NpcJusticeLife.confined(t,mayor,s));
            assertEquals(500,MunicipalLife.balance(t,MunicipalLife.CITY));assertEquals(100,mayor.money);
            assertEquals(1,NpcJusticeLife.convictions(t,mayor));CorruptionLife.tick(t,s);
            assertEquals(1,NpcJusticeLife.convictions(t,mayor));
        }
    }
}
