package com.holovin.npclife;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class JusticeLifeTest {
    @TempDir Path directory;
    @Test
    void reportedAssaultLeadsToCustodyCourtAndRelease() throws Exception {
        var clock=new WorldState();var town=TownLife.create(clock);
        var truth=new ArrayList<TownLife.Event>();var observations=new ArrayList<TownLife.Observation>();
        clock.minute=600;town.player.money=20;
        JusticeLife.report(town,clock,false,false,truth,observations);
        assertEquals("REPORTED",JusticeLife.view(town,clock).status());
        clock.minute=660;JusticeLife.tick(town,clock,truth,observations);
        assertTrue(JusticeLife.confined(town));
        assertEquals(TownLife.Place.OFFICE,town.player.place);
        assertThrows(IllegalArgumentException.class,()->JusticeLife.requireFree(town));
        clock.minute=720;JusticeLife.tick(town,clock,truth,observations);
        var sentence=JusticeLife.view(town,clock);
        assertEquals("PRISON",sentence.status());
        assertEquals(1440,sentence.minutesRemaining());
        assertEquals(1,sentence.convictions());
        assertEquals(20,sentence.finePaid());assertEquals(10,sentence.fineOutstanding());
        clock.day=2;JusticeLife.tick(town,clock,truth,observations);
        assertEquals("RELEASED",JusticeLife.view(town,clock).status());
        assertFalse(JusticeLife.confined(town));
        town.player.money=15;
        JusticeLife.payFine(town);
        assertEquals(5,town.player.money);
        assertEquals(0,JusticeLife.view(town,clock).fineOutstanding());
    }
    @Test
    void custodyPersistsAndNormalTravelIsRejectedByServer() throws Exception {
        Path file=directory.resolve("justice.db");
        try(var game=new GameSession(file)){}
        try(var db=new WorldDatabase(file)){
            var clock=db.load();var town=db.loadTown(clock);
            clock.minute=600;town.player.money=50;
            var truth=new ArrayList<TownLife.Event>();var observations=new ArrayList<TownLife.Observation>();
            JusticeLife.report(town,clock,true,true,truth,observations);
            db.save(clock,List.of(),List.of(),town,truth,observations,List.of());
        }
        try(var game=new GameSession(file)){
            assertEquals("DETAINED",game.snapshot().player().justice().status());
            assertThrows(IllegalArgumentException.class,()->game.move("PARK"));
            assertEquals("PRISON",game.waitMinutes(60).world().player().justice().status());
            assertEquals(2880,game.snapshot().player().justice().minutesRemaining());
            game.waitMinutes(1440);
        }
        try(var game=new GameSession(file)){
            assertEquals("PRISON",game.snapshot().player().justice().status());
            assertEquals(1440,game.snapshot().player().justice().minutesRemaining());
            assertEquals("RELEASED",game.waitMinutes(1440).world().player().justice().status());
        }
    }
}
