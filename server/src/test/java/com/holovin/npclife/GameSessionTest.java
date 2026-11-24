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
    void freshWorldHasFiniteEnterprisesAndTheirStoresPersist() throws Exception {
        Path file=directory.resolve("factory-world.db");
        try(var game=new GameSession(file)) {
            assertEquals(80,game.industry().ironRemaining());
            assertEquals(40,game.industry().copperRemaining());
            assertEquals(6,game.industry().workshopParts());
            assertEquals(4,game.industry().workshopSteel());
            assertEquals(3,game.industry().workshopCopper());
            assertTrue(game.destinations().stream().anyMatch(d->d.id().equals("QUARRY")));
            assertTrue(game.destinations().stream().anyMatch(d->d.id().equals("FACTORY")));
            // Existing player freedoms still work; viewing a stock report
            // never grants free components or changes the world.
            assertEquals(80,game.industry().ironRemaining());
        }
        try(var db=new WorldDatabase(file)) {
            var state=db.load();
            var town=db.loadTown(state);
            town.industryStocks.put("seam:iron-ore",77);
            town.industryStocks.put("factory:spare-parts",2);
            db.save(state,java.util.List.of(),java.util.List.of(),
                    town,java.util.List.of(),java.util.List.of(),java.util.List.of());
        }
        try(var game=new GameSession(file)) {
            assertEquals(77,game.industry().ironRemaining());
            assertEquals(2,game.industry().factoryParts());
            assertEquals(6,game.industry().workshopParts(),
                    "unfinished factory parts are not yet delivered to repairs");
        }
    }

    @Test
    void visitorChoosesCareerAndKeepsItAcrossRestart() throws Exception {
        Path file=directory.resolve("career-world.db");
        try(var game=new GameSession(file)) {
            var visitor=game.snapshot().player();
            assertEquals("Park",visitor.place());
            assertEquals("PARK",visitor.placeId());
            assertEquals(0,game.destinations().stream()
                    .filter(d->d.id().equals("PARK")).findFirst().orElseThrow().travelMinutes());
            assertEquals(10,game.destinations().stream()
                    .filter(d->d.id().equals("MARKET")).findFirst().orElseThrow().travelMinutes());
            assertEquals(50,visitor.money());
            assertNull(visitor.jobId());
            assertEquals(0,visitor.engineeringSkill());
            assertEquals(6,game.jobs().size());

            assertThrows(IllegalArgumentException.class,
                    ()->game.applyJob("shop-assistant"));
            assertEquals(50,game.snapshot().player().money());
            game.move("market");
            assertEquals("MARKET",game.snapshot().player().placeId());
            assertEquals(8,game.destinations().stream()
                    .filter(d->d.id().equals("SCHOOL")).findFirst().orElseThrow().travelMinutes());
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
            assertEquals("SCHOOL",restarted.snapshot().player().placeId());
            assertEquals("shop-assistant",restarted.snapshot().player().jobId());
            assertEquals(1,restarted.snapshot().player().engineeringSkill());
            assertEquals(6,restarted.jobs().size(),"job board remains stable");
        }
    }

    @Test
    void parkFountainTransfersLimitedWaterAndSurvivesRestart() throws Exception {
        Path file=directory.resolve("public-utilities.db");
        try(var db=new WorldDatabase(file)) {
            var state=db.load();
            var town=db.loadTown(state);
            town.technicalOrders.get("water-pump").condition=100;
            town.cleanWater=3;
            db.save(state,java.util.List.of(),java.util.List.of(),
                    town,java.util.List.of(),java.util.List.of(),java.util.List.of());
        }
        try(var game=new GameSession(file)) {
            assertTrue(game.snapshot().services().fountainWorking());
            assertEquals(3,game.services().cleanWater());
            assertEquals(50,game.snapshot().services().lightingCondition());
            var result=game.drink("park-fountain");
            assertEquals(2,result.world().services().cleanWater());
            assertEquals(0,result.world().player().thirst());
        }
        try(var reopened=new GameSession(file)) {
            assertEquals(2,reopened.snapshot().services().cleanWater());
            reopened.drink("park-fountain");
            reopened.drink("park-fountain");
            assertEquals(0,reopened.snapshot().services().cleanWater());
            assertThrows(IllegalArgumentException.class,
                    ()->reopened.drink("park-fountain"));
            assertEquals(0,reopened.snapshot().services().cleanWater());
        }
    }

    @Test
    void paidTechnicalShiftRequiresRealRepairAndSurvivesRestart() throws Exception {
        Path file=directory.resolve("technical-game.db");
        try(var game=new GameSession(file)) {
            assertEquals("OPEN",game.workOrders().stream()
                    .filter(o->o.id().equals("water-pump"))
                    .findFirst().orElseThrow().phase());
            assertFalse(game.services().fountainWorking());
            assertFalse(game.services().lightsWorking());
            game.move("school");
            game.waitMinutes(20); // Day 1 08:00
            for(int i=0;i<3;i++)game.study("engineering");
            game.move("workshop");
            game.applyJob("technician");
            assertThrows(IllegalArgumentException.class,game::work);
            game.diagnose("water-pump");
            var task=game.workOrders().stream()
                    .filter(o->o.id().equals("water-pump"))
                    .findFirst().orElseThrow();
            assertEquals("INSPECTED",task.phase());
            var finished=game.repair("water-pump");
            assertTrue(finished.message().contains("45 -> 100"));
            assertTrue(game.snapshot().services().fountainWorking());
            assertEquals(6,game.services().cleanWater());
            assertEquals(100,game.workOrders().stream()
                    .filter(o->o.id().equals("water-pump"))
                    .findFirst().orElseThrow().condition());
            assertEquals(20,game.snapshot().player().money());
            var paid=game.work();
            assertEquals(58,paid.world().player().money());
            assertThrows(IllegalArgumentException.class,()->game.repair("water-pump"));
        }
        try(var reopened=new GameSession(file)) {
            assertEquals(58,reopened.snapshot().player().money());
            var task=reopened.workOrders().stream()
                    .filter(o->o.id().equals("water-pump"))
                    .findFirst().orElseThrow();
            assertEquals(100,task.condition());
            assertEquals("COMPLETED",task.phase());
            assertThrows(IllegalArgumentException.class,reopened::work);
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
