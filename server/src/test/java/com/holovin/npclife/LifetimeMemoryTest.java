package com.holovin.npclife;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class LifetimeMemoryTest {
    @TempDir Path folder;
    @Test void longTermMemorySurvivesReloadAndRumorsAreNotFirsthand()throws Exception{
        var s=new WorldState();var t=TownLife.create(s);
        var people=PopulationGenerator.add(t,2);
        var a=people.get(0);var b=people.get(1);
        for(var c:people){c.place=TownLife.Place.PARK;c.destination=c.place;c.travelRemaining=0;c.activity="FREE_TIME";c.sociability=90;}
        var bond=ParentingLife.relation(t,a,b);bond.trust=90;
        var contact=new PlayerLife.Contact(a.id);contact.knownName="Alex";contact.conversations=1;t.player.contacts.put(a.id,contact);
        SocialMemoryLife.remember(t,a.id,SocialMemoryLife.Kind.SUPPORT,1);
        SocialMemoryLife.remember(t,a.id,SocialMemoryLife.Kind.SUPPORT,2);
        assertEquals(1,t.beliefs.get(a.id).size());
        s.day=7301;s.minute=600;
        KnowledgeLife.exchange(t,s,a.id,b.id);
        var claim=LifetimeMemoryLife.latest(t,b,false);
        assertNotNull(claim);assertEquals("HEARD",claim.origin());assertEquals(a.id,claim.learnedFrom());
        assertTrue(claim.confidence()<100);
        assertNull(LifetimeMemoryLife.recollect(t,b,"en")); // no recognition yet
        var known=new PlayerLife.Contact(b.id);known.conversations=1;t.player.contacts.put(b.id,known);
        assertTrue(LifetimeMemoryLife.recollect(t,b,"en").contains("wasn't there"));
        Path db=folder.resolve("memory.db");
        try(var save=new WorldDatabase(db)){save.save(s,List.of(),List.of(),t,List.of(),List.of(),List.of());}
        try(var save=new WorldDatabase(db)){
            var restored=save.loadTown(save.load());
            assertEquals("I remember day 1: you helped me.",LifetimeMemoryLife.recollect(restored,restored.get(a.id),"en"));
            assertEquals("HEARD",LifetimeMemoryLife.latest(restored,restored.get(b.id),false).origin());
            restored.player.incarnation++;
            assertNull(LifetimeMemoryLife.latest(restored,restored.get(a.id),true));
        }
    }
}
