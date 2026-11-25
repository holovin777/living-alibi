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
    void linkedAdultFamilyIsVoluntaryAndConservesCash() throws Exception {
        Path file=directory.resolve("linked-family.db");
        String adultId,partnerId;
        try(var db=new WorldDatabase(file)){
            var state=db.load();var town=db.loadTown(state);
            var people=PopulationGenerator.add(town,2);
            var a=people.get(0);var b=people.get(1);
            adultId=a.id;partnerId=b.id;
            a.age=30;b.age=30;a.money=200;b.money=200;
            a.place=TownLife.Place.PARK;b.place=a.place;a.travelRemaining=0;b.travelRemaining=0;
            a.activity="FREE_TIME";b.activity="FREE_TIME";
            town.player.alive=false;town.player.money=0;
            town.industryStocks.put(RebirthLife.PHASE,RebirthLife.LINKED_ADULT);
            town.industryStocks.put(RebirthLife.CHILD,a.generationIndex);
            DemographyLife.set(town,a,"played-incarnation",town.player.incarnation);
            var r=new SocialLife.Relationship(a.id,b.id);r.affinity=80;r.trust=80;r.tension=5;
            town.peerRelationships.put(SocialNetwork.key(a.id,b.id),r);
            state.day=31;state.minute=359;
            // The automatic family cycle must not assign a partner to the player.
            state.minute=360;
            DemographyLife.tick(town,state,new java.util.ArrayList<>(),new java.util.ArrayList<>());
            assertEquals(0,DemographyLife.get(town,a,"partner"));
            state.minute=850;
            db.save(state,java.util.List.of(),java.util.List.of(),town,java.util.List.of(),java.util.List.of(),java.util.List.of());
        }
        try(var game=new GameSession(file)){
            assertTrue(game.snapshot().rebirth().familyOptions().proposals().stream().anyMatch(p->p.id().equals(partnerId)));
            var paired=game.gardenAction("rebirth-propose-"+partnerId).world();
            assertEquals(partnerId,paired.rebirth().familyOptions().partner().id());
            assertThrows(IllegalArgumentException.class,()->game.gardenAction("rebirth-propose-"+partnerId));
            assertFalse(paired.rebirth().familyOptions().canHaveChild());
            assertThrows(IllegalArgumentException.class,()->game.gardenAction("rebirth-have-child"));
            var moved=game.gardenAction("rebirth-move-together").world();
            assertTrue(moved.rebirth().familyOptions().livingTogether());
            var supported=game.gardenAction("rebirth-support-partner").world();
            assertEquals(190,supported.player().money());
            assertEquals(210,supported.rebirth().familyOptions().partner().savings());
        }
        try(var db=new WorldDatabase(file)){
            var state=db.load();var town=db.loadTown(state);
            var a=town.get(adultId);var b=town.get(partnerId);
            assertEquals(a.homeUnit,b.homeUnit);
            state.day=301;state.minute=360;
            DemographyLife.tick(town,state,new java.util.ArrayList<>(),new java.util.ArrayList<>());
            assertEquals(0,DemographyLife.get(town,a,"children"));
            state.minute=850;
            db.save(state,java.util.List.of(),java.util.List.of(),town,java.util.List.of(),java.util.List.of(),java.util.List.of());
        }
        try(var game=new GameSession(file)){
            assertTrue(game.snapshot().rebirth().familyOptions().canHaveChild());
            var born=game.gardenAction("rebirth-have-child").world();
            assertEquals(180,born.player().money());
            assertEquals(200,born.rebirth().familyOptions().partner().savings());
            assertEquals(1,born.rebirth().familyOptions().children().size());
            assertEquals(20,born.rebirth().familyOptions().children().getFirst().savings());
            assertThrows(IllegalArgumentException.class,()->game.gardenAction("rebirth-have-child"));
        }
        try(var game=new GameSession(file)){
            assertEquals(1,game.snapshot().rebirth().familyOptions().children().size());
            assertTrue(game.snapshot().rebirth().familyOptions().livingTogether());
        }
    }
    @Test
    void linkedAdultGroceryUsesSharedRetailInventoryAndWallet() throws Exception {
        Path file=directory.resolve("linked-grocery.db");
        String id;
        try(var db=new WorldDatabase(file)){
            var state=db.load();var town=db.loadTown(state);
            var a=PopulationGenerator.add(town,1).getFirst();
            id=a.id;a.age=30;a.money=20;a.food=3;a.water=3;
            a.home=TownLife.Place.NORTH_RESIDENCES;a.place=TownLife.Place.PORTICI;
            a.destination=a.place;a.travelRemaining=0;a.activity="FREE_TIME";
            a.hunger=10;a.thirst=10;a.employed=false;
            town.player.alive=false;town.player.money=0;
            town.industryStocks.put(RebirthLife.PHASE,RebirthLife.LINKED_ADULT);
            town.industryStocks.put(RebirthLife.CHILD,a.generationIndex);
            DemographyLife.set(town,a,"played-incarnation",town.player.incarnation);
            state.day=2;state.minute=800;
            town.retailStock.put("bread",1);town.retailStock.put("water",2);
            assertThrows(IllegalArgumentException.class,()->RebirthLife.buyGrocery(town,2,1200,"bread"));
            assertThrows(IllegalArgumentException.class,()->RebirthLife.buyGrocery(town,2,800,"unknown"));
            int funds=town.employerFunds.getOrDefault("shop-assistant",0);
            RebirthLife.buyGrocery(town,2,800,"bread");
            assertEquals(17,a.money);assertEquals(4,a.food);
            assertEquals(0,town.retailStock.get("bread").intValue());
            assertEquals(funds+3,town.employerFunds.get("shop-assistant").intValue());
            assertThrows(IllegalArgumentException.class,()->RebirthLife.buyGrocery(town,2,800,"bread"));
            db.save(state,java.util.List.of(),java.util.List.of(),town,java.util.List.of(),java.util.List.of(),java.util.List.of());
        }
        try(var game=new GameSession(file)){
            assertTrue(game.snapshot().rebirth().grocery().open());
            var purchased=game.gardenAction("rebirth-grocery-water").world();
            assertEquals(16,purchased.player().money());
            assertEquals(4,purchased.rebirth().grocery().water());
            assertEquals(805,purchased.minute());
        }
        try(var db=new WorldDatabase(file)){
            var state=db.load();var town=db.loadTown(state);
            assertEquals(1,town.retailStock.get("water").intValue());
            assertEquals(4,town.get(id).water);
            assertEquals(16,town.get(id).money);
        }
    }
    @Test
    void bakeryCounterSharesBreadStockAndHasItsOwnHours() throws Exception {
        Path file=directory.resolve("linked-bakery.db");
        try(var db=new WorldDatabase(file)){
            var state=db.load();var town=db.loadTown(state);
            var c=PopulationGenerator.add(town,1).getFirst();
            c.age=30;c.money=20;c.food=3;c.water=3;c.home=TownLife.Place.NORTH_RESIDENCES;
            c.place=TownLife.Place.BAKERY;c.destination=c.place;c.activity="FREE_TIME";
            c.hunger=10;c.thirst=10;c.employed=false;
            town.player.alive=false;town.player.money=0;
            town.industryStocks.put(RebirthLife.PHASE,RebirthLife.LINKED_ADULT);
            town.industryStocks.put(RebirthLife.CHILD,c.generationIndex);
            DemographyLife.set(town,c,"played-incarnation",town.player.incarnation);
            state.day=2;state.minute=430;
            town.retailStock.put("bread",1);
            assertFalse(RebirthLife.grocery(town,2,419).open());
            assertTrue(RebirthLife.grocery(town,2,420).open());
            assertFalse(RebirthLife.grocery(town,2,1076).open());
            assertThrows(IllegalArgumentException.class,()->RebirthLife.buyGrocery(town,2,430,"eggs"));
            assertEquals("Panetteria del Borgo",RebirthLife.grocery(town,2,430).name());
            db.save(state,java.util.List.of(),java.util.List.of(),town,java.util.List.of(),java.util.List.of(),java.util.List.of());
        }
        try(var game=new GameSession(file)){
            var bought=game.gardenAction("rebirth-grocery-bread").world();
            assertEquals(17,bought.player().money());
            assertEquals(4,bought.rebirth().grocery().food());
            assertEquals(435,bought.minute());
            assertThrows(IllegalArgumentException.class,()->game.gardenAction("rebirth-grocery-bread"));
        }
        try(var game=new GameSession(file)){
            assertEquals(0,game.snapshot().rebirth().grocery().items().getFirst().stock());
            assertEquals(17,game.snapshot().player().money());
        }
    }
    @Test
    void linkedAdultCafeOrdersAndRestUseActualWalletAndTime() throws Exception {
        Path file=directory.resolve("linked-cafe.db");
        try(var db=new WorldDatabase(file)){
            var state=db.load();var town=db.loadTown(state);
            var c=PopulationGenerator.add(town,1).getFirst();
            c.age=30;c.money=20;c.fatigue=50;c.thirst=30;c.hunger=10;c.food=4;c.water=4;
            c.home=TownLife.Place.NORTH_RESIDENCES;c.place=TownLife.Place.CAFE;c.destination=c.place;c.activity="FREE_TIME";
            c.employed=false;town.player.alive=false;town.player.money=0;
            town.industryStocks.put(RebirthLife.PHASE,RebirthLife.LINKED_ADULT);
            town.industryStocks.put(RebirthLife.CHILD,c.generationIndex);
            DemographyLife.set(town,c,"played-incarnation",town.player.incarnation);
            state.day=2;state.minute=800;town.retailStock.put("water",1);
            assertFalse(RebirthLife.cafe(town,2,1260).open());
            assertThrows(IllegalArgumentException.class,()->RebirthLife.cafeAction(town,2,800,"unknown"));
            db.save(state,java.util.List.of(),java.util.List.of(),town,java.util.List.of(),java.util.List.of(),java.util.List.of());
        }
        try(var game=new GameSession(file)){
            var coffee=game.gardenAction("rebirth-cafe-espresso").world();
            assertEquals(19,coffee.player().money());
            assertEquals(805,coffee.minute());
            assertTrue(coffee.player().fatigue()<50);
            var water=game.gardenAction("rebirth-cafe-water").world();
            assertEquals(18,water.player().money());
            assertThrows(IllegalArgumentException.class,()->game.gardenAction("rebirth-cafe-water"));
            var rest=game.gardenAction("rebirth-cafe-rest").world();
            assertEquals(830,rest.minute());
            assertFalse(rest.rebirth().cafe().canRest());
        }
        try(var game=new GameSession(file)){
            assertFalse(game.snapshot().rebirth().cafe().canRest());
            assertEquals(18,game.snapshot().player().money());
        }
    }
    @Test
    void sickLinkedAdultCanVisitClinicAndReceiveBoundedCare() throws Exception {
        Path file=directory.resolve("linked-medical.db");
        try(var db=new WorldDatabase(file)){
            var state=db.load();var town=db.loadTown(state);
            var c=PopulationGenerator.add(town,1).getFirst();
            c.age=30;c.money=40;c.health=70;c.fatigue=30;c.hunger=10;c.thirst=10;c.food=5;c.water=5;
            c.home=TownLife.Place.NORTH_RESIDENCES;c.place=c.home;c.destination=c.home;c.activity="SICK_LEAVE";
            c.sickUntilDay=4;c.employed=false;town.player.alive=false;town.player.money=0;
            town.industryStocks.put(RebirthLife.PHASE,RebirthLife.LINKED_ADULT);
            town.industryStocks.put(RebirthLife.CHILD,c.generationIndex);
            DemographyLife.set(town,c,"played-incarnation",town.player.incarnation);
            state.day=2;state.minute=800;
            assertFalse(EducationLife.availableRole(town,c,"Doctor"));
            EducationLife.set(town,c,"academic",70);EducationLife.set(town,c,"social",40);
            assertFalse(EducationLife.availableRole(town,c,"Doctor"));
            EducationLife.set(town,c,"diploma-medicine",1);
            assertTrue(EducationLife.availableRole(town,c,"Doctor"));
            db.save(state,java.util.List.of(),java.util.List.of(),town,java.util.List.of(),java.util.List.of(),java.util.List.of());
        }
        try(var game=new GameSession(file)){
            assertTrue(game.snapshot().rebirth().medical().canVisit());
            assertThrows(IllegalArgumentException.class,()->game.gardenAction("rebirth-medical-consult"));
            var arrival=game.gardenAction("rebirth-medical-visit").world();
            assertEquals("CLINIC",arrival.player().placeId());
            assertTrue(arrival.rebirth().medical().atClinic());
            var consult=game.gardenAction("rebirth-medical-consult").world();
            assertEquals(25,consult.player().money());
            assertEquals(3,consult.rebirth().medical().sickUntilDay());
            assertTrue(consult.player().health()>=80);
            assertThrows(IllegalArgumentException.class,()->game.gardenAction("rebirth-medical-consult"));
            var aid=game.gardenAction("rebirth-medical-first-aid").world();
            assertEquals(20,aid.player().money());
            assertFalse(aid.rebirth().medical().canFirstAid());
        }
        try(var game=new GameSession(file)){
            assertFalse(game.snapshot().rebirth().medical().canConsult());
            assertFalse(game.snapshot().rebirth().medical().canFirstAid());
            assertTrue(game.gardenAction("rebirth-home-go").world().rebirth().homeOptions().atHome());
        }
    }
    @Test
    void collegeLessonsPersistAndUnlockQualifiedCareer() throws Exception {
        Path file=directory.resolve("linked-college.db");
        String id;
        try(var db=new WorldDatabase(file)){
            var state=db.load();var town=db.loadTown(state);
            var c=PopulationGenerator.add(town,1).getFirst();id=c.id;
            c.age=25;c.money=30;c.hunger=10;c.thirst=10;c.fatigue=20;c.food=5;c.water=5;
            c.home=TownLife.Place.NORTH_RESIDENCES;c.place=TownLife.Place.SCHOOL;c.destination=c.place;c.activity="FREE_TIME";
            c.employed=false;town.player.alive=false;town.player.money=0;
            town.industryStocks.put(RebirthLife.PHASE,RebirthLife.LINKED_ADULT);
            town.industryStocks.put(RebirthLife.CHILD,c.generationIndex);
            DemographyLife.set(town,c,"played-incarnation",town.player.incarnation);
            state.day=2;state.minute=800;
            EducationLife.set(town,c,"technical",70);
            assertFalse(EducationLife.availableRole(town,c,"Engineer"));
            assertThrows(IllegalArgumentException.class,()->EducationLife.enroll(town,6,800,"engineering"));
            db.save(state,java.util.List.of(),java.util.List.of(),town,java.util.List.of(),java.util.List.of(),java.util.List.of());
        }
        try(var game=new GameSession(file)){
            game.gardenAction("rebirth-enroll-engineering");
            var lesson=game.gardenAction("rebirth-study").world();
            assertEquals(28,lesson.player().money());
            assertEquals(1,lesson.rebirth().college().completed());
            assertEquals(860,lesson.minute());
            assertThrows(IllegalArgumentException.class,()->game.gardenAction("rebirth-study"));
            assertThrows(IllegalArgumentException.class,()->game.gardenAction("rebirth-enroll-programming"));
        }
        try(var db=new WorldDatabase(file)){
            var state=db.load();var town=db.loadTown(state);var c=town.get(id);
            assertEquals(1,EducationLife.get(town,c,"course-lessons"));
            EducationLife.set(town,c,"course-lessons",59);state.day=3;state.minute=800;
            db.save(state,java.util.List.of(),java.util.List.of(),town,java.util.List.of(),java.util.List.of(),java.util.List.of());
        }
        try(var game=new GameSession(file)){
            var graduation=game.gardenAction("rebirth-study").world();
            assertNull(graduation.rebirth().college().enrolled());
            assertTrue(graduation.rebirth().adultOptions().jobs().contains("Engineer"));
            assertFalse(graduation.rebirth().adultOptions().jobs().contains("Programmer"));
            game.gardenAction("rebirth-job-Engineer");
            assertEquals("Engineer",game.snapshot().rebirth().adultOptions().occupation());
        }
    }
    @Test
    void linkedEngineerRepairsSharedPumpAndEarnsFundedWage() throws Exception {
        Path file=directory.resolve("linked-engineer.db");
        String id;int payroll;
        try(var db=new WorldDatabase(file)){
            var state=db.load();var town=db.loadTown(state);
            var c=PopulationGenerator.add(town,1).getFirst();id=c.id;
            c.age=30;c.money=30;c.hunger=10;c.thirst=10;c.fatigue=20;c.food=5;c.water=5;
            c.home=TownLife.Place.NORTH_RESIDENCES;c.place=TownLife.Place.WORKSHOP;c.destination=c.place;c.activity="FREE_TIME";
            c.workplace=TownLife.Place.WORKSHOP;c.occupation="Engineer";c.employed=true;
            town.player.alive=false;town.player.money=0;
            town.industryStocks.put(RebirthLife.PHASE,RebirthLife.LINKED_ADULT);
            town.industryStocks.put(RebirthLife.CHILD,c.generationIndex);
            DemographyLife.set(town,c,"played-incarnation",town.player.incarnation);
            EducationLife.set(town,c,"technical",70);EducationLife.set(town,c,"diploma-engineering",1);
            town.technicalMaterials.put("spare-parts",6);
            payroll=town.employerFunds.getOrDefault("technician",0);
            state.day=2;state.minute=800;
            db.save(state,java.util.List.of(),java.util.List.of(),town,java.util.List.of(),java.util.List.of(),java.util.List.of());
        }
        try(var game=new GameSession(file)){
            assertThrows(IllegalArgumentException.class,()->game.gardenAction("rebirth-engineer-repair-water-pump"));
            var inspection=game.gardenAction("rebirth-engineer-inspect-water-pump").world();
            assertEquals(830,inspection.minute());
            var repair=game.gardenAction("rebirth-engineer-repair-water-pump").world();
            assertEquals(920,repair.minute());
            assertEquals(68,repair.player().money());
            assertThrows(IllegalArgumentException.class,()->game.gardenAction("rebirth-engineer-repair-water-pump"));
        }
        try(var db=new WorldDatabase(file)){
            var state=db.load();var town=db.loadTown(state);
            assertEquals(4,town.technicalMaterials.get("spare-parts").intValue());
            assertEquals(100,town.technicalOrders.get("water-pump").condition);
            assertTrue(UtilityLife.status(town).fountainWorking());
            assertEquals(payroll-38,town.employerFunds.get("technician").intValue());
            assertEquals(2,EducationLife.get(town,town.get(id),"last-engineering-order"));
        }
    }
    @Test
    void javaTicketChecksRealBoundaryCasesAndRejectsUnsupportedCode(){
        JavaTicketLife.verify("factory-planning","return Math.min(stock, Math.max(0, limit));");
        JavaTicketLife.verify("water-control","return Math.min(Math.min(stock, freeCapacity), requested);");
        assertThrows(IllegalArgumentException.class,()->JavaTicketLife.verify("factory-planning","return Math.min(stock, limit);"));
        assertThrows(IllegalArgumentException.class,()->JavaTicketLife.verify("water-control","return 0;"));
        assertThrows(IllegalArgumentException.class,()->JavaTicketLife.verify("factory-planning","return stock; System.exit(0);"));
    }
    @Test
    void programmerDeploysPersistentSoftwareWithFundedContract() throws Exception {
        Path file=directory.resolve("linked-software.db");
        String id;int budget;
        try(var db=new WorldDatabase(file)){
            var state=db.load();var town=db.loadTown(state);
            var c=PopulationGenerator.add(town,1).getFirst();id=c.id;
            c.age=30;c.money=30;c.hunger=10;c.thirst=10;c.fatigue=20;c.food=5;c.water=5;
            c.home=TownLife.Place.NORTH_RESIDENCES;c.place=TownLife.Place.OFFICE;c.destination=c.place;c.activity="FREE_TIME";
            c.workplace=TownLife.Place.OFFICE;c.occupation="Programmer";c.employed=true;
            town.player.alive=false;town.player.money=0;
            town.industryStocks.put(RebirthLife.PHASE,RebirthLife.LINKED_ADULT);
            town.industryStocks.put(RebirthLife.CHILD,c.generationIndex);
            DemographyLife.set(town,c,"played-incarnation",town.player.incarnation);
            EducationLife.set(town,c,"technical",70);EducationLife.set(town,c,"diploma-programming",1);
            budget=town.employerFunds.getOrDefault("city-maintenance",0);
            state.day=2;state.minute=800;
            db.save(state,java.util.List.of(),java.util.List.of(),town,java.util.List.of(),java.util.List.of(),java.util.List.of());
        }
        try(var game=new GameSession(file)){
            assertThrows(IllegalArgumentException.class,()->game.gardenAction("rebirth-software-water-control"));
            assertThrows(IllegalArgumentException.class,()->game.gardenAction("rebirth-java-submit:water-control:return Math.min(requested, Math.min(stock, freeCapacity));"));
            var onboarding=game.gardenAction("rebirth-java-onboard").world();
            assertTrue(onboarding.rebirth().javaWorkspace().onboarded());
            assertThrows(IllegalArgumentException.class,()->game.gardenAction("rebirth-java-onboard"));
            assertThrows(IllegalArgumentException.class,()->game.gardenAction("rebirth-git:merge:water-control"));
            assertThrows(IllegalArgumentException.class,()->game.gardenAction("rebirth-java-submit:water-control:return Math.min(requested, Math.min(stock, freeCapacity));"));
            game.gardenAction("rebirth-git:branch:water-control");
            assertThrows(IllegalArgumentException.class,()->game.gardenAction("rebirth-git:branch:water-control"));
            assertThrows(IllegalArgumentException.class,()->game.gardenAction("rebirth-java-submit:water-control:return requested;"));
            assertThrows(IllegalArgumentException.class,()->game.gardenAction("rebirth-java-submit:water-control:return Runtime.getRuntime().availableProcessors();"));
            var developed=game.gardenAction("rebirth-java-submit:water-control:return Math.min(requested, Math.min(stock, freeCapacity));").world();
            assertEquals(880,developed.minute());
            assertEquals("DEVELOPED",developed.rebirth().softwareProjects().getFirst().phase());
            assertEquals(30,developed.player().money());
        }
        try(var game=new GameSession(file)){
            assertThrows(IllegalArgumentException.class,()->game.gardenAction("rebirth-software-water-control"));
            assertThrows(IllegalArgumentException.class,()->game.gardenAction("rebirth-git:pr:water-control"));
            game.gardenAction("rebirth-git:commit:water-control");
            game.gardenAction("rebirth-git:pr:water-control");
            assertThrows(IllegalArgumentException.class,()->game.gardenAction("rebirth-git:merge:water-control"));
            var tested=game.gardenAction("rebirth-software-water-control").world();
            assertEquals("TESTED",tested.rebirth().softwareProjects().getFirst().phase());
            assertThrows(IllegalArgumentException.class,()->game.gardenAction("rebirth-software-water-control"));
            game.gardenAction("rebirth-git:merge:water-control");
            var deployed=game.gardenAction("rebirth-software-water-control").world();
            assertEquals(990,deployed.minute());
            assertEquals(75,deployed.player().money());
            assertTrue(deployed.rebirth().softwareProjects().getFirst().deployed());
            assertThrows(IllegalArgumentException.class,()->game.gardenAction("rebirth-software-water-control"));
        }
        try(var db=new WorldDatabase(file)){
            var state=db.load();var town=db.loadTown(state);
            assertTrue(SoftwareLife.deployed(town,"water-control"));
            assertEquals(4,SoftwareLife.gitStage(town,"water-control"));
            assertEquals(budget-45,town.employerFunds.get("city-maintenance").intValue());
            assertEquals(75,town.get(id).money);
            town.technicalOrders.get("water-pump").condition=100;
            town.cleanWater=0;town.naturalResources.get("river-water").stock=20;
            int before=town.naturalResources.get("river-water").stock;
            state.day=3;state.minute=0;
            UtilityLife.tick(town,state,new java.util.ArrayList<>(),new java.util.ArrayList<>());
            assertEquals(6,town.cleanWater);
            assertEquals(before-6,town.naturalResources.get("river-water").stock);
        }
    }
    @Test
    void linkedAdultHomeUsesRealSuppliesAndPrivateSpace() throws Exception {
        Path file=directory.resolve("linked-home.db");
        String id;
        try(var db=new WorldDatabase(file)){
            var state=db.load();var town=db.loadTown(state);
            var people=PopulationGenerator.add(town,2);
            var a=people.get(0);var b=people.get(1);
            id=a.id;a.age=30;b.age=30;
            a.place=TownLife.Place.PARK;a.destination=a.place;a.travelRemaining=0;
            a.home=TownLife.Place.NORTH_RESIDENCES;a.activity="FREE_TIME";
            a.hunger=70;a.thirst=70;a.fatigue=60;a.food=5;a.water=5;a.money=200;
            b.home=a.home;b.homeUnit=a.homeUnit;b.place=b.home;b.destination=b.home;b.travelRemaining=0;b.activity="AT_HOME";
            town.player.alive=false;town.player.money=0;
            town.industryStocks.put(RebirthLife.PHASE,RebirthLife.LINKED_ADULT);
            town.industryStocks.put(RebirthLife.CHILD,a.generationIndex);
            DemographyLife.set(town,a,"played-incarnation",town.player.incarnation);
            DemographyLife.set(town,a,"partner",b.generationIndex);DemographyLife.set(town,b,"partner",a.generationIndex);
            state.day=2;state.minute=1200;
            db.save(state,java.util.List.of(),java.util.List.of(),town,java.util.List.of(),java.util.List.of(),java.util.List.of());
        }
        try(var game=new GameSession(file)){
            assertThrows(IllegalArgumentException.class,()->game.gardenAction("rebirth-home-meal"));
            var home=game.gardenAction("rebirth-home-go").world();
            assertTrue(home.rebirth().homeOptions().atHome());
            assertTrue(home.rebirth().homeOptions().stayingHome());
            assertEquals("NORTH_RESIDENCES",home.player().placeId());
        }
        try(var db=new WorldDatabase(file)){
            var state=db.load();var town=db.loadTown(state);
            var a=town.get(id);a.hunger=60;a.thirst=60;a.fatigue=60;a.food=5;a.water=5;
            EducationLife.set(town,a,"dirt",40);
            db.save(state,java.util.List.of(),java.util.List.of(),town,java.util.List.of(),java.util.List.of(),java.util.List.of());
        }
        try(var game=new GameSession(file)){
            var before=game.snapshot();
            var meal=game.gardenAction("rebirth-home-meal").world();
            assertEquals(before.rebirth().homeOptions().food()-1,meal.rebirth().homeOptions().food());
            assertEquals(before.minute()+15,meal.minute());
            var drink=game.gardenAction("rebirth-home-drink").world();
            assertEquals(meal.rebirth().homeOptions().water()-1,drink.rebirth().homeOptions().water());
            var washed=game.gardenAction("rebirth-home-wash").world();
            assertEquals(drink.rebirth().homeOptions().water()-1,washed.rebirth().homeOptions().water());
            assertTrue(washed.rebirth().homeOptions().hygiene()>=98);
            var upgraded=game.gardenAction("rebirth-home-upgrade").world();
            assertEquals(washed.player().money()-80,upgraded.player().money());
            assertEquals(1,upgraded.rebirth().homeOptions().furnitureLevel());
            assertThrows(IllegalArgumentException.class,()->game.gardenAction("rebirth-home-upgrade"));
            var rest=game.gardenAction("rebirth-home-rest").world();
            assertEquals(upgraded.minute()+60,rest.minute());
            assertTrue(rest.rebirth().homeOptions().stayingHome());
            if(rest.rebirth().homeOptions().actions().contains("family")){
                var family=game.gardenAction("rebirth-home-family").world();
                assertFalse(family.rebirth().homeOptions().actions().contains("family"));
            }
            var routine=game.gardenAction("rebirth-routine").world();
            assertFalse(routine.rebirth().homeOptions().stayingHome());
        }
        try(var db=new WorldDatabase(file)){
            var state=db.load();var town=db.loadTown(state);var a=town.get(id);
            a.activity="AT_HOME";a.place=a.home;a.destination=a.home;a.travelRemaining=0;
            state.minute=800;
            db.save(state,java.util.List.of(),java.util.List.of(),town,java.util.List.of(),java.util.List.of(),java.util.List.of());
        }
        try(var game=new GameSession(file)){
            assertThrows(IllegalArgumentException.class,()->game.gardenAction("rebirth-road-CLINIC"));
            var outside=game.gardenAction("rebirth-home-exit").world();
            assertFalse(outside.rebirth().homeOptions().atHome());
            assertTrue(outside.rebirth().homeOptions().outsideHome());
            assertThrows(IllegalArgumentException.class,()->game.gardenAction("rebirth-home-meal"));
            assertThrows(IllegalArgumentException.class,()->game.gardenAction("rebirth-road-QUARRY"));
        }
        try(var game=new GameSession(file)){
            assertTrue(game.snapshot().rebirth().homeOptions().outsideHome());
            var road=game.gardenAction("rebirth-road-CLINIC").world();
            assertEquals("CLINIC",road.player().placeId());
            assertFalse(road.rebirth().homeOptions().outsideHome());
            var back=game.gardenAction("rebirth-road-NORTH_RESIDENCES").world();
            assertTrue(back.rebirth().homeOptions().outsideHome());
            assertTrue(game.gardenAction("rebirth-home-go").world().rebirth().homeOptions().atHome());
        }
    }
    @Test
    void orchardIsPerennialSeasonalAndConservesFruitOnSale() throws Exception {
        Path file=directory.resolve("orchard.db");
        try(var game=new GameSession(file)) {
            game.move("south");
            assertThrows(IllegalArgumentException.class,()->game.gardenAction("orchard-plant-apples"));
        }
        try(var db=new WorldDatabase(file)){
            var state=db.load();var town=db.loadTown(state);
            var p=town.player;p.ownsHouse=true;p.money=50;
            var apple=OrchardLife.CROPS.getFirst();
            OrchardLife.act(town,"orchard-plant-apples",1,720);
            assertEquals(38,p.money);
            assertThrows(IllegalArgumentException.class,()->OrchardLife.act(town,"orchard-plant-apples",1,720));
            for(int day=1;day<=5;day++){
                OrchardLife.set(town,OrchardLife.WATER,day);
                OrchardLife.tick(town,day,1080);OrchardLife.tick(town,day,1080);
            }
            assertEquals(5,OrchardLife.get(town,OrchardLife.key(apple,"age")));
            OrchardLife.set(town,OrchardLife.WATER,100);OrchardLife.tick(town,100,1080);
            assertEquals(0,OrchardLife.get(town,OrchardLife.key(apple,"fruit")));
            for(int day=186;day<=188;day++){
                OrchardLife.set(town,OrchardLife.WATER,day);OrchardLife.tick(town,day,1080);
            }
            OrchardLife.act(town,"orchard-harvest-apples",188,1100);
            assertEquals(3,FoodLife.quantity(p,"apples",true));
            assertThrows(IllegalArgumentException.class,()->OrchardLife.act(town,"orchard-harvest-apples",188,1100));
            for(int day=189;day<=192;day++){
                OrchardLife.set(town,OrchardLife.WATER,day);OrchardLife.tick(town,day,1080);
            }
            assertEquals(0,OrchardLife.get(town,OrchardLife.key(apple,"fruit")));
            OrchardLife.tick(town,276,1080);
            assertEquals(5,OrchardLife.get(town,OrchardLife.key(apple,"age")));
            for(int day=551;day<=553;day++){
                OrchardLife.set(town,OrchardLife.WATER,day);OrchardLife.tick(town,day,1080);
            }
            OrchardLife.act(town,"orchard-harvest-apples",553,1100);
            assertEquals(6,FoodLife.quantity(p,"apples",true));
            FoodLife.change(p,"apples",true,-1);FoodLife.change(p,"apples",false,1);
            p.place=TownLife.Place.MARKET;p.destination=p.place;
            town.employerFunds.put("shop-assistant",20);
            RetailLife.buyFruit(town,"apples",720);
            assertEquals(40,p.money);assertEquals(18,town.employerFunds.get("shop-assistant"));
            assertEquals(1,town.retailStock.get("apples"));
            db.save(state,java.util.List.of(),java.util.List.of(),town,java.util.List.of(),java.util.List.of(),java.util.List.of());
        }
        try(var game=new GameSession(file)){
            assertEquals(5,game.snapshot().player().pantry().get("apples"));
            assertEquals(5,game.snapshot().orchard().plants().getFirst().age());
            assertTrue(game.snapshot().orchard().plants().getFirst().planted());
        }
    }

    @Test
    void homeHensRequireCareAndPersistEggsAndMarketPayments() throws Exception {
        Path file=directory.resolve("hens.db");
        try(var game=new GameSession(file)) {
            game.move("south");
            assertThrows(IllegalArgumentException.class,()->game.gardenAction("build-coop"));
        }
        try(var db=new WorldDatabase(file)) {
            var state=db.load();var town=db.loadTown(state);
            var p=town.player;p.ownsHouse=true;p.money=100;
            FoodLife.change(p,"rice",false,2);FoodLife.change(p,"water",false,2);
            HenLife.act(town,"build-coop",1,720);
            for(int k=0;k<3;k++)HenLife.act(town,"buy-hen",1,720);
            assertEquals(44,p.money);
            assertThrows(IllegalArgumentException.class,()->HenLife.act(town,"buy-hen",1,720));
            HenLife.act(town,"feed-hens",1,720);HenLife.act(town,"water-hens",1,720);
            assertThrows(IllegalArgumentException.class,()->HenLife.act(town,"feed-hens",1,720));
            HenLife.tick(town,1,1080);HenLife.tick(town,1,1080);
            assertEquals(3,HenLife.get(town,HenLife.EGGS));
            HenLife.tick(town,2,1080);
            assertEquals(85,HenLife.get(town,HenLife.HEALTH));
            assertEquals(3,HenLife.get(town,HenLife.EGGS));
            HenLife.act(town,"feed-hens",3,720);HenLife.act(town,"water-hens",3,720);
            HenLife.tick(town,3,1080);
            assertEquals(6,HenLife.get(town,HenLife.EGGS));
            assertEquals(0,p.bottledWater);assertEquals(0,FoodLife.quantity(p,"rice",false));
            HenLife.act(town,"collect-eggs",3,1100);
            assertEquals(1,FoodLife.quantity(p,"eggs",true));
            db.save(state,java.util.List.of(),java.util.List.of(),town,java.util.List.of(),java.util.List.of(),java.util.List.of());
        }
        try(var game=new GameSession(file)) {
            assertEquals(3,game.snapshot().hens().hens());
            assertEquals(5,game.snapshot().hens().eggs());
            assertEquals(95,game.snapshot().hens().health());
            game.transferRoomItem("eggs",false);
        }
        try(var db=new WorldDatabase(file)) {
            var state=db.load();var town=db.loadTown(state);
            town.player.place=TownLife.Place.MARKET;town.player.destination=town.player.place;
            town.retailStock.put("eggs",16);
            assertThrows(IllegalArgumentException.class,()->RetailLife.buyEggs(town,720));
            town.retailStock.put("eggs",15);town.employerFunds.put("shop-assistant",10);
            RetailLife.buyEggs(town,720);
            assertEquals(46,town.player.money);
            assertEquals(8,town.employerFunds.get("shop-assistant"));
            assertEquals(16,town.retailStock.get("eggs"));
            assertEquals(0,FoodLife.quantity(town.player,"eggs",false));
        }
    }

    @Test
    void rainBarrelAndProduceTradeConserveWaterStockAndCash() throws Exception {
        Path file=directory.resolve("rain-trade.db");
        try(var game=new GameSession(file)) { game.move("south"); }
        try(var db=new WorldDatabase(file)) {
            var state=db.load();var town=db.loadTown(state);
            var p=town.player;p.ownsHouse=true;p.money=50;p.bottledWater=1;
            GardenLife.act(town,"buy-barrel",1,720);
            assertEquals(42,p.money);
            assertThrows(IllegalArgumentException.class,()->GardenLife.act(town,"buy-barrel",1,720));
            int rainDays=0;
            for(int day=1;rainDays<3;day++) {
                if(WeatherLife.at(day,1080).raining()) {
                    GardenLife.tick(town,day,1080);GardenLife.tick(town,day,1080);
                    rainDays++;
                    assertEquals(Math.min(8,rainDays*3),GardenLife.get(town,GardenLife.RAIN));
                }
            }
            int dry=1;
            while(GardenLife.winter(dry)||WeatherLife.at(dry,720).raining())dry++;
            GardenLife.act(town,"plant",dry,720);
            GardenLife.act(town,"water-rain",dry,720);
            assertEquals(7,GardenLife.get(town,GardenLife.RAIN));assertEquals(1,p.bottledWater);
            FoodLife.change(p,"vegetables",false,2);
            p.place=TownLife.Place.MARKET;p.destination=p.place;
            town.retailStock.put("vegetables",18);
            assertThrows(IllegalArgumentException.class,()->RetailLife.buyProduce(town,720));
            town.retailStock.put("vegetables",17);
            FreightLife.set(town,FreightLife.STAGE,1);FreightLife.set(town,FreightLife.KIND,2);FreightLife.set(town,FreightLife.IRON,1);
            assertThrows(IllegalArgumentException.class,()->RetailLife.buyProduce(town,720));
            FreightLife.clear(town);
            town.employerFunds.put("shop-assistant",1);
            assertThrows(IllegalArgumentException.class,()->RetailLife.buyProduce(town,720));
            town.employerFunds.put("shop-assistant",50);
            RetailLife.buyProduce(town,720);
            assertEquals(41,p.money);
            assertEquals(48,town.employerFunds.get("shop-assistant"));
            assertEquals(18,town.retailStock.get("vegetables"));
            assertEquals(1,FoodLife.quantity(p,"vegetables",false));
            db.save(state,java.util.List.of(),java.util.List.of(),town,java.util.List.of(),java.util.List.of(),java.util.List.of());
        }
        try(var game=new GameSession(file)) {
            assertTrue(game.snapshot().garden().barrel());
            assertEquals(7,game.snapshot().garden().rainwater());
            assertEquals(1,game.snapshot().player().bag().get("vegetables"));
            assertEquals(41,game.snapshot().player().money());
        }
    }

    @Test
    void familyRebirthPersistsChoiceAndCreatesExactlyOneRealChild() throws Exception {
        Path file=directory.resolve("family-rebirth.db");
        String firstId,secondId;
        int first,second;
        try(var db=new WorldDatabase(file)){
            var state=db.load();var town=db.loadTown(state);
            var adults=PopulationGenerator.add(town,2);
            var a=adults.get(0);var b=adults.get(1);
            firstId=a.id;secondId=b.id;first=a.generationIndex;second=b.generationIndex;
            a.age=30;b.age=31;a.money=150;b.money=150;
            DemographyLife.set(town,a,"partner",second);
            DemographyLife.set(town,b,"partner",first);
            DemographyLife.set(town,a,"union-day",1);
            DemographyLife.set(town,b,"union-day",1);
            town.player.alive=false;town.player.health=0;
            state.day=271;state.minute=600;
            db.save(state,java.util.List.of(),java.util.List.of(),town,java.util.List.of(),java.util.List.of(),java.util.List.of());
        }
        try(var game=new GameSession(file)){
            var view=game.snapshot();
            assertEquals("CHOICE",view.rebirth().phase());
            assertEquals(2,view.player().incarnation());
            assertEquals(0,view.player().money());
            assertEquals(1,view.rebirth().families().size());
            assertThrows(IllegalArgumentException.class,()->game.gardenAction("rebirth-family-9999-9998"));
            assertThrows(IllegalArgumentException.class,()->game.waitMinutes(1));
            assertThrows(IllegalArgumentException.class,()->game.gardenAction("rebirth-wait-fast"));
        }
        try(var game=new GameSession(file)){
            assertEquals("CHOICE",game.snapshot().rebirth().phase());
            var born=game.gardenAction("rebirth-family-"+first+"-"+second).world();
            assertEquals("CHILDHOOD",born.rebirth().phase());
            assertEquals(0,born.rebirth().age());
            assertEquals(20,born.rebirth().identity().savings());
            assertEquals(2,born.rebirth().identity().relationships().stream().filter(RebirthLife.KnownPerson::family).count());
            assertEquals(271,born.rebirth().identity().bornDay());
            assertEquals(7,born.rebirth().maxWaitDays());
            assertThrows(IllegalArgumentException.class,()->game.gardenAction("rebirth-focus-technical"));
            assertThrows(IllegalArgumentException.class,()->game.gardenAction("rebirth-child-travel-PARK"));
            assertThrows(IllegalArgumentException.class,()->game.gardenAction("rebirth-hobby-read"));
            assertThrows(IllegalArgumentException.class,()->game.gardenAction("rebirth-allowance"));
            assertThrows(IllegalArgumentException.class,()->game.gardenAction("rebirth-chat-"+firstId));
            assertEquals(1,born.demography().births());
            assertEquals(271,born.day());assertEquals(600,born.minute());
            assertThrows(IllegalArgumentException.class,()->game.gardenAction("rebirth-family-"+first+"-"+second));
            assertThrows(IllegalArgumentException.class,()->game.applyJob("shop-assistant"));
        }
        try(var db=new WorldDatabase(file)){
            var state=db.load();var town=db.loadTown(state);
            var child=RebirthLife.child(town);
            assertNotNull(child);assertTrue(child.alive);
            assertEquals(20,child.money);
            assertEquals(140,town.get(firstId).money);
            assertEquals(140,town.get(secondId).money);
            assertEquals(first,DemographyLife.get(town,child,"parent-a"));
            assertEquals(second,DemographyLife.get(town,child,"parent-b"));
            assertEquals(1,town.pastLives.size());
        }
        try(var game=new GameSession(file)){
            assertEquals("CHILDHOOD",game.snapshot().rebirth().phase());
            var next=game.gardenAction("rebirth-wait").world();
            assertEquals(272,next.day());
            assertEquals(1,next.demography().births());
            assertEquals(2,next.player().incarnation());
            assertEquals(7,next.rebirth().maxWaitDays());
            var fast=game.gardenAction("rebirth-wait-fast").world();
            assertEquals(279,fast.day());
            assertEquals(600,fast.minute());
        }
        // School focus persists and does not award free skills.
        try(var db=new WorldDatabase(file)){
            var state=db.load();var town=db.loadTown(state);
            var child=RebirthLife.child(town);
            assertNotNull(child);child.age=6;
            child.place=child.home;child.destination=child.home;child.travelRemaining=0;child.activity="AT_HOME";
            state.minute=800;
            db.save(state,java.util.List.of(),java.util.List.of(),town,java.util.List.of(),java.util.List.of(),java.util.List.of());
        }
        try(var game=new GameSession(file)){
            var before=game.snapshot();
            assertEquals(3,before.rebirth().maxWaitDays());
            var after=game.gardenAction("rebirth-focus-technical").world();
            assertEquals("technical",after.rebirth().schoolFocus());
            assertEquals(before.rebirth().skills(),after.rebirth().skills());
            assertEquals(before.minute(),after.minute());
            assertThrows(IllegalArgumentException.class,()->game.gardenAction("rebirth-focus-invalid"));
            assertEquals(java.util.List.of("PARK","LIBRARY"),after.rebirth().childOptions().destinations());
            assertThrows(IllegalArgumentException.class,()->game.gardenAction("rebirth-child-travel-CENTRO"));
            var outing=game.gardenAction("rebirth-child-travel-PARK").world();
            assertEquals("PARK",outing.player().placeId());
            assertTrue(outing.rebirth().childOptions().outing());
        }
        try(var db=new WorldDatabase(file)){
            var state=db.load();var town=db.loadTown(state);var child=RebirthLife.child(town);
            assertEquals("technical",RebirthLife.focus(town,child));
            assertEquals(TownLife.Place.PARK,RebirthLife.childOuting(town,child,state.day,state.minute));
            state.minute=1079;
            db.save(state,java.util.List.of(),java.util.List.of(),town,java.util.List.of(),java.util.List.of(),java.util.List.of());
        }
        try(var game=new GameSession(file)){
            assertTrue(game.snapshot().rebirth().childOptions().outing());
            var home=game.gardenAction("rebirth-child-hour").world();
            assertFalse(home.rebirth().childOptions().outing());
            assertTrue(home.rebirth().childOptions().destinations().isEmpty());
            assertThrows(IllegalArgumentException.class,()->game.gardenAction("rebirth-child-travel-PARK"));
        }
        try(var db=new WorldDatabase(file)){
            var state=db.load();var town=db.loadTown(state);var child=RebirthLife.child(town);
            assertEquals(child.home,child.place);
            child.age=13;
            assertTrue(RebirthLife.childOptions(town,state.day,state.minute).destinations().contains("CENTRO"));
            child.age=6;
            child.place=TownLife.Place.SCHOOL;child.travelRemaining=0;child.activity="STUDYING";
            state.day=281;state.minute=600;
            town.employerFunds.put("school",10);
            int skill=EducationLife.get(town,child,"technical");
            int days=EducationLife.get(town,child,"school-days");
            EducationLife.attendance(town,child,state);
            assertTrue(EducationLife.get(town,child,"technical")>skill);
            assertEquals(days+1,EducationLife.get(town,child,"school-days"));
            assertEquals(9,town.employerFunds.get("school"));
            EducationLife.attendance(town,child,state);
            assertEquals(days+1,EducationLife.get(town,child,"school-days"));
            child.place=child.home;child.activity="AT_HOME";
            var parent=town.get(firstId);
            parent.place=child.home;parent.home=child.home;parent.homeUnit=child.homeUnit;parent.travelRemaining=0;parent.activity="AT_HOME";
            var previous=town.peerRelationships.get(SocialNetwork.key(child.id,parent.id));
            int affinity=previous==null?50:previous.affinity;
            RebirthLife.chat(town,state.day,firstId);
            assertThrows(IllegalArgumentException.class,()->RebirthLife.chat(town,state.day,firstId));
            assertEquals(Math.min(100,affinity+2),town.peerRelationships.get(SocialNetwork.key(child.id,parent.id)).affinity);
            state.minute=850;
            child.destination=child.home;child.travelRemaining=0;child.hunger=20;child.thirst=20;child.fatigue=20;child.sickUntilDay=0;
            parent.money=100;
            int wallet=child.money;
            assertEquals(3,RebirthLife.allowanceAmount(town,child,state.day,state.minute));
            RebirthLife.allowance(town,state.day,state.minute);
            assertEquals(wallet+3,child.money);
            assertEquals(97,parent.money);
            assertThrows(IllegalArgumentException.class,()->RebirthLife.allowance(town,state.day,state.minute));
            int technical=EducationLife.get(town,child,"technical");
            int careful=child.carefulness;
            assertThrows(IllegalArgumentException.class,()->RebirthLife.chooseAmbition(town,"LEARN_SKILLS"));
            RebirthLife.hobby(town,state.day,state.minute,"build");
            assertEquals(Math.min(100,technical+1),EducationLife.get(town,child,"technical"));
            assertEquals(1,EducationLife.get(town,child,"hobby-days"));
            assertEquals(Math.min(100,careful+1),child.carefulness);
            assertThrows(IllegalArgumentException.class,()->RebirthLife.hobby(town,state.day,state.minute,"read"));
            db.save(state,java.util.List.of(),java.util.List.of(),town,java.util.List.of(),java.util.List.of(),java.util.List.of());
        }
        // Move the existing child to the evening before its eighteenth birthday.
        try(var db=new WorldDatabase(file)){
            var state=db.load();var town=db.loadTown(state);
            var child=RebirthLife.child(town);
            assertNotNull(child);
            assertThrows(IllegalArgumentException.class,()->RebirthLife.chat(town,state.day,firstId));
            assertEquals(3,EducationLife.get(town,child,"allowance-total"));
            assertEquals(1,EducationLife.get(town,child,"hobby-days"));
            child.age=17;
            RebirthLife.chooseAmbition(town,"LEARN_SKILLS");
            assertThrows(IllegalArgumentException.class,()->RebirthLife.chooseAmbition(town,"unknown"));
            state.day=DemographyLife.get(town,child,"born")+18*365-1;
            state.minute=600;
            db.save(state,java.util.List.of(),java.util.List.of(),town,java.util.List.of(),java.util.List.of(),java.util.List.of());
        }
        try(var game=new GameSession(file)){
            assertEquals(1,game.snapshot().rebirth().maxWaitDays());
            assertThrows(IllegalArgumentException.class,()->game.gardenAction("rebirth-wait-fast"));
            var previous=game.snapshot().rebirth().identity();
            var adult=game.gardenAction("rebirth-wait").world();
            assertEquals(18,adult.rebirth().age());
            assertEquals(previous.residentId(),adult.rebirth().identity().residentId());
            assertTrue(adult.rebirth().identity().graduated());
            assertEquals(3,adult.rebirth().identity().allowanceTotal());
            assertEquals(1,adult.rebirth().identity().hobbyDays());
            var ambition=adult.rebirth().identity().personality().ambition();
            assertEquals("LEARN_SKILLS",ambition.id());
            assertEquals(60,ambition.target());
            assertTrue(ambition.progress()>0);
            assertEquals("technical",adult.rebirth().schoolFocus());
            assertTrue(adult.rebirth().skills().get("technical")>0);
            assertEquals(2,adult.rebirth().identity().relationships().stream().filter(RebirthLife.KnownPerson::family).count());
            assertEquals(360,adult.minute());
            assertEquals(0,adult.rebirth().maxWaitDays());
            assertThrows(IllegalArgumentException.class,()->game.gardenAction("rebirth-wait"));
        }
        try(var game=new GameSession(file)){
            var adult=game.snapshot().rebirth();
            assertEquals(18,adult.age());assertTrue(adult.identity().graduated());
            assertEquals("technical",adult.schoolFocus());
            assertTrue(adult.skills().get("technical")>0);
            String id=adult.identity().residentId();
            int savings=adult.identity().savings();
            var continued=game.gardenAction("rebirth-continue").world();
            assertEquals("LINKED_ADULT",continued.rebirth().phase());
            assertEquals(id,continued.rebirth().identity().residentId());
            assertEquals(savings,continued.player().money());
            var changed=game.gardenAction("rebirth-ambition-SAVE_MONEY").world();
            assertEquals("SAVE_MONEY",changed.rebirth().identity().personality().ambition().id());
            assertEquals(savings,changed.rebirth().identity().personality().ambition().progress());
            assertTrue(continued.player().alive());
            assertThrows(IllegalArgumentException.class,()->game.gardenAction("rebirth-continue"));
            assertThrows(IllegalArgumentException.class,()->game.gardenAction("rebirth-job-unknown"));
            var hired=game.gardenAction("rebirth-job-Barista").world();
            assertEquals("Barista",hired.rebirth().adultOptions().occupation());
            assertEquals(savings,hired.player().money());
            var moved=game.gardenAction("rebirth-travel-PARK").world();
            assertEquals("PARK",moved.player().placeId());
            assertEquals(id,moved.rebirth().identity().residentId());
        }
        try(var game=new GameSession(file)){
            var adult=game.snapshot();
            assertEquals("LINKED_ADULT",adult.rebirth().phase());
            assertEquals("Barista",adult.rebirth().adultOptions().occupation());
            assertEquals("SAVE_MONEY",adult.rebirth().identity().personality().ambition().id());
            assertEquals("PARK",adult.rebirth().adultOptions().destination());
            assertEquals(adult.rebirth().identity().savings(),adult.player().money());
            var resumed=game.gardenAction("rebirth-routine").world();
            assertNull(resumed.rebirth().adultOptions().destination());
            var later=game.gardenAction("rebirth-wait-hour").world();
            assertEquals(resumed.minute()+60,later.minute());
        }
        try(var db=new WorldDatabase(file)){
            var state=db.load();var town=db.loadTown(state);
            assertEquals(0,town.player.money,"inactive shell never duplicates the citizen wallet");
            assertFalse(town.player.alive);
            // Exercise death while a linked adult is advancing the shared world.
            var c=RebirthLife.child(town);
            c.age=100;c.health=100;
            state.day=DemographyLife.get(town,c,"born")+101*365;
            state.minute=359;
            db.save(state,java.util.List.of(),java.util.List.of(),town,java.util.List.of(),java.util.List.of(),java.util.List.of());
        }
        String deceased;
        int deathCount;
        try(var game=new GameSession(file)){
            var before=game.snapshot();
            deceased=before.rebirth().identity().residentId();
            deathCount=before.demography().deaths();
            var ended=game.gardenAction("rebirth-wait-adult-day").world();
            assertEquals("CHOICE",ended.rebirth().phase());
            assertEquals(before.day(),ended.day());assertEquals(360,ended.minute());
            assertEquals(3,ended.player().incarnation());
            assertEquals(0,ended.player().money());
            assertEquals(deceased,ended.rebirth().pastLives().getFirst().residentId());
            assertEquals(101,ended.rebirth().pastLives().getFirst().age());
            assertEquals(2,ended.rebirth().pastLives().size());
            // Other residents may also die at this birthday; the specific estate is checked below.
            assertTrue(ended.demography().deaths()>deathCount);
        }
        try(var game=new GameSession(file)){
            assertEquals(3,game.snapshot().player().incarnation());
            assertEquals(2,game.snapshot().rebirth().pastLives().size());
        }
        try(var db=new WorldDatabase(file)){
            var state=db.load();var town=db.loadTown(state);var dead=town.get(deceased);
            assertFalse(dead.alive);assertEquals(0,dead.money);
            assertEquals(1,DemographyLife.get(town,dead,"estate"));
            int city=town.employerFunds.getOrDefault(MunicipalLife.CITY,0);
            DemographyLife.estate(town,dead);
            assertEquals(city,town.employerFunds.getOrDefault(MunicipalLife.CITY,0));
            assertEquals(2,town.pastLives.size());
        }
    }

    @Test
    void noEligibleFamilyAllowsExplicitAdultFallback() throws Exception {
        Path file=directory.resolve("rebirth-fallback.db");
        try(var db=new WorldDatabase(file)){
            var state=db.load();var town=db.loadTown(state);
            town.player.alive=false;town.player.health=0;
            db.save(state,java.util.List.of(),java.util.List.of(),town,java.util.List.of(),java.util.List.of(),java.util.List.of());
        }
        try(var game=new GameSession(file)){
            assertTrue(game.snapshot().rebirth().families().isEmpty());
            var fallback=game.gardenAction("rebirth-adult").world();
            assertEquals("ADULT",fallback.rebirth().phase());
            assertTrue(fallback.player().alive());
            assertEquals(50,fallback.player().money());
            assertEquals("PARK",fallback.player().placeId());
            assertEquals(2,fallback.player().incarnation());
        }
    }

    @Test
    void electionUsesResidentPreferencesPersistsMandateAndRespectsTreasury() throws Exception {
        Path file=directory.resolve("election.db");
        try(var db=new WorldDatabase(file)){
            var state=db.load();var town=db.loadTown(state);
            var residents=PopulationGenerator.add(town,6);
            for(int i=0;i<residents.size();i++){
                var c=residents.get(i);c.age=30;c.health=100;c.personalSeed=0;c.money=100;
                EducationLife.set(town,c,"social",i<3?100:0);
                EducationLife.set(town,c,"technical",100);
                EducationLife.set(town,c,"academic",0);
                OpinionLife.change(town,c,"city",-50);
                OpinionLife.change(town,c,"stall",50);
            }
            var p=town.player;p.place=TownLife.Place.OFFICE;p.destination=p.place;p.travelRemaining=0;
            ElectionLife.tick(town,2,360);
            assertEquals(32,ElectionLife.report(town,2).nextDay());
            assertEquals(3,ElectionLife.report(town,2).candidates().size());
            assertThrows(IllegalArgumentException.class,()->ElectionLife.act(town,"election-policy-2",2,600));
            ElectionLife.act(town,"election-register-3",2,600);
            ElectionLife.act(town,"election-vote-4",2,600);
            assertThrows(IllegalArgumentException.class,()->ElectionLife.act(town,"election-vote-1",2,600));
            assertEquals(0,ElectionLife.report(town,2).candidates().getFirst().votes());
            state.day=2;state.minute=600;
            db.save(state,java.util.List.of(),java.util.List.of(),town,java.util.List.of(),java.util.List.of(),java.util.List.of());
        }
        try(var db=new WorldDatabase(file)){
            var state=db.load();var town=db.loadTown(state);
            assertTrue(ElectionLife.report(town,31).registered());
            assertTrue(ElectionLife.report(town,31).voted());
            ElectionLife.tick(town,32,360);ElectionLife.tick(town,32,360);
            assertTrue(ElectionLife.playerMayor(town));
            assertEquals(4,ElectionLife.get(town,ElectionLife.votes(4)));
            assertEquals(32+5*365,ElectionLife.report(town,32).nextDay());
            assertEquals(1,town.archive.values().stream().filter(e->e.id().equals("election:32")).count());
            ElectionLife.act(town,"election-policy-2",32,600);
            assertEquals(ElectionLife.HEALTH,ElectionLife.policy(town));
            town.employerFunds.put(MunicipalLife.CITY,100);
            int school=town.employerFunds.get("school"),clinic=town.employerFunds.get(MunicipalLife.CLINIC);
            MunicipalLife.tick(town,36,360);
            assertEquals(school+2,town.employerFunds.get("school"));
            assertEquals(clinic+12,town.employerFunds.get(MunicipalLife.CLINIC));
            assertTrue(town.employerFunds.get(MunicipalLife.CITY)>=40);
            state.day=36;state.minute=600;
            db.save(state,java.util.List.of(),java.util.List.of(),town,java.util.List.of(),java.util.List.of(),java.util.List.of());
        }
        try(var db=new WorldDatabase(file)){
            var state=db.load();var town=db.loadTown(state);
            assertTrue(ElectionLife.playerMayor(town));
            assertEquals(ElectionLife.HEALTH,ElectionLife.policy(town));
            town.player.alive=false;
            assertEquals(ElectionLife.BALANCED,ElectionLife.policy(town));
            PlayerLife.beginNewLife(town,37,600);
            assertFalse(ElectionLife.playerMayor(town));
        }
    }

    @Test
    void opinionsFollowPersonalExperienceAndOnlyActuallyReadEditions() throws Exception {
        Path file=directory.resolve("opinions.db");
        String id,otherId;
        try(var db=new WorldDatabase(file)){
            var state=db.load();var town=db.loadTown(state);
            var residents=PopulationGenerator.add(town,2);
            var reader=residents.get(0);var other=residents.get(1);
            id=reader.id;otherId=other.id;reader.age=30;other.age=30;
            OpinionLife.fountain(town,reader,1,true);
            OpinionLife.fountain(town,reader,1,true);
            assertEquals(53,OpinionLife.score(town,reader,"city"));
            OpinionLife.purchase(town,reader,1,2);
            OpinionLife.purchase(town,reader,1,2);
            assertEquals(54,OpinionLife.score(town,reader,"stall"));
            MarketStallLife.set(town,MarketStallLife.LICENSE,1);
            ArchiveLife.record(town,"budget-low:1",8,360,"Municipality","Reserve reached.");
            var claim=new KnowledgeLife.Belief(reader.id,"news",8,600,other.id,"public:MARKET",
                "A resident recommended the private market stall to a neighbor.","HEARD",other.id,8,700,1,70);
            OpinionLife.edition(town,8,claim);
            reader.place=TownLife.Place.LIBRARY;reader.destination=reader.place;reader.travelRemaining=0;reader.activity="FREE_TIME";
            other.place=TownLife.Place.PARK;other.destination=other.place;other.travelRemaining=0;other.activity="FREE_TIME";
            OpinionLife.read(town,other,8,1020);
            OpinionLife.read(town,reader,8,1020);
            OpinionLife.read(town,reader,8,1080);
            assertEquals(51,OpinionLife.score(town,reader,"city"));
            assertEquals(55,OpinionLife.score(town,reader,"stall"));
            assertEquals(50,OpinionLife.score(town,other,"city"));
            assertEquals(1,OpinionLife.report(town).readers());
            state.day=8;state.minute=1080;
            db.save(state,java.util.List.of(),java.util.List.of(),town,java.util.List.of(),java.util.List.of(),java.util.List.of());
        }
        try(var db=new WorldDatabase(file)){
            var state=db.load();var town=db.loadTown(state);var reader=town.get(id);var other=town.get(otherId);
            OpinionLife.read(town,reader,8,1140);
            assertEquals(51,OpinionLife.score(town,reader,"city"));
            assertEquals(55,OpinionLife.score(town,reader,"stall"));
            assertEquals(50,OpinionLife.score(town,other,"stall"));
            OpinionLife.clearStall(town);
            assertEquals(50,OpinionLife.score(town,reader,"stall"));
            assertEquals(51,OpinionLife.score(town,reader,"city"));
        }
    }

    @Test
    void newspaperUsesAttributedPublicClaimsAndRealEditorialFundsAcrossRestart() throws Exception {
        Path file=directory.resolve("newspaper.db");
        String reporterId;
        try(var db=new WorldDatabase(file)){
            var state=db.load();var town=db.loadTown(state);
            var reporter=PopulationGenerator.add(town,1).getFirst();reporterId=reporter.id;
            reporter.age=30;reporter.occupation=NewspaperLife.OCCUPATION;
            reporter.employed=true;reporter.workplace=TownLife.Place.LIBRARY;
            reporter.place=reporter.workplace;reporter.destination=reporter.place;reporter.travelRemaining=0;
            reporter.activity="WORKING";reporter.money=100;
            town.player.place=TownLife.Place.LIBRARY;town.player.destination=town.player.place;town.player.travelRemaining=0;
            town.player.investigationSkill=2;
            CareerLife.apply(town,NewspaperLife.JOB);
            assertThrows(IllegalArgumentException.class,()->CareerLife.work(town,1,600));
            var publicEvent=new TownLife.Event(1,550,"marco",TownLife.Place.PARK,TownLife.publicSpace(TownLife.Place.PARK),
                "Marco Bianchi and Elena Rossi crossed paths at Park.");
            KnowledgeLife.witnessed(town,publicEvent,reporter.id);
            var privateEvent=new TownLife.Event(1,590,"marco",TownLife.Place.PARK,TownLife.privateUnit("marco"),
                "Private residents crossed paths at a private home.");
            KnowledgeLife.witnessed(town,privateEvent,reporter.id);
            assertTrue(NewspaperLife.interview(town,reporter,1,600).contains("first-hand account"));
            assertTrue(NewspaperLife.filed(town,1));
            assertTrue(NewspaperLife.interview(town,reporter,1,601).contains("already filed"));
            int cash=town.employerFunds.get(NewspaperLife.JOB);
            int playerCash=town.player.money;
            CareerLife.work(town,1,600);
            assertEquals(playerCash+20,town.player.money);
            ArchiveLife.record(town,"public-construction",1,840,"Construction","Via Verdi walls completed.");
            NewspaperLife.tick(town,1,959);
            assertTrue(NewspaperLife.report(town,1).editions().isEmpty());
            NewspaperLife.tick(town,1,960);NewspaperLife.tick(town,1,960);
            var issue=NewspaperLife.report(town,1).editions().getFirst();
            assertTrue(issue.text().contains("Official notice: Via Verdi walls completed."));
            assertTrue(issue.text().contains("first-hand account"));
            assertFalse(issue.text().contains("private home"));
            assertEquals(140,reporter.money);
            assertEquals(cash-60,town.employerFunds.get(NewspaperLife.JOB));
            assertEquals(1,NewspaperLife.report(town,1).editions().size());
            state.day=1;state.minute=960;
            db.save(state,java.util.List.of(),java.util.List.of(),town,java.util.List.of(),java.util.List.of(),java.util.List.of());
        }
        try(var db=new WorldDatabase(file)){
            var state=db.load();var town=db.loadTown(state);
            assertTrue(NewspaperLife.filed(town,1));
            assertEquals(1,NewspaperLife.report(town,1).editions().size());
            NewspaperLife.tick(town,1,960);
            assertEquals(140,town.get(reporterId).money);
            town.get(reporterId).place=TownLife.Place.PARK;
            NewspaperLife.tick(town,2,960);
            assertEquals(1,NewspaperLife.report(town,2).editions().size());
        }
    }

    @Test
    void historicalArchiveRetainsMilestonesAndYearPagesWithoutLeakingPrivateRecords() throws Exception {
        Path file=directory.resolve("historical-archive.db");
        try(var db=new WorldDatabase(file)){
            var state=db.load();var town=db.loadTown(state);
            ArchiveLife.begin(town,1,450);
            var candidate=town.copy();
            candidate.business.registered=true;candidate.business.ownerIncarnation=1;
            ArchiveLife.transitions(town,candidate,1,600);
            assertEquals(1,town.archive.size());
            assertEquals(2,candidate.archive.size());
            ArchiveLife.transitions(town,candidate,1,600);
            assertEquals(2,candidate.archive.size());
            for(int day=2;day<=10;day++)
                ArchiveLife.record(candidate,"test:"+day,day,600,"Public event","Recorded milestone "+day);
            ArchiveLife.annual(candidate,365,1439);
            ArchiveLife.annual(candidate,365,1439);
            assertEquals(12,candidate.archive.size());
            var first=ArchiveLife.report(candidate,365,1,1);
            assertEquals(2,first.pages());assertEquals(8,first.entries().size());
            assertEquals("Yearbook",first.entries().getFirst().category());
            assertEquals(4,ArchiveLife.report(candidate,365,1,2).entries().size());
            assertTrue(ArchiveLife.report(candidate,366,2,1).entries().isEmpty());
            candidate.player.place=TownLife.Place.LIBRARY;candidate.player.destination=candidate.player.place;
            candidate.player.travelRemaining=0;
            state.day=366;state.minute=600;
            db.save(state,java.util.List.of(),java.util.List.of(),candidate,java.util.List.of(),java.util.List.of(),java.util.List.of());
            assertTrue(candidate.pendingArchive.isEmpty());
            db.save(state,java.util.List.of(),java.util.List.of(),candidate,java.util.List.of(),java.util.List.of(),java.util.List.of());
        }
        try(var db=new WorldDatabase(file)){
            var state=db.load();var town=db.loadTown(state);
            assertEquals(12,town.archive.size());
            assertTrue(town.pendingArchive.isEmpty());
            assertEquals(12,ArchiveLife.report(town,366,1,1).total());
        }
        try(var game=new GameSession(file)){
            var current=game.snapshot();
            assertEquals(2,current.archive().year());
            var history=game.gardenAction("archive-year-1-2").world();
            assertEquals(1,history.archive().year());assertEquals(2,history.archive().page());
            assertEquals(current.minute(),history.minute());
            assertEquals(current.player().money(),history.player().money());
            game.move("PARK");
            assertNull(game.snapshot().archive());
            assertThrows(IllegalArgumentException.class,()->game.gardenAction("archive-year-1-1"));
        }
    }

    @Test
    void educationRequiresAttendanceAndBudgetAndPersistsCareerSkills() throws Exception {
        Path file=directory.resolve("education.db");
        String id;
        try(var db=new WorldDatabase(file)){
            var state=db.load();var town=db.loadTown(state);
            var pupil=PopulationGenerator.add(town,1).getFirst();id=pupil.id;
            pupil.age=10;pupil.employed=false;pupil.occupation="Child";
            for(String subject:EducationLife.SUBJECTS)EducationLife.set(town,pupil,subject,0);
            pupil.place=TownLife.Place.SCHOOL;pupil.destination=pupil.place;pupil.travelRemaining=0;
            state.day=1;state.minute=600;
            town.employerFunds.put("school",2);
            pupil.activity="FREE_TIME";EducationLife.attendance(town,pupil,state);
            assertEquals(0,EducationLife.report(town).lessons());
            pupil.activity="STUDYING";EducationLife.attendance(town,pupil,state);
            EducationLife.attendance(town,pupil,state);
            assertEquals(1,EducationLife.report(town).lessons());
            assertEquals(1,town.employerFunds.get("school"));
            state.day=2;EducationLife.attendance(town,pupil,state);
            state.day=3;EducationLife.attendance(town,pupil,state);
            assertEquals(2,EducationLife.report(town).lessons());
            assertEquals(2,town.employerFunds.get(EducationLife.SCHOOL_SERVICE));
            assertEquals(0,town.employerFunds.get("school"));
            pupil.age=18;pupil.occupation="Job seeker";
            EducationLife.graduate(town,pupil);EducationLife.graduate(town,pupil);
            assertEquals(1,EducationLife.report(town).graduates());
            for(String subject:EducationLife.SUBJECTS)EducationLife.set(town,pupil,subject,0);
            EducationLife.set(town,pupil,"technical",80);
            String[] jobs={"Teacher","Mechanic","Nurse","Baker"};
            assertEquals(1,EducationLife.preferredRole(town,pupil,jobs));
            EducationLife.set(town,pupil,"technical",0);
            assertEquals(3,EducationLife.preferredRole(town,pupil,jobs));
            EducationLife.set(town,pupil,"technical",80);
            EmploymentLife.hire(town,state,pupil,1);
            pupil.place=pupil.workplace;pupil.destination=pupil.place;pupil.activity="WORKING";
            state.minute=720;EducationLife.attendance(town,pupil,state);
            EducationLife.attendance(town,pupil,state);
            assertEquals(81,EducationLife.get(town,pupil,"technical"));
            assertEquals(1,EducationLife.report(town).workDays());
            db.save(state,java.util.List.of(),java.util.List.of(),town,java.util.List.of(),java.util.List.of(),java.util.List.of());
        }
        try(var db=new WorldDatabase(file)){
            var state=db.load();var town=db.loadTown(state);var pupil=town.get(id);
            assertEquals(81,EducationLife.get(town,pupil,"technical"));
            assertEquals(2,EducationLife.report(town).lessons());
            assertEquals(1,EducationLife.report(town).graduates());
            EducationLife.attendance(town,pupil,state);
            assertEquals(81,EducationLife.get(town,pupil,"technical"));
            assertEquals(2,town.employerFunds.get(EducationLife.SCHOOL_SERVICE));
        }
    }

    @Test
    void familyBirthSchoolRetirementAndInheritancePersistAcrossRestart() throws Exception {
        Path file=directory.resolve("demography.db");
        String firstId,secondId,childId;
        try(var db=new WorldDatabase(file)){
            var state=db.load();var town=db.loadTown(state);
            var adults=PopulationGenerator.add(town,2);
            var first=adults.get(0);var second=adults.get(1);
            firstId=first.id;secondId=second.id;
            first.age=30;second.age=29;first.money=150;second.money=150;
            var relation=new SocialLife.Relationship(first.id,second.id);
            relation.affinity=80;relation.trust=80;relation.tension=0;
            town.peerRelationships.put(SocialNetwork.key(first.id,second.id),relation);
            assertTrue(DemographyLife.pair(town,first,second,1));
            assertNull(DemographyLife.birth(town,first,second,270));
            var child=DemographyLife.birth(town,first,second,271);
            assertNotNull(child);childId=child.id;
            assertEquals(280,first.money+second.money);
            assertEquals(20,child.money);assertEquals(0,child.age);
            assertEquals(first.homeUnit,child.homeUnit);
            assertFalse(child.employed);
            assertNull(DemographyLife.birth(town,first,second,271));
            assertThrows(IllegalArgumentException.class,()->EmploymentLife.hire(town,state,child,0));
            assertEquals(1,DemographyLife.report(town).births());
            first.age=100;first.personalSeed=0;first.money=120;
            second.age=66;second.money=140;
            state.day=271;state.minute=360;
            db.save(state,java.util.List.of(),java.util.List.of(),town,java.util.List.of(),java.util.List.of(),java.util.List.of());
        }
        try(var db=new WorldDatabase(file)){
            var state=db.load();var town=db.loadTown(state);
            var first=town.get(firstId);var second=town.get(secondId);var child=town.get(childId);
            assertEquals(first.generationIndex,DemographyLife.get(town,child,"parent-a"));
            assertEquals(second.generationIndex,DemographyLife.get(town,child,"parent-b"));
            state.day=366;state.minute=360;
            DemographyLife.tick(town,state,new java.util.ArrayList<>(),new java.util.ArrayList<>());
            DemographyLife.tick(town,state,new java.util.ArrayList<>(),new java.util.ArrayList<>());
            assertFalse(first.alive);assertEquals(0,first.money);assertEquals(140,child.money);
            assertEquals(second.homeUnit,child.homeUnit);
            assertEquals("Retired",second.occupation);assertFalse(second.employed);
            assertEquals(120,DemographyLife.report(town).inherited());
            assertEquals(1,DemographyLife.report(town).deaths());
            assertThrows(IllegalArgumentException.class,()->EmploymentLife.hire(town,state,second,0));
            state.day=372;
            int city=town.employerFunds.get(MunicipalLife.CITY);
            DemographyLife.tick(town,state,new java.util.ArrayList<>(),new java.util.ArrayList<>());
            assertEquals(175,second.money);
            assertEquals(city-35,town.employerFunds.get(MunicipalLife.CITY));
            child.age=5;state.day=271+6*365;
            DemographyLife.tick(town,state,new java.util.ArrayList<>(),new java.util.ArrayList<>());
            assertEquals(6,child.age);
            child.place=TownLife.Place.SCHOOL;child.destination=child.place;child.travelRemaining=0;
            state.day=2466;state.minute=600;
            PopulationLife.tick(town,state,new java.util.ArrayList<>(),new java.util.ArrayList<>());
            assertFalse(child.employed);
            child.age=17;state.day=271+18*365;state.minute=360;
            DemographyLife.tick(town,state,new java.util.ArrayList<>(),new java.util.ArrayList<>());
            assertEquals(18,child.age);assertEquals("Job seeker",child.occupation);
            assertEquals(TownLife.privateUnit(child.id),child.homeUnit);
            assertFalse(child.employed);
            db.save(state,java.util.List.of(),java.util.List.of(),town,java.util.List.of(),java.util.List.of(),java.util.List.of());
        }
        try(var db=new WorldDatabase(file)){
            var state=db.load();var town=db.loadTown(state);
            assertEquals(18,town.get(childId).age);
            assertFalse(town.get(firstId).alive);
            assertEquals(1,DemographyLife.report(town).births());
            assertEquals(1,DemographyLife.report(town).deaths());
            assertEquals(120,DemographyLife.report(town).inherited());
        }
    }

    @Test
    void municipalBudgetTransfersTaxesAndFundsServicesWithoutMintingCash() throws Exception {
        Path file=directory.resolve("comune.db");
        String residentId;
        try(var db=new WorldDatabase(file)) {
            var state=db.load();var town=db.loadTown(state);var p=town.player;
            var resident=PopulationGenerator.add(town,1).getFirst();residentId=resident.id;
            p.money=200;resident.money=140;
            int city=town.employerFunds.get("city-maintenance");
            int school=town.employerFunds.get("school"),library=town.employerFunds.get("library");
            var pump=town.technicalOrders.get("water-pump");
            pump.condition=75;pump.phase=TechnicalWorkLife.Phase.COMPLETED;
            var lights=town.technicalOrders.get("street-lights");
            lights.condition=80;lights.phase=TechnicalWorkLife.Phase.COMPLETED;
            int parts=town.technicalMaterials.get("spare-parts"),steel=town.technicalMaterials.get("steel");
            MunicipalLife.tick(town,1,360);
            assertEquals(200,p.money);
            MunicipalLife.tick(town,8,360);MunicipalLife.tick(town,8,360);
            var report=MunicipalLife.report(town);
            assertEquals(195,p.money);assertEquals(138,resident.money);
            assertEquals(7,report.taxes());assertEquals(city+7-26,report.budget());
            assertEquals(school+6,report.schoolFunds());assertEquals(library+4,report.libraryFunds());
            assertEquals(8,report.clinicFunds());assertEquals(8,report.maintenance());
            assertEquals(83,pump.condition);assertEquals(85,lights.condition);
            assertEquals(parts-1,town.technicalMaterials.get("spare-parts"));
            assertEquals(steel-1,town.technicalMaterials.get("steel"));
            resident.money=70;
            assertEquals(8,MunicipalLife.medicalAid(town,resident,75));
            assertEquals(0,MunicipalLife.medicalAid(town,resident,75));
            assertEquals(8,town.employerFunds.get(MunicipalLife.CARE));
            state.day=8;state.minute=360;
            db.save(state,java.util.List.of(),java.util.List.of(),town,java.util.List.of(),java.util.List.of(),java.util.List.of());
        }
        try(var db=new WorldDatabase(file)) {
            var state=db.load();var town=db.loadTown(state);var p=town.player;
            assertEquals(7,MunicipalLife.report(town).taxes());
            assertEquals(8,MunicipalLife.report(town).medicalAid());
            assertEquals(8,town.employerFunds.get(MunicipalLife.CARE));
            assertEquals(83,town.technicalOrders.get("water-pump").condition);
            MunicipalLife.tick(town,8,360);
            assertEquals(195,p.money);assertEquals(7,MunicipalLife.report(town).taxes());
            town.employerFunds.put(MunicipalLife.CITY,40);
            p.money=100;town.get(residentId).money=70;
            MunicipalLife.tick(town,15,360);
            assertEquals(40,MunicipalLife.report(town).budget());
            assertEquals(8,MunicipalLife.report(town).maintenance());
            assertEquals(83,town.technicalOrders.get("water-pump").condition);
        }
    }

    @Test
    void constructionConsumesRealMaterialsAndPaidLaborAndPersistsCompletedHouse() throws Exception {
        Path file=directory.resolve("construction.db");
        try(var db=new WorldDatabase(file)) {
            var state=db.load();var town=db.loadTown(state);var p=town.player;
            p.place=TownLife.Place.OFFICE;p.destination=p.place;p.travelRemaining=0;p.money=200;
            town.industryStocks.put(IndustryLife.FACTORY_STEEL,3);
            town.industryStocks.put(IndustryLife.FACTORY_PARTS,2);
            int factory=town.employerFunds.getOrDefault("machinist",0);
            ConstructionLife.act(town,"construction-buy",600);
            assertThrows(IllegalArgumentException.class,()->ConstructionLife.act(town,"construction-buy",600));
            for(int i=0;i<3;i++)ConstructionLife.act(town,"construction-steel",600);
            for(int i=0;i<2;i++)ConstructionLife.act(town,"construction-parts",600);
            assertEquals(factory+22,town.employerFunds.get("machinist"));
            assertEquals(0,IndustryLife.stock(town,IndustryLife.FACTORY_STEEL));
            assertEquals(0,IndustryLife.stock(town,IndustryLife.FACTORY_PARTS));
            ConstructionLife.act(town,"construction-start",600);
            ConstructionLife.tick(town,1,840);
            assertEquals(0,ConstructionLife.report(town).stage());
            for(int i=0;i<3;i++)ConstructionLife.act(town,"construction-fund",600);
            ConstructionLife.tick(town,2,840);ConstructionLife.tick(town,2,840);
            assertEquals(1,ConstructionLife.report(town).stage());
            assertEquals(20,ConstructionLife.report(town).reserve());
            assertEquals(2,ConstructionLife.report(town).steel());
            assertEquals(2,ConstructionLife.report(town).parts());
            assertThrows(IllegalArgumentException.class,()->ConstructionLife.act(town,"construction-sell",600));
            state.day=2;state.minute=840;
            db.save(state,java.util.List.of(),java.util.List.of(),town,java.util.List.of(),java.util.List.of(),java.util.List.of());
        }
        try(var db=new WorldDatabase(file)) {
            var state=db.load();var town=db.loadTown(state);var p=town.player;
            assertEquals(1,ConstructionLife.report(town).stage());
            ConstructionLife.tick(town,2,840);
            ConstructionLife.tick(town,3,840);
            assertEquals(2,ConstructionLife.report(town).stage());
            ConstructionLife.tick(town,4,840);
            assertEquals(3,ConstructionLife.report(town).stage());
            assertEquals(0,ConstructionLife.report(town).steel());
            assertEquals(0,ConstructionLife.report(town).parts());
            assertEquals(0,ConstructionLife.report(town).reserve());
            assertEquals(30,town.employerFunds.get(ConstructionLife.CONTRACTOR));
            assertEquals(88,p.money);
            ConstructionLife.act(town,"construction-sell",900);
            assertEquals(238,p.money);
            assertFalse(ConstructionLife.report(town).owned());
            assertEquals(3,ConstructionLife.report(town).stage());
            assertThrows(IllegalArgumentException.class,()->ConstructionLife.act(town,"construction-buy",900));
            db.save(state,java.util.List.of(),java.util.List.of(),town,java.util.List.of(),java.util.List.of(),java.util.List.of());
        }
        try(var game=new GameSession(file)) {
            assertEquals(3,game.snapshot().construction().stage());
            assertFalse(game.snapshot().construction().owned());
        }
    }

    @Test
    void apartmentLeaseMovesResidentConservesRentAndSurvivesRestart() throws Exception {
        Path file=directory.resolve("rental.db");
        String tenantId;
        try(var db=new WorldDatabase(file)) {
            var state=db.load();var town=db.loadTown(state);var p=town.player;
            var tenant=PopulationGenerator.add(town,1).getFirst();tenantId=tenant.id;
            tenant.home=TownLife.Place.SOUTH_RESIDENCES;tenant.place=tenant.home;tenant.destination=tenant.home;
            tenant.travelRemaining=0;tenant.money=200;tenant.unpaidBills=0;
            p.place=TownLife.Place.OFFICE;p.destination=p.place;p.travelRemaining=0;p.money=250;
            int city=town.employerFunds.getOrDefault("city-maintenance",0);
            RentalLife.act(town,"rental-buy",600);
            assertEquals(70,p.money);assertEquals(city+180,town.employerFunds.get("city-maintenance"));
            RentalLife.act(town,"rental-list",600);
            RentalLife.tick(town,1,360);RentalLife.tick(town,1,360);
            assertEquals(125,p.money);assertEquals(145,tenant.money);
            assertEquals(TownLife.Place.NORTH_RESIDENCES,tenant.home);
            assertEquals(TownLife.Place.SOUTH_RESIDENCES,tenant.place);
            assertEquals(RentalLife.UNIT,tenant.homeUnit);
            assertEquals(8,RentalLife.report(town).paidUntilDay());
            assertThrows(IllegalArgumentException.class,()->RentalLife.act(town,"rental-sell",600));
            state.day=4;state.minute=480;
            EconomyLife.tick(town,state,new java.util.ArrayList<>(),new java.util.ArrayList<>());
            assertEquals(145,tenant.money);assertEquals(0,tenant.unpaidBills);
            db.save(state,java.util.List.of(),java.util.List.of(),town,java.util.List.of(),java.util.List.of(),java.util.List.of());
        }
        try(var db=new WorldDatabase(file)) {
            var state=db.load();var town=db.loadTown(state);var p=town.player;var tenant=town.get(tenantId);
            assertTrue(RentalLife.report(town).occupied());
            assertEquals(RentalLife.UNIT,tenant.homeUnit);
            assertEquals(55,RentalLife.report(town).revenue());
            RentalLife.tick(town,8,360);RentalLife.tick(town,8,360);
            assertEquals(180,p.money);assertEquals(90,tenant.money);
            assertEquals(15,RentalLife.report(town).paidUntilDay());
            RentalLife.act(town,"rental-unlist",600);
            assertTrue(RentalLife.housed(town,tenant,14));
            RentalLife.tick(town,15,360);
            assertFalse(RentalLife.report(town).occupied());
            assertEquals(TownLife.Place.SOUTH_RESIDENCES,tenant.home);
            assertEquals(TownLife.privateUnit(tenant.id),tenant.homeUnit);
            assertEquals(90,tenant.money);
            RentalLife.act(town,"rental-sell",600);
            assertEquals(320,p.money);
            assertFalse(RentalLife.report(town).owned());
        }
    }

    @Test
    void bankConservesCashProtectsDepositsAndPersistsWeeklyInterest() throws Exception {
        Path file=directory.resolve("bank.db");
        try(var db=new WorldDatabase(file)) {
            var state=db.load();var town=db.loadTown(state);var p=town.player;
            p.money=200;p.place=TownLife.Place.PARK;p.destination=p.place;p.travelRemaining=0;
            assertThrows(IllegalArgumentException.class,()->BankLife.act(town,"bank-deposit-personal",1,600));
            p.place=TownLife.Place.OFFICE;p.destination=p.place;
            int bank=BankLife.cash(town);
            assertThrows(IllegalArgumentException.class,()->BankLife.act(town,"bank-borrow",1,600));
            assertThrows(IllegalArgumentException.class,()->BankLife.act(town,"bank-deposit-business",1,600));
            for(int i=0;i<10;i++)BankLife.act(town,"bank-deposit-personal",1,600);
            assertEquals(100,p.money);assertEquals(bank+100,BankLife.cash(town));
            assertEquals(bank,BankLife.free(town));
            p.jobId="engineer";
            BankLife.act(town,"bank-borrow",1,600);
            assertEquals(150,p.money);assertEquals(bank+50,BankLife.cash(town));
            assertThrows(IllegalArgumentException.class,()->BankLife.act(town,"bank-borrow",1,600));
            BankLife.tick(town,8,360);BankLife.tick(town,8,360);
            assertEquals(52,BankLife.report(town).debt());
            assertEquals(101,BankLife.report(town).personal());
            assertEquals(1,BankLife.report(town).interestEarned());
            assertEquals(bank+50,BankLife.cash(town));
            state.day=8;state.minute=360;
            db.save(state,java.util.List.of(),java.util.List.of(),town,java.util.List.of(),java.util.List.of(),java.util.List.of());
        }
        try(var db=new WorldDatabase(file)) {
            var state=db.load();var town=db.loadTown(state);var p=town.player;
            assertEquals(52,BankLife.report(town).debt());
            assertEquals(101,BankLife.report(town).personal());
            BankLife.tick(town,8,360);
            assertEquals(52,BankLife.report(town).debt());
            int before=p.money+BankLife.cash(town);
            BankLife.act(town,"bank-repay",8,600);
            BankLife.act(town,"bank-withdraw-personal",8,600);
            assertEquals(42,BankLife.report(town).debt());
            assertEquals(91,BankLife.report(town).personal());
            assertEquals(before,p.money+BankLife.cash(town));
            MarketStallLife.set(town,MarketStallLife.LICENSE,1);
            BankLife.act(town,"bank-deposit-business",8,600);
            assertEquals(10,BankLife.report(town).business());
            int city=town.employerFunds.getOrDefault("city-maintenance",0);
            BankLife.clear(town);
            assertEquals(city+101,town.employerFunds.get("city-maintenance"));
            assertEquals(0,BankLife.report(town).debt());
            assertEquals(0,BankLife.report(town).personal());
            assertEquals(0,BankLife.report(town).business());
        }
    }

    @Test
    void standingFarmOrderReservesCargoAndMoneyAcrossRestart() throws Exception {
        Path file=directory.resolve("stall-supply.db");
        try(var db=new WorldDatabase(file)) {
            var state=db.load();var town=db.loadTown(state);var p=town.player;
            p.place=TownLife.Place.MARKET;p.destination=p.place;p.travelRemaining=0;p.money=100;
            MarketStallLife.act(town,"stall-register",1,480);
            MarketStallLife.act(town,"stall-supply-fund",1,480);
            MarketStallLife.act(town,"stall-supply-fund",1,480);
            MarketStallLife.act(town,"stall-supply-enable",1,480);
            assertEquals(60,p.money);
            FarmLife.set(town,FarmLife.barn("vegetables"),8);
            int fuel=FuelLife.stock(town),farm=town.employerFunds.getOrDefault(FarmLife.ACCOUNT,0);
            StallSupplyLife.tick(town,1,540);StallSupplyLife.tick(town,1,540);
            assertEquals(4,StallSupplyLife.reserved(town));
            assertEquals(4,FarmLife.stock(town,FarmLife.barn("vegetables")));
            assertEquals(fuel-1,FuelLife.stock(town));
            assertEquals(farm,town.employerFunds.getOrDefault(FarmLife.ACCOUNT,0));
            assertEquals(9,StallSupplyLife.report(town).reserve());
            assertEquals(0,MarketStallLife.total(town));
            FoodLife.change(p,"bread",false,9);
            for(int i=0;i<8;i++)MarketStallLife.act(town,"stall-stock-bread",1,550);
            assertThrows(IllegalArgumentException.class,()->MarketStallLife.act(town,"stall-stock-bread",1,550));
            assertEquals(1,FoodLife.quantity(p,"bread",false));
            MarketStallLife.act(town,"stall-supply-disable",1,550);
            p.place=TownLife.Place.PARK;p.destination=p.place;
            state.day=1;state.minute=550;
            db.save(state,java.util.List.of(),java.util.List.of(),town,java.util.List.of(),java.util.List.of(),java.util.List.of());
        }
        try(var db=new WorldDatabase(file)) {
            var state=db.load();var town=db.loadTown(state);
            assertFalse(StallSupplyLife.report(town).enabled());
            assertEquals(4,StallSupplyLife.reserved(town));
            int farm=town.employerFunds.getOrDefault(FarmLife.ACCOUNT,0);
            StallSupplyLife.tick(town,1,599);
            assertEquals(8,MarketStallLife.total(town));
            StallSupplyLife.tick(town,1,600);StallSupplyLife.tick(town,1,600);
            assertEquals(12,MarketStallLife.total(town));
            assertEquals(4,MarketStallLife.get(town,MarketStallLife.stock("vegetables")));
            assertEquals(0,StallSupplyLife.reserved(town));
            assertEquals(farm+8,town.employerFunds.getOrDefault(FarmLife.ACCOUNT,0));
            assertEquals(4,StallSupplyLife.report(town).delivered());
            StallSupplyLife.tick(town,2,540);
            assertEquals(0,StallSupplyLife.reserved(town));
            db.save(state,java.util.List.of(),java.util.List.of(),town,java.util.List.of(),java.util.List.of(),java.util.List.of());
        }
        try(var game=new GameSession(file)) {
            assertEquals(4,game.snapshot().stall().supply().delivered());
            assertEquals(12,game.snapshot().stall().stocked());
        }
    }

    @Test
    void sellerWorksOnlyPaidHoursAndPayrollSurvivesRestart() throws Exception {
        Path file=directory.resolve("stall-seller.db");
        String sellerId,buyerId;
        try(var db=new WorldDatabase(file)) {
            var state=db.load();var town=db.loadTown(state);var p=town.player;
            var residents=PopulationGenerator.add(town,2);
            var seller=residents.get(0);var buyer=residents.get(1);
            sellerId=seller.id;buyerId=buyer.id;
            p.place=TownLife.Place.MARKET;p.destination=p.place;p.travelRemaining=0;p.money=100;
            seller.place=p.place;seller.destination=p.place;seller.travelRemaining=0;seller.employed=false;seller.money=20;
            buyer.place=p.place;buyer.destination=p.place;buyer.travelRemaining=0;buyer.food=0;buyer.money=10;
            MarketStallLife.act(town,"stall-register",1,720);
            assertThrows(IllegalArgumentException.class,()->MarketStallLife.act(town,"stall-hire-"+seller.id,1,720));
            MarketStallLife.act(town,"stall-fund",1,720);
            MarketStallLife.act(town,"stall-hire-"+seller.id,1,720);
            assertEquals(70,p.money);
            FoodLife.change(p,"vegetables",false,2);
            MarketStallLife.act(town,"stall-stock-vegetables",1,720);
            MarketStallLife.act(town,"stall-stock-vegetables",1,720);
            MarketStallLife.act(town,"stall-open",1,720);
            p.place=TownLife.Place.PARK;p.destination=p.place;
            assertNull(MarketStallLife.purchase(town,buyer,1,720));
            state.day=1;state.minute=720;
            MarketStallLife.pay(town,seller,state,new java.util.ArrayList<>(),new java.util.ArrayList<>());
            MarketStallLife.pay(town,seller,state,new java.util.ArrayList<>(),new java.util.ArrayList<>());
            assertEquals(24,seller.money);assertEquals(6,MarketStallLife.get(town,MarketStallLife.RESERVE));
            assertTrue(MarketStallLife.staffed(town,1,720));
            assertNotNull(MarketStallLife.purchase(town,buyer,1,721));
            assertEquals(73,p.money);assertEquals(7,buyer.money);
            db.save(state,java.util.List.of(),java.util.List.of(),town,java.util.List.of(),java.util.List.of(),java.util.List.of());
        }
        try(var db=new WorldDatabase(file)) {
            var state=db.load();var town=db.loadTown(state);var seller=town.get(sellerId);var buyer=town.get(buyerId);
            assertEquals(sellerId,MarketStallLife.worker(town).id);
            assertTrue(MarketStallLife.staffed(town,1,720));
            assertFalse(MarketStallLife.staffed(town,2,720));
            MarketStallLife.pay(town,seller,state,new java.util.ArrayList<>(),new java.util.ArrayList<>());
            assertEquals(24,seller.money);
            state.minute=780;
            MarketStallLife.pay(town,seller,state,new java.util.ArrayList<>(),new java.util.ArrayList<>());
            assertEquals(28,seller.money);assertEquals(2,MarketStallLife.get(town,MarketStallLife.RESERVE));
            assertTrue(MarketStallLife.staffed(town,1,780));
            seller.sickUntilDay=1;
            assertNull(MarketStallLife.purchase(town,buyer,1,780));
            seller.sickUntilDay=0;
            assertNull(MarketStallLife.purchase(town,buyer,1,840));
            state.day=2;state.minute=720;
            MarketStallLife.pay(town,seller,state,new java.util.ArrayList<>(),new java.util.ArrayList<>());
            assertFalse(MarketStallLife.staffed(town,2,720));assertEquals(28,seller.money);
            MarketStallLife.clear(town);
            assertFalse(seller.employed);assertNull(MarketStallLife.worker(town));
        }
    }

    @Test
    void stallLoyaltyRequiresSeparateShoppingDaysAndTrustedLocalRecommendation() throws Exception {
        Path file=directory.resolve("stall-reputation.db");
        String buyerId,listenerId;
        try(var db=new WorldDatabase(file)) {
            var state=db.load();var town=db.loadTown(state);var p=town.player;
            var citizens=PopulationGenerator.add(town,2);
            var buyer=citizens.get(0);var listener=citizens.get(1);
            buyerId=buyer.id;listenerId=listener.id;
            p.place=TownLife.Place.MARKET;p.destination=p.place;p.money=100;
            buyer.place=p.place;buyer.destination=p.place;buyer.travelRemaining=0;buyer.money=30;buyer.food=0;
            listener.place=p.place;listener.destination=p.place;listener.travelRemaining=0;listener.money=30;listener.food=0;
            MarketStallLife.act(town,"stall-register",1,720);
            FoodLife.change(p,"vegetables",false,5);
            for(int i=0;i<5;i++)MarketStallLife.act(town,"stall-stock-vegetables",1,720);
            MarketStallLife.act(town,"stall-open",1,720);
            assertNotNull(MarketStallLife.purchase(town,buyer,1,720));
            assertNotNull(MarketStallLife.purchase(town,buyer,1,721));
            assertFalse(MarketStallLife.regular(town,buyer));
            buyer.food=0;
            assertNotNull(MarketStallLife.purchase(town,buyer,2,720));
            assertTrue(MarketStallLife.regular(town,buyer));
            assertFalse(MarketStallLife.recommend(town,buyer,listener));
            var relation=new SocialLife.Relationship(buyer.id,listener.id);
            relation.trust=70;relation.tension=0;
            town.peerRelationships.put(SocialNetwork.key(buyer.id,listener.id),relation);
            listener.place=TownLife.Place.PARK;
            assertFalse(MarketStallLife.recommend(town,buyer,listener));
            listener.place=TownLife.Place.MARKET;
            assertTrue(MarketStallLife.recommend(town,buyer,listener));
            assertFalse(MarketStallLife.recommend(town,buyer,listener));
            MarketStallLife.act(town,"stall-price-up-vegetables",1,720);
            assertEquals("vegetables",MarketStallLife.offer(town,buyer,1,720));
            listener.hunger=20;
            assertNull(MarketStallLife.offer(town,listener,1,720));
            listener.hunger=80;
            assertEquals("vegetables",MarketStallLife.offer(town,listener,1,720));
            db.save(state,java.util.List.of(),java.util.List.of(),town,java.util.List.of(),java.util.List.of(),java.util.List.of());
        }
        try(var db=new WorldDatabase(file)) {
            var state=db.load();var town=db.loadTown(state);
            assertTrue(MarketStallLife.regular(town,town.get(buyerId)));
            assertEquals(1,MarketStallLife.report(town,1,720).customers());
            assertEquals(1,MarketStallLife.report(town,1,720).regularCustomers());
            assertEquals(1,MarketStallLife.report(town,1,720).recommendedCustomers());
            assertEquals("vegetables",MarketStallLife.offer(town,town.get(listenerId),1,720));
            MarketStallLife.clear(town);
            assertFalse(MarketStallLife.regular(town,town.get(buyerId)));
            assertEquals(0,MarketStallLife.report(town,1,720).recommendedCustomers());
        }
    }

    @Test
    void marketStallUsesRealStockPricesAndCustomerMoneyAcrossRestart() throws Exception {
        Path file=directory.resolve("stall.db");
        try(var db=new WorldDatabase(file)) {
            var state=db.load();var town=db.loadTown(state);var p=town.player;
            p.place=TownLife.Place.PARK;p.destination=p.place;p.travelRemaining=0;p.money=100;
            assertThrows(IllegalArgumentException.class,()->MarketStallLife.act(town,"stall-register",1,720));
            p.place=TownLife.Place.MARKET;p.destination=p.place;
            MarketStallLife.act(town,"stall-register",1,720);
            assertEquals(80,p.money);
            FoodLife.change(p,"vegetables",false,2);
            MarketStallLife.act(town,"stall-stock-vegetables",1,720);
            MarketStallLife.act(town,"stall-stock-vegetables",1,720);
            assertEquals(0,FoodLife.quantity(p,"vegetables",false));
            MarketStallLife.act(town,"stall-price-up-vegetables",1,720);
            MarketStallLife.act(town,"stall-open",1,720);
            var buyer=new TownLife.Citizen("test-buyer","Test buyer","Worker",TownLife.Place.MARKET);
            buyer.generationIndex=99;buyer.money=10;buyer.food=0;
            assertNull(MarketStallLife.purchase(town,buyer,1,720));
            MarketStallLife.act(town,"stall-price-down-vegetables",1,720);
            MarketStallLife.act(town,"stall-price-down-vegetables",1,720);
            p.place=TownLife.Place.PARK;
            assertNull(MarketStallLife.purchase(town,buyer,1,720));
            p.place=TownLife.Place.MARKET;
            assertNotNull(MarketStallLife.purchase(town,buyer,1,720));
            assertEquals(8,buyer.money);assertEquals(1,buyer.food);
            assertEquals(82,p.money);
            assertEquals(1,MarketStallLife.total(town));
            db.save(state,java.util.List.of(),java.util.List.of(),town,java.util.List.of(),java.util.List.of(),java.util.List.of());
        }
        try(var db=new WorldDatabase(file)) {
            var state=db.load();var town=db.loadTown(state);var p=town.player;
            var report=MarketStallLife.report(town,1,720);
            assertTrue(report.registered());assertTrue(report.open());assertEquals(1,report.stocked());
            assertEquals(1,report.sales());assertEquals(2,report.revenue());assertEquals(82,p.money);
            var buyer=new TownLife.Citizen("test-buyer","Test buyer","Worker",TownLife.Place.MARKET);
            buyer.generationIndex=99;buyer.money=10;buyer.food=0;buyer.home=TownLife.Place.NORTH_RESIDENCES;
            buyer.employed=false;
            for(var resident:town.people.values())resident.food=3;
            town.people.put(buyer.id,buyer);
            state.minute=720;
            PopulationLife.tick(town,state,new java.util.ArrayList<>(),new java.util.ArrayList<>());
            assertEquals(0,MarketStallLife.total(town));assertEquals(84,p.money);
            assertEquals(8,buyer.money);
            assertNull(MarketStallLife.purchase(town,buyer,1,720));
        }
    }

    @Test
    void cellarConservesGrapesAndMaturesOneBatchAcrossRestart() throws Exception {
        Path file=directory.resolve("cellar.db");
        try(var db=new WorldDatabase(file)) {
            var state=db.load();var town=db.loadTown(state);var p=town.player;
            p.ownsHouse=true;p.place=HousingLife.HOUSE_DISTRICT;p.destination=p.place;
            p.travelRemaining=0;p.money=100;
            assertThrows(IllegalArgumentException.class,()->OrchardLife.act(town,"orchard-press-juice",186,720));
            OrchardLife.act(town,"orchard-build-cellar",186,720);
            assertEquals(65,p.money);
            assertThrows(IllegalArgumentException.class,()->OrchardLife.act(town,"orchard-build-cellar",186,720));
            FoodLife.change(p,"grapes",true,5);
            OrchardLife.act(town,"orchard-press-juice",186,720);
            assertEquals(3,FoodLife.quantity(p,"grapes",true));
            assertEquals(1,FoodLife.quantity(p,"grape-juice",true));
            OrchardLife.act(town,"orchard-start-wine",186,720);
            assertEquals(0,FoodLife.quantity(p,"grapes",true));
            assertThrows(IllegalArgumentException.class,()->OrchardLife.act(town,"orchard-start-wine",186,720));
            OrchardLife.tick(town,192,1080);
            assertFalse(OrchardLife.report(town,192,1080).wineReady());
            db.save(state,java.util.List.of(),java.util.List.of(),town,java.util.List.of(),java.util.List.of(),java.util.List.of());
        }
        try(var db=new WorldDatabase(file)) {
            var state=db.load();var town=db.loadTown(state);var p=town.player;
            assertTrue(OrchardLife.report(town,193,720).cellar());
            assertEquals(193,OrchardLife.get(town,OrchardLife.WINE_DUE));
            OrchardLife.tick(town,193,1080);OrchardLife.tick(town,193,1080);
            OrchardLife.act(town,"orchard-collect-wine",193,1100);
            assertThrows(IllegalArgumentException.class,()->OrchardLife.act(town,"orchard-collect-wine",193,1100));
            assertEquals(1,FoodLife.quantity(p,"wine",true));
            FoodLife.change(p,"wine",true,-1);FoodLife.change(p,"wine",false,1);
            FoodLife.change(p,"grape-juice",true,-1);FoodLife.change(p,"grape-juice",false,1);
            p.place=TownLife.Place.MARKET;p.destination=p.place;
            int money=p.money,funds=town.employerFunds.getOrDefault("shop-assistant",0);
            OrchardLife.act(town,"sell-wine",194,720);
            OrchardLife.act(town,"sell-grape-juice",194,720);
            assertEquals(money+13,p.money);
            assertEquals(funds-13,town.employerFunds.get("shop-assistant"));
            assertEquals(1,town.retailStock.get("wine"));
            assertEquals(1,town.retailStock.get("grape-juice"));
            assertThrows(IllegalArgumentException.class,()->OrchardLife.act(town,"sell-wine",194,720));
        }
    }

    @Test
    void beekeepingProducesFiniteHoneyAndPollinatesOrchardAcrossRestart() throws Exception {
        Path file=directory.resolve("bees.db");
        try(var db=new WorldDatabase(file)) {
            var state=db.load();var town=db.loadTown(state);var p=town.player;
            p.ownsHouse=true;p.place=HousingLife.HOUSE_DISTRICT;p.destination=p.place;
            p.travelRemaining=0;p.money=100;p.bottledWater=12;
            OrchardLife.act(town,"orchard-build-hive",186,720);
            assertEquals(70,p.money);
            OrchardLife.act(town,"orchard-plant-apples",186,720);
            var apple=OrchardLife.CROPS.getFirst();
            OrchardLife.set(town,OrchardLife.key(apple,"age"),5);
            int cared=0;
            for(int day=186;day<275&&cared<2;day++) {
                if(WeatherLife.at(day,1080).raining()||WeatherLife.at(day,720).raining())continue;
                OrchardLife.act(town,"orchard-water-bees",day,720);
                OrchardLife.act(town,"orchard-water",day,720);
                OrchardLife.tick(town,day,1080);OrchardLife.tick(town,day,1080);
                cared++;
            }
            assertEquals(2,cared);
            assertEquals(1,OrchardLife.get(town,OrchardLife.HONEY));
            assertEquals(1,OrchardLife.get(town,OrchardLife.key(apple,"pollinated")));
            db.save(state,java.util.List.of(),java.util.List.of(),town,java.util.List.of(),java.util.List.of(),java.util.List.of());
        }
        try(var db=new WorldDatabase(file)) {
            var state=db.load();var town=db.loadTown(state);var p=town.player;
            assertTrue(OrchardLife.report(town,200,720).hive());
            OrchardLife.act(town,"orchard-collect-honey",200,720);
            assertEquals(1,FoodLife.quantity(p,"honey",true));
            assertThrows(IllegalArgumentException.class,()->OrchardLife.act(town,"orchard-collect-honey",200,720));
            var apple=OrchardLife.CROPS.getFirst();
            OrchardLife.set(town,OrchardLife.key(apple,"fruit"),3);
            OrchardLife.act(town,"orchard-harvest-apples",200,720);
            assertEquals(4,FoodLife.quantity(p,"apples",true));
            FoodLife.change(p,"honey",true,-1);FoodLife.change(p,"honey",false,1);
            p.place=TownLife.Place.MARKET;p.destination=p.place;
            int money=p.money;
            OrchardLife.act(town,"sell-honey",200,720);
            assertEquals(money+3,p.money);
            assertEquals(1,town.retailStock.get("honey"));
            assertThrows(IllegalArgumentException.class,()->OrchardLife.act(town,"sell-honey",200,720));
        }
    }

    @Test
    void compostUsesCookingScrapsAndBoostsOnlyOneCrop() throws Exception {
        Path file=directory.resolve("compost.db");
        try(var db=new WorldDatabase(file)) {
            var state=db.load();var town=db.loadTown(state);var p=town.player;
            p.ownsHouse=true;p.place=HousingLife.HOUSE_DISTRICT;p.destination=p.place;
            p.travelRemaining=0;p.money=50;
            GardenLife.act(town,"build-composter",1,720);
            assertEquals(40,p.money);
            assertThrows(IllegalArgumentException.class,()->GardenLife.act(town,"build-composter",1,720));
            FoodLife.change(p,"apples",true,3);
            for(int n=0;n<3;n++) {
                p.hunger=90;
                GardenLife.cookWithCompost(town,FoodLife.recipe("apple-plate"),1,720);
            }
            assertEquals(0,FoodLife.quantity(p,"apples",true));
            assertEquals(3,GardenLife.get(town,GardenLife.SCRAPS));
            assertThrows(IllegalArgumentException.class,()->GardenLife.cookWithCompost(town,FoodLife.recipe("apple-plate"),1,720));
            assertEquals(3,GardenLife.get(town,GardenLife.SCRAPS));
            GardenLife.act(town,"start-compost",1,720);
            assertEquals(0,GardenLife.get(town,GardenLife.SCRAPS));
            GardenLife.tick(town,3,1080);
            assertEquals(0,GardenLife.get(town,GardenLife.COMPOST));
            db.save(state,java.util.List.of(),java.util.List.of(),town,java.util.List.of(),java.util.List.of(),java.util.List.of());
        }
        try(var db=new WorldDatabase(file)) {
            var state=db.load();var town=db.loadTown(state);
            assertEquals(4,GardenLife.get(town,GardenLife.COMPOST_DUE));
            GardenLife.tick(town,4,1080);GardenLife.tick(town,4,1080);
            assertEquals(1,GardenLife.get(town,GardenLife.COMPOST));
            GardenLife.act(town,"plant",5,720);
            GardenLife.act(town,"fertilize",5,720);
            assertEquals(0,GardenLife.get(town,GardenLife.COMPOST));
            assertThrows(IllegalArgumentException.class,()->GardenLife.act(town,"fertilize",5,720));
            assertEquals(6,GardenLife.harvestUnits(town));
            GardenLife.set(town,GardenLife.GROWTH,3);
            GardenLife.act(town,"harvest",8,720);
            assertEquals(6,FoodLife.quantity(town.player,"vegetables",true));
            assertEquals(4,GardenLife.harvestUnits(town));
            assertThrows(IllegalArgumentException.class,()->GardenLife.act(town,"harvest",8,720));
            db.save(state,java.util.List.of(),java.util.List.of(),town,java.util.List.of(),java.util.List.of(),java.util.List.of());
        }
        try(var game=new GameSession(file)) {
            assertTrue(game.snapshot().garden().composter());
            assertFalse(game.snapshot().garden().fertilized());
            assertEquals(6,game.snapshot().player().pantry().get("vegetables"));
        }
    }

    @Test
    void greenhouseAllowsWinterGrowthAndPersistsWithoutDuplicateHarvest() throws Exception {
        Path file=directory.resolve("greenhouse.db");
        try(var game=new GameSession(file)) {
            game.move("south");
            assertThrows(IllegalArgumentException.class,()->game.gardenAction("build-greenhouse"));
        }
        try(var db=new WorldDatabase(file)) {
            var state=db.load();var town=db.loadTown(state);
            var p=town.player;p.ownsHouse=true;p.place=HousingLife.HOUSE_DISTRICT;
            p.destination=p.place;p.travelRemaining=0;p.money=50;p.bottledWater=3;
            assertFalse(GardenLife.report(town,276,720).greenhouse());
            assertThrows(IllegalArgumentException.class,()->GardenLife.act(town,"plant",276,720));
            int funds=town.employerFunds.getOrDefault("technician",0);
            GardenLife.act(town,"build-greenhouse",276,720);
            assertEquals(25,p.money);
            assertEquals(funds+25,town.employerFunds.get("technician"));
            assertThrows(IllegalArgumentException.class,()->GardenLife.act(town,"build-greenhouse",276,720));
            GardenLife.act(town,"plant",276,720);
            GardenLife.tick(town,276,1080);
            assertEquals(0,GardenLife.get(town,GardenLife.GROWTH));
            GardenLife.act(town,"water",277,720);
            assertThrows(IllegalArgumentException.class,()->GardenLife.act(town,"water",277,720));
            GardenLife.tick(town,277,1080);GardenLife.tick(town,277,1080);
            assertEquals(1,GardenLife.get(town,GardenLife.GROWTH));
            db.save(state,java.util.List.of(),java.util.List.of(),town,java.util.List.of(),java.util.List.of(),java.util.List.of());
        }
        try(var db=new WorldDatabase(file)) {
            var state=db.load();var town=db.loadTown(state);
            assertTrue(GardenLife.report(town,278,720).greenhouse());
            assertEquals(1,GardenLife.get(town,GardenLife.GROWTH));
            for(int day=278;day<=279;day++) {
                GardenLife.act(town,"water",day,720);
                GardenLife.tick(town,day,1080);
            }
            GardenLife.act(town,"harvest",279,1100);
            assertEquals(4,FoodLife.quantity(town.player,"vegetables",true));
            assertEquals(0,town.player.bottledWater);
            assertThrows(IllegalArgumentException.class,()->GardenLife.act(town,"harvest",279,1100));
            db.save(state,java.util.List.of(),java.util.List.of(),town,java.util.List.of(),java.util.List.of(),java.util.List.of());
        }
        try(var game=new GameSession(file)) {
            assertTrue(game.snapshot().garden().greenhouse());
            assertFalse(game.snapshot().garden().planted());
            assertEquals(4,game.snapshot().player().pantry().get("vegetables"));
        }
    }

    @Test
    void homeGardenUsesWaterAndWeatherAndPersistsCrop() throws Exception {
        Path file=directory.resolve("garden.db");
        try(var game=new GameSession(file)) {
            game.move("south");
            assertThrows(IllegalArgumentException.class,()->game.gardenAction("plant"));
        }
        try(var db=new WorldDatabase(file)) {
            var state=db.load();var town=db.loadTown(state);
            town.player.ownsHouse=true;town.player.money=50;town.player.bottledWater=4;
            int day=1;
            while(GardenLife.winter(day)||WeatherLife.at(day,720).raining())day++;
            GardenLife.act(town,"plant",day,720);
            assertEquals(47,town.player.money);
            assertThrows(IllegalArgumentException.class,()->GardenLife.act(town,"harvest",1,720));
            GardenLife.act(town,"water",day,720);
            assertEquals(3,town.player.bottledWater);
            final int plantedDay=day;
            assertThrows(IllegalArgumentException.class,()->GardenLife.act(town,"water",plantedDay,720));
            GardenLife.tick(town,day,1080);
            GardenLife.tick(town,day,1080);
            assertEquals(1,GardenLife.get(town,GardenLife.GROWTH));
            for(int n=day+1;GardenLife.get(town,GardenLife.GROWTH)<3;n++) {
                if(GardenLife.winter(n))continue;
                if(!WeatherLife.at(n,720).raining())GardenLife.act(town,"water",n,720);
                GardenLife.tick(town,n,1080);
            }
            db.save(state,java.util.List.of(),java.util.List.of(),town,java.util.List.of(),java.util.List.of(),java.util.List.of());
        }
        try(var game=new GameSession(file)) {
            assertEquals(3,game.snapshot().garden().growth());
            game.gardenAction("harvest");
            assertEquals(4,game.snapshot().player().pantry().get("vegetables"));
            assertFalse(game.snapshot().garden().planted());
            assertThrows(IllegalArgumentException.class,()->game.gardenAction("harvest"));
        }
    }

    @Test
    void housePurchaseEndsRentalAndKeepsPantryAcrossRestart() throws Exception {
        Path file=directory.resolve("owned-home.db");
        try(var game=new GameSession(file)) {
            game.move("south");
            assertThrows(IllegalArgumentException.class,game::buyHouse);
            assertThrows(IllegalArgumentException.class,()->game.sleepInRoom(2));
        }
        try(var db=new WorldDatabase(file)) {
            var state=db.load();var town=db.loadTown(state);
            town.player.money=400;
            town.player.roomUntil=HousingLife.now(state.day,state.minute)+10080;
            town.player.storedBread=2;town.player.storedWater=1;
            db.save(state,java.util.List.of(),java.util.List.of(),town,java.util.List.of(),java.util.List.of(),java.util.List.of());
        }
        try(var game=new GameSession(file)) {
            game.buyHouse();
            assertEquals(80,game.snapshot().player().money());
            assertTrue(game.snapshot().player().ownsHouse());
            assertFalse(game.snapshot().player().roomActive());
            assertEquals(2,game.snapshot().player().storedBread());
            assertThrows(IllegalArgumentException.class,game::buyHouse);
            game.sleepInRoom(2);
            game.transferRoomItem("bread",false);
            game.move("north");
            assertThrows(IllegalArgumentException.class,game::rentRoom);
            assertThrows(IllegalArgumentException.class,()->game.sleepInRoom(2));
        }
        try(var game=new GameSession(file)) {
            assertTrue(game.snapshot().player().ownsHouse());
            assertEquals(1,game.snapshot().player().storedBread());
            game.move("south");
            game.cookRoomMeal();
            assertEquals(0,game.snapshot().player().storedBread());
            assertEquals(0,game.snapshot().player().storedWater());
        }
    }

    @Test
    void logisticsDepotIsPublicAndSupportsCompanyManagement() throws Exception {
        Path file=directory.resolve("depot.db");
        try(var game=new GameSession(file)) {
            game.move("logistics_depot");
            assertEquals("LOGISTICS_DEPOT",game.snapshot().player().placeId());
            game.freightAction("register",null);
            assertEquals(30,game.snapshot().player().money());
            assertTrue(game.snapshot().logistics().yours());
        }
        try(var db=new WorldDatabase(file)) {
            var state=db.load();var town=db.loadTown(state);
            LogisticsLife.set(town,LogisticsLife.CASH,500);
            LogisticsLife.manage(town,"buy-truck");
            SecondFleetLife.manage(town,"buy-truck-2",null,state.day);
            assertEquals(TownLife.Place.LOGISTICS_DEPOT,LogisticsLife.truckPlace(town));
            assertEquals(TownLife.Place.LOGISTICS_DEPOT,SecondFleetLife.place(town));
            assertEquals(60,LogisticsLife.get(town,LogisticsLife.CASH));
            var worker=town.people.values().stream().filter(c->c.generationIndex>0&&c.alive).findFirst().orElseThrow();
            worker.place=TownLife.Place.LOGISTICS_DEPOT;worker.destination=worker.place;worker.travelRemaining=0;worker.sickUntilDay=0;
            LogisticsLife.hire(town,worker.id,state.day);
            assertEquals(TownLife.Place.LOGISTICS_DEPOT,worker.workplace);
            town.player.carPlace=TownLife.Place.LOGISTICS_DEPOT;
            town.player.carFuel=6;town.player.carCondition=80;
            db.save(state,java.util.List.of(),java.util.List.of(),town,java.util.List.of(),java.util.List.of(),java.util.List.of());
        }
        try(var game=new GameSession(file)) {
            assertEquals("LOGISTICS_DEPOT",game.snapshot().logistics().truckPlace());
            assertEquals("LOGISTICS_DEPOT",game.snapshot().logistics().secondTruck().place());
            assertTrue(game.snapshot().player().carDestinations().contains("FUEL_STATION"));
            game.drive("fuel_station");game.drive("logistics_depot");
            assertEquals("LOGISTICS_DEPOT",game.snapshot().player().placeId());
            assertEquals(4,game.snapshot().player().carFuel());
        }
    }

    @Test
    void twoTrucksReserveDistinctCargoAndPersistIndependentDrivers() throws Exception {
        Path file=directory.resolve("two-trucks.db");
        try(var game=new GameSession(file)) {
            game.move("workshop");game.waitMinutes(90);game.freightAction("register",null);
        }
        try(var db=new WorldDatabase(file)) {
            var state=db.load();var town=db.loadTown(state);
            LogisticsLife.set(town,LogisticsLife.CASH,400);
            LogisticsLife.park(town,TownLife.Place.WORKSHOP);
            SecondFleetLife.manage(town,"buy-truck-2",null,state.day);
            var workers=town.people.values().stream().filter(c->c.generationIndex>0&&c.alive).limit(2).toList();
            for(var worker:workers) {
                worker.place=TownLife.Place.WORKSHOP;worker.destination=worker.place;worker.travelRemaining=0;
                worker.sickUntilDay=0;worker.money=100;
            }
            LogisticsLife.hire(town,workers.get(0).id,state.day);
            assertThrows(IllegalArgumentException.class,()->SecondFleetLife.manage(town,"hire-driver-2",workers.get(0).id,state.day));
            SecondFleetLife.manage(town,"hire-driver-2",workers.get(1).id,state.day);
            FarmLife.set(town,FarmLife.barn("vegetables"),8);FarmLife.set(town,FarmLife.barn("rice"),8);
            town.retailStock.put("vegetables",14);town.retailStock.put("rice",18);
            FreightLife.dispatch(town,FreightLife.ROUTES.get(2),true,true);
            SecondFleetLife.dispatch(town,FreightLife.ROUTES.get(2));
            assertEquals(4,FreightLife.reservedCrop(town,"vegetables"));
            assertEquals(2,FreightLife.reservedCrop(town,"rice"));
            assertEquals(0,FarmLife.deliver(town));
            assertEquals(88,LogisticsLife.get(town,LogisticsLife.CASH));
            db.save(state,java.util.List.of(),java.util.List.of(),town,java.util.List.of(),java.util.List.of(),java.util.List.of());
        }
        try(var db=new WorldDatabase(file)) {
            var state=db.load();var town=db.loadTown(state);
            assertTrue(FreightLife.automated(town));assertTrue(SecondFleetLife.active(town));
            var a=LogisticsLife.driver(town);var b=SecondFleetLife.driver(town);
            assertNotEquals(a.id,b.id);
            assertEquals(TownLife.Place.FARM,LogisticsLife.activeDriverDestination(town,a));
            assertEquals(TownLife.Place.FARM,LogisticsLife.activeDriverDestination(town,b));
            for(int k=0;k<5;k++) {
                a.place=FreightLife.truckPlace(town);a.destination=a.place;a.travelRemaining=0;
                b.place=SecondFleetLife.place(town);b.destination=b.place;b.travelRemaining=0;
                state.minute=555+15*k;
                LogisticsLife.tick(town,state,new java.util.ArrayList<>(),new java.util.ArrayList<>());
                SecondFleetLife.tick(town,state,new java.util.ArrayList<>(),new java.util.ArrayList<>());
            }
            assertFalse(FreightLife.active(town));assertFalse(SecondFleetLife.active(town));
            assertEquals(18,town.retailStock.get("vegetables"));assertEquals(20,town.retailStock.get("rice"));
            assertEquals(128,a.money);assertEquals(128,b.money);
            assertEquals(85,LogisticsLife.get(town,LogisticsLife.CONDITION));
            assertEquals(85,LogisticsLife.get(town,SecondFleetLife.HEALTH));
            assertEquals(168,LogisticsLife.get(town,LogisticsLife.CASH));
            assertEquals(2,LogisticsLife.financials(town).deliveries());
            assertEquals(80,LogisticsLife.financials(town).revenue());
        }
    }

    @Test
    void logisticsMaintenanceAndLedgerPersistAndChargeRealResources() throws Exception {
        Path file=directory.resolve("carrier-service.db");
        try(var game=new GameSession(file)) {
            game.move("workshop");game.waitMinutes(90);game.freightAction("register",null);
        }
        try(var db=new WorldDatabase(file)) {
            var state=db.load();var town=db.loadTown(state);
            LogisticsLife.set(town,LogisticsLife.CASH,100);
            LogisticsLife.park(town,TownLife.Place.WORKSHOP);
            LogisticsLife.set(town,LogisticsLife.CONDITION,2);
            town.technicalMaterials.put("spare-parts",1);
            town.industryStocks.put(IndustryLife.FACTORY_PARTS,2);
            town.industryStocks.put(IndustryLife.FACTORY_STEEL,1);
            assertThrows(IllegalArgumentException.class,()->FreightLife.dispatch(town,FreightLife.ROUTES.get(1),true,false));
            assertEquals(100,LogisticsLife.get(town,LogisticsLife.CASH));
            assertEquals(2,IndustryLife.stock(town,IndustryLife.FACTORY_PARTS));
            db.save(state,java.util.List.of(),java.util.List.of(),town,java.util.List.of(),java.util.List.of(),java.util.List.of());
        }
        try(var game=new GameSession(file)) {
            game.freightAction("service-truck",null);
            assertEquals(32,game.snapshot().logistics().condition());
            assertEquals(85,game.snapshot().logistics().cash());
            assertEquals(15,game.snapshot().logistics().financials().maintenance());
            assertThrows(IllegalArgumentException.class,()->game.freightAction("service-truck",null));
            game.move("factory");game.freightAction("load-company",null);
            assertEquals(29,game.snapshot().logistics().condition());
            game.freightAction("drive","workshop");game.freightAction("unload",null);
            assertEquals(26,game.snapshot().logistics().condition());
            assertEquals(113,game.snapshot().logistics().cash());
        }
        try(var game=new GameSession(file)) {
            var report=game.snapshot().logistics();
            assertEquals(26,report.condition());
            assertEquals(40,report.financials().revenue());
            assertEquals(4,report.financials().fuel());
            assertEquals(8,report.financials().relocation());
            assertEquals(13,report.financials().operatingNet());
            assertEquals(1,report.financials().deliveries());
        }
        try(var db=new WorldDatabase(file)) {
            var town=db.loadTown(db.load());
            assertEquals(2,town.technicalMaterials.get("spare-parts"));
        }
    }

    @Test
    void hiredDriverDeliversWhileOwnerIsElsewhereAndReceivesRealPay() throws Exception {
        Path file=directory.resolve("auto-carrier.db");
        try(var game=new GameSession(file)) {
            game.move("workshop");game.waitMinutes(90);
            game.freightAction("register",null);
        }
        String id;
        try(var db=new WorldDatabase(file)) {
            var state=db.load();var town=db.loadTown(state);
            LogisticsLife.set(town,LogisticsLife.CASH,100);
            LogisticsLife.park(town,TownLife.Place.FACTORY);
            town.industryStocks.put(IndustryLife.FACTORY_PARTS,2);
            town.industryStocks.put(IndustryLife.FACTORY_STEEL,1);
            var worker=town.people.values().stream().filter(c->c.generationIndex>0&&c.alive).findFirst().orElseThrow();
            worker.place=TownLife.Place.WORKSHOP;worker.destination=worker.place;worker.travelRemaining=0;
            worker.sickUntilDay=0;worker.money=100;worker.food=8;worker.water=8;
            id=worker.id;
            LogisticsLife.hire(town,id,state.day);
            assertEquals(90,LogisticsLife.get(town,LogisticsLife.CASH));
            assertEquals(110,worker.money);
            LogisticsLife.set(town,LogisticsLife.AUTO,1);
            // Explicitly model the scheduled dispatch at 09:15, independent of incidental NPC actions.
            FreightLife.dispatch(town,FreightLife.ROUTES.get(1),true,true);
            LogisticsLife.set(town,LogisticsLife.LAST_DAY,state.day);
            assertEquals(70,LogisticsLife.get(town,LogisticsLife.CASH));
            town.player.place=TownLife.Place.PARK;town.player.destination=town.player.place;
            db.save(state,java.util.List.of(),java.util.List.of(),town,java.util.List.of(),java.util.List.of(),java.util.List.of());
        }
        try(var db=new WorldDatabase(file)) {
            var state=db.load();var town=db.loadTown(state);
            assertTrue(FreightLife.automated(town));
            var worker=LogisticsLife.driver(town);
            assertEquals(id,worker.id);
            worker.place=TownLife.Place.FACTORY;worker.destination=worker.place;worker.travelRemaining=0;
            state.minute=555;
            LogisticsLife.tick(town,state,new java.util.ArrayList<>(),new java.util.ArrayList<>());
            assertEquals(TownLife.Place.WORKSHOP,FreightLife.truckPlace(town));
            worker.place=TownLife.Place.WORKSHOP;worker.destination=worker.place;worker.travelRemaining=0;
            state.minute=570;
            LogisticsLife.tick(town,state,new java.util.ArrayList<>(),new java.util.ArrayList<>());
            assertFalse(FreightLife.active(town));
            assertEquals(110,LogisticsLife.get(town,LogisticsLife.CASH));
            assertEquals(128,worker.money);
            assertEquals(30,town.player.money);
            assertEquals(1,LogisticsLife.get(town,LogisticsLife.COMPLETED));
            LogisticsLife.tick(town,state,new java.util.ArrayList<>(),new java.util.ArrayList<>());
            assertEquals(128,worker.money);
        }
    }

    @Test
    void logisticsCompanyOwnsTruckAndReceivesContractRevenueNotPersonalWages() throws Exception {
        Path file=directory.resolve("logistics.db");
        try(var game=new GameSession(file)) {
            game.move("workshop");game.waitMinutes(90);
            game.freightAction("register",null);
            assertEquals(30,game.snapshot().player().money());
            assertThrows(IllegalArgumentException.class,()->game.freightAction("buy-truck",null));
        }
        try(var db=new WorldDatabase(file)) {
            var state=db.load();var town=db.loadTown(state);
            town.player.money=400;
            town.industryStocks.put(IndustryLife.FACTORY_PARTS,2);
            town.industryStocks.put(IndustryLife.FACTORY_STEEL,1);
            db.save(state,java.util.List.of(),java.util.List.of(),town,java.util.List.of(),java.util.List.of(),java.util.List.of());
        }
        try(var game=new GameSession(file)) {
            for(int k=0;k<3;k++)game.freightAction("invest-100",null);
            game.freightAction("buy-truck",null);
            assertEquals(80,game.snapshot().logistics().cash());
            assertEquals(100,game.snapshot().player().money());
            assertTrue(game.snapshot().logistics().truck());
            game.move("factory");game.freightAction("load-company",null);
            assertTrue(game.snapshot().freight().companyRun());
            assertEquals(68,game.snapshot().logistics().cash());
            assertEquals("FACTORY",game.snapshot().logistics().truckPlace());
            assertEquals(100,game.snapshot().player().money());
            game.freightAction("drive","workshop");
        }
        try(var game=new GameSession(file)) {
            assertTrue(game.snapshot().logistics().yours());
            assertTrue(game.snapshot().freight().companyRun());
            assertEquals("WORKSHOP",game.snapshot().logistics().truckPlace());
            game.freightAction("unload",null);
            assertEquals(108,game.snapshot().logistics().cash());
            assertEquals(100,game.snapshot().player().money());
            game.freightAction("withdraw-25",null);
            assertEquals(83,game.snapshot().logistics().cash());
            assertEquals(125,game.snapshot().player().money());
            assertThrows(IllegalArgumentException.class,()->game.freightAction("unload",null));
        }
    }

    @Test
    void farmFreightReservesMarketCapacityAndDeliversOnlyAtEnd() throws Exception {
        Path file=directory.resolve("farm-freight.db");
        try(var game=new GameSession(file)) {
            game.move("quarry");game.waitMinutes(60);game.applyJob("freight-driver");game.move("farm");
        }
        try(var db=new WorldDatabase(file)) {
            var state=db.load();var town=db.loadTown(state);
            FarmLife.set(town,FarmLife.barn("vegetables"),8);
            FarmLife.set(town,FarmLife.barn("rice"),8);
            town.retailStock.put("vegetables",16);town.retailStock.put("rice",19);
            db.save(state,java.util.List.of(),java.util.List.of(),town,java.util.List.of(),java.util.List.of(),java.util.List.of());
        }
        try(var game=new GameSession(file)) {
            game.freightAction("load",null);
            assertEquals("Farm produce",game.snapshot().freight().cargoLabel());
            assertEquals(2,game.snapshot().freight().firstUnits());
            assertEquals(1,game.snapshot().freight().secondUnits());
            game.freightAction("drive","bakery");
            game.freightAction("drive","cafe");
        }
        try(var db=new WorldDatabase(file)) {
            var state=db.load();var town=db.loadTown(state);
            assertEquals(0,FarmLife.deliver(town),"reserved retail spaces cannot be filled by scheduled deliveries");
            assertEquals(16,town.retailStock.get("vegetables"));
        }
        try(var game=new GameSession(file)) {
            assertEquals("SOUTH_RESIDENCES",game.snapshot().freight().nextDestination());
            game.freightAction("drive","south_residences");
            game.freightAction("drive","market");
            game.freightAction("unload",null);
            assertEquals(82,game.snapshot().player().money());
            assertEquals(18,game.snapshot().farm().marketVegetables());
            assertEquals(20,game.snapshot().farm().marketRice());
        }
    }

    @Test
    void industrialFreightSuppliesWorkshopWithoutDuplicatingParts() throws Exception {
        Path file=directory.resolve("parts-freight.db");
        try(var game=new GameSession(file)) {
            game.move("quarry");game.waitMinutes(60);game.applyJob("freight-driver");game.move("factory");
        }
        int parts,steel;
        try(var db=new WorldDatabase(file)) {
            var state=db.load();var town=db.loadTown(state);
            town.industryStocks.put(IndustryLife.FACTORY_PARTS,2);
            town.industryStocks.put(IndustryLife.FACTORY_STEEL,1);
            parts=town.technicalMaterials.getOrDefault("spare-parts",0);
            steel=town.technicalMaterials.getOrDefault("steel",0);
            db.save(state,java.util.List.of(),java.util.List.of(),town,java.util.List.of(),java.util.List.of(),java.util.List.of());
        }
        try(var game=new GameSession(file)) {
            game.freightAction("load",null);
            assertEquals("Industrial materials",game.snapshot().freight().cargoLabel());
            game.freightAction("drive","workshop");game.freightAction("unload",null);
            assertEquals(82,game.snapshot().player().money());
        }
        try(var db=new WorldDatabase(file)) {
            var town=db.loadTown(db.load());
            assertEquals(parts+2,town.technicalMaterials.get("spare-parts"));
            assertEquals(steel+1,town.technicalMaterials.get("steel"));
        }
    }

    @Test
    void freightCargoAndReservedPayPersistUntilActualDelivery() throws Exception {
        Path file=directory.resolve("freight.db");
        try(var game=new GameSession(file)) {
            // Seed the fixture after the scheduled 09:00 mining step, before 11:00 dispatch.
            game.move("quarry");game.waitMinutes(90);game.applyJob("freight-driver");
            assertThrows(IllegalArgumentException.class,game::work);
        }
        try(var db=new WorldDatabase(file)) {
            var state=db.load();var town=db.loadTown(state);
            town.industryStocks.put(IndustryLife.QUARRY_IRON,2);
            town.industryStocks.put(IndustryLife.QUARRY_COPPER,1);
            db.save(state,java.util.List.of(),java.util.List.of(),town,
                    java.util.List.of(),java.util.List.of(),java.util.List.of());
        }
        try(var game=new GameSession(file)) {
            int fuel=game.snapshot().fuel().stock();
            game.freightAction("load",null);
            assertEquals(50,game.snapshot().player().money());
            assertEquals(0,game.snapshot().freight().quarryOre());
            assertEquals(2,game.snapshot().freight().iron());
            assertEquals(fuel-2,game.snapshot().fuel().stock());
            assertThrows(IllegalArgumentException.class,()->game.freightAction("drive","factory"));
            assertThrows(IllegalArgumentException.class,()->game.freightAction("unload",null));
            game.freightAction("drive","workshop");
            assertEquals("WORKSHOP",game.snapshot().freight().truckPlace());
        }
        try(var game=new GameSession(file)) {
            assertEquals("FACTORY",game.snapshot().freight().nextDestination());
            assertEquals(2,game.snapshot().freight().iron());
            game.freightAction("drive","factory");
            assertEquals(50,game.snapshot().player().money());
            game.freightAction("unload",null);
            assertEquals(82,game.snapshot().player().money());
            assertFalse(game.snapshot().freight().active());
            assertThrows(IllegalArgumentException.class,()->game.freightAction("unload",null));
        }
        try(var db=new WorldDatabase(file)) {
            var town=db.loadTown(db.load());
            assertEquals(2,IndustryLife.stock(town,IndustryLife.FACTORY_IRON));
            assertEquals(1,IndustryLife.stock(town,IndustryLife.FACTORY_COPPER_ORE));
            assertEquals(0,FreightLife.stock(town,FreightLife.WAGES));
        }
    }

    @Test
    void stationFuelConservesStockAndMoneyAndRestarts() throws Exception {
        Path file=directory.resolve("fuel.db");
        try(var game=new GameSession(file)) {
            game.move("fuel_station");game.waitMinutes(60);
            assertEquals(2,game.snapshot().shop().size());
            assertThrows(IllegalArgumentException.class,()->game.buy("milk"));
            game.buy("water");game.applyJob("fuel-attendant");game.work();
            assertEquals(74,game.snapshot().player().money());
            assertThrows(IllegalArgumentException.class,()->game.carAction("refuel"));
        }
        try(var db=new WorldDatabase(file)) {
            var state=db.load();var town=db.loadTown(state);
            town.player.carPlace=TownLife.Place.FUEL_STATION;
            town.player.carFuel=4;town.player.carCondition=80;
            db.save(state,java.util.List.of(),java.util.List.of(),town,
                    java.util.List.of(),java.util.List.of(),java.util.List.of());
        }
        try(var game=new GameSession(file)) {
            int stock=game.snapshot().fuel().stock();
            int cash=game.snapshot().fuel().payroll();
            game.carAction("refuel");
            assertEquals(7,game.snapshot().player().carFuel());
            assertEquals(stock-3,game.snapshot().fuel().stock());
            assertEquals(cash+6,game.snapshot().fuel().payroll());
            assertEquals(68,game.snapshot().player().money());
            game.drive("quarry");
            assertEquals(6,game.snapshot().player().carFuel());
            assertThrows(IllegalArgumentException.class,()->game.carAction("refuel"));
        }
        try(var db=new WorldDatabase(file)) {
            var state=db.load();var town=db.loadTown(state);
            assertEquals(45,FuelLife.stock(town));
            int funds=town.employerFunds.get(FuelLife.ACCOUNT);
            int supplier=town.employerFunds.get(FuelLife.SUPPLIER);
            FuelLife.tick(town,state.day+1,420);
            assertEquals(48,FuelLife.stock(town));
            assertEquals(funds-3,town.employerFunds.get(FuelLife.ACCOUNT));
            assertEquals(supplier+3,town.employerFunds.get(FuelLife.SUPPLIER));
            FuelLife.tick(town,state.day+1,420);
            assertEquals(funds-3,town.employerFunds.get(FuelLife.ACCOUNT));
        }
    }

    @Test
    void carTripsConserveMoneyFuelAndLocationAcrossRestart() throws Exception {
        Path file=directory.resolve("car.db");
        try(var game=new GameSession(file)) {
            game.move("workshop");game.waitMinutes(60);
            assertThrows(IllegalArgumentException.class,()->game.carAction("buy"));
        }
        try(var db=new WorldDatabase(file)) {
            var state=db.load();var town=db.loadTown(state);
            town.player.money=250;
            db.save(state,java.util.List.of(),java.util.List.of(),town,
                    java.util.List.of(),java.util.List.of(),java.util.List.of());
        }
        try(var game=new GameSession(file)) {
            game.carAction("buy");
            assertEquals(70,game.snapshot().player().money());
            game.carAction("refuel");game.carAction("repair");
            assertEquals(49,game.snapshot().player().money());
            assertEquals(9,game.snapshot().player().carFuel());
            assertEquals(100,game.snapshot().player().carCondition());
            assertThrows(IllegalArgumentException.class,()->game.drive("centro"));
            int minute=game.snapshot().minute();
            game.drive("library");
            assertEquals(minute+3,game.snapshot().minute());
            assertEquals("LIBRARY",game.snapshot().player().carPlace());
            assertEquals(8,game.snapshot().player().carFuel());
            assertEquals(97,game.snapshot().player().carCondition());
            game.move("park");
            assertThrows(IllegalArgumentException.class,()->game.drive("workshop"));
        }
        try(var game=new GameSession(file)) {
            assertEquals("LIBRARY",game.snapshot().player().carPlace());
            assertEquals(8,game.snapshot().player().carFuel());
            game.move("library");game.drive("workshop");
            assertEquals("WORKSHOP",game.snapshot().player().carPlace());
            assertEquals(7,game.snapshot().player().carFuel());
        }
    }

    @Test
    void parkedBicycleCannotTeleportAndSurvivesRestart() throws Exception {
        Path file=directory.resolve("parking.db");
        try(var game=new GameSession(file)) {
            game.move("workshop");game.waitMinutes(60);game.buyBicycle();
            game.transportMode("cycle");
            game.move("park");
            game.transportMode("park");
            assertEquals("PARK",game.snapshot().player().bicycleParked());
            assertFalse(game.snapshot().player().cycling());
            game.move("market");
            assertThrows(IllegalArgumentException.class,()->game.transportMode("cycle"));
            assertThrows(IllegalArgumentException.class,()->game.transportMode("retrieve"));
        }
        try(var game=new GameSession(file)) {
            assertEquals("PARK",game.snapshot().player().bicycleParked());
            game.move("park");
            game.transportMode("retrieve");
            assertNull(game.snapshot().player().bicycleParked());
            game.transportMode("cycle");
            assertTrue(game.snapshot().player().cycling());
        }
    }

    @Test
    void bicycleOwnershipAndTravelModePersistAndCostRealMoney() throws Exception {
        Path file=directory.resolve("bicycle.db");
        try(var game=new GameSession(file)) {
            assertThrows(IllegalArgumentException.class,()->game.transportMode("cycle"));
            assertThrows(IllegalArgumentException.class,game::buyBicycle);
            game.move("workshop");game.waitMinutes(60);
            int before=game.jobs().stream().filter(x->x.id().equals("technician")).findFirst().orElseThrow().remainingPayroll();
            game.buyBicycle();
            assertEquals(5,game.snapshot().player().money());
            assertTrue(game.snapshot().player().ownsBicycle());
            assertEquals(before+45,game.jobs().stream().filter(x->x.id().equals("technician")).findFirst().orElseThrow().remainingPayroll());
            assertThrows(IllegalArgumentException.class,game::buyBicycle);
            assertEquals(10,game.destinations().stream().filter(x->x.id().equals("PARK")).findFirst().orElseThrow().travelMinutes());
            game.transportMode("cycle");
            assertEquals(5,game.destinations().stream().filter(x->x.id().equals("PARK")).findFirst().orElseThrow().travelMinutes());
        }
        try(var game=new GameSession(file)) {
            assertTrue(game.snapshot().player().ownsBicycle());
            assertTrue(game.snapshot().player().cycling());
            int minute=game.snapshot().minute();
            game.move("park");
            assertEquals(minute+5,game.snapshot().minute());
            game.transportMode("walk");
            assertFalse(game.snapshot().player().cycling());
        }
    }

    @Test
    void farmHarvestAndWholesaleAreConservedAndPersistent() throws Exception {
        Path file=directory.resolve("farm.db");
        int barn;
        try(var game=new GameSession(file)) {
            game.move("farm");
            game.waitMinutes(60);
            game.applyJob("farm-worker");
            var before=game.snapshot().farm();
            game.work();
            var after=game.snapshot().farm();
            assertEquals(76,game.snapshot().player().money());
            assertEquals(before.payroll()-26,after.payroll());
            assertEquals(before.vegetablesInField()-4,after.vegetablesInField());
            assertEquals(before.vegetablesInBarn()+4,after.vegetablesInBarn());
            barn=after.vegetablesInBarn();
        }
        try(var game=new GameSession(file)) {
            assertEquals(barn,game.snapshot().farm().vegetablesInBarn());
        }
        try(var db=new WorldDatabase(file)) {
            var state=db.load();
            var town=db.loadTown(state);
            town.retailStock.put("vegetables",0);
            town.retailStock.put("rice",0);
            int money=town.employerFunds.get("shop-assistant")+town.employerFunds.get("farm-worker");
            int stock=FarmLife.stock(town,FarmLife.barn("vegetables"))+FarmLife.stock(town,FarmLife.barn("rice"));
            assertEquals(6,FarmLife.deliver(town));
            assertEquals(stock-6,FarmLife.stock(town,FarmLife.barn("vegetables"))+FarmLife.stock(town,FarmLife.barn("rice")));
            assertEquals(6,town.retailStock.get("vegetables")+town.retailStock.get("rice"));
            assertEquals(money,town.employerFunds.get("shop-assistant")+town.employerFunds.get("farm-worker"));
            RetailLife.tick(town,360);
            assertEquals(6,town.retailStock.get("vegetables")+town.retailStock.get("rice"));
        }
    }

    @Test
    void newIngredientsPersistAndRecipesConsumeExactStock() throws Exception {
        Path file=directory.resolve("recipes.db");
        try(var game=new GameSession(file)) {
            game.move("market");game.waitMinutes(60);
            game.buy("pasta");game.buy("vegetables");game.buy("water");
            assertEquals(44,game.snapshot().player().money());
            assertEquals(1,game.snapshot().player().bag().get("pasta"));
            assertThrows(IllegalArgumentException.class,()->game.consume("pasta"));
            game.move("north");game.rentRoom();
            for(String id:java.util.List.of("pasta","vegetables","water"))
                game.transferRoomItem(id,true);
            game.waitMinutes(120);
            assertThrows(IllegalArgumentException.class,()->game.cookRoomMeal("frittata"));
            assertEquals(1,game.snapshot().player().pantry().get("vegetables"));
        }
        try(var game=new GameSession(file)) {
            assertEquals(1,game.snapshot().player().pantry().get("pasta"));
            int hunger=game.snapshot().player().hunger();
            game.cookRoomMeal("pasta-verdure");
            assertTrue(game.snapshot().player().hunger()<hunger);
            for(String id:java.util.List.of("pasta","vegetables","water"))
                assertEquals(0,game.snapshot().player().pantry().get(id));
        }
        var p=new PlayerLife.Player();
        FoodLife.change(p,"milk",false,12);
        assertEquals(12,FoodLife.total(p,false));
        p.roomUntil=600;FoodLife.change(p,"eggs",true,1);
        HousingLife.expire(p,1,600);
        assertEquals(0,FoodLife.total(p,true));
        assertEquals(12,FoodLife.total(p,false));
    }

    @Test
    void gardenSoupAndOmeletteConsumeOnlyRealIngredients() throws Exception {
        Path file=directory.resolve("home-recipes.db");
        try(var game=new GameSession(file)) {
            game.move("market");
            game.waitMinutes(60);
            for(String id:java.util.List.of("bread","vegetables","water","eggs","milk"))
                game.buy(id);
            game.move("north");
            game.rentRoom();
            for(String id:java.util.List.of("bread","vegetables","water","eggs","milk"))
                game.transferRoomItem(id,true);
            game.waitMinutes(120);
            game.cookRoomMeal("garden-soup");
            for(String id:java.util.List.of("bread","vegetables","water"))
                assertEquals(0,game.snapshot().player().pantry().get(id));
            assertEquals(1,game.snapshot().player().pantry().get("eggs"));
            assertEquals(1,game.snapshot().player().pantry().get("milk"));
        }
        try(var game=new GameSession(file)) {
            game.waitMinutes(120);
            int hunger=game.snapshot().player().hunger();
            game.cookRoomMeal("omelette");
            assertTrue(game.snapshot().player().hunger()<hunger);
            assertEquals(0,game.snapshot().player().pantry().get("eggs"));
            assertEquals(0,game.snapshot().player().pantry().get("milk"));
            game.waitMinutes(120);
            assertThrows(IllegalArgumentException.class,()->game.cookRoomMeal("omelette"));
            assertEquals(0,game.snapshot().player().pantry().get("eggs"));
        }
    }

    @Test
    void rentedKitchenConservesFoodAndPersistsHygiene() throws Exception {
        Path file=directory.resolve("kitchen.db");
        try(var game=new GameSession(file)) {
            game.move("market");
            game.waitMinutes(60);
            game.buy("bread");game.buy("water");
            assertThrows(IllegalArgumentException.class,()->game.transferRoomItem("bread",true));
            game.move("north");game.rentRoom();
            game.transferRoomItem("bread",true);
            game.transferRoomItem("water",true);
            assertEquals(0,game.snapshot().player().bread());
            assertEquals(1,game.snapshot().player().storedBread());
            game.transferRoomItem("bread",false);
            assertEquals(1,game.snapshot().player().bread());
            game.transferRoomItem("bread",true);
            game.waitMinutes(360);
            assertTrue(game.snapshot().player().hygiene()<95);
            game.washInRoom();
            assertTrue(game.snapshot().player().hygiene()>=99);
        }
        try(var game=new GameSession(file)) {
            assertEquals(1,game.snapshot().player().storedBread());
            assertEquals(1,game.snapshot().player().storedWater());
            assertTrue(game.snapshot().player().hygiene()>=99);
            int hunger=game.snapshot().player().hunger();
            game.cookRoomMeal();
            assertTrue(game.snapshot().player().hunger()<hunger);
            assertEquals(0,game.snapshot().player().storedBread());
            assertEquals(0,game.snapshot().player().storedWater());
            assertThrows(IllegalArgumentException.class,game::cookRoomMeal);
        }
        var player=new PlayerLife.Player();
        player.roomUntil=600;player.storedBread=2;
        HousingLife.expire(player,1,600);
        assertEquals(0,player.storedBread);
    }

    @Test
    void porticiGrocerySharesTheExistingMarketStock() throws Exception {
        Path file=directory.resolve("portici.db");
        try(var game=new GameSession(file)) {
            game.move("portici");
            game.waitMinutes(60);
            assertEquals("PORTICI",game.snapshot().player().placeId());
            int bread=game.snapshot().shop().stream().filter(x->x.id().equals("bread")).findFirst().orElseThrow().stock();
            game.buy("bread");
            assertEquals(47,game.snapshot().player().money());
            game.move("market");
            assertEquals(bread-1,game.snapshot().shop().stream().filter(x->x.id().equals("bread")).findFirst().orElseThrow().stock());
        }
        try(var game=new GameSession(file)) {
            assertEquals(1,game.snapshot().player().bread());
        }
    }

    @Test
    void historicCentreIsPublicAndParkRemainsFirstSpawn() throws Exception {
        Path file=directory.resolve("centro.db");
        try(var game=new GameSession(file)) {
            assertEquals("PARK",game.snapshot().player().placeId());
            assertTrue(game.destinations().stream().anyMatch(x->x.id().equals("CENTRO")));
            assertEquals("CENTRO",game.move("centro").world().player().placeId());
        }
        try(var game=new GameSession(file)) {
            assertEquals("CENTRO",game.snapshot().player().placeId());
            assertEquals("MARKET",game.move("market").world().player().placeId());
        }
    }

    @Test
    void prepaidRoomIsLocalAndSurvivesRestart() throws Exception {
        Path file=directory.resolve("room.db");
        long expiry;
        try(var game=new GameSession(file)) {
            assertFalse(game.snapshot().player().roomActive());
            assertThrows(IllegalArgumentException.class,game::rentRoom);
            game.move("north");
            assertThrows(IllegalArgumentException.class,()->game.sleepInRoom(1));
            game.rentRoom();
            assertEquals(15,game.snapshot().player().money());
            assertTrue(game.snapshot().player().roomActive());
            expiry=game.snapshot().player().roomUntil();
            assertThrows(IllegalArgumentException.class,game::rentRoom);
            int health=game.snapshot().player().health();
            game.sleepInRoom(2);
            assertTrue(game.snapshot().player().health()>=health);
        }
        try(var game=new GameSession(file)) {
            assertEquals(expiry,game.snapshot().player().roomUntil());
            game.move("park");
            assertThrows(IllegalArgumentException.class,()->game.sleepInRoom(1));
        }
        try(var db=new WorldDatabase(file)) {
            var state=db.load();
            var town=db.loadTown(state);
            assertEquals(35,town.employerFunds.get("guesthouse"));
            town.player.place=HousingLife.DISTRICT;
            town.player.roomUntil=HousingLife.now(state.day,state.minute)+30;
            assertThrows(IllegalArgumentException.class,()->HousingLife.sleep(town.player,1,state.day,state.minute));
            assertFalse(HousingLife.active(town.player,state.day+1,state.minute));
        }
    }

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
            assertEquals(10,game.jobs().size());
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
            assertEquals(10,game.jobs().size());

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
            assertEquals(10,restarted.jobs().size(),"job board remains stable");
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
