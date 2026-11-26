package com.holovin.npclife;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class ConversationAvailabilityTest {
    @TempDir Path directory;
    private static TownLife.Citizen officer(TownLife.Town t){
        var c=t.get("marco");
        c.employed=true;c.occupation=PoliceLife.OFFICER;c.activity="WORKING";
        c.health=100;c.hunger=20;c.thirst=20;c.fatigue=20;
        return c;
    }
    @Test
    void dutyLimitsPersistAndDoNotResetWithRepeatedRequests() throws Exception{
        Path file=directory.resolve("patience.db");
        int minute;
        try(var db=new WorldDatabase(file)){
            var clock=db.load();var town=db.loadTown(clock);var c=officer(town);
            minute=PoliceLife.shiftStart(c)+10;
            assertEquals(4,ConversationAvailability.minutes(town,c,1,minute));
            assertTrue(ConversationAvailability.allowed(town,c,1,minute));
            assertFalse(ConversationAvailability.record(town,c,1,minute));
            assertTrue(ConversationAvailability.record(town,c,1,minute+1));
            assertFalse(ConversationAvailability.allowed(town,c,1,minute+2));
            assertTrue(ConversationAvailability.farewell(c,1,minute+2,"it").contains("turno"));
            clock.minute=minute+2;
            db.save(clock,List.of(),List.of(),town,List.of(),List.of(),List.of());
        }
        try(var db=new WorldDatabase(file)){
            var clock=db.load();var town=db.loadTown(clock);var c=officer(town);
            assertFalse(ConversationAvailability.allowed(town,c,1,minute+3));
            assertTrue(ConversationAvailability.allowed(town,c,1,minute+16));
        }
    }
    @Test
    void friendshipAndNeedsChangePatience(){
        var t=TownLife.create(new WorldState());var c=t.get("marco");
        c.activity="FREE_TIME";c.health=100;c.hunger=20;c.thirst=20;c.fatigue=20;
        int initial=ConversationAvailability.minutes(t,c,1,1100);
        var contact=new PlayerLife.Contact(c.id);
        contact.trust=85;contact.affinity=40;t.player.contacts.put(c.id,contact);
        assertTrue(ConversationAvailability.minutes(t,c,1,1100)>initial);
        c.hunger=95;
        assertEquals(2,ConversationAvailability.minutes(t,c,1,1100));
        assertTrue(ConversationAvailability.record(t,c,1,1100));
        assertFalse(ConversationAvailability.allowed(t,c,1,1101));
    }
}
