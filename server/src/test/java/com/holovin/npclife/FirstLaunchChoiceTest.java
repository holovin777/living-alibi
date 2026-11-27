package com.holovin.npclife;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;
class FirstLaunchChoiceTest {
    @TempDir Path dir;
    @Test void productionStartExposesChoiceAndPersistsIt()throws Exception{
        Path file=dir.resolve("first.db");
        try(var game=new GameSession(file,240,true,false,true)){
            assertTrue(game.snapshot().rebirth().firstSexPending());
            assertEquals(0,game.snapshot().rebirth().maxWaitDays());
            assertThrows(IllegalArgumentException.class,()->game.gardenAction("rebirth-wait"));
        }
        try(var game=new GameSession(file,240,true,false,true)){
            assertTrue(game.snapshot().rebirth().firstSexPending());
            game.gardenAction("rebirth-first-boy");
            assertFalse(game.snapshot().rebirth().firstSexPending());
        }
    }
}
