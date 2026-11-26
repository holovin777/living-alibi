package com.holovin.npclife;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class ConversationMemoryTest {
    @TempDir Path directory;

    @Test
    void historyIsPrivateBoundedRelevantAndPersistent() throws Exception {
        Path file=directory.resolve("memory.db");
        try(var db=new WorldDatabase(file)){
            var state=db.load();var town=db.loadTown(state);
            var marco=new PlayerLife.Contact("marco");
            town.player.contacts.put("marco",marco);
            for(int i=0;i<35;i++)
                ConversationMemory.remember(marco,i+1,720,"it","Argomento "+i,"Risposta "+i);
            ConversationMemory.remember(marco,36,720,"it","Cerco lavoro come elettricista","Capisco.");
            for(int i=0;i<8;i++)
                ConversationMemory.remember(marco,37+i,720,"it","Saluto numero "+i,"Ciao.");
            assertEquals(32,marco.dialogue.size());
            assertTrue(ConversationMemory.context(marco,"Hai notizie di lavoro elettricista?")
                    .stream().anyMatch(t->t.player().contains("elettricista")));
            assertTrue(ConversationMemory.context(marco,"lavoro").size()<=10);
            assertNotSame(marco.dialogue,marco.copy().dialogue);
            assertNull(town.player.contacts.get("elena"));
            db.save(state,List.of(),List.of(),town,List.of(),List.of(),List.of());
        }
        try(var db=new WorldDatabase(file)){
            var state=db.load();var town=db.loadTown(state);
            var marco=town.player.contacts.get("marco");
            assertEquals(32,marco.dialogue.size());
            assertEquals("Saluto numero 7",marco.dialogue.getLast().player());
            assertTrue(ConversationMemory.recall(marco,"it").contains("Saluto numero 7"));
            assertFalse(ConversationMemory.recall(null,"it").contains("Saluto"));
            assertNull(town.player.contacts.get("elena"));
            town.player.alive=false;
            PlayerLife.beginNewLife(town,state.day,state.minute);
            assertTrue(town.player.contacts.isEmpty());
            db.save(state,List.of(),List.of(),town,List.of(),List.of(),List.of());
        }
        try(var db=new WorldDatabase(file)){
            var town=db.loadTown(db.load());
            assertEquals(2,town.player.incarnation);
            assertTrue(town.player.contacts.isEmpty());
        }
    }

    @Test
    void importantMemoriesSurviveOrdinaryForgettingAndRestart() throws Exception {
        Path file=directory.resolve("milestones.db");
        try(var db=new WorldDatabase(file)){
            var state=db.load();var town=db.loadTown(state);
            var contact=new PlayerLife.Contact("marco");
            town.player.contacts.put("marco",contact);
            ConversationMemory.remember(contact,1,720,"it","Ciao, mi chiamo Luca.","Piacere.");
            ConversationMemory.mark(contact,ConversationMemory.Milestone.FIRST_MEETING,contact.dialogue.getLast());
            for(int day=2;day<=100;day++)ConversationMemory.remember(contact,day,720,"it","Ciao "+day,"Ciao.");
            assertEquals(32,contact.dialogue.size());
            assertFalse(contact.dialogue.stream().anyMatch(t->t.day()==1));
            assertTrue(ConversationMemory.firstMeeting(contact,"it").contains("Luca"));
            assertEquals(contact.important,contact.copy().important);
            assertNotSame(contact.important,contact.copy().important);
            db.save(state,List.of(),List.of(),town,List.of(),List.of(),List.of());
        }
        try(var db=new WorldDatabase(file)){
            var town=db.loadTown(db.load());
            assertTrue(ConversationMemory.firstMeeting(town.player.contacts.get("marco"),"it").contains("Luca"));
            assertFalse(ConversationMemory.firstMeeting(town.player.contacts.get("elena"),"it").contains("Luca"));
            var legacy=new PlayerLife.Contact("elena");
            ConversationMemory.decode(legacy,"[]");
            assertTrue(legacy.important.isEmpty());
            assertTrue(legacy.dialogue.isEmpty());
        }
    }

    @Test
    void gameStoresOnlyUnderstoodTurns() throws Exception {
        Path file=directory.resolve("game-memory.db");
        try(var game=new GameSession(file)){}
        try(var db=new WorldDatabase(file)){
            var state=db.load();var town=db.loadTown(state);
            var resident=town.get("marco");
            resident.place=TownLife.Place.PARK;resident.travelRemaining=0;resident.activity="FREE_TIME";
            town.player.place=TownLife.Place.PARK;
            db.save(state,List.of(),List.of(),town,List.of(),List.of(),List.of());
        }
        try(var game=new GameSession(file)){
            assertTrue(game.speak("marco","My name is Alex.","en").message().contains("non parlo inglese"));
            assertTrue(game.speak("marco","Mi chiamo Luca.","it").message().contains("Luca"));
            assertTrue(game.speak("marco","Di cosa abbiamo parlato?","it").message().contains("Mi chiamo Luca."));
        }
        try(var db=new WorldDatabase(file)){
            var c=db.loadTown(db.load()).player.contacts.get("marco");
            assertEquals("Luca",c.knownName);
            assertEquals(2,c.dialogue.size());
            assertEquals("Mi chiamo Luca.",c.important.get(ConversationMemory.Milestone.FIRST_MEETING).player());
            assertEquals("Mi chiamo Luca.",c.important.get(ConversationMemory.Milestone.INTRODUCTION).player());
            assertTrue(c.dialogue.stream().noneMatch(t->t.player().contains("Alex")));
        }
    }
}
