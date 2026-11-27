package com.holovin.npclife;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class PersistentWorldClockTest {
    @TempDir Path folder;
    @Test void oneHourIsExactlyOneWorldDay(){
        assertEquals(450,PersistentWorldClock.target(450,2_499_999_999L));
        assertEquals(451,PersistentWorldClock.target(450,2_500_000_000L));
        assertEquals(1170,PersistentWorldClock.target(450,1_800_000_000_000L));
        assertEquals(1890,PersistentWorldClock.target(450,3_600_000_000_000L));
    }
    @Test void serverOwnsWaitingAndDoesNotAdvanceTwice()throws Exception{
        try(var game=new GameSession(folder.resolve("server.db"),12,true,true)){
            long start=game.persistentMinute();
            game.persistentTickTo(start+3,false);
            assertEquals(start+3,game.persistentMinute());
            game.persistentTickTo(start+3,false);
            assertEquals(start+3,game.persistentMinute());
            assertThrows(IllegalArgumentException.class,()->game.waitMinutes(60));
            assertThrows(IllegalArgumentException.class,()->game.gardenAction("rebirth-wait"));
            game.persistentTickTo(start+4,true);
            assertEquals(start+4,game.persistentMinute());
        }
    }
}
