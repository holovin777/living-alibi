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
            assertTrue(c.dialogue.stream().noneMatch(t->t.player().contains("Alex")));
        }
    }
}
