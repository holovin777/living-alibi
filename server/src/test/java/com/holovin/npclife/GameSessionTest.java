package com.holovin.npclife;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

/**
 * API adapter works on first launch and keeps authoritative world state on disk.
 * No old database or pre-generated player information is required.
 */
class GameSessionTest {
    @TempDir Path directory;

    @Test
    void visitorChoosesCareerAndKeepsItAcrossRestart() throws Exception {
        Path file=directory.resolve("career-world.db");
        try(var game=new GameSession(file)) {
            var visitor=game.snapshot().player();
            assertEquals("Park",visitor.place());
            assertEquals(50,visitor.money());
            assertNull(visitor.jobId());
            assertEquals(0,visitor.engineeringSkill());
            assertEquals(4,game.jobs().size());

            assertThrows(IllegalArgumentException.class,
                    ()->game.applyJob("shop-assistant"));
            assertEquals(50,game.snapshot().player().money());
            game.move("market");
            game.applyJob("shop-assistant");
            assertEquals("shop-assistant",game.snapshot().player().jobId());
            game.waitMinutes(5);
            var shift=game.work();
            assertEquals(74,shift.world().player().money());
            assertThrows(IllegalArgumentException.class,game::work);
            game.move("school");
            var lesson=game.study("engineering");
            assertEquals(64,lesson.world().player().money());
            assertEquals(1,lesson.world().player().engineeringSkill());
            assertEquals(456,game.jobs().stream()
                    .filter(j->j.id().equals("shop-assistant"))
                    .findFirst().orElseThrow().remainingPayroll());
        }
        try(var restarted=new GameSession(file)) {
            assertEquals(64,restarted.snapshot().player().money());
            assertEquals("shop-assistant",restarted.snapshot().player().jobId());
            assertEquals(1,restarted.snapshot().player().engineeringSkill());
            assertEquals(4,restarted.jobs().size(),"job board remains stable");
        }
    }

    @Test
    void newWorldActionsAndRestart() throws Exception {
        Path file=directory.resolve("fresh-world.db");
        try(var game=new GameSession(file)) {
            var initial=game.snapshot();
            assertEquals(1,initial.day());
            assertEquals(450,initial.minute());
            assertEquals("SPRING",initial.season());
            assertEquals(1,initial.year());
            assertNotNull(initial.weather());
            assertEquals(WeatherLife.at(1,450).sky().name(),initial.weather().sky());
            assertEquals(WeatherLife.at(1,450).temperatureC(),
                    initial.weather().temperatureC());
            assertEquals(1,initial.player().incarnation());
            assertTrue(initial.player().alive());
            assertNull(initial.player().privateName());
            assertEquals("Park",initial.player().place());
            assertTrue(initial.sources().stream()
                    .anyMatch(s->s.id().equals("puddle-water")));
            var walk=game.move("river");
            assertEquals("River",walk.world().player().place());
            assertEquals(460,walk.world().minute());
            assertEquals(120,walk.world().sources().getFirst().stock());
            var drink=game.drink("river-water");
            assertEquals(119,drink.world().sources().getFirst().stock());
            var waited=game.waitMinutes(60);
            assertEquals(525,waited.world().minute());
            assertThrows(IllegalArgumentException.class,()->game.waitMinutes(1441));
            assertEquals(525,game.snapshot().minute());
        }
        // The brand-new city should start populated rather than requiring debug commands.
        try(var db=new WorldDatabase(file)) {
            var town=db.loadTown(db.load());
            assertEquals(14,town.people.size(),"12 generated NPCs + Marco + Elena");
        }
        try(var reloaded=new GameSession(file)) {
            assertEquals(525,reloaded.snapshot().minute());
            assertEquals("River",reloaded.snapshot().player().place());
            assertEquals(119,reloaded.snapshot().sources().getFirst().stock());
            var next=reloaded.move("woods");
            assertEquals("Woods",next.world().player().place());
            assertTrue(next.world().sources().stream()
                    .anyMatch(s->s.id().equals("wild-berries")));
        }
    }
}
