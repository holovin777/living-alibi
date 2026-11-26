package com.holovin.npclife;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class ContactTimeLifeTest {
    @TempDir Path directory;
    private static PlayerLife.Contact contact(TownLife.Town town,String id,int trust,int affinity){
        var c=new PlayerLife.Contact(id);
        c.lastDay=1;c.trust=trust;c.affinity=affinity;c.knownName="Luca";
        town.player.contacts.put(id,c);
        return c;
    }
    @Test
    void warmthFadesSlowlyButIdentityAndMemoryRemain() throws Exception {
        Path file=directory.resolve("drift.db");
        try(var db=new WorldDatabase(file)){
            var state=db.load();var town=db.loadTown(state);
            var c=contact(town,"marco",80,40);
            ConversationMemory.remember(c,1,720,"it","Ciao","Ciao");
            ConversationMemory.mark(c,ConversationMemory.Milestone.FIRST_MEETING,c.dialogue.getLast());
            for(int d=2;d<=31;d++)ContactTimeLife.tick(town,d,0);
            assertEquals(37,c.affinity);
            assertEquals(79,c.trust);
            ContactTimeLife.tick(town,31,0);
            assertEquals(79,c.trust);
            assertEquals("Luca",c.knownName);
            assertEquals(1,c.important.size());
            state.day=31;state.minute=0;
            db.save(state,List.of(),List.of(),town,List.of(),List.of(),List.of());
        }
        try(var db=new WorldDatabase(file)){
            var town=db.loadTown(db.load());var c=town.player.contacts.get("marco");
            ContactTimeLife.tick(town,31,0);
            assertEquals(79,c.trust);
            assertEquals(37,c.affinity);
            c.lastDay=31;
            for(int d=32;d<=44;d++)ContactTimeLife.tick(town,d,0);
            assertEquals(37,c.affinity);
            assertEquals(1,c.important.size());
        }
    }
    @Test
    void ordinaryHurtSoftensButBetrayalDoesNotAutomaticallyResolve(){
        var town=TownLife.create(new WorldState());
        var marco=contact(town,"marco",25,-40);
        var elena=contact(town,"elena",25,-40);
        SocialMemoryLife.remember(town,"marco",SocialMemoryLife.Kind.HURT,1);
        SocialMemoryLife.remember(town,"elena",SocialMemoryLife.Kind.BROKEN_TRUST,1);
        for(int d=2;d<=91;d++)ContactTimeLife.tick(town,d,0);
        assertEquals(28,marco.trust);
        assertEquals(-29,marco.affinity);
        assertFalse(SocialMemoryLife.unresolved(town,"marco"));
        assertEquals(1,SocialMemoryLife.last(town,"marco",SocialMemoryLife.Kind.HURT));
        assertTrue(SocialMemoryLife.recollection(town,"marco").contains("disagreement"));
        assertEquals(25,elena.trust);
        assertTrue(SocialMemoryLife.unresolved(town,"elena"));
        SocialMemoryLife.remember(town,"elena",SocialMemoryLife.Kind.APOLOGY,92);
        assertFalse(SocialMemoryLife.unresolved(town,"elena"));
    }
}
