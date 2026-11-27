package com.holovin.npclife;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class PostalLifeTest {
    @TempDir Path folder;
    @Test void statementWaitsForRealOfficeWorkerAndDoesNotBillAgain() throws Exception{
        var file=folder.resolve("postal.db");
        try(var db=new WorldDatabase(file)){
            var s=db.load();var t=db.loadTown(s);
            s.day=2;s.minute=540;
            var recipient=t.get("marco");
            recipient.generationIndex=9001;recipient.age=35;recipient.alive=true;
            recipient.home=TownLife.Place.MARCO_HOME;
            assertEquals(12,CondominiumLife.allocate(t,s,recipient,155));
            assertEquals(0,CondominiumLife.allocate(t,s,recipient,155));
            assertEquals(1,PostalLife.report(t).pending());
            var c=new TownLife.Citizen("postal-test","Test Clerk",PostalLife.OCCUPATION,TownLife.Place.OFFICE);
            c.generationIndex=9000;c.age=35;c.alive=true;c.employed=true;c.workplace=TownLife.Place.OFFICE;
            c.home=TownLife.Place.SOUTH_RESIDENCES;c.activity="WORKING";c.health=90;
            c.hunger=20;c.thirst=20;c.fatigue=20;c.money=50;
            // Isolate the dispatch from any existing office workers.
            for(var p:t.people.values())if("Office clerk".equals(p.occupation)||PostalLife.postalWorker(p))p.employed=false;
            int payroll=t.employerFunds.get(PostalLife.ACCOUNT);
            t.people.put(c.id,c);
            PostalLife.tick(t,s);
            assertTrue(PostalLife.assigned(t,c,s));
            s.minute++;PostalLife.tick(t,s);
            assertEquals(0,PostalLife.deliveredTo(t,recipient));
            c.place=TownLife.Place.NORTH_RESIDENCES;c.travelRemaining=0;c.activity="WORKING";
            for(int i=0;i<14;i++){s.minute++;PostalLife.tick(t,s);}
            assertEquals(0,PostalLife.deliveredTo(t,recipient));
            s.minute++;PostalLife.tick(t,s);
            assertEquals(1,PostalLife.deliveredTo(t,recipient));
            assertEquals(0,PostalLife.report(t).pending());
            assertEquals(1,PostalLife.report(t).delivered());
            assertEquals(12,CondominiumLife.report(t).arrears());
            assertEquals(50+PostalLife.WAGE,c.money);
            assertEquals(payroll-PostalLife.WAGE,t.employerFunds.get(PostalLife.ACCOUNT));
            assertEquals(1,PostalLife.report(t).employees());
            db.save(s,List.of(),List.of(),t,List.of(),List.of(),List.of());
        }
        try(var db=new WorldDatabase(file)){
            var t=db.loadTown(db.load());
            assertEquals(1,PostalLife.deliveredTo(t,t.get("marco")));
            assertEquals(1,PostalLife.report(t).delivered());
            assertEquals(0,PostalLife.report(t).pending());
        }
    }
}
