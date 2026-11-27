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
            int turns=ConversationAvailability.turns(town,c,1,minute);
            assertTrue(turns>=6);
            assertTrue(ConversationAvailability.minutes(town,c,1,minute)>=9);
            for(int n=0;n<turns;n++){
                assertTrue(ConversationAvailability.allowed(town,c,1,minute+n));
                assertEquals(n==turns-1,ConversationAvailability.record(town,c,1,minute+n));
            }
            assertFalse(ConversationAvailability.allowed(town,c,1,minute+turns));
            assertTrue(ConversationAvailability.farewell(c,1,minute+turns,"it").contains("turno"));
            clock.minute=minute+turns;
            db.save(clock,List.of(),List.of(),town,List.of(),List.of(),List.of());
        }
        try(var db=new WorldDatabase(file)){
            var clock=db.load();var town=db.loadTown(clock);var c=officer(town);
            assertFalse(ConversationAvailability.allowed(town,c,1,clock.minute));
            assertTrue(ConversationAvailability.allowed(town,c,1,clock.minute+16));
        }
    }
    @Test
    void customerServiceAndQuietResidentsStillHaveTimeToListen(){
        var t=TownLife.create(new WorldState());var c=t.get("marco");
        c.occupation="Baker";c.employed=true;c.place=TownLife.Place.BAKERY;c.workplace=c.place;
        c.activity="WORKING";c.health=100;c.hunger=20;c.thirst=20;c.fatigue=20;c.sociability=20;
        assertTrue(ConversationAvailability.customerService(c));
        assertTrue(ConversationAvailability.turns(t,c,1,600)>=12);
        int quiet=ConversationAvailability.minutes(t,c,1,600);
        c.sociability=90;
        assertTrue(ConversationAvailability.minutes(t,c,1,600)>quiet);
        c.hunger=95;
        assertFalse(ConversationAvailability.customerService(c));
        assertEquals(1,ConversationAvailability.turns(t,c,1,600));
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
