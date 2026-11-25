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
    void marketPurchaseConservesCashAndSuppliesAcrossRestart() throws Exception {
        Path file=directory.resolve("retail.db");
        try(var game=new GameSession(file)) {
            assertThrows(IllegalArgumentException.class,()->game.buy("bread"));
            game.move("market");
            game.waitMinutes(60);
            int stock=game.snapshot().shop().stream().filter(x->x.id().equals("bread")).findFirst().orElseThrow().stock();
            int cash=game.jobs().stream().filter(x->x.id().equals("shop-assistant")).findFirst().orElseThrow().remainingPayroll();
            game.buy("bread");
            assertEquals(47,game.snapshot().player().money());
            assertEquals(1,game.snapshot().player().bread());
            assertEquals(stock-1,game.snapshot().shop().stream().filter(x->x.id().equals("bread")).findFirst().orElseThrow().stock());
            assertEquals(cash+3,game.jobs().stream().filter(x->x.id().equals("shop-assistant")).findFirst().orElseThrow().remainingPayroll());
            game.buy("water");
            assertEquals(46,game.snapshot().player().money());
        }
        try(var game=new GameSession(file)) {
            assertEquals(1,game.snapshot().player().bread());
            assertEquals(1,game.snapshot().player().bottledWater());
            game.move("park");
            game.waitMinutes(120);
            int hunger=game.snapshot().player().hunger();
            game.consume("bread");
            assertEquals(0,game.snapshot().player().bread());
            assertTrue(game.snapshot().player().hunger()<hunger);
            assertThrows(IllegalArgumentException.class,()->game.consume("bread"));
            game.consume("water");
            assertEquals(0,game.snapshot().player().bottledWater());
        }
    }

    @Test
    void conversationIsLocalAndMemorySurvivesRestart() throws Exception {
        Path file=directory.resolve("conversation.db");
        try(var game=new GameSession(file)) { }
        String id;
        try(var db=new WorldDatabase(file)) {
            var state=db.load();
            var town=db.loadTown(state);
            var resident=town.people.values().iterator().next();
            id=resident.id;
            resident.place=TownLife.Place.PARK;
            resident.travelRemaining=0;
            resident.activity="FREE_TIME";
            db.save(state,java.util.List.of(),java.util.List.of(),town,
                    java.util.List.of(),java.util.List.of(),java.util.List.of());
        }
        try(var game=new GameSession(file)) {
            assertThrows(IllegalArgumentException.class,()->game.speak("missing","Hello"));
            int before=game.snapshot().minute();
            assertTrue(game.speak(id,"My name is Alex.").message().contains("Alex"));
            assertEquals((before+1)%1440,game.snapshot().minute());
        }
        try(var db=new WorldDatabase(file)) {
            var town=db.loadTown(db.load());
            assertEquals("Alex",town.player.contacts.get(id).knownName);
            assertEquals(1,town.player.contacts.get(id).conversations);
            assertEquals(53,town.player.contacts.get(id).trust);
            assertTrue(town.player.contacts.get(id).affinity>0);
        }
        try(var game=new GameSession(file)) {
            game.move("quarry");
            assertThrows(IllegalArgumentException.class,()->game.speak(id,"Hello"));
        }
    }

    @Test
    void publicVisitorNewsUsesActualSocialEncountersAndPersists() throws Exception {
        Path file=directory.resolve("visitor-news.db");
        try(var game=new GameSession(file)) { }
        String key;
        try(var db=new WorldDatabase(file)) {
            var state=db.load();
            var town=db.loadTown(state);
            var speaker=town.get("marco");
            var listener=town.get("elena");
            speaker.place=TownLife.Place.PARK;
            listener.place=TownLife.Place.CAFE;
            speaker.activity=listener.activity="FREE_TIME";
            speaker.travelRemaining=listener.travelRemaining=0;
            speaker.sociability=80;
            town.relationship.trust=70;
            PlayerLife.setPrivateName(town.player,"Private identity");
            PlayerLife.speak(town,speaker.id,"My name is Alex.",state.day,state.minute);
            assertFalse(town.beliefs.getOrDefault(speaker.id,new java.util.LinkedHashMap<>())
                    .values().stream().anyMatch(b->b.actorId().startsWith("visitor-")));
            PlayerLife.speak(town,speaker.id,"You can tell people my name.",state.day,state.minute);
            var claim=town.beliefs.get(speaker.id).values().stream()
                    .filter(b->b.actorId().startsWith("visitor-")).findFirst().orElseThrow();
            key=claim.eventKey();
            assertFalse(claim.statement().contains("Private identity"));
            assertTrue(claim.statement().contains("Alex"));
            KnowledgeLife.exchange(town,state,speaker.id,listener.id);
            assertFalse(town.beliefs.getOrDefault(listener.id,new java.util.LinkedHashMap<>()).containsKey(key));
            listener.place=TownLife.Place.PARK;
            KnowledgeLife.exchange(town,state,speaker.id,listener.id);
            var heard=town.beliefs.get(listener.id).get(key);
            assertEquals("HEARD",heard.origin());
            assertEquals(speaker.id,heard.learnedFrom());
            assertEquals(1,heard.hops());
            assertTrue(heard.confidence()<claim.confidence());
            assertFalse(town.player.contacts.containsKey(listener.id));
            assertTrue(KnowledgeLife.visitorNews(town,listener,state.day,state.minute).contains("Alex"));
            db.save(state,java.util.List.of(),java.util.List.of(),town,
                    java.util.List.of(),java.util.List.of(),java.util.List.of());
        }
        try(var db=new WorldDatabase(file)) {
            var state=db.load();
            var town=db.loadTown(state);
            assertEquals("HEARD",town.beliefs.get("elena").get(key).origin());
            assertTrue(KnowledgeLife.visitorNews(town,town.get("elena"),state.day,state.minute).contains("can't verify"));
        }
    }

    @Test
    void socialAttitudeIsPersonalAndCannotBeFarmedInOneDay() throws Exception {
        Path file=directory.resolve("attitude.db");
        try(var game=new GameSession(file)) { }
        try(var db=new WorldDatabase(file)) {
            var state=db.load();
            var town=db.loadTown(state);
            var resident=town.people.values().iterator().next();
            resident.place=TownLife.Place.PARK;
            resident.travelRemaining=0;
            resident.activity="FREE_TIME";
            PlayerLife.speak(town,resident.id,"Hello",1,450);
            var contact=town.player.contacts.get(resident.id);
            int initialAffinity=contact.affinity;
            for(int i=0;i<20;i++)PlayerLife.speak(town,resident.id,"Hello",1,450+i);
            assertEquals(initialAffinity,contact.affinity);
            for(int day=2;day<=12;day++)PlayerLife.speak(town,resident.id,"Hello",day,450);
            assertEquals("FRIENDLY",PlayerLife.relationship(contact));
            assertEquals("STRANGER",PlayerLife.relationship(null));
            for(int i=0;i<8;i++)PlayerLife.speak(town,resident.id,"Leave me alone!",12,460+i);
            assertEquals("WARY",PlayerLife.relationship(contact));
            assertEquals(1,town.player.contacts.size());
            db.save(state,java.util.List.of(),java.util.List.of(),town,
                    java.util.List.of(),java.util.List.of(),java.util.List.of());
        }
        try(var db=new WorldDatabase(file)) {
            var town=db.loadTown(db.load());
            var contact=town.player.contacts.values().iterator().next();
            assertEquals("WARY",PlayerLife.relationship(contact));
            assertEquals(12,contact.lastPositiveDay);
        }
    }

    @Test
    void businessMachineServiceConsumesRealCityPartsAndSurvivesServerRestart() throws Exception {
        Path file=directory.resolve("worn-machine.db");
        try(var game=new GameSession(file)) {
            game.move("workshop");
            game.openBusiness();
            game.investBusiness(25);
            game.buyBusinessMachine();
            assertEquals(100,game.business().machineCondition());
            assertThrows(IllegalArgumentException.class,game::serviceBusinessMachine,
                    "machine in perfect condition should not waste repair parts");
        }
        try(var db=new WorldDatabase(file)) {
            var state=db.load();
            var town=db.loadTown(state);
            town.business.machineCondition=40;
            town.business.cash=30;
            db.save(state,java.util.List.of(),java.util.List.of(),town,
                    java.util.List.of(),java.util.List.of(),java.util.List.of());
        }
        try(var game=new GameSession(file)) {
            assertEquals(40,game.business().machineCondition());
            int initialCityParts=6;
            var repaired=game.serviceBusinessMachine();
            assertTrue(repaired.message().contains("100/100"));
            assertEquals(100,game.business().machineCondition());
            assertEquals(20,game.business().cash());
            // Repaired equipment is a material-consuming transaction.
            try(var db=new WorldDatabase(file)) {
                var town=db.loadTown(db.load());
                assertEquals(initialCityParts-2,town.technicalMaterials.get("spare-parts"));
            }
        }
        try(var game=new GameSession(file)) {
            assertEquals(100,game.business().machineCondition());
            assertEquals(20,game.business().cash());
            assertThrows(IllegalArgumentException.class,game::serviceBusinessMachine);
        }
    }

    @Test
    void founderCanOpenCapitalFundedWorkshopWithoutCreatingMoney() throws Exception {
        Path file=directory.resolve("first-workshop.db");
        try(var game=new GameSession(file)) {
            assertFalse(game.business().registered());
            assertThrows(IllegalArgumentException.class,game::openBusiness,
                    "you cannot register a distant company by clicking from the park");
            game.move("workshop");
            var opened=game.openBusiness();
            assertTrue(opened.message().contains("Registered"));
            assertTrue(game.business().registered());
            assertTrue(game.business().yours());
            assertEquals(35,game.snapshot().player().money());
            game.investBusiness(25);
            assertEquals(25,game.business().cash());
            assertEquals(10,game.snapshot().player().money());
            game.buyBusinessMachine();
            assertTrue(game.business().machine());
            assertEquals(0,game.business().cash());
            assertThrows(IllegalArgumentException.class,
                    ()->game.hireBusinessWorker("resident-0001"),
                    "a company without enough money for wages cannot recruit people");
            assertThrows(IllegalArgumentException.class,
                    ()->game.withdrawBusiness(1));
            assertThrows(IllegalArgumentException.class,
                    ()->game.investBusiness(100));
        }
        try(var reopened=new GameSession(file)) {
            assertTrue(reopened.business().registered());
            assertTrue(reopened.business().machine());
            assertTrue(reopened.business().yours());
            assertEquals(0,reopened.business().cash());
            assertEquals(10,reopened.snapshot().player().money());
            assertTrue(reopened.businessCandidates().stream()
                    .allMatch(c->c.id().startsWith("resident-")));
        }
    }

    @Test
    void ordinaryVisitorCanBecomeMinerOrMachinistAndSurveyNewOre() throws Exception {
        Path mine=directory.resolve("visitor-miner.db");
        try(var game=new GameSession(mine)) {
            assertEquals(6,game.jobs().size());
            game.move("quarry");
            game.applyJob("miner");
            game.waitMinutes(30);
            var paid=game.work();
            assertEquals("miner",paid.world().player().jobId());
            assertEquals(80,paid.world().player().money(),
                    "the player earns real company cash and actually mines");
            assertTrue(game.industry().ironRemaining()<80);
            int beforeSurveyIron=game.industry().ironRemaining();
            var surveyed=game.prospect();
            assertTrue(surveyed.message().contains("Western"));
            assertTrue(game.industry().westDepositDiscovered());
            assertEquals(beforeSurveyIron+30,game.industry().ironRemaining(),
                    "geological survey reveals thirty additional finite iron units");
            assertThrows(IllegalArgumentException.class,game::prospect);
        }
        try(var restarted=new GameSession(mine)) {
            assertEquals("miner",restarted.snapshot().player().jobId());
            assertEquals(80,restarted.snapshot().player().money());
            assertTrue(restarted.industry().westDepositDiscovered(),
                    "geology and company finances survive server restarts");
        }
        Path factory=directory.resolve("visitor-machinist.db");
        try(var game=new GameSession(factory)) {
            game.move("factory");
            game.applyJob("machinist");
            game.waitMinutes(30);
            var worked=game.work();
            assertEquals("machinist",worked.world().player().jobId());
            assertEquals(84,worked.world().player().money());
            assertTrue(game.industry().scrapRemaining()<10,
                    "machinist genuinely converts stored waste into steel");
        }
    }

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
